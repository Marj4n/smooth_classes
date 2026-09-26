package org.marj4n.smooth_classes.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.marj4n.smooth_classes.SmoothClasses;

/** Continued-style two-slot ability HUD: signature (V) + ascendancy (R). */
public final class AbilityHud {
    private static final Identifier FRAME = new Identifier("minecraft", "textures/gui/widgets.png");
    private static final Identifier COOLDOWN = SmoothClasses.id("textures/gui/cooldown_overlay.png");

    public void render(DrawContext context, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.player.isSpectator() || client.isPaused() || client.currentScreen != null) return;

        int x = (client.getWindow().getScaledWidth() / 2) + 86;
        int y = client.getWindow().getScaledHeight() - 29;
        renderSlot(context, client, x, y, AbilityHudState.signatureAbility, AbilityHudState.signatureIcon(),
                AbilityHudState.signatureCooldownMs, AbilityHudState.signatureRemainingMs(), SmoothClassesClient.signatureKey());
        renderSlot(context, client, x + 22, y, AbilityHudState.ascendancyAbility, AbilityHudState.ascendancyIcon(),
                AbilityHudState.ascendancyCooldownMs, AbilityHudState.ascendancyRemainingMs(), SmoothClassesClient.ascendancyKey());
    }

    private void renderSlot(DrawContext context, MinecraftClient client, int x, int y, String ability, Identifier icon,
                            int totalMs, long remainingMs, KeyBinding key) {
        if (ability == null || ability.isBlank()) return;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        context.drawTexture(FRAME, x + 5, y + 6, 58, 22, 24, 24, 256, 256);
        context.drawTexture(icon, x + 10, y + 10, 0, 0, 16, 16, 16, 16);

        if (remainingMs > 0) {
            int overlayHeight = Math.max(1, Math.min(16, (int)(16F * (remainingMs / (float)Math.max(1,totalMs)))));
            int overlayY = y + 10 + (16 - overlayHeight);
            context.drawTexture(COOLDOWN, x + 10, overlayY, 0, 16 - overlayHeight, 16, overlayHeight, 16, 16);
            int secs = (int)Math.ceil(remainingMs / 1000D);
            context.drawCenteredTextWithShadow(client.textRenderer, Text.literal(Integer.toString(secs)), x + 18, y + 14, 0xFFFFFF);
        }
        context.drawCenteredTextWithShadow(client.textRenderer, key.getBoundKeyLocalizedText(), x + 18, y, 0xFFFFFF);
        RenderSystem.disableBlend();
    }
}
