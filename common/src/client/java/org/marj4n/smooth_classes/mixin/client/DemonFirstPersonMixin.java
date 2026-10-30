package org.marj4n.smooth_classes.mixin.client;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.marj4n.smooth_classes.client.origin.demon.DemonVisualModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Replace only the arm mesh; HeldItemRenderer retains item/equip/use transforms. */
@Mixin(PlayerEntityRenderer.class)
public abstract class DemonFirstPersonMixin {
    @Inject(method="renderRightArm",at=@At("HEAD"),cancellable=true)
    private void demon$right(MatrixStack matrices,VertexConsumerProvider buffers,int light,AbstractClientPlayerEntity player,CallbackInfo ci){if(DemonVisualModel.applies(player)&&!org.marj4n.smooth_classes.origin.demon.DemonBodyGeometry.applies(player)){DemonVisualModel.renderArm(player,((PlayerEntityRenderer)(Object)this).getModel(),matrices,buffers,light,false);ci.cancel();}}
    @Inject(method="renderLeftArm",at=@At("HEAD"),cancellable=true)
    private void demon$left(MatrixStack matrices,VertexConsumerProvider buffers,int light,AbstractClientPlayerEntity player,CallbackInfo ci){if(DemonVisualModel.applies(player)&&!org.marj4n.smooth_classes.origin.demon.DemonBodyGeometry.applies(player)){DemonVisualModel.renderArm(player,((PlayerEntityRenderer)(Object)this).getModel(),matrices,buffers,light,true);ci.cancel();}}
}
