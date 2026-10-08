package org.marj4n.smooth_classes.client.origin.appearance;

import net.minecraft.client.model.*;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;

/** Armor-stand / alchemical-puppet body for the Homunculus origin. */
public final class HomunculusReplacementModel<T extends LivingEntity> extends EntityModel<T> {
    private final ModelPart root;
    private final ModelPart head, body, rightArm, leftArm, rightLeg, leftLeg;

    public HomunculusReplacementModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.body = root.getChild("body");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
    }

    public static TexturedModelData createData() {
        ModelData data = new ModelData();
        ModelPartData root = data.getRoot();
        root.addChild("head", ModelPartBuilder.create()
                        .uv(0,0).cuboid(-3.5F,-7.0F,-3.5F,7,7,7)
                        .uv(28,0).cuboid(0.5F,-6.8F,-3.8F,3,5,1),
                ModelTransform.NONE);
        root.addChild("body", ModelPartBuilder.create()
                        .uv(0,14).cuboid(-3.0F,0.0F,-1.5F,6,9,3)
                        .uv(18,14).cuboid(-1.5F,9.0F,-1.0F,3,3,2),
                ModelTransform.NONE);
        root.addChild("right_arm", ModelPartBuilder.create().uv(28,8)
                        .cuboid(-1.0F,-1.0F,-1.0F,2,11,2),
                ModelTransform.pivot(-4.0F,1.5F,0.0F));
        root.addChild("left_arm", ModelPartBuilder.create().uv(28,8)
                        .cuboid(-1.0F,-1.0F,-1.0F,2,11,2),
                ModelTransform.pivot(4.0F,1.5F,0.0F));
        root.addChild("right_leg", ModelPartBuilder.create().uv(36,8)
                        .cuboid(-1.0F,0.0F,-1.0F,2,12,2),
                ModelTransform.pivot(-1.5F,12.0F,0.0F));
        root.addChild("left_leg", ModelPartBuilder.create().uv(36,8)
                        .cuboid(-1.0F,0.0F,-1.0F,2,12,2),
                ModelTransform.pivot(1.5F,12.0F,0.0F));
        return TexturedModelData.of(data,64,64);
    }

    public void copyFrom(PlayerEntityModel<?> parent) {
        head.copyTransform(parent.head);
        body.copyTransform(parent.body);
        rightArm.copyTransform(parent.rightArm);
        leftArm.copyTransform(parent.leftArm);
        rightLeg.copyTransform(parent.rightLeg);
        leftLeg.copyTransform(parent.leftLeg);
    }

    @Override public void setAngles(T entity,float limbAngle,float limbDistance,float animationProgress,float headYaw,float headPitch) {}

    @Override
    public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay,
                       float red, float green, float blue, float alpha) {
        root.render(matrices, vertices, light, overlay, red, green, blue, alpha);
    }
}
