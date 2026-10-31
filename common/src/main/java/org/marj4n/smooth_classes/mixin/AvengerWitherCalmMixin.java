package org.marj4n.smooth_classes.mixin;

import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.LivingEntity;
import org.marj4n.smooth_classes.content.avenger.runtime.AvengerReworkRuntime;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keep vanilla AI/combat, but never permit owner-bound random-coordinate skulls or birth blasts. */
@Mixin(value=WitherEntity.class, priority=500)
public abstract class AvengerWitherCalmMixin {
    @Shadow private int blockBreakingCooldown;
    @Unique private LivingEntity smooth_classes$aimedSoulTarget;
    @Inject(method="mobTick",at=@At("HEAD"))
    private void calm(CallbackInfo ci){
        var self=(WitherEntity)(Object)this;
        if(!AvengerReworkRuntime.isAvengerSummon(self))return;
        self.setInvulTimer(0);blockBreakingCooldown=0;
        if(!AvengerReworkRuntime.mayBoundWitherAttack(self,self.getTarget())){
            self.setTarget(null);
            for(int head=0;head<3;head++)self.setTrackedEntityId(head,0);
        }
    }
    @Inject(method="shootSkullAt(ILnet/minecraft/entity/LivingEntity;)V",at=@At("HEAD"),cancellable=true)
    private void aimed(int head,LivingEntity target,CallbackInfo ci){
        var self=(WitherEntity)(Object)this;
        if(!AvengerReworkRuntime.isAvengerSummon(self))return;
        if(!AvengerReworkRuntime.mayBoundWitherAttack(self,target)){ci.cancel();return;}
        smooth_classes$aimedSoulTarget=target;
    }
    @Inject(method="shootSkullAt(ILnet/minecraft/entity/LivingEntity;)V",at=@At("RETURN"))
    private void clearAim(int head,LivingEntity target,CallbackInfo ci){smooth_classes$aimedSoulTarget=null;}
    @Inject(method="shootSkullAt(IDDDZ)V",at=@At("HEAD"),cancellable=true)
    private void rejectRandom(int head,double x,double y,double z,boolean charged,CallbackInfo ci){
        var self=(WitherEntity)(Object)this;
        if(AvengerReworkRuntime.isAvengerSummon(self)
                && !AvengerReworkRuntime.mayBoundWitherAttack(self,smooth_classes$aimedSoulTarget))ci.cancel();
    }
}
