package org.marj4n.smooth_classes.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import org.marj4n.smooth_classes.origin.OriginRuntime;
import org.marj4n.smooth_classes.origin.OriginType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Neutral Man-Bat inventory head (Survival AND Creative, including the creative
 * "Survival Inventory" tab).
 *
 * Both screens eventually call InventoryScreen.drawEntity(..., mouseX, mouseY, player)
 * to compute the player's body/head angles from a cursor-relative displacement.
 * Previous patches only intercepted InventoryScreen.drawBackground, which never
 * runs when the current screen is CreativeInventoryScreen. This hooks the common
 * mouse-following drawEntity overload instead so BOTH callers are covered.
 *
 * The initial actual mouse position becomes the neutral (0,0) pose.
 * Subsequent cursor movement is still forwarded to vanilla as a delta; no
 * player yaw/pitch is changed or persisted outside of inventory rendering.
 */
@Mixin(InventoryScreen.class)
public abstract class ManBatInventoryLookMixin {
    @Unique private static Screen smooth_classes$lastInventory;
    @Unique private static boolean smooth_classes$anchorXReady;
    @Unique private static boolean smooth_classes$anchorYReady;
    @Unique private static double smooth_classes$openingX;
    @Unique private static double smooth_classes$openingY;

    @Unique
    private static boolean smooth_classes$allowNeutralPreview() {
        MinecraftClient client = MinecraftClient.getInstance();
        Screen screen = client.currentScreen;
        // The method is used by other UIs/mods as well; scope this strictly
        // to the two vanilla inventory screens and a locally selected Man-Bat.
        if (!(screen instanceof InventoryScreen || screen instanceof CreativeInventoryScreen)
                || client.player == null) return false;
        var state = OriginRuntime.state(client.player);
        return state.origin() == OriginType.VAMPIRE && state.hasFlag("vampire.form.man_bat");
    }

    /** Current cursor in Minecraft GUI units, NOT the cached mouseX/mouseY
     * fields used by screens before their first render callback. Those fields
     * are commonly zero on opening, causing the exact R8-R10 false head turn. */
    @Unique
    private static double smooth_classes$currentGuiMouseX() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client.mouse.getX() * client.getWindow().getScaledWidth()
                / (double) Math.max(1, client.getWindow().getWidth());
    }

    @Unique
    private static double smooth_classes$currentGuiMouseY() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client.mouse.getY() * client.getWindow().getScaledHeight()
                / (double) Math.max(1, client.getWindow().getHeight());
    }

    @Unique
    private static void smooth_classes$refreshScreenAnchor() {
        Screen screen = MinecraftClient.getInstance().currentScreen;
        if (smooth_classes$lastInventory != screen) {
            smooth_classes$lastInventory = screen;
            smooth_classes$anchorXReady = false;
            smooth_classes$anchorYReady = false;
        }
    }

    @ModifyVariable(
            method = "drawEntity(Lnet/minecraft/client/gui/DrawContext;IIIFFLnet/minecraft/entity/LivingEntity;)V",
            at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 1)
    private static float smooth_classes$neutralPreviewYaw(float cursorOffsetX) {
        if (!smooth_classes$allowNeutralPreview()) return cursorOffsetX;
        smooth_classes$refreshScreenAnchor();
        double current = smooth_classes$currentGuiMouseX();
        if (!smooth_classes$anchorXReady) {
            smooth_classes$openingX = current;
            smooth_classes$anchorXReady = true;
        }
        // Vanilla mouse-relative yaw: previewCenterX - GUI cursorX.
        // The center cancels out, so starting the preview facing front is
        // equivalent to openingCursorX - currentCursorX.
        return (float) (smooth_classes$openingX - current);
    }

    @ModifyVariable(
            method = "drawEntity(Lnet/minecraft/client/gui/DrawContext;IIIFFLnet/minecraft/entity/LivingEntity;)V",
            at = @At("HEAD"), argsOnly = true, ordinal = 1, require = 1)
    private static float smooth_classes$neutralPreviewPitch(float cursorOffsetY) {
        if (!smooth_classes$allowNeutralPreview()) return cursorOffsetY;
        smooth_classes$refreshScreenAnchor();
        double current = smooth_classes$currentGuiMouseY();
        if (!smooth_classes$anchorYReady) {
            smooth_classes$openingY = current;
            smooth_classes$anchorYReady = true;
        }
        return (float) (smooth_classes$openingY - current);
    }
}
