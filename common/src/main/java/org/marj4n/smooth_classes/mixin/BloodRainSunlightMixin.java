package org.marj4n.smooth_classes.mixin;
import net.minecraft.entity.mob.MobEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(MobEntity.class)
public abstract class BloodRainSunlightMixin {
    @Inject(method="isAffectedByDaylight", at=@At("HEAD"), cancellable=true)
    private void smooth_classes$stormShade(CallbackInfoReturnable<Boolean> cir) {
        if (org.marj4n.smooth_classes.runtime.BloodRainRuntime.shelters((MobEntity)(Object)this)) cir.setReturnValue(false);
    }
}
