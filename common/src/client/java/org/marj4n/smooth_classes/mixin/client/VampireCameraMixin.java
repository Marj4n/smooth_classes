package org.marj4n.smooth_classes.mixin.client;

import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.marj4n.smooth_classes.origin.OriginRuntime;
import org.marj4n.smooth_classes.origin.OriginType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * INACTIVE compatibility shim retained in source history.
 * Bat camera height now comes from LivingEntity#getActiveEyeHeight via
 * OriginBodyGeometry so FPV, collision and TPV share one geometry source.
 */
@Mixin(Camera.class)
public abstract class VampireCameraMixin {
    @Shadow private Entity focusedEntity;
    @Shadow private float cameraY;
    @Shadow private float lastCameraY;

    @Inject(method = "updateEyeHeight", at = @At("TAIL"))
    private void smooth_classes$batCameraHeight(CallbackInfo ci) {
        if (!(focusedEntity instanceof PlayerEntity player)) return;
        var state = OriginRuntime.state(player);
        if (state.origin() == OriginType.VAMPIRE && state.hasFlag("vampire.form.bat")) {
            // Read the exact same eye-height constant used by Bat Form geometry; no separate camera magic number.
            cameraY = org.marj4n.smooth_classes.origin.OriginBodyGeometry.BAT_EYE_HEIGHT;
            lastCameraY = org.marj4n.smooth_classes.origin.OriginBodyGeometry.BAT_EYE_HEIGHT;
        }
    }
}
