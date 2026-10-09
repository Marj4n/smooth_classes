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
import org.marj4n.smooth_classes.client.origin.appearance.ManBatFormModel;
import org.marj4n.smooth_classes.client.origin.appearance.HomunculusReplacementModel;
import org.marj4n.smooth_classes.client.origin.appearance.VsbArmorRenderScale;
import org.marj4n.smooth_classes.origin.OriginType;
import org.marj4n.smooth_classes.origin.OriginRuntime;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Armor follows the same stealth state as the player model. */
@Mixin(ArmorFeatureRenderer.class)
public abstract class ArmorStealthRendererMixin {
    @Inject(method = "renderArmor", at = @At("HEAD"), cancellable = true)
    private void smooth_classes$renderArmor(MatrixStack matrices, VertexConsumerProvider consumers,
            LivingEntity entity, EquipmentSlot slot, int light, BipedEntityModel<LivingEntity> model,
            CallbackInfo ci) {
        VsbArmorRenderScale.end();
        if (entity.isInvisible() && StealthVisualState.active(entity)) {
            ci.cancel();
            return;
        }
        if (entity instanceof net.minecraft.entity.player.PlayerEntity player) {
            var state = org.marj4n.smooth_classes.origin.OriginRuntime.state(player);
            if (state.origin() == OriginType.HOMUNCULUS) {
                matrices.push();
                float scale = HomunculusReplacementModel.RENDER_SCALE;
                matrices.translate(0.0D, 1.8D * (1.0D - scale), 0.0D);
                matrices.scale(scale, scale, scale);
            }
            if (state.origin() == org.marj4n.smooth_classes.origin.OriginType.VAMPIRE
                    && state.hasFlag("vampire.form.bat")) {
                ci.cancel();
            }
        }
    }

    /** Align the still-vanilla armor meshes to VSB's real bone pivots. */
    @Inject(method = "renderArmor", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/render/entity/feature/ArmorFeatureRenderer;setVisible(Lnet/minecraft/client/render/entity/model/BipedEntityModel;Lnet/minecraft/entity/EquipmentSlot;)V",
            shift = At.Shift.AFTER), require = 1)
    private void smooth_classes$alignVsbArmor(MatrixStack matrices, VertexConsumerProvider consumers,
            LivingEntity entity, EquipmentSlot slot, int light, BipedEntityModel<LivingEntity> model,
            CallbackInfo ci) {
        if (entity instanceof net.minecraft.entity.player.PlayerEntity player) {
            var state = OriginRuntime.state(player);
            if (state.origin() == OriginType.HOMUNCULUS) {
                // The approved bbmodel stays close to a humanoid anchor, so vanilla
                // armor can remain on standard pivots instead of the older temporary rig offsets.
            }
            if (state.origin() == OriginType.VAMPIRE && state.hasFlag("vampire.form.man_bat")) {
                ManBatFormModel.alignArmor(player, slot, model);
                VsbArmorRenderScale.begin(model, slot);
            }
        }
    }

    @Inject(method = "renderArmor", at = @At("RETURN"))
    private void smooth_classes$finishArmorSlot(MatrixStack matrices, VertexConsumerProvider consumers,
            LivingEntity entity, EquipmentSlot slot, int light, BipedEntityModel<LivingEntity> model,
            CallbackInfo ci) {
        VsbArmorRenderScale.end();
        if (entity instanceof net.minecraft.entity.player.PlayerEntity player
                && OriginRuntime.state(player).origin() == OriginType.HOMUNCULUS) {
            matrices.pop();
        }
    }
}
