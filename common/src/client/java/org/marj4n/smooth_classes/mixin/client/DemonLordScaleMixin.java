package org.marj4n.smooth_classes.mixin.client;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.marj4n.smooth_classes.origin.demon.DemonBodyGeometry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(PlayerEntityRenderer.class)
public abstract class DemonLordScaleMixin {
    @Inject(method="scale(Lnet/minecraft/client/network/AbstractClientPlayerEntity;Lnet/minecraft/client/util/math/MatrixStack;F)V",at=@At("TAIL"))
    private void demon$scale(AbstractClientPlayerEntity p,MatrixStack matrices,float tickDelta,CallbackInfo ci){if(DemonBodyGeometry.applies(p)){float s=DemonBodyGeometry.RENDER_MULTIPLIER;matrices.scale(s,s,s);}}
}
