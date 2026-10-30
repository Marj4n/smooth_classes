package org.marj4n.smooth_classes.origin.demon;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.entity.effect.*;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import org.marj4n.smooth_classes.origin.*;

public final class DemonBiology {
    private DemonBiology(){}
    public static boolean protectFire(ServerPlayerEntity p,DamageSource source){
        if(!DemonProgression.demon(p)||!source.isIn(DamageTypeTags.IS_FIRE))return false;
        trigger(p);return DemonProgression.has(p,"netherforged")||OriginRuntime.state(p).progress("demon.cinder_active")>0;
    }
    public static void trigger(ServerPlayerEntity p){
        OriginState s=OriginRuntime.state(p);
        if(s.hasFlag("demon.netherforged")||s.progress("demon.cinder_active")>0||s.progress("demon.cinder_recovery")>0)return;
        boolean first=s.flag("demon.cinder_unlocked");s.progress("demon.cinder_active",DemonRules.CINDER);
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE,DemonRules.CINDER,0,false,true,true));
        DemonProgression.sync(p,s);
        if(first){org.marj4n.smooth_classes.network.SmoothClassesNetworking.sendOriginState(p);DemonNetworking.openPath(p);}
    }
    public static void tick(ServerPlayerEntity p,OriginState s){
        var cycle=DemonRules.advance(s.progress("demon.cinder_active"),s.progress("demon.cinder_recovery"));
        s.progress("demon.cinder_active",cycle.active());s.progress("demon.cinder_recovery",cycle.cooldown());
        if(p.isOnFire()||p.isInLava())trigger(p);
        if(s.hasFlag("demon.netherforged")){s.progress("demon.cinder_active",0);s.progress("demon.cinder_recovery",0);}
        if(s.hasFlag("demon.netherforged")||s.progress("demon.cinder_active")>0){
            var effect=p.getStatusEffect(StatusEffects.FIRE_RESISTANCE);
            if(effect==null||effect.getDuration()<30)p.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE,
                    s.hasFlag("demon.netherforged")?80:s.progress("demon.cinder_active"),0,false,true,true));
        }
        int old=s.progress("demon.decay");
        int decay=DemonRules.decay(old,p.getWorld().getRegistryKey()==World.NETHER,s.hasFlag("demon.lord"));
        s.progress("demon.decay",decay);
        int stage=DemonRules.decayStage(decay);
        if(stage==3)s.flag("demon.zombified");else s.unflag("demon.zombified");
        if(stage>DemonRules.decayStage(old))p.sendMessage(Text.literal(stage==3?"Netherbound: Zombified Demon":stage==2?"Netherbound: your soul is decaying.":"Netherbound: return to the Nether."),false);
        if(stage>=2){
            refresh(p,StatusEffects.SLOWNESS,stage==3?2:0);refresh(p,StatusEffects.MINING_FATIGUE,stage==3?2:0);
            refresh(p,StatusEffects.HUNGER,stage==3?1:0);
        }
        if(stage>=1&&p.age%20==0)p.getServerWorld().spawnParticles(ParticleTypes.ASH,p.getX(),p.getBodyY(.6),p.getZ(),stage==3?9:3,.3,.6,.3,.005);
        if(s.progress("demon.protection")>0)s.progress("demon.protection",s.progress("demon.protection")-1);
        if(s.progress("demon.tribute_cd")>0)s.progress("demon.tribute_cd",s.progress("demon.tribute_cd")-1);
        if(s.progress("demon.throne_cd")>0)s.progress("demon.throne_cd",s.progress("demon.throne_cd")-1);
    }
    private static void refresh(ServerPlayerEntity p,StatusEffect effect,int amp){
        StatusEffectInstance current=p.getStatusEffect(effect);
        if(current==null||current.getAmplifier()<amp||current.getDuration()<10)p.addStatusEffect(new StatusEffectInstance(effect,30,amp,false,true,true));
    }
}
