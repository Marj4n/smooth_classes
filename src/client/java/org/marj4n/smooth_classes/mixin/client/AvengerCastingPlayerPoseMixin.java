package org.marj4n.smooth_classes.mixin.client;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import org.marj4n.smooth_classes.client.effects.AvengerSoulAnimationClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Reach down to raise the dead, then extend the palm to reabsorb a soul.
 * Blends in/out after vanilla and Better Combat pose calculations without
 * changing input, physical sneaking state or attack cooldowns.
 */
@Mixin(PlayerEntityModel.class)
public abstract class AvengerCastingPlayerPoseMixin {
    @Inject(method = "setAngles(Lnet/minecraft/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void smooth_classes$ritualRightHand(LivingEntity entity, float limbAngle,
            float limbDistance, float animationProgress, float headYaw,
            float headPitch, CallbackInfo ci) {
        if (!(entity instanceof AbstractClientPlayerEntity player)) return;
        float pose = AvengerSoulAnimationClient.casterPose(player, animationProgress - player.age);
        if (pose <= 0F) return;
        PlayerEntityModel<?> model = (PlayerEntityModel<?>) (Object) this;
        boolean raising = AvengerSoulAnimationClient.casterAction(player)
                == org.marj4n.smooth_classes.content.avenger.runtime.AvengerSoulAnimationRuntime.EMERGE;
        if (raising) {
            // Kneel toward the dirt: lower the right shoulder so the hand
            // can actually reach ground level instead of pointing horizontally.
            model.body.pitch = MathHelper.lerp(pose, model.body.pitch, 0.78F);
            model.body.pivotY += 2.0F * pose;
            model.head.pitch += 0.25F * pose;
            model.head.pivotY += 1.5F * pose;
            model.hat.copyTransform(model.head);
            model.rightArm.pitch = MathHelper.lerp(pose, model.rightArm.pitch, -0.42F);
            model.rightArm.yaw = MathHelper.lerp(pose, model.rightArm.yaw, -0.18F);
            model.rightArm.pivotY += 7.0F * pose;
            model.rightLeg.pitch = MathHelper.lerp(pose, model.rightLeg.pitch, 0.48F);
            model.leftLeg.pitch = MathHelper.lerp(pose, model.leftLeg.pitch, -0.92F);
            model.rightPants.copyTransform(model.rightLeg);
            model.leftPants.copyTransform(model.leftLeg);
        } else {
            // Open-palm pull: fully extend the right arm toward the soul.
            model.body.pitch = MathHelper.lerp(pose, model.body.pitch, 0.08F);
            model.rightArm.pitch = MathHelper.lerp(pose, model.rightArm.pitch, -1.30F);
            model.rightArm.yaw = MathHelper.lerp(pose, model.rightArm.yaw, -0.10F);
        }
        model.leftArm.pitch = MathHelper.lerp(pose, model.leftArm.pitch, -0.25F);
        model.rightSleeve.copyTransform(model.rightArm);
        model.leftSleeve.copyTransform(model.leftArm);
    }
}
