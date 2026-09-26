package org.marj4n.smooth_classes.client.effects;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.spell_engine.api.effect.CustomModelStatusEffect;
import net.spell_engine.api.render.CustomLayers;
import net.spell_engine.api.render.CustomModels;
import net.spell_engine.api.render.LightEmission;
import org.marj4n.smooth_classes.SmoothClasses;


public class UndyingRenderer implements CustomModelStatusEffect.Renderer {
    public static final Identifier modelId_base = new Identifier(SmoothClasses.MOD_ID, "effect/undying");
    public static final Identifier modelId_overlay = new Identifier(SmoothClasses.MOD_ID, "effect/undying_glow");
    public static final Identifier modelId_barrier = new Identifier(SmoothClasses.MOD_ID, "effect/undying_barrier");

    private static final RenderLayer BASE_RENDER_LAYER =
            RenderLayer.getEntityTranslucent(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE);
    private static final RenderLayer OVERLAY_RENDER_LAYER =
            CustomLayers.spellEffect(LightEmission.GLOW, true);


    @Override
    public void renderEffect(long startTime, int amplifier, LivingEntity livingEntity, float delta, MatrixStack matrixStack, VertexConsumerProvider vertexConsumers, int light) {
        float yOffset = 0.68F;
        float proneOffset = 0.30F; // Things get weird when prone, and render code hurts my head
        boolean prone = livingEntity.isFallFlying() || livingEntity.isCrawling() || livingEntity.isInSwimmingPose();
        matrixStack.push();

        matrixStack.translate(0, yOffset, 0);
        if (prone)
            matrixStack.translate(0, -proneOffset, 0);

        // Apply rotation to match the entity's orientation
        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-livingEntity.bodyYaw));
        if (prone)
            matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(livingEntity.getPitch() + 90));

        CustomModels.render(BASE_RENDER_LAYER, MinecraftClient.getInstance().getItemRenderer(), modelId_base,
                matrixStack, vertexConsumers, light, livingEntity.getId());
        matrixStack.pop();

        float overlayScale = 1.05F;
        matrixStack.push();
        matrixStack.translate(0, yOffset, 0);
        if (prone)
            matrixStack.translate(0, -proneOffset, 0);
        matrixStack.scale(overlayScale, overlayScale, overlayScale);

        // Apply rotation to match the entity's orientation
        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-livingEntity.bodyYaw));
        if (prone)
            matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(livingEntity.getPitch() + 90));

        CustomModels.render(OVERLAY_RENDER_LAYER, MinecraftClient.getInstance().getItemRenderer(), modelId_overlay,
                matrixStack, vertexConsumers, light, livingEntity.getId());
        matrixStack.pop();

        float shieldWidth=Math.max(1.1F,livingEntity.getWidth()*1.5F);
        float shieldHeight=(livingEntity.getHeight()+.15F)/1.94F;
        matrixStack.push();
        matrixStack.translate(0,livingEntity.getHeight()*.37F,0);
        matrixStack.scale(shieldWidth,shieldHeight,shieldWidth);
        CustomModels.render(BASE_RENDER_LAYER,MinecraftClient.getInstance().getItemRenderer(),modelId_barrier,
                matrixStack,vertexConsumers,light,livingEntity.getId());
        matrixStack.pop();
    }
}
