package org.marj4n.smooth_classes.client.origin.appearance;

import net.minecraft.client.model.*;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;

/**
 * Lightweight cosmetic rig for Origin appendages.
 */
public final class OriginCosmeticModel<T extends LivingEntity> extends EntityModel<T> {
    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart body;

    private final ModelPart demonHornLeft;
    private final ModelPart demonHornRight;
    private final ModelPart halo;
    private final ModelPart voidCrown;
    private final ModelPart facelessPlate;
    private final ModelPart sprigganBranchLeft;
    private final ModelPart sprigganBranchRight;
    private final ModelPart batWingLeft;
    private final ModelPart batWingRight;
    private final ModelPart demonTailBase;
    private final ModelPart demonTailTip;

    public OriginCosmeticModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.body = root.getChild("body");

        this.demonHornLeft = head.getChild("demon_horn_left");
        this.demonHornRight = head.getChild("demon_horn_right");
        this.halo = head.getChild("halo");
        this.voidCrown = head.getChild("void_crown");
        this.facelessPlate = head.getChild("faceless_plate");
        this.sprigganBranchLeft = head.getChild("spriggan_branch_left");
        this.sprigganBranchRight = head.getChild("spriggan_branch_right");
        this.batWingLeft = body.getChild("bat_wing_left");
        this.batWingRight = body.getChild("bat_wing_right");
        this.demonTailBase = body.getChild("demon_tail_base");
        this.demonTailTip = demonTailBase.getChild("demon_tail_tip");
    }

    public static TexturedModelData createTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData root = modelData.getRoot();

        ModelPartData head = root.addChild("head", ModelPartBuilder.create(), ModelTransform.NONE);
        head.addChild("demon_horn_left",
                ModelPartBuilder.create()
                        .uv(0, 0).cuboid(2.5F, -10.25F, -2.2F, 2.0F, 3.0F, 2.0F)
                        .uv(0, 5).cuboid(3.0F, -12.0F, -1.8F, 1.0F, 2.0F, 1.0F),
                ModelTransform.of(0.0F, 0.0F, 0.0F, -0.10F, 0.0F, -0.22F));
        head.addChild("demon_horn_right",
                ModelPartBuilder.create()
                        .uv(0, 0).cuboid(-4.5F, -10.25F, -2.2F, 2.0F, 3.0F, 2.0F)
                        .uv(0, 5).cuboid(-4.0F, -12.0F, -1.8F, 1.0F, 2.0F, 1.0F),
                ModelTransform.of(0.0F, 0.0F, 0.0F, -0.10F, 0.0F, 0.22F));
        head.addChild("halo",
                ModelPartBuilder.create()
                        .uv(16, 0).cuboid(-4.0F, -11.0F, -4.5F, 8.0F, 1.0F, 1.0F)
                        .uv(16, 2).cuboid(-4.0F, -11.0F, 3.5F, 8.0F, 1.0F, 1.0F)
                        .uv(16, 4).cuboid(-4.5F, -11.0F, -3.5F, 1.0F, 1.0F, 7.0F)
                        .uv(16, 6).cuboid(3.5F, -11.0F, -3.5F, 1.0F, 1.0F, 7.0F),
                ModelTransform.NONE);
        head.addChild("faceless_plate",
                ModelPartBuilder.create().uv(32, 0).cuboid(-4.0F, -8.0F, -4.03F, 8.0F, 8.0F, 0.08F),
                ModelTransform.NONE);
        head.addChild("spriggan_branch_left",
                ModelPartBuilder.create()
                        .uv(40, 0).cuboid(2.5F, -11.5F, -0.5F, 1.0F, 5.0F, 1.0F)
                        .uv(44, 0).cuboid(3.0F, -10.0F, -0.5F, 3.0F, 1.0F, 1.0F)
                        .uv(44, 2).cuboid(4.5F, -12.0F, -0.5F, 1.0F, 3.0F, 1.0F),
                ModelTransform.NONE);
        head.addChild("spriggan_branch_right",
                ModelPartBuilder.create()
                        .uv(40, 0).cuboid(-3.5F, -11.5F, -0.5F, 1.0F, 5.0F, 1.0F)
                        .uv(44, 0).cuboid(-6.0F, -10.0F, -0.5F, 3.0F, 1.0F, 1.0F)
                        .uv(44, 2).cuboid(-5.5F, -12.0F, -0.5F, 1.0F, 3.0F, 1.0F),
                ModelTransform.NONE);
        head.addChild("void_crown",
                ModelPartBuilder.create()
                        .uv(0, 8).cuboid(-4.0F, -9.5F, -4.5F, 8.0F, 1.0F, 1.0F)
                        .uv(0, 10).cuboid(-4.5F, -9.5F, -4.0F, 1.0F, 1.0F, 8.0F)
                        .uv(18, 8).cuboid(3.5F, -9.5F, -4.0F, 1.0F, 1.0F, 8.0F)
                        .uv(20, 10).cuboid(-4.0F, -9.5F, 3.5F, 8.0F, 1.0F, 1.0F)
                        .uv(0, 20).cuboid(-0.5F, -12.5F, -4.0F, 1.0F, 3.0F, 1.0F)
                        .uv(4, 20).cuboid(-4.0F, -12.0F, 0.0F, 1.0F, 2.5F, 1.0F)
                        .uv(8, 20).cuboid(3.0F, -11.5F, -1.0F, 1.0F, 2.0F, 1.0F),
                ModelTransform.NONE);

        ModelPartData body = root.addChild("body", ModelPartBuilder.create(), ModelTransform.NONE);
        body.addChild("bat_wing_left",
                ModelPartBuilder.create().uv(38, 24).cuboid(0.0F, 0.0F, 0.0F, 1.0F, 8.0F, 13.0F),
                ModelTransform.of(2.5F, 1.0F, 2.0F, 0.0F, -0.65F, -0.25F));
        body.addChild("bat_wing_right",
                ModelPartBuilder.create().uv(38, 24).cuboid(-1.0F, 0.0F, 0.0F, 1.0F, 8.0F, 13.0F),
                ModelTransform.of(-2.5F, 1.0F, 2.0F, 0.0F, 0.65F, 0.25F));

        ModelPartData demonTail = body.addChild("demon_tail_base",
                ModelPartBuilder.create().uv(44, 0).cuboid(-0.5F, 0.0F, 0.0F, 1.0F, 6.0F, 1.0F),
                ModelTransform.of(0.0F, 9.5F, 2.0F, 0.95F, 0.0F, 0.0F));
        demonTail.addChild("demon_tail_tip",
                ModelPartBuilder.create()
                        .uv(48, 0).cuboid(-0.5F, 0.0F, 0.0F, 1.0F, 5.0F, 1.0F)
                        .uv(52, 0).cuboid(-1.5F, 4.5F, -0.5F, 3.0F, 3.0F, 2.0F),
                ModelTransform.of(0.0F, 5.5F, 0.0F, 0.45F, 0.0F, 0.0F));

        return TexturedModelData.of(modelData, 64, 64);
    }

    @Override
    public void setAngles(T entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
    }

    public void prepare(PlayerEntityModel<?> parent, T entity, float limbAngle, float limbDistance, float animationProgress) {
        head.copyTransform(parent.head);
        body.copyTransform(parent.body);

        float walk = Math.min(1.0F, limbDistance);
        float batFlap = MathHelper.sin(animationProgress * 0.55F) * 0.38F + walk * 0.08F;
        batWingLeft.yaw = -0.85F - batFlap;
        batWingLeft.roll = -0.25F;
        batWingRight.yaw = 0.85F + batFlap;
        batWingRight.roll = 0.25F;

        demonTailBase.pitch = 0.90F + MathHelper.cos(animationProgress * 0.11F) * 0.08F;
        demonTailBase.yaw = MathHelper.sin(animationProgress * 0.09F) * 0.16F;
        demonTailTip.pitch = 0.35F + MathHelper.sin(animationProgress * 0.15F + 0.7F) * 0.18F;
        halo.roll = MathHelper.sin(animationProgress * 0.08F) * 0.05F;
        voidCrown.yaw = MathHelper.sin(animationProgress * 0.08F) * 0.08F;
        sprigganBranchLeft.roll = -0.08F;
        sprigganBranchRight.roll = 0.08F;
    }

    public void renderAngel(MatrixStack matrices, VertexConsumer vertices, int light, int overlay,
                            float red, float green, float blue, float alpha) {
        halo.render(matrices, vertices, light, overlay, red, green, blue, alpha);
    }

    public void renderDemon(MatrixStack matrices, VertexConsumer vertices, int light, int overlay,
                            float red, float green, float blue, float alpha) {
        demonHornLeft.render(matrices, vertices, light, overlay, red, green, blue, alpha);
        demonHornRight.render(matrices, vertices, light, overlay, red, green, blue, alpha);
        demonTailBase.render(matrices, vertices, light, overlay, red, green, blue, alpha);
    }

    public void renderWerewolf(MatrixStack matrices, VertexConsumer vertices, int light, int overlay,
                               float red, float green, float blue, float alpha) {
    }

    public void renderVampire(MatrixStack matrices, VertexConsumer vertices, int light, int overlay,
                              float red, float green, float blue, float alpha) {
        batWingLeft.render(matrices, vertices, light, overlay, red, green, blue, alpha);
        batWingRight.render(matrices, vertices, light, overlay, red, green, blue, alpha);
    }

    public void renderMermaid(MatrixStack matrices, VertexConsumer vertices, int light, int overlay,
                              float red, float green, float blue, float alpha) {
    }

    public void renderSlime(MatrixStack matrices, VertexConsumer vertices, int light, int overlay,
                            float red, float green, float blue, float alpha) {
    }

    public void renderVoid(MatrixStack matrices, VertexConsumer vertices, int light, int overlay,
                           float red, float green, float blue, float alpha) {
        voidCrown.render(matrices, vertices, light, overlay, red, green, blue, alpha);
    }

    public void renderHomunculus(MatrixStack matrices, VertexConsumer vertices, int light, int overlay,
                                 float red, float green, float blue, float alpha) {
    }

    public void renderSpriggan(MatrixStack matrices, VertexConsumer vertices, int light, int overlay,
                               float red, float green, float blue, float alpha) {
        sprigganBranchLeft.render(matrices, vertices, light, overlay, red, green, blue, alpha);
        sprigganBranchRight.render(matrices, vertices, light, overlay, red, green, blue, alpha);
    }

    public void renderDoppelganger(MatrixStack matrices, VertexConsumer vertices, int light, int overlay,
                                   float red, float green, float blue, float alpha) {
        facelessPlate.render(matrices, vertices, light, overlay, red, green, blue, alpha);
    }

    public void renderUndead(MatrixStack matrices, VertexConsumer vertices, int light, int overlay,
                             float red, float green, float blue, float alpha) {
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay,
                       float red, float green, float blue, float alpha) {
        root.render(matrices, vertices, light, overlay, red, green, blue, alpha);
    }
}
