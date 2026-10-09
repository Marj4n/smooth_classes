package org.marj4n.smooth_classes.client.origin.appearance;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.PlayerEntityModel;

/**
 * Direct vanilla (and Player Animator/Better Combat) head pose bridge.
 * Exactly parallels VsbVanillaArmPose / VsbVanillaLegPose: Minecraft uses
 * model-space Y-down, while the Figura mesh uses Y-up and the avatar renderer
 * reverses X/Z before applying rotations to MatrixStack.
 */
final class VsbVanillaHeadPose {
    private static final float DEGREES = 180F / (float) Math.PI;

    private VsbVanillaHeadPose() {}

    static VsbFiguraAvatarRenderer.Vec3 capture(PlayerEntityModel<?> vanilla,
                                                float headYaw, float headPitch) {
        // Only used if the API invokes EntityModel#setAngles without the
        // parent renderer. Normal TPV, inventory and armor paths use the
        // finished vanilla model captured by ManBatPosePreparerFeatureRenderer.
        if (vanilla == null) return new VsbFiguraAvatarRenderer.Vec3(-headPitch, headYaw, 0F);
        ModelPart head = vanilla.head;
        if (!Float.isFinite(head.pitch) || !Float.isFinite(head.yaw)
                || !Float.isFinite(head.roll)) {
            return VsbFiguraAvatarRenderer.Vec3.ZERO;
        }
        return new VsbFiguraAvatarRenderer.Vec3(
                -head.pitch * DEGREES,
                head.yaw * DEGREES,
                -head.roll * DEGREES);
    }
}
