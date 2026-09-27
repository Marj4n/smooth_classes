package org.marj4n.smooth_classes.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import org.marj4n.smooth_classes.entity.RiderHippogryphEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Ctrl/Sprint boost FOV for Rider Hippogryph flight.
 *
 * Intentionally limited to boost only. There is no banking, camera roll,
 * steering tilt, or dive FOV in this mixin.
 */
@Mixin(GameRenderer.class)
public abstract class RiderFlightFovMixin {
    private static final double NORMAL_FOV_MULTIPLIER = 1.0D;
    private static final double BOOST_FOV_MULTIPLIER = 1.075D;

    // Smooth enough to feel like acceleration instead of an instant FOV pop.
    private static final double BOOST_IN_SMOOTHING = 0.115D;
    private static final double BOOST_OUT_SMOOTHING = 0.090D;

    private static double smooth_classes$riderBoostFov = NORMAL_FOV_MULTIPLIER;

    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void smooth_classes$riderBoostFov(
            Camera camera,
            float tickDelta,
            boolean changingFov,
            CallbackInfoReturnable<Double> cir
    ) {
        // GameRenderer also asks for FOV in auxiliary render paths. Only touch
        // the actual changing world FOV so held-item rendering does not pulse.
        if (!changingFov) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        boolean boosting = client.player != null
                && client.player.getVehicle() instanceof RiderHippogryphEntity hippo
                && hippo.isRiderFlying()
                && hippo.isRiderBoosting();

        double target = boosting
                ? BOOST_FOV_MULTIPLIER
                : NORMAL_FOV_MULTIPLIER;

        double smoothing = target > smooth_classes$riderBoostFov
                ? BOOST_IN_SMOOTHING
                : BOOST_OUT_SMOOTHING;

        smooth_classes$riderBoostFov +=
                (target - smooth_classes$riderBoostFov) * smoothing;

        if (Math.abs(smooth_classes$riderBoostFov - target) < 0.00025D) {
            smooth_classes$riderBoostFov = target;
        }

        cir.setReturnValue(cir.getReturnValue() * smooth_classes$riderBoostFov);
    }
}
