package org.marj4n.smooth_classes.mixin;

import net.minecraft.entity.mob.MobEntity;
import org.marj4n.smooth_classes.content.avenger.runtime.AvengerReworkRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Guarded and following Death List summons remain real persistent world entities.
 * This guards the actual despawn check even when another mod changes the vanilla
 * named/equipped-item persistence condition. Natural chunk unloading is untouched.
 */
@Mixin(value = MobEntity.class, priority = 2000)
public abstract class AvengerBoundPersistenceMixin {
    @Inject(method = "checkDespawn", at = @At("HEAD"), cancellable = true)
    private void smooth_classes$preventBoundSoulDespawn(CallbackInfo ci) {
        MobEntity self = (MobEntity) (Object) this;
        if (self.getWorld().isClient || !AvengerReworkRuntime.isAvengerSummon(self)) return;
        AvengerReworkRuntime.maintainSummonPersistence(self);
        ci.cancel();
    }

    @Inject(method = "cannotDespawn", at = @At("HEAD"), cancellable = true)
    private void smooth_classes$alwaysKeepBoundSoul(CallbackInfoReturnable<Boolean> cir) {
        if (AvengerReworkRuntime.isAvengerSummon((MobEntity) (Object) this)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "isAffectedByDaylight", at = @At("HEAD"), cancellable = true)
    private void smooth_classes$protectBoundUndeadFromDaylight(CallbackInfoReturnable<Boolean> cir) {
        if (AvengerReworkRuntime.isAvengerSummon((MobEntity) (Object) this)) {
            cir.setReturnValue(false);
        }
    }
}
