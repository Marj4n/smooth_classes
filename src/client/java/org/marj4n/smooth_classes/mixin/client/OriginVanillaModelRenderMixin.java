package org.marj4n.smooth_classes.mixin.client;

import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.AnimalModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import org.marj4n.smooth_classes.client.origin.appearance.OriginVanillaBodyGuard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * AnimalModel owns the actual PlayerEntityModel.render implementation in 1.20.1.
 * Suppress only vanilla player-body draw calls, never pose calculation, armor
 * BipedEntityModels, the animated Man-Bat mesh or other creatures' models.
 */
@Mixin(AnimalModel.class)
public abstract class OriginVanillaModelRenderMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void smooth_classes$preventVanillaBodyUnderReplacement(MatrixStack matrices,
            VertexConsumer vertices, int light, int overlay, float red, float green,
            float blue, float alpha, CallbackInfo ci) {
        if ((Object) this instanceof PlayerEntityModel<?>
                && OriginVanillaBodyGuard.shouldSuppressVanillaPlayerGeometry()) {
            ci.cancel();
        }
    }
}
