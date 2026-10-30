package org.marj4n.smooth_classes.mixin.client;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import org.marj4n.smooth_classes.client.origin.demon.DemonVisualModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Suppress the base draw at its call site, including modded model overrides.
 * Pose calculations and equipment feature renderers still run normally. */
@Mixin(LivingEntityRenderer.class)
public abstract class DemonBaseBodyMixin {
    @org.spongepowered.asm.mixin.Unique private boolean demon$loggedBaseSuppression;
    @Redirect(method="render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
        at=@At(value="INVOKE",target="Lnet/minecraft/client/render/entity/model/EntityModel;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;IIFFFF)V"))
    private void demon$baseDraw(EntityModel<?> model,MatrixStack matrices,VertexConsumer vertices,
            int light,int overlay,float red,float green,float blue,float alpha,
            LivingEntity entity,float yaw,float tickDelta,MatrixStack originalMatrices,
            VertexConsumerProvider buffers,int packedLight){
        if(entity instanceof AbstractClientPlayerEntity player&&DemonVisualModel.applies(player)){
            if(Boolean.getBoolean("smooth_classes.demonTest")&&!demon$loggedBaseSuppression){demon$loggedBaseSuppression=true;org.marj4n.smooth_classes.SmoothClasses.LOGGER.info("DEMON_BASE_SUPPRESSED model={}",model.getClass().getName());}
            return;
        }
        model.render(matrices,vertices,light,overlay,red,green,blue,alpha);
    }
}
