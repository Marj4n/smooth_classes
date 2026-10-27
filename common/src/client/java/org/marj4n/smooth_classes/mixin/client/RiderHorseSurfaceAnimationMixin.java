package org.marj4n.smooth_classes.mixin.client;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.HorseEntityModel;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.util.math.MathHelper;
import org.marj4n.smooth_classes.entity.RiderDreadSteedEntity;
import org.marj4n.smooth_classes.entity.RiderHorseEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Vanilla horses that are physically touching water/lava can keep the visual
 * paddling gait even when our Rider physics marks the fluid surface as ground.
 * For Rider mounts, replace only the four main leg pitches at the very end of
 * HorseEntityModel#setAngles with a normal ground-running gait.
 */
@Mixin(HorseEntityModel.class)
public abstract class RiderHorseSurfaceAnimationMixin {
    @Shadow @Final private ModelPart rightHindLeg;
    @Shadow @Final private ModelPart leftHindLeg;
    @Shadow @Final private ModelPart rightFrontLeg;
    @Shadow @Final private ModelPart leftFrontLeg;

    @Inject(method = "setAngles", at = @At("TAIL"))
    private void smooth_classes$forceGroundGaitOnFluid(
            AbstractHorseEntity entity,
            float limbAngle,
            float limbDistance,
            float animationProgress,
            float headYaw,
            float headPitch,
            CallbackInfo ci
    ) {
        boolean surfaceWalking = entity instanceof RiderHorseEntity horse && horse.isRiderSurfaceWalking()
                || entity instanceof RiderDreadSteedEntity dread && dread.isRiderSurfaceWalking();
        if (!surfaceWalking) return;

        double horizontal = Math.sqrt(
                entity.getVelocity().x * entity.getVelocity().x
                        + entity.getVelocity().z * entity.getVelocity().z
        );
        float amount = MathHelper.clamp((float) horizontal * 3.35F, 0.0F, 1.0F);
        if (amount < 0.015F) {
            rightHindLeg.pitch = 0.0F;
            leftHindLeg.pitch = 0.0F;
            rightFrontLeg.pitch = 0.0F;
            leftFrontLeg.pitch = 0.0F;
            return;
        }

        // Drive the phase from world animation time rather than fluid-damped
        // limbAnimator values. The diagonal pairing reads like the normal horse
        // trot/gallop instead of all four legs paddling through the fluid.
        float phase = animationProgress * (0.42F + amount * 0.30F);
        float front = MathHelper.cos(phase) * 1.15F * amount;
        float hind = MathHelper.cos(phase + (float) Math.PI) * 0.95F * amount;

        rightFrontLeg.pitch = front;
        leftFrontLeg.pitch = -front;
        rightHindLeg.pitch = hind;
        leftHindLeg.pitch = -hind;
    }
}
