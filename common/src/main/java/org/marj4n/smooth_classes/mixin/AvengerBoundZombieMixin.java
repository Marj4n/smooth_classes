package org.marj4n.smooth_classes.mixin;

import net.minecraft.entity.mob.ZombieEntity;
import org.marj4n.smooth_classes.content.avenger.runtime.AvengerReworkRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keep bound zombie guards intact through sunlight and underwater patrols. */
@Mixin(ZombieEntity.class)
public abstract class AvengerBoundZombieMixin {
    @Inject(method = "burnsInDaylight", at = @At("HEAD"), cancellable = true)
    private void smooth_classes$boundZombieIgnoresSun(CallbackInfoReturnable<Boolean> cir) {
        if (AvengerReworkRuntime.isAvengerSummon((ZombieEntity) (Object) this)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "canConvertInWater", at = @At("HEAD"), cancellable = true)
    private void smooth_classes$boundZombieKeepsItsIdentity(CallbackInfoReturnable<Boolean> cir) {
        if (AvengerReworkRuntime.isAvengerSummon((ZombieEntity) (Object) this)) {
            cir.setReturnValue(false);
        }
    }
}
