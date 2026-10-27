package org.marj4n.smooth_classes.mixin.client;

import org.marj4n.smooth_classes.client.StealthVisualState;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** stealth hides the body and first-person hands, including remote players. */
@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerStealthRendererMixin {
    @Inject(method = "renderArm(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/network/AbstractClientPlayerEntity;Lnet/minecraft/client/model/ModelPart;Lnet/minecraft/client/model/ModelPart;)V", at = @At("HEAD"), cancellable = true)
    private void smooth_classes$renderArm(MatrixStack matrices, VertexConsumerProvider consumers,
            int light, AbstractClientPlayerEntity player, ModelPart arm, ModelPart sleeve, CallbackInfo ci) {
        if (player != null && StealthVisualState.active(player)) ci.cancel();
    }

    @Inject(method = "render(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At("HEAD"), cancellable = true)
    private void smooth_classes$render(AbstractClientPlayerEntity player, float yaw, float tickDelta,
            MatrixStack matrices, VertexConsumerProvider consumers, int light, CallbackInfo ci) {
        if (player != null && StealthVisualState.active(player)) ci.cancel();
    }
}
