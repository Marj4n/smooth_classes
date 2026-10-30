package org.marj4n.smooth_classes.mixin.client;
import net.minecraft.client.render.entity.feature.HorseArmorFeatureRenderer;
import net.minecraft.client.render.entity.model.HorseEntityModel;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.passive.HorseEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.marj4n.smooth_classes.client.avenger.dance.AvengerDanceClient;
@Mixin(HorseArmorFeatureRenderer.class)
public abstract class AvengerDanceHorseArmorMixin {
    @Shadow @Final private HorseEntityModel<HorseEntity> model;
    @Inject(method="render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/entity/passive/HorseEntity;FFFFFF)V",at=@At(value="INVOKE",target="Lnet/minecraft/client/render/entity/model/HorseEntityModel;setAngles(Lnet/minecraft/entity/passive/AbstractHorseEntity;FFFFF)V",shift=At.Shift.AFTER))
    private void pose(MatrixStack m,VertexConsumerProvider v,int light,HorseEntity e,float a,float b,float delta,float age,float yaw,float pitch,CallbackInfo ci){AvengerDanceClient.applyLayer(model,e,delta,m);}
    @Inject(method="render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/entity/passive/HorseEntity;FFFFFF)V",at=@At("RETURN"))
    private void restore(MatrixStack m,VertexConsumerProvider v,int light,HorseEntity e,float a,float b,float delta,float age,float yaw,float pitch,CallbackInfo ci){AvengerDanceClient.finishLayer(model,m);}
}
