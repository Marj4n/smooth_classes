package org.marj4n.smooth_classes.mixin.client;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import org.marj4n.smooth_classes.client.origin.demon.DemonVisualModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keep vanilla material/dye/trim/glint selection, fit its draw to the Demon rig. */
@Mixin(ArmorFeatureRenderer.class)
public abstract class DemonArmorMixin {
    @Unique private AbstractClientPlayerEntity demon$player;
    @Unique private EquipmentSlot demon$slot;
    @Inject(method="renderArmor",at=@At("HEAD"))
    private void demon$begin(MatrixStack matrices,VertexConsumerProvider buffers,LivingEntity entity,EquipmentSlot slot,int light,BipedEntityModel<?> model,CallbackInfo ci){demon$player=entity instanceof AbstractClientPlayerEntity p&&DemonVisualModel.applies(p)?p:null;demon$slot=slot;}
    @Inject(method="renderArmor",at=@At("RETURN"))
    private void demon$end(MatrixStack matrices,VertexConsumerProvider buffers,LivingEntity entity,EquipmentSlot slot,int light,BipedEntityModel<?> model,CallbackInfo ci){demon$player=null;demon$slot=null;}
    @Redirect(method={"renderArmorParts","renderTrim","renderGlint"},at=@At(value="INVOKE",target="Lnet/minecraft/client/render/entity/model/BipedEntityModel;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;IIFFFF)V"))
    private void demon$draw(BipedEntityModel<?> model,MatrixStack matrices,VertexConsumer vertices,int light,int overlay,float red,float green,float blue,float alpha){
        if(demon$player!=null&&DemonVisualModel.renderArmor(demon$player,demon$slot,matrices,vertices,light,overlay,red,green,blue,alpha))return;
        model.render(matrices,vertices,light,overlay,red,green,blue,alpha);
    }
}
