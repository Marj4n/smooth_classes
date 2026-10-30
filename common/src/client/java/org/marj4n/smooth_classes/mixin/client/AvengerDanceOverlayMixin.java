package org.marj4n.smooth_classes.mixin.client;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.marj4n.smooth_classes.client.avenger.dance.AvengerDanceClient;
/** Drowned/Stray and other original vanilla secondary meshes. Root matrix is already inherited. */
@Mixin(FeatureRenderer.class)
public abstract class AvengerDanceOverlayMixin {
    @Inject(method="render(Lnet/minecraft/client/render/entity/model/EntityModel;Lnet/minecraft/client/render/entity/model/EntityModel;Lnet/minecraft/util/Identifier;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/entity/LivingEntity;FFFFFFFFF)V",at=@At(value="INVOKE",target="Lnet/minecraft/client/render/entity/model/EntityModel;setAngles(Lnet/minecraft/entity/Entity;FFFFF)V",shift=At.Shift.AFTER))
    private static void pose(EntityModel<?> context,EntityModel<?> model,Identifier texture,MatrixStack matrices,VertexConsumerProvider vertices,int light,LivingEntity e,float a,float b,float age,float yaw,float pitch,float delta,float red,float green,float blue,CallbackInfo ci){if(context!=model)AvengerDanceClient.applyLayer(model,e,delta,matrices);}
    @Inject(method="render(Lnet/minecraft/client/render/entity/model/EntityModel;Lnet/minecraft/client/render/entity/model/EntityModel;Lnet/minecraft/util/Identifier;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/entity/LivingEntity;FFFFFFFFF)V",at=@At("RETURN"))
    private static void restore(EntityModel<?> context,EntityModel<?> model,Identifier texture,MatrixStack matrices,VertexConsumerProvider vertices,int light,LivingEntity e,float a,float b,float age,float yaw,float pitch,float delta,float red,float green,float blue,CallbackInfo ci){if(context!=model)AvengerDanceClient.finishLayer(model,matrices);}
}
