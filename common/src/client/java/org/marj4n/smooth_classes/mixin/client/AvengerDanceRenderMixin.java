package org.marj4n.smooth_classes.mixin.client;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.marj4n.smooth_classes.client.avenger.dance.AvengerDanceClient;
@Mixin(LivingEntityRenderer.class)
public abstract class AvengerDanceRenderMixin {
    @Shadow protected EntityModel<?> model;
    @Unique private boolean smoothClasses$danceApplied;
    @Inject(method="render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",at=@At("HEAD"))
    private void danceBegin(LivingEntity e,float yaw,float delta,MatrixStack m,VertexConsumerProvider v,int light,CallbackInfo ci){AvengerDanceClient.restore(model);smoothClasses$danceApplied=false;}
    @Inject(method="render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",at=@At(value="INVOKE",target="Lnet/minecraft/client/render/entity/model/EntityModel;setAngles(Lnet/minecraft/entity/Entity;FFFFF)V",shift=At.Shift.AFTER))
    private void dancePose(LivingEntity e,float yaw,float delta,MatrixStack m,VertexConsumerProvider v,int light,CallbackInfo ci){smoothClasses$danceApplied=AvengerDanceClient.apply(model,e,delta,m);}
    @Inject(method="render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",at=@At(value="INVOKE",target="Lnet/minecraft/client/util/math/MatrixStack;pop()V",shift=At.Shift.BEFORE))
    private void beforeVanillaPop(LivingEntity e,float yaw,float delta,MatrixStack m,VertexConsumerProvider v,int light,CallbackInfo ci){if(smoothClasses$danceApplied){AvengerDanceClient.restore(model);m.pop();smoothClasses$danceApplied=false;}}
    @Inject(method="render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",at=@At("RETURN"))
    private void danceEnd(LivingEntity e,float yaw,float delta,MatrixStack m,VertexConsumerProvider v,int light,CallbackInfo ci){if(smoothClasses$danceApplied){AvengerDanceClient.restore(model);m.pop();smoothClasses$danceApplied=false;}}
}
