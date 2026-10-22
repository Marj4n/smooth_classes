package org.marj4n.smooth_classes.client.origin.appearance;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;

/**
 * Vampire Bat Form uses Minecraft's real 1.20.1 BatEntityModel geometry.
 * Only the pose is changed while grounded so the vanilla wings act as the
 * animal's forelegs; airborne animation returns to the normal bat silhouette.
 */
public final class BatFormModel<T extends PlayerEntity> extends EntityModel<T> {
    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart rightWing;
    private final ModelPart leftWing;
    private final ModelPart rightWingTip;
    private final ModelPart leftWingTip;
    private final float originalHeadPivotY;
    private final float originalRightWingPivotX;
    private final float originalRightWingPivotY;
    private final float originalRightWingPivotZ;
    private final float originalLeftWingPivotX;
    private final float originalLeftWingPivotY;
    private final float originalLeftWingPivotZ;

    public BatFormModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.body = root.getChild("body");
        this.rightWing = body.getChild("right_wing");
        this.leftWing = body.getChild("left_wing");
        this.rightWingTip = rightWing.getChild("right_wing_tip");
        this.leftWingTip = leftWing.getChild("left_wing_tip");
        this.originalHeadPivotY = head.pivotY;
        this.originalRightWingPivotX = rightWing.pivotX;
        this.originalRightWingPivotY = rightWing.pivotY;
        this.originalRightWingPivotZ = rightWing.pivotZ;
        this.originalLeftWingPivotX = leftWing.pivotX;
        this.originalLeftWingPivotY = leftWing.pivotY;
        this.originalLeftWingPivotZ = leftWing.pivotZ;
    }

    public static TexturedModelData createData() {
        // Never replace this with a custom mesh: this is the vanilla Minecraft bat.
        return net.minecraft.client.render.entity.model.BatEntityModel.getTexturedModelData();
    }

    public void prepare(T player, float animationProgress, float limbAngle, float limbDistance, float flightBlend) {
        float blend = MathHelper.clamp(flightBlend, 0.0F, 1.0F);
        // The same shared ModelPart instances render grounded/flying/roosting.
        // Always restore the original pivots before recomputing the next pose.
        head.pivotY = originalHeadPivotY;
        rightWing.pivotX = originalRightWingPivotX;
        rightWing.pivotY = originalRightWingPivotY;
        rightWing.pivotZ = originalRightWingPivotZ;
        leftWing.pivotX = originalLeftWingPivotX;
        leftWing.pivotY = originalLeftWingPivotY;
        leftWing.pivotZ = originalLeftWingPivotZ;

        // Grounded locomotion transplanted from V1.5.6 because that is the
        // version whose walk/crawl movement felt right in-game. Keep its torso
        // lean and alternating foreleg stride intact; only bend the wing tips
        // slightly farther down so the hands actually meet the floor.
        float movement = Math.min(1.0F, limbDistance * 2.5F);
        float stride = MathHelper.sin(limbAngle * 0.6662F) * 0.48F * movement;
        float bob = Math.abs(MathHelper.cos(limbAngle * 0.6662F)) * 0.05F * movement;

        float groundBodyPitch = 1.02F + bob;
        float groundHeadPitch = -0.72F + player.getPitch() * ((float)Math.PI / 180.0F) * 0.12F;
        float groundRightWingPitch = 0.95F + stride;
        float groundLeftWingPitch = 0.95F - stride;
        float groundRightWingYaw = -0.30F;
        float groundLeftWingYaw = 0.30F;
        float groundRightWingRoll = -0.22F;
        float groundLeftWingRoll = 0.22F;

        // V1.5.6 used these yaw values for the lower wing/hand. Add a small
        // downward pitch so the tips contact the ground without changing the
        // walk rhythm or turning the wings into a T-pose.
        float groundRightTipPitch = 0.36F - stride * 0.10F;
        float groundLeftTipPitch = 0.36F + stride * 0.10F;
        float groundRightTipYaw = -0.38F - stride * 0.35F;
        float groundLeftTipYaw = 0.38F + stride * 0.35F;
        float groundRightTipRoll = -0.08F;
        float groundLeftTipRoll = 0.08F;

        // Current vanilla-like airborne state stays untouched.
        float flap = MathHelper.cos(animationProgress * 1.30F) * (float)Math.PI * 0.25F;
        float airBodyPitch = 0.35F;
        float airHeadPitch = player.getPitch() * ((float)Math.PI / 180.0F) * 0.22F;
        float airRightWingPitch = 0.0F;
        float airLeftWingPitch = 0.0F;
        float airRightWingYaw = -0.10F - flap;
        float airLeftWingYaw = 0.10F + flap;
        float airRightWingRoll = 0.0F;
        float airLeftWingRoll = 0.0F;
        float airRightTipPitch = 0.0F;
        float airLeftTipPitch = 0.0F;
        float airRightTipYaw = airRightWingYaw * 0.55F;
        float airLeftTipYaw = airLeftWingYaw * 0.55F;
        float airRightTipRoll = 0.0F;
        float airLeftTipRoll = 0.0F;

        body.pitch = MathHelper.lerp(blend, groundBodyPitch, airBodyPitch);
        body.roll = 0.0F;
        body.yaw = 0.0F;

        head.pitch = MathHelper.lerp(blend, groundHeadPitch, airHeadPitch);
        head.yaw = 0.0F;
        head.roll = 0.0F;

        rightWing.pitch = MathHelper.lerp(blend, groundRightWingPitch, airRightWingPitch);
        leftWing.pitch = MathHelper.lerp(blend, groundLeftWingPitch, airLeftWingPitch);
        rightWing.yaw = MathHelper.lerp(blend, groundRightWingYaw, airRightWingYaw);
        leftWing.yaw = MathHelper.lerp(blend, groundLeftWingYaw, airLeftWingYaw);
        rightWing.roll = MathHelper.lerp(blend, groundRightWingRoll, airRightWingRoll);
        leftWing.roll = MathHelper.lerp(blend, groundLeftWingRoll, airLeftWingRoll);

        rightWingTip.pitch = MathHelper.lerp(blend, groundRightTipPitch, airRightTipPitch);
        leftWingTip.pitch = MathHelper.lerp(blend, groundLeftTipPitch, airLeftTipPitch);
        rightWingTip.yaw = MathHelper.lerp(blend, groundRightTipYaw, airRightTipYaw);
        leftWingTip.yaw = MathHelper.lerp(blend, groundLeftTipYaw, airLeftTipYaw);
        rightWingTip.roll = MathHelper.lerp(blend, groundRightTipRoll, airRightTipRoll);
        leftWingTip.roll = MathHelper.lerp(blend, groundLeftTipRoll, airLeftTipRoll);
    }


    /**
     * Actual vanilla BatEntityModel roosting angles (1.20-era), rather than
     * turning a spread-wing flying pose over like a cardboard cutout.
     *
     * The head and torso each rotate separately, the main wings wrap around
     * the flanks, and their tips fold back over the chest. Keep this animation
     * local to Bat Form so the approved flying and ground poses stay unchanged.
     */
    public void preparePerch(float blend) {
        blend = MathHelper.clamp(blend, 0.0F, 1.0F);
        if (blend <= 0.0F) return;

        // The vanilla roost pose is achieved IN the model parts, not by
        // rotating its entire rendering matrix by 180 degrees.
        body.pitch = MathHelper.lerp(blend, body.pitch, MathHelper.PI);
        body.roll = MathHelper.lerp(blend, body.roll, 0.0F);
        body.yaw = MathHelper.lerp(blend, body.yaw, 0.0F);
        head.pitch = MathHelper.lerp(blend, head.pitch, 0.0F);
        head.yaw = MathHelper.lerp(blend, head.yaw, 0.0F);
        head.roll = MathHelper.lerp(blend, head.roll, MathHelper.PI);
        // Vanilla's hanging silhouette raises the head a little and brings
        // both wing hinges close to the chest, instead of leaving the extended
        // flying hinges in place. ModelPart pivots are in 1/16-block units.
        head.pivotY = MathHelper.lerp(blend, originalHeadPivotY, -2.0F);
        rightWing.pivotX = MathHelper.lerp(blend, originalRightWingPivotX, -3.0F);
        rightWing.pivotY = MathHelper.lerp(blend, originalRightWingPivotY, 0.0F);
        rightWing.pivotZ = MathHelper.lerp(blend, originalRightWingPivotZ, 3.0F);
        leftWing.pivotX = MathHelper.lerp(blend, originalLeftWingPivotX, 3.0F);
        leftWing.pivotY = MathHelper.lerp(blend, originalLeftWingPivotY, 0.0F);
        leftWing.pivotZ = MathHelper.lerp(blend, originalLeftWingPivotZ, 3.0F);

        // These values reproduce the folded, embracing silhouette of the
        // classic vanilla roosting bat: wing yaw +/-72 degrees and wing-tip
        // yaw +/-99 degrees. The small wing pitch tucks the outer membrane.
        final float foldedWingPitch = -MathHelper.PI * 0.05F;
        final float foldedWingYaw = MathHelper.PI * 0.40F;
        final float foldedTipYaw = MathHelper.PI * 0.55F;
        rightWing.pitch = MathHelper.lerp(blend, rightWing.pitch, foldedWingPitch);
        leftWing.pitch = MathHelper.lerp(blend, leftWing.pitch, foldedWingPitch);
        rightWing.yaw = MathHelper.lerp(blend, rightWing.yaw, -foldedWingYaw);
        leftWing.yaw = MathHelper.lerp(blend, leftWing.yaw, foldedWingYaw);
        rightWing.roll = MathHelper.lerp(blend, rightWing.roll, 0.0F);
        leftWing.roll = MathHelper.lerp(blend, leftWing.roll, 0.0F);
        rightWingTip.pitch = MathHelper.lerp(blend, rightWingTip.pitch, 0.0F);
        leftWingTip.pitch = MathHelper.lerp(blend, leftWingTip.pitch, 0.0F);
        rightWingTip.yaw = MathHelper.lerp(blend, rightWingTip.yaw, -foldedTipYaw);
        leftWingTip.yaw = MathHelper.lerp(blend, leftWingTip.yaw, foldedTipYaw);
        rightWingTip.roll = MathHelper.lerp(blend, rightWingTip.roll, 0.0F);
        leftWingTip.roll = MathHelper.lerp(blend, leftWingTip.roll, 0.0F);
    }

    @Override
    public void setAngles(T entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay,
                       float red, float green, float blue, float alpha) {
        root.render(matrices, vertices, light, overlay, red, green, blue, alpha);
    }
}
