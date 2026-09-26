package org.marj4n.smooth_classes.mixin.client;

import org.marj4n.smooth_classes.client.StealthVisualState;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Armor follows the same stealth state as the player model. */
@Mixin(ArmorFeatureRenderer.class)
public abstract class ArmorStealthRendererMixin {
    @Inject(method = "renderArmor", at = @At("HEAD"), cancellable = true)
    private void smooth_classes$renderArmor(MatrixStack matrices, VertexConsumerProvider consumers,
            LivingEntity entity, EquipmentSlot slot, int light, BipedEntityModel<LivingEntity> model,
            CallbackInfo ci) {
        if (entity.isInvisible() && StealthVisualState.active(entity)) ci.cancel();
    }
}
