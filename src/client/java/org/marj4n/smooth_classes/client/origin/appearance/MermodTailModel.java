package org.marj4n.smooth_classes.client.origin.appearance;

import net.minecraft.client.model.*;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;

/**
 * Internal Siren tail port based on Mermod's 1.20.1 model proportions and wave chain.
 * Self-contained: Mermod is not required at runtime.
 */
public final class MermodTailModel<T extends PlayerEntity> extends EntityModel<T> {
    private final ModelPart root;
    private final ModelPart main;
    private final ModelPart waist;
    private final ModelPart tail1, tail2, tail3, tail4, tail5, tail6, tail7, fin;
    private final ModelPart[] chain;

    public MermodTailModel(ModelPart root) {
        this.root = root;
        this.main = root.getChild("main");
        this.waist = main.getChild("waist");
        this.tail1 = waist.getChild("tail1");
        this.tail2 = tail1.getChild("tail2");
        this.tail3 = tail2.getChild("tail3");
        this.tail4 = tail3.getChild("tail4");
        this.tail5 = tail4.getChild("tail5");
        this.tail6 = tail5.getChild("tail6");
        this.tail7 = tail6.getChild("tail7");
        this.fin = tail7.getChild("fin");
        this.chain = new ModelPart[]{tail1, tail2, tail3, tail4, tail5, tail6, tail7};
    }

    public static TexturedModelData createData() {
        ModelData data = new ModelData();
        ModelPartData root = data.getRoot();

        // Mermod Siren base dimensions.
        ModelPartData main = root.addChild("main", ModelPartBuilder.create(), ModelTransform.NONE);
        ModelPartData waist = main.addChild("waist", ModelPartBuilder.create().uv(24, 0)
                .cuboid(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, new Dilation(0.10F)), ModelTransform.NONE);
        addSideFins(waist, 4.0F, 8.0F, 5.0F, 4.0F, 24, 40);

        ModelPartData t1 = waist.addChild("tail1", ModelPartBuilder.create().uv(0, 0)
                .cuboid(-4.0F, 0.0F, -2.0F, 8.0F, 3.0F, 4.0F), ModelTransform.pivot(0.0F, 12.0F, 0.0F));
        addSideFins(t1, 4.0F, 0.0F, 5.0F, 3.0F, 24, 44);

        ModelPartData t2 = t1.addChild("tail2", ModelPartBuilder.create().uv(0, 7)
                .cuboid(-3.5F, 0.0F, -2.0F, 7.0F, 3.0F, 4.0F, new Dilation(-0.10F)), ModelTransform.pivot(0.0F, 3.0F, 0.0F));
        addSideFins(t2, 3.0F, 0.0F, 6.0F, 3.0F, 24, 47);

        ModelPartData t3 = t2.addChild("tail3", ModelPartBuilder.create().uv(0, 14)
                .cuboid(-3.0F, 0.0F, -1.5F, 6.0F, 3.0F, 3.0F), ModelTransform.pivot(0.0F, 3.0F, 0.0F));
        addSideFins(t3, 3.0F, 0.0F, 6.0F, 3.0F, 24, 50);

        ModelPartData t4 = t3.addChild("tail4", ModelPartBuilder.create().uv(0, 20)
                .cuboid(-2.5F, 0.0F, -1.5F, 5.0F, 3.0F, 3.0F, new Dilation(-0.08F)), ModelTransform.pivot(0.0F, 3.0F, 0.0F));
        addSideFins(t4, 2.0F, 0.0F, 7.0F, 3.0F, 24, 53);

        ModelPartData t5 = t4.addChild("tail5", ModelPartBuilder.create().uv(0, 26)
                .cuboid(-2.0F, 0.0F, -1.0F, 4.0F, 3.0F, 2.0F), ModelTransform.pivot(0.0F, 3.0F, 0.0F));
        addSideFins(t5, 2.0F, 0.0F, 7.0F, 3.0F, 24, 56);

        ModelPartData t6 = t5.addChild("tail6", ModelPartBuilder.create().uv(0, 31)
                .cuboid(-2.0F, 0.0F, -1.0F, 4.0F, 3.0F, 2.0F, new Dilation(-0.08F)), ModelTransform.pivot(0.0F, 3.0F, 0.0F));
        addSideFins(t6, 1.5F, 0.0F, 8.0F, 3.0F, 32, 40);

        ModelPartData t7 = t6.addChild("tail7", ModelPartBuilder.create().uv(0, 36)
                .cuboid(-1.5F, 0.0F, -0.5F, 3.0F, 3.0F, 1.0F), ModelTransform.pivot(0.0F, 3.0F, 0.0F));
        addSideFins(t7, 1.5F, 0.0F, 8.0F, 6.0F, 32, 43);

        // Mermod Siren fin is a broad flat 23x24 sheet. A tiny depth avoids z-fighting
        // while keeping the silhouette identical.
        t7.addChild("fin", ModelPartBuilder.create().uv(0, 40)
                .cuboid(-11.5F, 0.0F, 0.0F, 23.0F, 24.0F, 0.0F), ModelTransform.pivot(0.0F, 3.0F, 0.0F));

        return TexturedModelData.of(data, 48, 64);
    }

