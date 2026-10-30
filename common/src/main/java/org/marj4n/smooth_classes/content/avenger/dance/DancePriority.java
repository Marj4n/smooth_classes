package org.marj4n.smooth_classes.content.avenger.dance;
import net.minecraft.entity.*;
import net.minecraft.entity.mob.WardenEntity;
import net.minecraft.entity.boss.WitherEntity;
/** Pure state read; never starts, stops or retimes a vanilla special ability. */
public final class DancePriority {
    public static boolean special(LivingEntity entity){
        if(entity.isUsingItem())return true;
        if(entity instanceof net.minecraft.entity.mob.WitchEntity witch&&witch.isDrinking())return true;
        if(entity instanceof net.minecraft.entity.mob.SpellcastingIllagerEntity caster&&caster.isSpellcasting())return true;
        if(entity instanceof WardenEntity w){
            EntityPose pose=w.getPose();
            if(pose==EntityPose.STANDING)return false;
            // STAY may freeze a harmless sniff/roar pose after combat. Let its finite
            // vanilla visual clip finish without rewriting the entity pose or its AI.
            if(w.isAiDisabled()&&(pose==EntityPose.SNIFFING||pose==EntityPose.ROARING)){
                var state=pose==EntityPose.SNIFFING?w.sniffingAnimationState:w.roaringAnimationState;
                if(!state.isRunning())return true;
                long elapsed=w.getWorld().isClient?state.getTimeRunning():Math.max(0L,w.age*50L-((org.marj4n.smooth_classes.mixin.AvengerDanceAnimationClockAccessor)(Object)state).smoothClasses$danceUpdatedAt());
                return frozenAmbientBusy(pose,elapsed,true);
            }
            return true;
        }
        if(entity instanceof WitherEntity w){if(w.getInvulnerableTimer()>0)return true;for(int i=0;i<3;i++){var target=w.getWorld().getEntityById(w.getTrackedEntityId(i));if(target instanceof LivingEntity l&&l.isAlive())return true;}}
        return false;
    }
    /** Verified vanilla1.20.1 lengths4.16s sniff /4.2s roar +one conservative game tick. */
    public static boolean frozenAmbientBusy(EntityPose pose,long elapsedMillis,boolean running){
        if(!running||elapsedMillis<0)return true;
        if(pose==EntityPose.SNIFFING)return elapsedMillis<4210;
        if(pose==EntityPose.ROARING)return elapsedMillis<4250;
        return true;
    }
    private DancePriority(){}
}
