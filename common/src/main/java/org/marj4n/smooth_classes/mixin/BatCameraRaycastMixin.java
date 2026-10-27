package org.marj4n.smooth_classes.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.marj4n.smooth_classes.origin.OriginBodyGeometry;
import org.marj4n.smooth_classes.origin.OriginRuntime;
import org.marj4n.smooth_classes.origin.OriginType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Keeps the actual aim/raycast origin on the same vertical point as Bat Form's
 * visible first-person camera. Vanilla targeting asks Entity#getCameraPosVec;
 * the visual Camera has its own eye-height path, so only changing cameraY can
 * leave the crosshair drawing low while block targeting still comes from the
 * old humanoid eye position.
 */
@Mixin(Entity.class)
public abstract class BatCameraRaycastMixin {
    @Shadow public double prevX;
    @Shadow public double prevY;
    @Shadow public double prevZ;

    @Shadow public abstract double getX();
    @Shadow public abstract double getY();
    @Shadow public abstract double getZ();

    @Inject(method = "getCameraPosVec", at = @At("HEAD"), cancellable = true)
    private void smooth_classes$batCameraRayOrigin(float tickDelta, CallbackInfoReturnable<Vec3d> cir) {
        Entity self = (Entity) (Object) this;
        if (!(self instanceof PlayerEntity player)) return;

        var state = OriginRuntime.state(player);
        if (state.origin() != OriginType.VAMPIRE || !state.hasFlag("vampire.form.bat")) return;

        double x = MathHelper.lerp((double) tickDelta, prevX, getX());
        double y = MathHelper.lerp((double) tickDelta, prevY, getY()) + OriginBodyGeometry.BAT_EYE_HEIGHT;
        double z = MathHelper.lerp((double) tickDelta, prevZ, getZ());
        cir.setReturnValue(new Vec3d(x, y, z));
    }
}
