package org.marj4n.smooth_classes.mixin.client;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.HeldItemFeatureRenderer;
import net.minecraft.client.render.entity.model.ModelWithArms;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import org.marj4n.smooth_classes.client.origin.demon.DemonVisualModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
/** Keep vanilla item rendering but attach it to the same frame's real Demon wrist. */
@Mixin(HeldItemFeatureRenderer.class)
public abstract class DemonHeldItemMixin {
    @Redirect(method="renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;Lnet/minecraft/util/Arm;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
        at=@At(value="INVOKE",target="Lnet/minecraft/client/render/entity/model/ModelWithArms;setArmAngle(Lnet/minecraft/util/Arm;Lnet/minecraft/client/util/math/MatrixStack;)V"))
    private void demon$wrist(ModelWithArms model,Arm arm,MatrixStack matrices,LivingEntity entity,ItemStack stack,ModelTransformationMode mode,Arm enclosingArm,MatrixStack enclosingMatrices,VertexConsumerProvider buffers,int light){
        if(entity instanceof AbstractClientPlayerEntity player&&DemonVisualModel.applyHeldArm(player,arm,matrices))return;
        model.setArmAngle(arm,matrices);
    }
}
