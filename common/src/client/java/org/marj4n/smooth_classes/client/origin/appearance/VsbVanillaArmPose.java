package org.marj4n.smooth_classes.client.origin.appearance;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.PlayerEntityModel;

import java.util.HashMap;
import java.util.Map;

/**
 * Mirror the finished vanilla PlayerEntityModel arm pose into VSB/Blockbench
 * source axes. Better Combat / Player Animator has already modified this model
 * before feature layers run: it is the ONLY authority for TPV weapon gestures.
 *
 * Blockbench VSB: -X = physical right ("LeftArm"), +X = physical left ("RightArm").
 * Source model uses Y-up; Minecraft ModelPart uses Y-down. The VSB renderer
 * negates rotations around X and Z, so invert those values here as well.
 */
final class VsbVanillaArmPose {
    private static final float DEGREES = 180.0F / (float) Math.PI;

    private VsbVanillaArmPose() {}

    record Arms(Map<String, VsbFiguraAvatarRenderer.Vec3> rotations,
                Map<String, VsbFiguraAvatarRenderer.Vec3> offsets) {}

    static Arms capture(PlayerEntityModel<?> model) {
        if (model == null) return new Arms(Map.of(), Map.of());
        Map<String, VsbFiguraAvatarRenderer.Vec3> rotations = new HashMap<>(2);
        Map<String, VsbFiguraAvatarRenderer.Vec3> offsets = new HashMap<>(2);
        put(rotations, offsets, "LeftArm", model.rightArm, -5F);
        put(rotations, offsets, "RightArm", model.leftArm, 5F);
        return new Arms(rotations, offsets);
    }

    private static void put(Map<String, VsbFiguraAvatarRenderer.Vec3> rotation,
                            Map<String, VsbFiguraAvatarRenderer.Vec3> offset,
                            String sourceBone, ModelPart part, float vanillaPivotX) {
        if (!Float.isFinite(part.pitch) || !Float.isFinite(part.yaw)
                || !Float.isFinite(part.roll) || !Float.isFinite(part.pivotX)
                || !Float.isFinite(part.pivotY) || !Float.isFinite(part.pivotZ)) return;
        rotation.put(sourceBone, new VsbFiguraAvatarRenderer.Vec3(
                -part.pitch * DEGREES,
                part.yaw * DEGREES,
                -part.roll * DEGREES));
        // Copy extra animator arm translations relative to the vanilla biped
        // shoulder. Flip only Y because VSB Blockbench uses Y-up.
        offset.put(sourceBone, new VsbFiguraAvatarRenderer.Vec3(
                part.pivotX - vanillaPivotX,
                -(part.pivotY - 2F),
                part.pivotZ));
    }
}
