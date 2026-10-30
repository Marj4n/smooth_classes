package org.marj4n.smooth_classes.mixin.client;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.marj4n.smooth_classes.client.origin.demon.ForbiddenBirthClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(PlayerEntityRenderer.class)
public abstract class ForbiddenBirthPlayerMixin {
    @Inject(method="render(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",at=@At("HEAD"),cancellable=true)
    private void forbidden$hide(AbstractClientPlayerEntity p,float yaw,float partial,MatrixStack m,VertexConsumerProvider v,int light,CallbackInfo ci){if(ForbiddenBirthClient.hidden(p.getUuid()))ci.cancel();}
    @Inject(method="scale(Lnet/minecraft/client/network/AbstractClientPlayerEntity;Lnet/minecraft/client/util/math/MatrixStack;F)V",at=@At("TAIL"))
    private void forbidden$emerge(AbstractClientPlayerEntity p,MatrixStack m,float partial,CallbackInfo ci){float scale=ForbiddenBirthClient.scale(p.getUuid(),partial);if(scale!=1)m.scale(Math.max(.001f,scale),Math.max(.001f,scale),Math.max(.001f,scale));}
}
