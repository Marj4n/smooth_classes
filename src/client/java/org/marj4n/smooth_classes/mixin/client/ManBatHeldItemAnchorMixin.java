package org.marj4n.smooth_classes.mixin.client;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.HeldItemFeatureRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.util.Arm;
import org.marj4n.smooth_classes.origin.OriginRuntime;
import org.marj4n.smooth_classes.origin.OriginType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * R8: The VSB Man-Bat shoulder is three model pixels ABOVE and two pixels
 * BEHIND the vanilla shoulder. In R6/R7 the real vanilla held-item feature used
 * the right Better Combat rotation, but applied it about Steve's shoulder.
 * On large swings the difference makes the sword appear to hang below the claw.
 *
 * Offset the attachment origin BEFORE the vanilla arm's animated rotation;
 * both the arm and sword still receive the exact same Better Combat angles.
 * Do not change the item's scale, grip rotation, glint, or animation itself.
 */
@Mixin(HeldItemFeatureRenderer.class)
public abstract class ManBatHeldItemAnchorMixin {
    @Inject(method = "renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;Lnet/minecraft/util/Arm;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/render/entity/model/ModelWithArms;setArmAngle(Lnet/minecraft/util/Arm;Lnet/minecraft/client/util/math/MatrixStack;)V"),
            require = 1)
    private void smooth_classes$alignManBatItemToShoulder(LivingEntity entity, ItemStack stack,
                                                             ModelTransformationMode mode, Arm arm,
                                                             MatrixStack matrices,
                                                             VertexConsumerProvider vertexConsumers,
                                                             int light, CallbackInfo ci) {
        if (!(entity instanceof PlayerEntity player) || stack.isEmpty()) return;
        var origin = OriginRuntime.state(player);
        if (origin.origin() != OriginType.VAMPIRE || !origin.hasFlag("vampire.form.man_bat")) return;

        // Figura VSB physical shoulders: Y=25, Z=2 (feet-at-zero, Y-up).
        // Vanilla biped shoulders: model Y=2, Z=0 (head-at-zero, Y-down).
        // Convert VSB Y=25 -> Minecraft model Y=24-25=-1, i.e. -3 px.
        matrices.translate(0.0D, -3.0D / 16.0D, 2.0D / 16.0D);
    }
}
