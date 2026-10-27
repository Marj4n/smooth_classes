package org.marj4n.smooth_classes.client.renderer;

import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.marj4n.smooth_classes.entity.LancerImpaleEntity;

/** Renders the exact spear ItemStack held by the Lancer instead of vanilla trident geometry. */
public final class LancerImpaleRenderer extends EntityRenderer<LancerImpaleEntity> {
    private final ItemRenderer itemRenderer;

    public LancerImpaleRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
        this.shadowRadius = 0F;
    }

    @Override
    public void render(
            LancerImpaleEntity entity,
            float yaw,
            float tickDelta,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light
    ) {
        ItemStack stack = entity.getVisualStack();
        if (stack.isEmpty()) return;

        matrices.push();

        if (entity.isGroundSpike()) {
            // Rise from below the ground, linger briefly, then get pulled back down instead of popping away.
            float age = entity.age + tickDelta;
            float rise = Math.min(1.0F, age / 6.0F);
            float sink = age <= 22.0F ? 0.0F : Math.min(1.0F, (age - 22.0F) / 12.0F);
            float riseEase = 1.0F - (1.0F - rise) * (1.0F - rise);
            float sinkEase = sink * sink;
            // Peak center sits ~0.75 block above the surface; a normal spear model then
            // reaches roughly 1.5 blocks above the ground instead of barely peeking out.
            float height = -0.55F + (riseEase * 2.35F) - (sinkEase * 2.35F);
            matrices.translate(0.0D, height, 0.0D);
        }

        // Match projectile-style orientation first, then use the item's own baked model.
        // This keeps custom spear/lance geometry recognisable while aiming the shaft into the victim.
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(entity.getYaw() - 90.0F));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(entity.getPitch() + 90.0F));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90.0F));
        matrices.scale(1.00F, 1.00F, 1.00F);

        itemRenderer.renderItem(
                stack,
                ModelTransformationMode.FIXED,
                entity.isGroundSpike() ? 0xF000F0 : light,
                OverlayTexture.DEFAULT_UV,
                matrices,
                vertexConsumers,
                entity.getWorld(),
                entity.getId()
        );

        matrices.pop();
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    @Override
    public Identifier getTexture(LancerImpaleEntity entity) {
        return SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE;
    }
}
