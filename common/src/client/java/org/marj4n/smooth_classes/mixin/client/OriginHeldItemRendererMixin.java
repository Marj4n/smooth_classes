package org.marj4n.smooth_classes.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.Arm;
import org.marj4n.smooth_classes.origin.OriginRuntime;
import org.marj4n.smooth_classes.origin.OriginType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Preserve Minecraft's real first-person arm and held-item renderer for Man-Bat.
 *
 * The previous override rendered the entire VSB third-person claw *before*
 * Minecraft rendered an item. That produced two giant rigid arms on screen and
 * broke Better Combat's first-person attack animations. Man-Bat should use the
 * exact same item, empty-hand, swing, equip, blocking and use transformations as
 * an ordinary player. The VSB mesh remains exclusive to third-person rendering.
 */
@Mixin(HeldItemRenderer.class)
public abstract class OriginHeldItemRendererMixin {
    @Inject(method = "renderFirstPersonItem", at = @At("HEAD"), cancellable = true)
    private void smooth_classes$originFirstPersonItem(AbstractClientPlayerEntity player, float tickDelta,
                                                       float pitch, Hand hand, float swingProgress,
                                                       ItemStack item, float equipProgress, MatrixStack matrices,
                                                       VertexConsumerProvider vertexConsumers, int light,
                                                       CallbackInfo ci) {
        if (player == null) return;
        var state = OriginRuntime.state(player);

        // Man-Bat is intentionally NOT overridden: vanilla/Better Combat owns
        // empty right-hand rendering and all item animations, without extra claws.
        if ((state.origin() == OriginType.SLIME && !state.hasFlag("slime.form.humanoid"))
                || (state.origin() == OriginType.VAMPIRE && state.hasFlag("vampire.form.bat"))) {
            ci.cancel();
        }
    }

    /**
     * VSB Man-Bat: keep an empty right hand in the exact vanilla position,
     * but with a held item render just the item (no second first-person arm).
     * We cancel only the arm DRAW call, never the item or its vanilla/Better
     * Combat transformations. Maps keep their specialized two-hand visuals.
     */
    @Inject(method = "renderArmHoldingItem", at = @At("HEAD"), cancellable = true)
    private void smooth_classes$itemOnlyInManBat(MatrixStack matrices, VertexConsumerProvider consumers,
                                                  int light, float equipProgress, float swingProgress,
                                                  Arm arm, CallbackInfo ci) {
        var player = MinecraftClient.getInstance().player;
        if (player == null) return;
        var state = OriginRuntime.state(player);
        if (state.origin() != OriginType.VAMPIRE || !state.hasFlag("vampire.form.man_bat")) return;
        // The occupied arm must not draw a large extra arm alongside the item.
        // Leave an empty main hand to vanilla so it animates like Steve's hand.
        boolean mainArm = player.getMainArm() == arm;
        ItemStack inHand = mainArm ? player.getMainHandStack() : player.getOffHandStack();
        if (!inHand.isEmpty()) ci.cancel();
    }
}
