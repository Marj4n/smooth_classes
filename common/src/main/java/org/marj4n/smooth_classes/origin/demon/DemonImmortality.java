package org.marj4n.smooth_classes.origin.demon;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.world.World;
import org.marj4n.smooth_classes.origin.*;

public final class DemonImmortality {
    private DemonImmortality(){}
    /** Invoked by vanilla's fatal-damage totem checkpoint, after armor/resistance/absorption. */
    public static boolean rescue(ServerPlayerEntity p,DamageSource source){
        if(!DemonProgression.demon(p))return false;
        OriginState state=OriginRuntime.state(p);
        if(state.progress("demon.return_pending")>0)return true;
        if(state.hasFlag("demon.sovereign")&&state.progress("demon.tribute_cd")==0){
            MobEntity nearest=null;double distance=24*24;
            for(MobEntity m:p.getWorld().getEntitiesByClass(MobEntity.class,p.getBoundingBox().expand(24),m->DemonArmy.available(m,p)&&!m.hasPassengers())){
                double d=m.squaredDistanceTo(p);if(d<=distance){distance=d;nearest=m;}
            }
            if(nearest!=null){
                // Vanilla /kill semantics guarantee a real death; invulnerable/totem entities
                // are not accepted as a sacrifice unless they actually die.
                nearest.kill();
                if(!nearest.isAlive()){
                    state.progress("demon.tribute_cd",DemonRules.TRIBUTE);state.progress("demon.protection",DemonConfig.INSTANCE.teleportProtectionTicks);
                    p.setHealth(6);p.getServerWorld().spawnParticles(ParticleTypes.SOUL,nearest.getX(),nearest.getBodyY(.5),nearest.getZ(),30,.4,.8,.4,.05);
                    p.sendMessage(net.minecraft.text.Text.literal("Last Tribute: a Nether soul died in your place."),true);return true;
                }
            }
        }
        if(!state.hasFlag("demon.lord")||p.getWorld().getRegistryKey()==World.NETHER||state.progress("demon.throne_cd")>0||!DemonThrone.returnReady(p))return false;
        p.stopRiding();p.stopFallFlying();p.setHealth(1);state.progress("demon.return_pending",20);state.progress("demon.protection",DemonConfig.INSTANCE.teleportProtectionTicks+20);
        // Reserve the cooldown immediately, so repeated damage/logout cannot retrigger it.
        state.progress("demon.throne_cd",DemonRules.THRONE);return true;
    }
    public static void tick(ServerPlayerEntity p){
        var s=OriginRuntime.state(p);int pending=s.progress("demon.return_pending");if(pending<=0)return;
        p.setVelocity(0,.035,0);p.velocityModified=true;p.fallDistance=0;
        if(p.age%3==0)p.getServerWorld().spawnParticles(ParticleTypes.SOUL_FIRE_FLAME,p.getX(),p.getBodyY(.4),p.getZ(),9,.35,.6,.35,.02);
        s.progress("demon.return_pending",pending-1);
        if(pending==1){
            boolean returned=DemonThrone.returnReady(p)&&DemonThrone.returnTo(p);
            if(!returned){s.progress("demon.protection",0);p.sendMessage(net.minecraft.text.Text.literal("Your throne was destroyed before your soul could return."),false);p.kill();return;}
            p.setHealth(6);s.progress("demon.throne_cd",DemonRules.THRONE);s.progress("demon.protection",DemonConfig.INSTANCE.teleportProtectionTicks);
            p.setVelocity(0,0,0);p.velocityModified=true;
        }
    }
}