    private static void addSideFins(ModelPartData part, float halfWidth, float y, float length, float height, int u, int v) {
        part.addChild("fin_left_" + v, ModelPartBuilder.create().uv(u, v)
                        .cuboid(halfWidth, y, 0.0F, length, height, 0.0F),
                ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.18F));
        part.addChild("fin_right_" + v, ModelPartBuilder.create().uv(u, v)
                        .cuboid(-halfWidth - length, y, 0.0F, length, height, 0.0F),
                ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.18F));
    }

    public void prepare(PlayerEntityModel<?> parent, T player, float limbAngle, float limbDistance, float animationProgress) {
        main.copyTransform(parent.body);
        resetPose();

        final boolean swimming = player.isSwimming();
        final boolean submerged = player.isSubmergedInWater();
        final boolean wet = player.isTouchingWaterOrRain() || submerged;
        final float speed = (float) player.getVelocity().horizontalLength();

        if (swimming || submerged) {
            // Mermod-like swim pose: tail leaves the waist horizontally behind the player.
            tail1.pitch = -MathHelper.HALF_PI;
            for (int i = 1; i < chain.length; i++) chain[i].pitch = 0.034906585F;

            float motion = MathHelper.clamp(speed * 7.0F, 0.35F, 1.25F);
            float phase = animationProgress * (0.055F + 0.025F * motion);
            animatePitchWave(phase, 0.05235988F + 0.045F * motion);
            animateYawWave(phase + 0.18F, 0.08F + 0.11F * motion);

            // The fluke follows the last segment instead of acting like a rigid shovel.
            fin.pitch = 0.05235988F + tail7.pitch * 1.75F;
            fin.yaw = tail7.yaw * 1.55F;
            fin.roll = MathHelper.sin(animationProgress * 0.09F) * 0.04F;
        } else if (wet) {
            // Rain / shallow water: keep the tail downward but alive rather than snapping horizontal.
            for (int i = 0; i < chain.length; i++) chain[i].pitch = 0.18F;
            tail6.pitch = 0.10F;
            animatePitchWave(animationProgress * 0.025F, 0.07F);
            animateYawWave(animationProgress * 0.025F, 0.04F);
            fin.pitch = tail7.pitch * 1.8F;
        } else {
            // This branch is normally invisible because dry Mermaid restores human legs,
            // but keeping a sane pose prevents a one-frame pop while crossing water edges.
            for (int i = 0; i < chain.length; i++) chain[i].pitch = 0.18F;
            fin.pitch = 0.26F;
        }
    }

    private void resetPose() {
        for (ModelPart part : chain) {
            part.pitch = 0.0F;
            part.yaw = 0.0F;
            part.roll = 0.0F;
        }
        fin.pitch = fin.yaw = fin.roll = 0.0F;
    }

    private void animatePitchWave(float phase, float amplitude) {
        for (int i = 0; i < chain.length; i++) {
            chain[i].pitch += wave(phase - 0.10F * i, 1.0F, amplitude);
        }
    }

    private void animateYawWave(float phase, float amplitude) {
        for (int i = 0; i < chain.length; i++) {
            // Delayed S-curve along the chain. The tip moves more than the waist.
            float gain = 0.45F + (i / 6.0F) * 0.75F;
            chain[i].yaw = wave(phase - 0.11F * i, 1.0F, amplitude * gain);
        }
    }

    private static float wave(float time, float frequency, float amplitude) {
        return MathHelper.sin((float) (Math.PI * 2.0) * time * frequency) * amplitude;
    }

    @Override
    public void setAngles(T entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay,
                       float red, float green, float blue, float alpha) {
        main.render(matrices, vertices, light, overlay, red, green, blue, alpha);
    }
}
