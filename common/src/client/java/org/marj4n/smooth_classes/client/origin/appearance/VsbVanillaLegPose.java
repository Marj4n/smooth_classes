package org.marj4n.smooth_classes.client.origin.appearance;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.PlayerEntityModel;

import java.util.HashMap;
import java.util.Map;

/**
 * Mirrors the already-animated vanilla/Better Combat leg transforms into the
 * user-supplied Figura skeleton. This is deliberately parallel to the working
 * R6 arm bridge: the player's rendered rig is the only animation authority.
 *
 * VSB's source LeftLeg sits on physical right (-X), matching the vanilla
 * rightLeg, and source RightLeg sits on physical left (+X).
 */
final class VsbVanillaLegPose {
    private static final float DEGREES = 180.0F / (float) Math.PI;

    private VsbVanillaLegPose() {}

    record Legs(Map<String, VsbFiguraAvatarRenderer.Vec3> rotations,
                Map<String, VsbFiguraAvatarRenderer.Vec3> offsets) {}

    static Legs capture(PlayerEntityModel<?> model) {
        if (model == null) return new Legs(Map.of(), Map.of());
        Map<String, VsbFiguraAvatarRenderer.Vec3> rotations = new HashMap<>(2);
        Map<String, VsbFiguraAvatarRenderer.Vec3> offsets = new HashMap<>(2);
        put(rotations, offsets, "LeftLeg", model.rightLeg, -1.9F);
        put(rotations, offsets, "RightLeg", model.leftLeg, 1.9F);
        return new Legs(rotations, offsets);
    }

    private static void put(Map<String, VsbFiguraAvatarRenderer.Vec3> rotations,
                            Map<String, VsbFiguraAvatarRenderer.Vec3> offsets,
                            String bone, ModelPart part, float vanillaPivotX) {
        if (!Float.isFinite(part.pitch) || !Float.isFinite(part.yaw)
                || !Float.isFinite(part.roll) || !Float.isFinite(part.pivotX)
                || !Float.isFinite(part.pivotY) || !Float.isFinite(part.pivotZ)) return;
        // The source renderer reverses X/Z rotation signs to convert Figura's
        // Y-up geometry to Minecraft's Y-down model space.
        rotations.put(bone, new VsbFiguraAvatarRenderer.Vec3(
                -part.pitch * DEGREES,
                part.yaw * DEGREES,
                -part.roll * DEGREES));
        // Minecraft biped legs rest at Y=12, Z=0. Preserve VSB's exact
        // geometry/hip locations and apply only animator-produced offsets.
        offsets.put(bone, new VsbFiguraAvatarRenderer.Vec3(
                part.pivotX - vanillaPivotX,
                -(part.pivotY - 12F),
                part.pivotZ));
    }
}
