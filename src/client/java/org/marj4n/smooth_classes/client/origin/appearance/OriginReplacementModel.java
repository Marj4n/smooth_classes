package org.marj4n.smooth_classes.client.origin.appearance;

import net.minecraft.client.model.*;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;

/**
 * Independent body model used when an Origin fully replaces the vanilla skin.
 * This is intentionally separate from PlayerEntityModel so the vanilla player
 * can be hidden while this body remains visible.
 */
public final class OriginReplacementModel<T extends LivingEntity> extends EntityModel<T> {
    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    public OriginReplacementModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.body = root.getChild("body");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
    }

    public static TexturedModelData createHumanoidData() {
        ModelData data = new ModelData();
        ModelPartData root = data.getRoot();
        root.addChild("head", ModelPartBuilder.create().uv(0, 0)
                .cuboid(-4, -8, -4, 8, 8, 8), ModelTransform.pivot(0, 0, 0));
        root.addChild("body", ModelPartBuilder.create().uv(16, 16)
                .cuboid(-4, 0, -2, 8, 12, 4), ModelTransform.pivot(0, 0, 0));
        root.addChild("right_arm", ModelPartBuilder.create().uv(40, 16)
                .cuboid(-3, -2, -2, 4, 12, 4), ModelTransform.pivot(-5, 2, 0));
        root.addChild("left_arm", ModelPartBuilder.create().uv(32, 48)
                .cuboid(-1, -2, -2, 4, 12, 4), ModelTransform.pivot(5, 2, 0));
        root.addChild("right_leg", ModelPartBuilder.create().uv(0, 16)
                .cuboid(-2, 0, -2, 4, 12, 4), ModelTransform.pivot(-1.9F, 12, 0));
        root.addChild("left_leg", ModelPartBuilder.create().uv(16, 48)
                .cuboid(-2, 0, -2, 4, 12, 4), ModelTransform.pivot(1.9F, 12, 0));
        return TexturedModelData.of(data, 64, 64);
    }

    /** Geometry reconstructed from Lost Relics' RelicSkeletonModel. */
    public static TexturedModelData createRelicSkeletonData() {
        ModelData data = new ModelData();
        ModelPartData root = data.getRoot();
        root.addChild("head", ModelPartBuilder.create()
                .uv(86, 0).cuboid(-3.5F, -7.5F, -4.75F, 7, 6, 7)
                .uv(104, 15).cuboid(-2, -1.5F, -4.75F, 4, 1, 1, new Dilation(0.01F))
                .uv(86, 13).cuboid(-4, -6.5F, -4.8F, 8, 4, 1, new Dilation(0.1F)),
                ModelTransform.pivot(0, -2, 0));
        root.addChild("body", ModelPartBuilder.create()
                .uv(87, 45).cuboid(-4, 0, -2, 8, 8, 4, new Dilation(-0.01F))
                .uv(113, 44).cuboid(-1, -1.75F, 1, 2, 12, 1)
                .uv(87, 58).cuboid(-4, 9, -2, 8, 3, 4, new Dilation(-0.01F)),
                ModelTransform.NONE);
        root.addChild("right_arm", ModelPartBuilder.create().uv(84, 29)
                .cuboid(-1, -2, -1, 2, 12, 2), ModelTransform.pivot(-5, 2, 0));
        root.addChild("left_arm", ModelPartBuilder.create().uv(84, 29)
                .cuboid(-1, -2, -1, 2, 12, 2), ModelTransform.pivot(5, 2, 0));
        root.addChild("right_leg", ModelPartBuilder.create().uv(96, 29)
                .cuboid(-1, 0, -1, 2, 12, 2), ModelTransform.pivot(-2, 12, 0));
        root.addChild("left_leg", ModelPartBuilder.create().uv(96, 29)
                .cuboid(-1, 0, -1, 2, 12, 2), ModelTransform.pivot(2, 12, 0));
        return TexturedModelData.of(data, 128, 128);
    }

    public void copyFrom(PlayerEntityModel<?> parent) {
        head.copyTransform(parent.head);
        body.copyTransform(parent.body);
        rightArm.copyTransform(parent.rightArm);
        leftArm.copyTransform(parent.leftArm);
        rightLeg.copyTransform(parent.rightLeg);
        leftLeg.copyTransform(parent.leftLeg);
    }

    @Override
    public void setAngles(T entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
        // Parent player transforms are copied directly by copyFrom().
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay,
                       float red, float green, float blue, float alpha) {
        root.render(matrices, vertices, light, overlay, red, green, blue, alpha);
    }
}
