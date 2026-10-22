package org.marj4n.smooth_classes.client.origin.appearance;

import net.minecraft.client.model.*;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;

/**
 * Vanilla Minecraft slime geometry, adapted to a player render feature.
 * Medium slime proportions are used: ~1 block body / hitbox.
 */
public final class SlimeReplacementModel<T extends PlayerEntity> extends EntityModel<T> {
    private final ModelPart root;
    private final ModelPart outer;
    private final ModelPart inner;
    private final ModelPart rightEye;
    private final ModelPart leftEye;
    private final ModelPart mouth;

    public SlimeReplacementModel(ModelPart root) {
        this.root = root;
        this.outer = root.getChild("outer");
        this.inner = root.getChild("inner");
        this.rightEye = root.getChild("right_eye");
        this.leftEye = root.getChild("left_eye");
        this.mouth = root.getChild("mouth");
    }

    public static TexturedModelData createData() {
        ModelData data = new ModelData();
        ModelPartData root = data.getRoot();
        // Matches vanilla SlimeEntityModel proportions/UV layout.
        root.addChild("outer", ModelPartBuilder.create().uv(0, 0)
                        .cuboid(-4.0F, 16.0F, -4.0F, 8.0F, 8.0F, 8.0F),
                ModelTransform.NONE);
        root.addChild("inner", ModelPartBuilder.create().uv(0, 16)
                        .cuboid(-3.0F, 17.0F, -3.0F, 6.0F, 6.0F, 6.0F),
                ModelTransform.NONE);
        root.addChild("right_eye", ModelPartBuilder.create().uv(32, 0)
                        .cuboid(-3.25F, 18.0F, -3.5F, 2.0F, 2.0F, 2.0F),
                ModelTransform.NONE);
        root.addChild("left_eye", ModelPartBuilder.create().uv(32, 4)
                        .cuboid(1.25F, 18.0F, -3.5F, 2.0F, 2.0F, 2.0F),
                ModelTransform.NONE);
        root.addChild("mouth", ModelPartBuilder.create().uv(32, 8)
                        .cuboid(0.0F, 21.0F, -3.5F, 1.0F, 1.0F, 1.0F),
                ModelTransform.NONE);
        return TexturedModelData.of(data, 64, 32);
    }

    public void prepare(PlayerEntityModel<?> parent, T player, float limbAngle, float limbDistance, float animationProgress) {
        float movement = Math.min(1.0F, limbDistance);
        float pulse = MathHelper.sin(animationProgress * 0.30F) * 0.035F;
        float movingSquish = movement * 0.04F;
        float yScale = 1.0F - pulse - movingSquish;
        float xzScale = 1.0F + (pulse + movingSquish) * 0.55F;

        for (ModelPart part : new ModelPart[]{outer, inner, rightEye, leftEye, mouth}) {
            part.pitch = 0.0F;
            part.yaw = parent.body.yaw;
            part.roll = 0.0F;
            part.xScale = xzScale;
            part.yScale = yScale;
            part.zScale = xzScale;
        }
    }

    public void renderInner(MatrixStack matrices, VertexConsumer vertices, int light, int overlay) {
        inner.render(matrices, vertices, light, overlay, 1, 1, 1, 1);
        rightEye.render(matrices, vertices, light, overlay, 1, 1, 1, 1);
        leftEye.render(matrices, vertices, light, overlay, 1, 1, 1, 1);
        mouth.render(matrices, vertices, light, overlay, 1, 1, 1, 1);
    }

    public void renderOuter(MatrixStack matrices, VertexConsumer vertices, int light, int overlay) {
        outer.render(matrices, vertices, light, overlay, 1, 1, 1, 0.78F);
    }

    @Override public void setAngles(T entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {}

    @Override
    public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay,
                       float red, float green, float blue, float alpha) {
        root.render(matrices, vertices, light, overlay, red, green, blue, alpha);
    }
}
