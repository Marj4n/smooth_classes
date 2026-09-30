package org.marj4n.smooth_classes.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import org.marj4n.smooth_classes.client.charge.ChargeHudState;
import org.marj4n.smooth_classes.effects.SmoothEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Gentle zoom-in during heavy charge casts so Portal of Sovereignty feels as weighty
 * as Arcane Slash. Works only while the world FOV is being calculated.
 */
@Mixin(GameRenderer.class)
public abstract class ChargeCastingFovMixin {
    private static final double NORMAL_FOV_MULTIPLIER = 1.0D;
    private static final double CHARGE_FOV_MULTIPLIER = 0.92D;
    private static final double IN_SMOOTHING = 0.16D;
    private static final double OUT_SMOOTHING = 0.11D;

    private static double smooth_classes$chargeFov = NORMAL_FOV_MULTIPLIER;

    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void smooth_classes$chargeFov(Camera camera, float tickDelta, boolean changingFov,
                                          CallbackInfoReturnable<Double> cir) {
        if (!changingFov) return;

        MinecraftClient client = MinecraftClient.getInstance();
        boolean heavyCharge = client.player != null && (
                client.player.hasStatusEffect(SmoothEffects.ARCANE_SLASH)
                        || ChargeHudState.active(ChargeHudState.PORTAL_OF_SOVEREIGNTY)
        );

        double target = heavyCharge ? CHARGE_FOV_MULTIPLIER : NORMAL_FOV_MULTIPLIER;
        double smoothing = target < smooth_classes$chargeFov ? IN_SMOOTHING : OUT_SMOOTHING;
        smooth_classes$chargeFov += (target - smooth_classes$chargeFov) * smoothing;
        if (Math.abs(smooth_classes$chargeFov - target) < 0.00025D) smooth_classes$chargeFov = target;
        cir.setReturnValue(cir.getReturnValue() * smooth_classes$chargeFov);
    }
}
