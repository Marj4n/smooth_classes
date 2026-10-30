package org.marj4n.smooth_classes.content.avenger.dance;

import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.block.entity.JukeboxBlockEntity;
import net.minecraft.entity.*;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.item.MusicDiscItem;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.*;
import org.marj4n.smooth_classes.content.avenger.runtime.*;
import org.marj4n.smooth_classes.content.avenger.AvengerClass;
import org.marj4n.smooth_classes.runtime.AbilityRuntime;
import org.marj4n.smooth_classes.mixin.AvengerJukeboxAccessor;

/** Cosmetic authority only: never writes AI, commands, targets, ownership or position. */
public final class AvengerDanceRuntime {
    public static final Identifier SYNC=Identifier.of("smooth_classes","avenger_dance_v2");
    public record Song(BlockPos pos,long start,long end,int disc) {}
    public record State(int entityId,UUID uuid,String profile,Song song) {}
    private static final String OWNER="smooth_classes_avenger_owner_";
    private static final Map<ServerWorld,WorldState> WORLDS=new IdentityHashMap<>();
    public static long tickNanos, maxTickNanos, packets, packetBytes;
    private static final class WorldState {
        final Map<Long,Song> songs=new HashMap<>();
        final Map<Long,Set<Long>> buckets=new HashMap<>();
        final Map<UUID,MobEntity> candidates=new HashMap<>();
        final Map<UUID,State> dancing=new HashMap<>();
        final Map<UUID,Long> attackUntil=new HashMap<>();
    }
    private static WorldState data(ServerWorld w){return WORLDS.computeIfAbsent(w,k->new WorldState());}
    public static void register(){
        DanceChecks.register();
        ServerEntityEvents.ENTITY_LOAD.register((e,w)->{if(e instanceof MobEntity m&&AvengerReworkRuntime.isAvengerSummon(e))data(w).candidates.put(e.getUuid(),m);});
        ServerEntityEvents.ENTITY_UNLOAD.register((e,w)->{var d=WORLDS.get(w);if(d==null)return;d.candidates.remove(e.getUuid());var old=d.dancing.remove(e.getUuid());if(old!=null)send(w,old,false);if(e.getRemovalReason()!=null&&e.getRemovalReason().shouldDestroy())DanceBindings.get(w.getServer()).forget(e.getUuid());});
        ServerPlayConnectionEvents.DISCONNECT.register((handler,server)->DanceBindings.get(server).recordOwner(handler.player.getUuid(),AbilityRuntime.isClass(handler.player,AvengerClass.ID)));
        EntityTrackingEvents.START_TRACKING.register((e,p)->{var d=WORLDS.get(p.getServerWorld());if(d!=null){var state=d.dancing.get(e.getUuid());if(state!=null)send(p,state,true);}});
        ServerTickEvents.END_WORLD_TICK.register(AvengerDanceRuntime::tick);
        ServerWorldEvents.UNLOAD.register((server,world)->WORLDS.remove(world));
        ServerLifecycleEvents.SERVER_STOPPED.register(s->{WORLDS.clear();tickNanos=maxTickNanos=packets=packetBytes=0;});
    }
    /** Called only after actual Death List spawn + successful soul/charge/material costs. */
    public static void issue(ServerPlayerEntity owner,LivingEntity entity){
        String type=org.marj4n.smooth_classes.platform.PlatformRegistries.ENTITY_TYPE.getId(entity.getType()).toString();
        if(!(entity instanceof MobEntity mob)||!type.startsWith("minecraft:")||!AvengerSummonRecipes.supported(type)||!AvengerReworkRuntime.isOwnedSummon(owner,entity))return;
        DanceBindings.get(owner.getServer()).issue(entity.getUuid(),owner.getUuid(),type);
        data((ServerWorld)entity.getWorld()).candidates.put(entity.getUuid(),mob);
    }
    public static boolean authorized(MobEntity mob){
        if(!(mob.getWorld() instanceof ServerWorld w)||!AvengerReworkRuntime.isAvengerSummon(mob))return false;
        var binding=DanceBindings.get(w.getServer()).get(mob.getUuid());if(binding==null)return false;
        String type=org.marj4n.smooth_classes.platform.PlatformRegistries.ENTITY_TYPE.getId(mob.getType()).toString();
        if(!type.equals(binding.type())||DanceProfiles.profile(type)==null||!AvengerSummonRecipes.supported(type)||!AvengerReworkRuntime.isOwnedSummon(binding.owner(),mob))return false;
        int count=0;for(String t:mob.getCommandTags())if(t.startsWith(OWNER)){if(!t.equals(OWNER+binding.owner()))return false;count++;}if(count!=1)return false;
        var ledger=AvengerDeathListState.get(w.getServer());if(!ledger.hasAwakened(binding.owner())||ledger.kills(binding.owner(),type)<1)return false;
        if(mob instanceof TameableEntity tame&&!binding.owner().equals(tame.getOwnerUuid()))return false;
        if(mob instanceof net.minecraft.entity.passive.AbstractHorseEntity horse&&!binding.owner().equals(horse.getOwnerUuid()))return false;
        var owner=w.getServer().getPlayerManager().getPlayer(binding.owner());
        var registry=DanceBindings.get(w.getServer());if(owner==null)return registry.ownerEligible(binding.owner());
        boolean eligible=AbilityRuntime.isClass(owner,AvengerClass.ID);registry.recordOwner(binding.owner(),eligible);return eligible;
    }
    public static void observe(JukeboxBlockEntity be){
        if(!(be.getWorld() instanceof ServerWorld w))return;
        var d=data(w);long key=be.getPos().asLong();
        if(!be.isPlayingRecord()||be.isRemoved()||!(be.getStack(0).getItem() instanceof MusicDiscItem disc)){removeSong(d,key);return;}
        var accessor=(AvengerJukeboxAccessor)be;long elapsed=Math.max(0,accessor.smoothClasses$danceTick()-accessor.smoothClasses$danceStart());
        if(elapsed>=disc.getSongLengthInTicks()){removeSong(d,key);return;}
        long start=w.getTime()-elapsed;int id=net.minecraft.item.Item.getRawId(disc);Song old=d.songs.get(key);
        // Preserve established origin despite tick ordering; distinguish actual replay using record identity/start.
        if(old!=null&&old.disc()==id&&Math.abs(old.start()-start)<=1)return;
        Song song=new Song(be.getPos().toImmutable(),start,start+disc.getSongLengthInTicks(),id);d.songs.put(key,song);
        d.buckets.computeIfAbsent(ChunkPos.toLong(be.getPos().getX()>>4,be.getPos().getZ()>>4),k->new HashSet<>()).add(key);
    }
    private static void removeSong(WorldState d,long key){var old=d.songs.remove(key);if(old==null)return;long bucket=ChunkPos.toLong(old.pos().getX()>>4,old.pos().getZ()>>4);var set=d.buckets.get(bucket);if(set!=null){set.remove(key);if(set.isEmpty())d.buckets.remove(bucket);}}
    public static boolean combat(MobEntity m){
        if(m.getWorld() instanceof ServerWorld sw){var state=WORLDS.get(sw);if(state!=null&&state.attackUntil.getOrDefault(m.getUuid(),0L)>sw.getTime())return true;}
        if(m instanceof net.minecraft.entity.mob.WardenEntity w && (w.getAnger()>0))return true;
        if(DancePriority.special(m))return true;
        if(!m.isAlive()||m.isRemoved()||m.hurtTime>0||m.isOnFire()||m.fallDistance>2||AvengerSoulAnimationRuntime.busy(m))return true;
        if(!m.getNavigation().isIdle()||m.isAttacking()||m.getTarget()!=null&&m.getTarget().isAlive())return true;
        if(m.getBrain().hasMemoryModule(net.minecraft.entity.ai.brain.MemoryModuleType.ATTACK_TARGET)&&m.getBrain().getOptionalRegisteredMemory(net.minecraft.entity.ai.brain.MemoryModuleType.ATTACK_TARGET).filter(LivingEntity::isAlive).isPresent())return true;
        if(m.getAttacker()!=null&&m.getAttacker().isAlive()&&m.age-m.getLastAttackedTime()<60)return true;
        UUID owner=AvengerReworkRuntime.summonOwnerUuid(m);if(owner==null)return true;var p=((ServerWorld)m.getWorld()).getServer().getPlayerManager().getPlayer(owner);if(p==null)return false;
        for(String tag:p.getCommandTags())if(tag.startsWith("smooth_classes_avenger_attack_")){try{var e=p.getServerWorld().getEntity(UUID.fromString(tag.substring("smooth_classes_avenger_attack_".length())));if(e instanceof LivingEntity l&&l.isAlive())return true;}catch(IllegalArgumentException ignored){}}
        return false;
    }
    private static Song nearest(WorldState d,MobEntity mob){
        int cx=mob.getBlockX()>>4,cz=mob.getBlockZ()>>4;Song best=null;double distance=256;
        for(int x=cx-1;x<=cx+1;x++)for(int z=cz-1;z<=cz+1;z++){var keys=d.buckets.get(ChunkPos.toLong(x,z));if(keys==null)continue;for(long key:keys){var s=d.songs.get(key);double sq=mob.getPos().squaredDistanceTo(Vec3d.ofCenter(s.pos()));if(sq<=256&&(best==null||sq<distance||sq==distance&&s.pos().asLong()<best.pos().asLong())){best=s;distance=sq;}}}
        return best;
    }
    private static void tick(ServerWorld w){
        var d=WORLDS.get(w);if(d==null)return;long begin=System.nanoTime();
        if(w.getTime()%20==0)d.attackUntil.entrySet().removeIf(e->e.getValue()<=w.getTime()||!d.candidates.containsKey(e.getKey()));
        if(w.getTime()%10==0)for(var song:List.copyOf(d.songs.values())){
            var pos=song.pos();var chunk=w.getChunkManager().getWorldChunk(pos.getX()>>4,pos.getZ()>>4);
            if(w.getTime()>=song.end()||chunk==null||!(chunk.getBlockEntity(pos) instanceof JukeboxBlockEntity be)||!be.isPlayingRecord())removeSong(d,pos.asLong());
        }
        for(var it=d.candidates.entrySet().iterator();it.hasNext();){var entry=it.next();var m=entry.getValue();var old=d.dancing.get(entry.getKey());
            if(m.isRemoved()||m.getWorld()!=w){if(old!=null){d.dancing.remove(entry.getKey());send(w,old,false);}it.remove();continue;}
            boolean urgent=combat(m);Song song=old==null?null:old.song();
            if(urgent||song!=null&&(!d.songs.containsKey(song.pos().asLong())||w.getTime()>=song.end()||m.getPos().squaredDistanceTo(Vec3d.ofCenter(song.pos()))>256))song=null;
            if(w.getTime()%5==Math.floorMod(m.getId(),5))song=!urgent&&authorized(m)?nearest(d,m):null;
            State next=song==null?null:old!=null&&old.song().equals(song)?old:new State(m.getId(),m.getUuid(),DanceProfiles.profile(org.marj4n.smooth_classes.platform.PlatformRegistries.ENTITY_TYPE.getId(m.getType()).toString()),song);
            if(!Objects.equals(old,next)){if(next==null)d.dancing.remove(entry.getKey());else d.dancing.put(entry.getKey(),next);send(w,next==null?old:next,next!=null);}
        }
        tickNanos=System.nanoTime()-begin;maxTickNanos=Math.max(maxTickNanos,tickNanos);
    }
    private static void send(ServerWorld w,State s,boolean active){if(s==null)return;var e=w.getEntity(s.uuid());if(e!=null)for(var p:PlayerLookup.tracking(e))send(p,s,active);}
    private static void send(ServerPlayerEntity p,State s,boolean active){if(!ServerPlayNetworking.canSend(p,SYNC))return;var b=PacketByteBufs.create();b.writeString(p.getWorld().getRegistryKey().getValue().toString());b.writeVarInt(s.entityId());b.writeUuid(s.uuid());b.writeBoolean(active);if(active){b.writeString(s.profile());b.writeBlockPos(s.song().pos());b.writeLong(s.song().start());b.writeLong(s.song().end());b.writeVarInt(s.song().disc());}else{var e=p.getServerWorld().getEntity(s.uuid());b.writeBoolean(!(e instanceof MobEntity m)||combat(m)||!authorized(m));}packets++;packetBytes+=b.readableBytes();ServerPlayNetworking.send(p,SYNC,b);}
    public static int active(ServerWorld w){var d=WORLDS.get(w);return d==null?0:d.dancing.size();}
    public static void interrupt(MobEntity entity,int ticks){if(!(entity.getWorld() instanceof ServerWorld w))return;var d=WORLDS.get(w);if(d==null||!d.candidates.containsKey(entity.getUuid()))return;d.attackUntil.put(entity.getUuid(),w.getTime()+ticks);var old=d.dancing.remove(entity.getUuid());if(old!=null)send(w,old,false);}
    public static Collection<State> states(ServerWorld w){var d=WORLDS.get(w);return d==null?List.of():List.copyOf(d.dancing.values());}
    private AvengerDanceRuntime(){}
}
