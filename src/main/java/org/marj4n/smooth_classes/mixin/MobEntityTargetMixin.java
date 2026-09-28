package org.marj4n.smooth_classes.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import org.marj4n.smooth_classes.content.avenger.runtime.AvengerReworkRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Stops vanilla goal AI from re-acquiring Avenger allies after runtime cleanup. */
@Mixin(MobEntity.class)
public abstract class MobEntityTargetMixin {
    @Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
    private void smooth_classes$filterAvengerSummonTarget(LivingEntity target, CallbackInfo ci) {
        if (target == null) return;
        MobEntity self = (MobEntity) (Object) this;
        if (AvengerReworkRuntime.shouldIgnoreSummonTarget(self, target)) {
            self.getNavigation().stop();
            ci.cancel();
        }
    }
}
