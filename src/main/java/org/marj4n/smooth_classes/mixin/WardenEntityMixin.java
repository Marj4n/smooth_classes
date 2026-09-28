package org.marj4n.smooth_classes.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.WardenEntity;
import org.marj4n.smooth_classes.content.avenger.runtime.AvengerReworkRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Makes a bound Warden obey the same faction rules as every other Death List summon. */
@Mixin(WardenEntity.class)
public abstract class WardenEntityMixin {
    @Inject(method = "isValidTarget", at = @At("HEAD"), cancellable = true)
    private void smooth_classes$filterAvengerTargets(Entity target, CallbackInfoReturnable<Boolean> cir) {
        WardenEntity self = (WardenEntity) (Object) this;
        if (AvengerReworkRuntime.shouldWardenIgnore(self, target)) cir.setReturnValue(false);
    }

    @Inject(method = "increaseAngerAt(Lnet/minecraft/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
    private void smooth_classes$ignoreFriendlyAnger(Entity target, CallbackInfo ci) {
        WardenEntity self = (WardenEntity) (Object) this;
        if (AvengerReworkRuntime.shouldWardenIgnore(self, target)) ci.cancel();
    }

    @Inject(method = "increaseAngerAt(Lnet/minecraft/entity/Entity;IZ)V", at = @At("HEAD"), cancellable = true)
    private void smooth_classes$ignoreFriendlyAngerDetailed(Entity target, int amount, boolean listening, CallbackInfo ci) {
        WardenEntity self = (WardenEntity) (Object) this;
        if (AvengerReworkRuntime.shouldWardenIgnore(self, target)) ci.cancel();
    }
}
