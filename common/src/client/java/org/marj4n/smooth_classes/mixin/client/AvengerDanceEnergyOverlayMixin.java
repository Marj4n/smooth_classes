package org.marj4n.smooth_classes.mixin.client;
import net.minecraft.client.render.entity.feature.EnergySwirlOverlayFeatureRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.marj4n.smooth_classes.client.avenger.dance.AvengerDanceClient;
/** Preserve vanilla charged Wither/Creeper energy shells on the same animated joints. */
@Mixin(EnergySwirlOverlayFeatureRenderer.class)
public abstract class AvengerDanceEnergyOverlayMixin {
    @Shadow protected abstract EntityModel<?> getEnergySwirlModel();
    @Inject(method="render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/entity/Entity;FFFFFF)V",at=@At(value="INVOKE",target="Lnet/minecraft/client/render/entity/model/EntityModel;setAngles(Lnet/minecraft/entity/Entity;FFFFF)V",shift=At.Shift.AFTER))
    private void pose(MatrixStack m,VertexConsumerProvider v,int light,Entity e,float a,float b,float delta,float age,float yaw,float pitch,CallbackInfo ci){if(e instanceof LivingEntity l)AvengerDanceClient.applyLayer(getEnergySwirlModel(),l,delta,m);}
    @Inject(method="render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/entity/Entity;FFFFFF)V",at=@At("RETURN"))
    private void restore(MatrixStack m,VertexConsumerProvider v,int light,Entity e,float a,float b,float delta,float age,float yaw,float pitch,CallbackInfo ci){AvengerDanceClient.finishLayer(getEnergySwirlModel(),m);}
}
