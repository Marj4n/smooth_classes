package org.marj4n.smooth_classes.origin.demon;

import java.util.*;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.*;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/** Server-owned, nonpersistent passive. Army-owner credit is intentionally excluded. */
public final class DemonKillStreak {
    public static final Identifier SYNC=new Identifier("smooth_classes:demon_infernal_streak");
    private static final UUID HEALTH=UUID.fromString("65d777d3-0458-40ce-b04d-bcc88b061662"),ARMOR=UUID.fromString("00731a89-b91c-40b1-89cf-606d0df6f174");
    private static final Map<UUID,DemonStreakCounter> ACTIVE=new HashMap<>();
    private DemonKillStreak(){}
    public static boolean eligible(ServerPlayerEntity p){return p.isAlive()&&!p.isSpectator()&&DemonProgression.demon(p)&&DemonProgression.has(p,"lord");}
    public static void confirmedDeath(LivingEntity victim,DamageSource source){
        if(victim.getWorld().isClient||victim.isAlive()||!(source.getAttacker() instanceof ServerPlayerEntity p)||!eligible(p)||victim==p)return;
        var data=DemonEntityData.of(victim);
        if(data.streakDeathCredited||data.commander!=null||victim.isTeammate(p)||victim instanceof TameableEntity tame&&tame.isTamed())return;
        boolean enemy=victim instanceof HostileEntity||victim instanceof MobEntity mob&&mob.getTarget()==p||victim instanceof ServerPlayerEntity other&&p.getServer().isPvpEnabled()&&!other.isCreative()&&!other.isSpectator();
        if(!enemy)return;
        data.streakDeathCredited=true;
        DemonStreakCounter counter=ACTIVE.computeIfAbsent(p.getUuid(),id->new DemonStreakCounter());
        counter.kill(p.getServer().getOverworld().getTime());
        update(p,counter);p.setHealth(Math.min(p.getMaxHealth(),p.getHealth()+1));send(p,counter);
    }
    private static void modifier(ServerPlayerEntity p,EntityAttribute attr,UUID id,double amount){
        EntityAttributeInstance instance=p.getAttributeInstance(attr);if(instance==null)return;
        instance.removeModifier(id);
        if(amount>0)instance.addTemporaryModifier(new EntityAttributeModifier(id,"Infernal Kill Streak",amount,EntityAttributeModifier.Operation.ADDITION));
    }
    private static void update(ServerPlayerEntity p,DemonStreakCounter c){modifier(p,EntityAttributes.GENERIC_MAX_HEALTH,HEALTH,c.healthBonus());modifier(p,EntityAttributes.GENERIC_ARMOR,ARMOR,c.armorBonus());}
    public static void reset(ServerPlayerEntity p){
        boolean had=ACTIVE.remove(p.getUuid())!=null;
        modifier(p,EntityAttributes.GENERIC_MAX_HEALTH,HEALTH,0);modifier(p,EntityAttributes.GENERIC_ARMOR,ARMOR,0);
        if(p.isAlive()&&p.getHealth()>p.getMaxHealth())p.setHealth(p.getMaxHealth());
        if(had)send(p,new DemonStreakCounter());
    }
    public static void tick(MinecraftServer server){
        long now=server.getOverworld().getTime();
        for(var entry:new ArrayList<>(ACTIVE.entrySet())){
            ServerPlayerEntity p=server.getPlayerManager().getPlayer(entry.getKey());
            if(p==null){ACTIVE.remove(entry.getKey());continue;}
            if(!eligible(p)||entry.getValue().expire(now)){reset(p);continue;}
            if(now%20==0)send(p,entry.getValue());
        }
    }
    private static void send(ServerPlayerEntity p,DemonStreakCounter c){for(var receiver:p.getServer().getPlayerManager().getPlayerList())sendTo(receiver,p,c);}
    private static void sendTo(ServerPlayerEntity receiver,ServerPlayerEntity p,DemonStreakCounter c){
        if(!ServerPlayNetworking.canSend(receiver,SYNC))return;
        var buf=PacketByteBufs.create();buf.writeUuid(p.getUuid());buf.writeVarLong(c.stacks());buf.writeVarInt(c.remaining(p.getServer().getOverworld().getTime()));ServerPlayNetworking.send(receiver,SYNC,buf);
    }
    public static void join(ServerPlayerEntity p){reset(p);for(var other:p.getServer().getPlayerManager().getPlayerList())sendTo(p,other,ACTIVE.getOrDefault(other.getUuid(),new DemonStreakCounter()));}
    public static void stop(MinecraftServer server){for(var p:server.getPlayerManager().getPlayerList())reset(p);ACTIVE.clear();}
    public static long stacks(ServerPlayerEntity p){var c=ACTIVE.get(p.getUuid());return c==null?0:c.stacks();}
}
