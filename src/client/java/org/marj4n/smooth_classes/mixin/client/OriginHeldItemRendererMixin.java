package org.marj4n.smooth_classes.mixin.client;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import org.marj4n.smooth_classes.SmoothClasses;
import org.marj4n.smooth_classes.client.origin.appearance.ManBatFormModel;
import org.marj4n.smooth_classes.origin.OriginRuntime;
import org.marj4n.smooth_classes.origin.OriginType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Origin-specific first-person hands/items. Man-Bat shows its real Dark Form claws. */
@Mixin(HeldItemRenderer.class)
public abstract class OriginHeldItemRendererMixin {
    @Unique private static final Identifier SMOOTH_CLASSES$MAN_BAT =
            SmoothClasses.id("textures/entity/origin/man_bat_nycto.png");
    @Unique private static ManBatFormModel<AbstractClientPlayerEntity> smooth_classes$manBatModel;

    @Inject(method = "renderFirstPersonItem", at = @At("HEAD"), cancellable = true)
    private void smooth_classes$originFirstPersonItem(AbstractClientPlayerEntity player, float tickDelta,
                                                       float pitch, Hand hand, float swingProgress,
                                                       ItemStack item, float equipProgress, MatrixStack matrices,
                                                       VertexConsumerProvider vertexConsumers, int light,
                                                       CallbackInfo ci) {
        if (player == null) return;
        var state = OriginRuntime.state(player);

        if (state.origin() == OriginType.VAMPIRE && state.hasFlag("vampire.form.man_bat")) {
            if (smooth_classes$manBatModel == null) {
                smooth_classes$manBatModel = new ManBatFormModel<>(ManBatFormModel.createData().createModel());
            }
            // Dark Form uses a Nycto-style first-person claw: left side, matching Dark Form.
            // Off-hand is suppressed so only Nycto's single claw view is rendered.
            if (hand == Hand.MAIN_HAND) {
                VertexConsumer vertices = vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(SMOOTH_CLASSES$MAN_BAT));
                smooth_classes$manBatModel.renderFirstPersonLeftClaw(
                        swingProgress, equipProgress, matrices, vertices, light, OverlayTexture.DEFAULT_UV);
            }
            ci.cancel();
            return;
        }

        if ((state.origin() == OriginType.SLIME && !state.hasFlag("slime.form.humanoid"))
                || (state.origin() == OriginType.VAMPIRE && state.hasFlag("vampire.form.bat"))) {
            ci.cancel();
        }
    }
}
