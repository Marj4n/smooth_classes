package org.marj4n.smooth_classes.origin.demon;

import java.util.*;
import net.minecraft.block.*;
import net.minecraft.entity.*;
import net.minecraft.entity.attribute.*;
import net.minecraft.entity.effect.*;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.*;
import org.marj4n.smooth_classes.origin.*;
import org.marj4n.smooth_classes.origin.OriginAbilityDispatcher.Result;

/** Anchored spherical domain. Global scan/write budgets, loaded chunks only, no rollback. */
public final class DemonDomain {
    private static final UUID ARMOR=UUID.fromString("890e711c-143a-47c3-9933-e6355afbe301"), ATTACK=UUID.fromString("890e711c-143a-47c3-9933-e6355afbe302");
    private static final Map<UUID,Domain> ACTIVE=new LinkedHashMap<>();
    private static int rr;
    private static final class Domain {
        final ServerPlayerEntity owner;final ServerWorld world;final BlockPos center;int cursor;
        final Map<UUID,Integer> exposure=new HashMap<>();
        Domain(ServerPlayerEntity p,BlockPos center,int cursor){owner=p;world=p.getServerWorld();this.center=center;this.cursor=cursor;}
    }
    private DemonDomain(){}
    public static Result activate(ServerPlayerEntity p){
        var s=OriginRuntime.state(p);
        if(s.progress("demon.domain_active")>0)return Result.fail("Pandemonium is already active.");
        if(s.progress("demon.domain_cd")>0)return Result.fail("Pandemonium is recovering.");
        s.progress("demon.domain_active",DemonRules.DOMAIN);
        for(String flag:s.flagsSnapshot())if(flag.startsWith("demon.domain_dim."))s.unflag(flag);
        // BlockPos longs can be negative; store signed position as component integers instead.
        s.progress("demon.domain_x_encoded",p.getBlockX()+30_000_000);s.progress("demon.domain_y_encoded",p.getBlockY()+2048);s.progress("demon.domain_z_encoded",p.getBlockZ()+30_000_000);
        s.progress("demon.domain_cursor",0);s.flag("demon.domain_dim."+p.getWorld().getRegistryKey().getValue());
        ACTIVE.put(p.getUuid(),new Domain(p,p.getBlockPos(),0));
        return Result.ok("Pandemonium: permanent evaporation within 64 blocks for 45 seconds.");
    }
    public static void tickPlayer(ServerPlayerEntity p){
        var s=OriginRuntime.state(p);
        if(s.progress("demon.domain_active")>0){
            if(!s.hasFlag("demon.domain_dim."+p.getWorld().getRegistryKey().getValue())){end(p);return;}
            Domain d=ACTIVE.computeIfAbsent(p.getUuid(),ignored->new Domain(p,new BlockPos(s.progress("demon.domain_x_encoded")-30_000_000,s.progress("demon.domain_y_encoded")-2048,s.progress("demon.domain_z_encoded")-30_000_000),s.progress("demon.domain_cursor")));
            s.progress("demon.domain_active",s.progress("demon.domain_active")-1);
            boolean inside=p.getPos().squaredDistanceTo(Vec3d.ofCenter(d.center))<=64*64;
            if(inside){int tier=DemonProgression.tier(p);add(p,EntityAttributes.GENERIC_ARMOR,ARMOR,DemonConfig.INSTANCE.domainArmor[tier]);add(p,EntityAttributes.GENERIC_ATTACK_DAMAGE,ATTACK,DemonConfig.INSTANCE.domainAttack[tier]);}
            else removeBuffs(p);
            if(p.age%20==0)expose(d);
            if(s.progress("demon.domain_active")==0)end(p);
        }else{
            removeBuffs(p);if(s.progress("demon.domain_cd")>0)s.progress("demon.domain_cd",s.progress("demon.domain_cd")-1);
        }
    }
    private static void add(ServerPlayerEntity p,EntityAttribute a,UUID id,double amount){var v=p.getAttributeInstance(a);if(v==null)return;var old=v.getModifier(id);if(old==null||old.getValue()!=amount){v.removeModifier(id);v.addTemporaryModifier(new EntityAttributeModifier(id,"Pandemonium",amount,EntityAttributeModifier.Operation.ADDITION));}}
    private static void removeBuffs(ServerPlayerEntity p){for(var pair:Map.of(EntityAttributes.GENERIC_ARMOR,ARMOR,EntityAttributes.GENERIC_ATTACK_DAMAGE,ATTACK).entrySet()){var a=p.getAttributeInstance(pair.getKey());if(a!=null)a.removeModifier(pair.getValue());}}
    public static void end(ServerPlayerEntity p){
        var s=OriginRuntime.state(p);ACTIVE.remove(p.getUuid());s.progress("demon.domain_active",0);s.progress("demon.domain_cd",DemonRules.DOMAIN_RECOVERY);
        for(String flag:s.flagsSnapshot())if(flag.startsWith("demon.domain_dim."))s.unflag(flag);removeBuffs(p);
    }
    public static void disconnect(ServerPlayerEntity p){ACTIVE.remove(p.getUuid());removeBuffs(p);}
    public static void clear(){ACTIVE.clear();rr=0;}
    private static void expose(Domain d){
        int threshold=DemonConfig.INSTANCE.domainExposureSeconds[DemonProgression.tier(d.owner)];Set<UUID> present=new HashSet<>();
        for(LivingEntity e:d.world.getEntitiesByClass(LivingEntity.class,new Box(d.center).expand(64),LivingEntity::isAlive)){
            if(e==d.owner||e.squaredDistanceTo(Vec3d.ofCenter(d.center))>4096||DemonMobIdentity.nativeMob(e)||e.isFireImmune())continue;
            if(e instanceof ServerPlayerEntity q&&(!d.world.getServer().isPvpEnabled()||d.owner.isTeammate(q)))continue;
            if(!DemonProtection.allowCombat(d.owner,d.world,e.getBlockPos()))continue;
            present.add(e.getUuid());int seconds=d.exposure.merge(e.getUuid(),1,Integer::sum);
            if(seconds>=threshold&&!e.hasStatusEffect(StatusEffects.FIRE_RESISTANCE))e.setOnFireFor(3);
        }
        d.exposure.keySet().retainAll(present);
        // Fixed particle budget per domain, independent of the number of affected blocks.
        d.world.spawnParticles(ParticleTypes.ASH,d.center.getX()+.5,d.center.getY()+2,d.center.getZ()+.5,18,7,3,7,.01);
        d.world.spawnParticles(ParticleTypes.FLAME,d.owner.getX(),d.owner.getBodyY(.5),d.owner.getZ(),4,.3,.5,.3,.005);
    }
    public static void tickWorld(MinecraftServer server){
        if(ACTIVE.isEmpty())return;
        List<Domain> domains=new ArrayList<>(ACTIVE.values());int scan=DemonConfig.INSTANCE.domainScanBudget, writes=DemonConfig.INSTANCE.domainBlockBudget;
        int turns=0,first=Math.floorMod(rr++,domains.size());
        while(scan>0&&turns<domains.size()){
            Domain d=domains.get((first+turns)%domains.size());turns++;
            int slice=Math.max(1,scan/(domains.size()-turns+1));int end=Math.min(DemonDomainCursor.size(),d.cursor+slice);
            while(d.cursor<end){int i=d.cursor++;scan--;int x=DemonDomainCursor.x(i),y=DemonDomainCursor.y(i),z=DemonDomainCursor.z(i);
                BlockPos pos=d.center.add(x,y,z);
                if(pos.getY()<d.world.getBottomY()||pos.getY()>=d.world.getTopY()||!d.world.isChunkLoaded(pos))continue;
                if(d.world.getFluidState(pos).isIn(FluidTags.WATER)&&DemonProtection.allow(d.owner,d.world,pos)){
                    if(writes<=0){d.cursor--;break;} // Resume here next tick; never skip unprocessed water.
                    BlockState block=d.world.getBlockState(pos);
                    d.world.setBlockState(pos,dry(block),Block.NOTIFY_LISTENERS);writes--;
                    d.world.spawnParticles(ParticleTypes.SMOKE,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,1,0,0,0,0);
                }
                if(writes>0&&DemonConfig.INSTANCE.domainFire&&server.getTicks()%DemonConfig.INSTANCE.domainFireInterval==0&&d.world.getRandom().nextInt(DemonConfig.INSTANCE.domainFireChanceDivisors[DemonProgression.tier(d.owner)])==0&&d.world.isAir(pos)&&Blocks.FIRE.getDefaultState().canPlaceAt(d.world,pos)&&DemonProtection.allow(d.owner,d.world,pos)){
                    d.world.setBlockState(pos,AbstractFireBlock.getState(d.world,pos),Block.NOTIFY_ALL);writes--;
                }
            }
            OriginRuntime.state(d.owner).progress("demon.domain_cursor",d.cursor);
        }
    }
    public static BlockState dry(BlockState s){return s.contains(Properties.WATERLOGGED)?s.with(Properties.WATERLOGGED,false):Blocks.AIR.getDefaultState();}
    public static boolean vaporizeNewWater(ServerWorld w,BlockPos pos,BlockState proposed){
        if(!proposed.getFluidState().isIn(FluidTags.WATER))return false;
        for(Domain d:ACTIVE.values())if(d.world==w&&DemonRules.inSphere(pos.getX()-d.center.getX(),pos.getY()-d.center.getY(),pos.getZ()-d.center.getZ(),64)&&DemonProtection.allow(d.owner,w,pos))return true;
        return false;
    }
}
