package org.marj4n.smooth_classes.mixin.client;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.RenderLayer;
import net.spell_engine.api.render.CustomModels;
import net.spell_engine.mixin.client.render.ItemRendererAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Last-resort render safety for Spell Engine 1.10.x.
 *
 * CustomModels.render(...) can reach renderModel(...) with a null BakedModel
 * when a third-party/custom model was not baked. Vanilla ItemRenderer then
 * dereferences that null model and crashes the render thread. Missing visuals
 * must never be allowed to force-close the client, so skip that single model.
 */
@Mixin(value = CustomModels.class, remap = false)
public abstract class CustomModelsMixin {

    @Inject(method = "renderModel", at = @At("HEAD"), cancellable = true)
    private static void smooth_classes$skipMissingBakedModel(
            RenderLayer renderLayer,
            ItemRendererAccessor itemRenderer,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            BakedModel model,
            CallbackInfo ci
    ) {
        if (model == null) {
            ci.cancel();
        }
    }
}
