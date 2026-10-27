package org.marj4n.smooth_classes.mixin.client;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.marj4n.smooth_classes.client.origin.appearance.ManBatFormModel;
import org.marj4n.smooth_classes.client.origin.appearance.VsbArmorRenderScale;
import org.marj4n.smooth_classes.origin.OriginRuntime;
import org.marj4n.smooth_classes.origin.OriginType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Optional Armor Model API / RPG Series compatibility, without a hard dependency.
 *
 * All of the 1.20.1 RPG Series geo armor uses Armor Model API's Fabric hook.
 * That hook renders its *own* GeoArmorModel rather than running the vanilla
 * ArmorFeatureRenderer#setVisible pathway patched by ArmorStealthRendererMixin.
 * As a result the VSB model's real shoulders, hip pivots and combat poses
 * never reached RPG Series armor, even though vanilla armor aligned correctly.
 *
 * The Armor Model API dispatcher copies the parent biped pose to its geo model
 * once per equipped piece. Intercept that precise copy, preserve it, then align
 * the geo model with the same Man-Bat bone mapping as vanilla armor. The rest
 * of the original dispatcher runs untouched: armor textures, trim, glow masks,
 * glint and slot visibility are all still provided by Armor Model API.
 *
 * @Pseudo and a string mixin target allow Smooth Classes to launch without
 * Armor Model API. This mixin deliberately references only Minecraft classes,
 * so the RPG API is never a compile-time or runtime hard dependency.
 */
@Pseudo
@Mixin(targets = "net.rpg_foundation.armor_api.client.ArmorRenderDispatcher", remap = false)
public abstract class RpgGeoArmorManBatMixin {

    /**
     * Armor Model API renders registered RPG Series geo equipment through its
     * own dispatcher, bypassing ArmorFeatureRenderer.renderArmor's vanilla
     * Bat Form suppression. Stop the entire dispatcher before it poses or
     * draws any geometry: base armor, trims, glint and emissive layers all
     * disappear in the tiny Bat Form. The actual equipment stays equipped.
     *
     * Keep normal humanoids and Man-Bat untouched so R26's working armor
     * alignment and animations remain identical after re-transforming.
     */
    @Inject(method = "render", at = @At("HEAD"), cancellable = true,
            remap = false, require = 0)
    private static void smooth_classes$hideGeoArmorForTinyBat(
            MatrixStack matrices, VertexConsumerProvider consumers,
            ItemStack stack, LivingEntity entity, EquipmentSlot slot,
            int light, BipedEntityModel<LivingEntity> contextModel,
            CallbackInfoReturnable<Boolean> cir) {
        if (!(entity instanceof PlayerEntity player)) return;
        var state = OriginRuntime.state(player);
        if (state.origin() != OriginType.VAMPIRE || !state.hasFlag("vampire.form.bat")) return;

        // No stale limb scaling may survive into the next player's armor pass.
        VsbArmorRenderScale.end();
        // Fabric's ArmorRenderer registration ignores the dispatcher return;
        // reporting handled avoids implying a fallback should render armor.
        cir.setReturnValue(true);
    }

    @Redirect(
            method = "render",
            at = @At(value = "INVOKE", target =
                    "Lnet/minecraft/client/render/entity/model/BipedEntityModel;copyBipedStateTo(Lnet/minecraft/client/render/entity/model/BipedEntityModel;)V",
                    remap = true),
            remap = false,
            require = 0
    )
    private static void smooth_classes$syncGeoArmorPose(
            BipedEntityModel<LivingEntity> playerPose,
            BipedEntityModel<LivingEntity> geoArmorModel,
            MatrixStack matrices,
            VertexConsumerProvider consumers,
            ItemStack stack,
            LivingEntity entity,
            EquipmentSlot slot,
            int light,
            BipedEntityModel<LivingEntity> contextModel) {
        // Never replace the Armor Model API's usual vanilla pose propagation.
        playerPose.copyBipedStateTo(geoArmorModel);

        if (!(entity instanceof PlayerEntity player)) return;
        var origin = OriginRuntime.state(player);
        if (origin.origin() != OriginType.VAMPIRE || !origin.hasFlag("vampire.form.man_bat")) return;

        // Pose capture already happened in ManBatPosePreparerFeatureRenderer,
        // the first feature of the player's renderer (before all armor features).
        ManBatFormModel.alignArmor(player, slot, geoArmorModel);
        VsbArmorRenderScale.begin(geoArmorModel, slot);
    }

    @Inject(method = "render", at = @At("RETURN"), remap = false, require = 0)
    private static void smooth_classes$restoreGeoArmorScale(
            MatrixStack matrices, VertexConsumerProvider consumers,
            ItemStack stack, LivingEntity entity, EquipmentSlot slot,
            int light, BipedEntityModel<LivingEntity> contextModel,
            CallbackInfoReturnable<Boolean> cir) {
        // Renderer models are cached across players and passes: no VSB limb
        // scale may leak into the next ordinary humanoid's equipment.
        VsbArmorRenderScale.end();
    }
}
