package org.marj4n.smooth_classes.mixin.client;
import net.minecraft.client.render.entity.feature.WardenFeatureRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.mob.WardenEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.marj4n.smooth_classes.client.avenger.dance.AvengerDanceClient;
/** Modulate the EXISTING emissive heart alpha; heartbeat timing/data and texture stay vanilla. */
@Mixin(WardenFeatureRenderer.class)
public abstract class AvengerDanceWardenGlowMixin {
    @Shadow @Final private Identifier texture;
    @Unique private WardenEntity smoothClasses$danceWarden;
    @Unique private float smoothClasses$danceDelta;
    @Inject(method="render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/entity/mob/WardenEntity;FFFFFF)V",at=@At("HEAD"))
    private void begin(MatrixStack m,VertexConsumerProvider v,int light,WardenEntity e,float a,float b,float delta,float age,float yaw,float pitch,CallbackInfo ci){smoothClasses$danceWarden=e;smoothClasses$danceDelta=delta;}
    @ModifyArg(method="render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/entity/mob/WardenEntity;FFFFFF)V",at=@At(value="INVOKE",target="Lnet/minecraft/client/render/entity/model/WardenEntityModel;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;IIFFFF)V"),index=7)
    private float pulse(float alpha){var e=smoothClasses$danceWarden;if(e==null||!texture.getPath().contains("heart"))return alpha;var p=AvengerDanceClient.playback(e);if(p==null)return alpha;double phase=(e.getWorld().getTime()+smoothClasses$danceDelta-p.start())/10d;return alpha*(float)(.9+.1*Math.cos(phase*Math.PI*2));}
    @Inject(method="render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/entity/mob/WardenEntity;FFFFFF)V",at=@At("RETURN"))
    private void end(MatrixStack m,VertexConsumerProvider v,int light,WardenEntity e,float a,float b,float delta,float age,float yaw,float pitch,CallbackInfo ci){smoothClasses$danceWarden=null;}
}
