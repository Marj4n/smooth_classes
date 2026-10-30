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
/** Forge Mixin AP erases the intersection-bound T to L/;. Use the exact SRG
 * descriptor verified against 47.4.0 EnergySwirlLayer bytecode instead. */
@Mixin(EnergySwirlOverlayFeatureRenderer.class)
public abstract class ForgeAvengerDanceEnergyOverlayMixin {
    @Shadow protected abstract EntityModel<?> getEnergySwirlModel();
    @SuppressWarnings("target") // AP cannot resolve SRG names in its named generic stub; build validates the runtime binary.
    @Dynamic("Forge SRG runtime binding; checked against the target bytecode")
    @Inject(method="m_6494_(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/Entity;FFFFFF)V",remap=false,at=@At(value="INVOKE",target="Lnet/minecraft/client/model/EntityModel;m_6973_(Lnet/minecraft/world/entity/Entity;FFFFF)V",remap=false,shift=At.Shift.AFTER))
    private void pose(MatrixStack m,VertexConsumerProvider v,int light,Entity e,float a,float b,float delta,float age,float yaw,float pitch,CallbackInfo ci){if(e instanceof LivingEntity l)AvengerDanceClient.applyLayer(getEnergySwirlModel(),l,delta,m);}
    @SuppressWarnings("target") // AP cannot resolve SRG names in its named generic stub; build validates the runtime binary.
    @Dynamic("Forge SRG runtime binding; checked against the target bytecode")
    @Inject(method="m_6494_(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/Entity;FFFFFF)V",remap=false,at=@At("RETURN"))
    private void restore(MatrixStack m,VertexConsumerProvider v,int light,Entity e,float a,float b,float delta,float age,float yaw,float pitch,CallbackInfo ci){AvengerDanceClient.finishLayer(getEnergySwirlModel(),m);}
}
