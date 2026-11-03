package org.marj4n.smooth_classes.mixin.client;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.model.Model;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import org.marj4n.smooth_classes.client.origin.demon.DemonVisualModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Forge delegates material, trim and glint draws to its general Model armor hook. */
@Mixin(ArmorFeatureRenderer.class)
public abstract class ForgeDemonArmorMixin {
    @Unique private AbstractClientPlayerEntity demon$player;
    @Unique private EquipmentSlot demon$slot;

    @Inject(method = "renderArmor", at = @At("HEAD"))
    private void demon$begin(
            MatrixStack matrices,
            VertexConsumerProvider buffers,
            LivingEntity entity,
            EquipmentSlot slot,
            int light,
            BipedEntityModel<?> model,
            CallbackInfo ci) {
        demon$player =
                entity instanceof AbstractClientPlayerEntity p && DemonVisualModel.applies(p)
                        ? p
                        : null;
        demon$slot = slot;
    }

    @Inject(method = "renderArmor", at = @At("RETURN"))
    private void demon$end(
            MatrixStack matrices,
            VertexConsumerProvider buffers,
            LivingEntity entity,
            EquipmentSlot slot,
            int light,
            BipedEntityModel<?> model,
            CallbackInfo ci) {
        demon$player = null;
        demon$slot = null;
    }

    @SuppressWarnings("target") // Forge-only hooks are checked in validateForgeArmorTargets.
    @Dynamic("Exact Forge SRG Model hooks, absent from the named generic AP stub")
    @Redirect(
            method = {
                "renderModel(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/item/ArmorItem;Lnet/minecraft/client/model/Model;ZFFFLnet/minecraft/resources/ResourceLocation;)V",
                "renderTrim(Lnet/minecraft/world/item/ArmorMaterial;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/item/armortrim/ArmorTrim;Lnet/minecraft/client/model/Model;Z)V",
                "renderGlint(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/model/Model;)V"
            },
            remap = false,
            require = 3,
            at =
                    @At(
                            value = "INVOKE",
                            remap = false,
                            target =
                                    "Lnet/minecraft/client/model/Model;m_7695_(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V"))
    private void demon$draw(
            Model model,
            MatrixStack matrices,
            VertexConsumer vertices,
            int light,
            int overlay,
            float red,
            float green,
            float blue,
            float alpha) {
        if (demon$player != null
                && DemonVisualModel.renderArmor(
                        demon$player,
                        demon$slot,
                        matrices,
                        vertices,
                        light,
                        overlay,
                        red,
                        green,
                        blue,
                        alpha)) return;
        model.render(matrices, vertices, light, overlay, red, green, blue, alpha);
    }
}
