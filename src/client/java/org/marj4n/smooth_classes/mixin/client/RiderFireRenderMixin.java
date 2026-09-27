package org.marj4n.smooth_classes.mixin.client;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.marj4n.smooth_classes.entity.RiderDreadSteedEntity;
import org.marj4n.smooth_classes.entity.RiderHippogryphEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Never draw vanilla fire around Rider lava mounts or their rider. */
@Mixin(Entity.class)
public abstract class RiderFireRenderMixin {
    @Inject(method = "doesRenderOnFire", at = @At("HEAD"), cancellable = true)
    private void smooth_classes$hideRiderMountFire(CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;
        if (self instanceof RiderDreadSteedEntity || self instanceof RiderHippogryphEntity) {
            cir.setReturnValue(false);
            return;
        }
        if (self instanceof PlayerEntity player
                && (player.getVehicle() instanceof RiderDreadSteedEntity
                || player.getVehicle() instanceof RiderHippogryphEntity)) {
            cir.setReturnValue(false);
        }
    }
}
