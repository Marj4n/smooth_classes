package org.marj4n.smooth_classes.client.renderer;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

/** Rendering policy shared by the bound floating minions; geometry is supplied separately. */
public final class SummonedMobRenderer<T extends MobEntity>
        extends MobEntityRenderer<T, EntityModel<T>> {
    private final Identifier texture;
    private final float size;
    private final boolean followLook;

    public SummonedMobRenderer(EntityRendererFactory.Context context, EntityModel<T> model,
                               Identifier texture, float size, boolean followLook) {
        super(context, model, 1F);
        this.texture = texture;
        this.size = size;
        this.followLook = followLook;
    }

    @Override
    public Identifier getTexture(T entity) {
        return texture;
    }

    @Override
    protected void scale(T entity, MatrixStack matrices, float delta) {
        if (size != 1F) matrices.scale(size, size, size);
    }

    @Override
    public void render(T entity, float yaw, float delta, MatrixStack matrices,
                       VertexConsumerProvider consumers, int light) {
        if (!followLook) {
            super.render(entity, yaw, delta, matrices, consumers, light);
            return;
        }
        matrices.push();
        try {
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(entity.getPitch(delta)));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(entity.getYaw(delta)));
            super.render(entity, yaw, delta, matrices, consumers, light);
        } finally {
            matrices.pop();
        }
    }
}
