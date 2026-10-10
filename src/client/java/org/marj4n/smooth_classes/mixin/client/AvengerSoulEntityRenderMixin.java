package org.marj4n.smooth_classes.mixin.client;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import org.marj4n.smooth_classes.client.effects.AvengerSoulAnimationClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The ritual modifies only the renderer matrix; real mob coordinates stay safe. */
@Mixin(LivingEntityRenderer.class)
public abstract class AvengerSoulEntityRenderMixin {
    @Inject(method = "render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At("HEAD"))
    private void smooth_classes$ritualBegin(LivingEntity entity, float yaw, float tickDelta,
            MatrixStack matrices, VertexConsumerProvider consumers, int light, CallbackInfo ci) {
        matrices.push();
        AvengerSoulAnimationClient.transform(entity, tickDelta, matrices);
    }

    @Inject(method = "render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At("RETURN"))
    private void smooth_classes$ritualEnd(LivingEntity entity, float yaw, float tickDelta,
            MatrixStack matrices, VertexConsumerProvider consumers, int light, CallbackInfo ci) {
        matrices.pop();
    }
}
