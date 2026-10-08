package org.marj4n.smooth_classes.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.marj4n.smooth_classes.SmoothClasses;
import org.marj4n.smooth_classes.client.origin.OriginAbilityHud;
import org.marj4n.smooth_classes.client.origin.OriginClientState;

/** Ability HUD: signature (V), ascendancy (R), plus the current class-special H slot. */
public final class AbilityHud {
    private static final Identifier FRAME = new Identifier("minecraft", "textures/gui/widgets.png");
    private static final Identifier COOLDOWN = SmoothClasses.id("textures/gui/cooldown_overlay.png");

    public void render(DrawContext context, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.player.isSpectator() || client.isPaused() || client.currentScreen != null) return;

        int x = (client.getWindow().getScaledWidth() / 2) + 86;
        int y = client.getWindow().getScaledHeight() - 29;

        if (AbilityPageState.isOriginPage() && !OriginClientState.originId.isBlank()) {
            renderPageLabel(context, client, x, y, true);
            renderOriginSlot(context, client, x, y, 0, SmoothClassesClient.signatureKey());
            renderOriginSlot(context, client, x + 22, y, 1, SmoothClassesClient.ascendancyKey());
            renderOriginSlot(context, client, x + 44, y, 2, SmoothClassesClient.classSpecialKey());
            return;
        }

        renderPageLabel(context, client, x, y, false);
        renderSlot(context, client, x, y, AbilityHudState.signatureAbility, AbilityHudState.signatureIcon(),
                AbilityHudState.signatureCooldownMs, AbilityHudState.signatureRemainingMs(), SmoothClassesClient.signatureKey());
        renderSlot(context, client, x + 22, y, AbilityHudState.ascendancyAbility, AbilityHudState.ascendancyIcon(),
                AbilityHudState.ascendancyCooldownMs, AbilityHudState.ascendancyRemainingMs(), SmoothClassesClient.ascendancyKey());
        if (AbilityHudState.avengerSummonVisible) {
            renderAvengerSummonSlot(context, client, x + 44, y);
        } else if (AbilityHudState.classSpecialVisible) {
            renderClassSpecialSlot(context, client, x + 44, y);
        }
    }


    private void renderPageLabel(DrawContext context, MinecraftClient client, int x, int y, boolean originPage) {
        String label = originPage ? "CLASS | > ORIGIN <" : "> CLASS < | ORIGIN";
        String toggle = SmoothClassesClient.abilityPageKey() == null ? ""
                : "  [" + SmoothClassesClient.abilityPageKey().getBoundKeyLocalizedText().getString() + "]";
        context.drawTextWithShadow(client.textRenderer, Text.literal(label + toggle), x + 4, y - 10,
                originPage ? 0xFFE59A : 0xF1F1F1);
    }

    private void renderOriginSlot(DrawContext context, MinecraftClient client, int x, int y, int slot, KeyBinding key) {
        var data = OriginAbilityHud.slot(slot);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        context.drawTexture(FRAME, x + 5, y + 6, 58, 22, 24, 24, 256, 256);
        context.drawItem(new ItemStack(data.icon()), x + 10, y + 10);
        context.draw();
        context.getMatrices().push();
        context.getMatrices().translate(0, 0, 300);
        long originCooldownMs = 0L;
        int originCooldownTotalMs = 0;
        if (slot == 0 && org.marj4n.smooth_classes.origin.OriginType.VAMPIRE.id().equals(OriginClientState.originId)) {
            originCooldownMs = OriginClientState.vampireBatCooldownRemainingMs();
            originCooldownTotalMs = 10_000;
        }
        if (originCooldownMs > 0L) {
            int overlayHeight = Math.max(1, Math.min(16,
                    (int)(16F * (originCooldownMs / (float)Math.max(1, originCooldownTotalMs)))));
            int overlayY = y + 10 + (16 - overlayHeight);
            context.drawTexture(COOLDOWN, x + 10, overlayY, 0, 16 - overlayHeight, 16, overlayHeight, 16, 16);
            int secs = (int)Math.ceil(originCooldownMs / 1000D);
            context.drawCenteredTextWithShadow(client.textRenderer, Text.literal(Integer.toString(secs)), x + 18, y + 14, 0xFFFFFF);
        }
        context.drawCenteredTextWithShadow(client.textRenderer, key.getBoundKeyLocalizedText(), x + 18, y, 0xFFFFFF);
        context.draw();
        context.getMatrices().pop();
        RenderSystem.disableBlend();
    }

    private void renderSlot(DrawContext context, MinecraftClient client, int x, int y, String ability, Identifier icon,
                            int totalMs, long remainingMs, KeyBinding key) {
        if (ability == null || ability.isBlank()) return;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        context.drawTexture(FRAME, x + 5, y + 6, 58, 22, 24, 24, 256, 256);
        if ("sacred_orb".equals(ability))
            context.drawItem(new net.minecraft.item.ItemStack(org.marj4n.smooth_classes.registry.SmoothItems.SACRED_BANNER_ICON), x + 10, y + 10);
        else context.drawTexture(icon, x + 10, y + 10, 0, 0, 16, 16, 16, 16);

        // Item icons write GUI depth around z=150. Flush them before drawing
        // cooldown/text on a higher plane so the banner cannot hide the timer.
        context.draw();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        context.getMatrices().push();
        context.getMatrices().translate(0, 0, 300);
        if ("preparation".equals(ability) && AbilityHudState.shadowActive) {
            long shadowRemaining = AbilityHudState.shadowRemainingMs();
            int secs = Math.max(0, (int)Math.ceil(shadowRemaining / 1000D));
            // Same bottom-right extra-number language as Avenger charges. Keep the
            // Shadow icon unobscured: this number is anchor lifetime, not cooldown.
            context.drawTextWithShadow(client.textRenderer, Text.literal(Integer.toString(secs)), x + 21, y + 20, 0xDDA0FF);
        } else if (("sacred_orb".equals(ability) && AbilityHudState.bannerActive)
                || ("magic_circle".equals(ability) && AbilityHudState.bloodRainActive)
                || ("agony".equals(ability) && AbilityHudState.whenOnHighActive)) {
            context.fill(x+10,y+10,x+26,y+26,0xB0000000);
            context.drawCenteredTextWithShadow(client.textRenderer,Text.literal("X"),x+18,y+14,0xFF5555);
        } else if (remainingMs > 0) {
            int overlayHeight = Math.max(1, Math.min(16, (int)(16F * (remainingMs / (float)Math.max(1,totalMs)))));
            int overlayY = y + 10 + (16 - overlayHeight);
            context.drawTexture(COOLDOWN, x + 10, overlayY, 0, 16 - overlayHeight, 16, overlayHeight, 16, 16);
            int secs = (int)Math.ceil(remainingMs / 1000D);
            context.drawCenteredTextWithShadow(client.textRenderer, Text.literal(Integer.toString(secs)), x + 18, y + 14, 0xFFFFFF);
        }
        context.drawCenteredTextWithShadow(client.textRenderer, key.getBoundKeyLocalizedText(), x + 18, y, 0xFFFFFF);
        context.draw();
        context.getMatrices().pop();
        RenderSystem.disableBlend();
    }

    private void renderClassSpecialSlot(DrawContext context, MinecraftClient client, int x, int y) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        context.drawTexture(FRAME, x + 5, y + 6, 58, 22, 24, 24, 256, 256);
        Identifier specialIcon = AbilityHudState.classSpecialIcon();
        if (client.getResourceManager().getResource(specialIcon).isEmpty()) {
            specialIcon = AbilityHudState.classSpecialFallbackIcon();
        }
        context.drawTexture(specialIcon, x + 10, y + 10, 0, 0, 16, 16, 16, 16);
        context.draw();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        context.getMatrices().push();
        context.getMatrices().translate(0, 0, 300);

        long remaining = AbilityHudState.classSpecialRemainingMs();
        if (AbilityHudState.classSpecialActive) {
            context.fill(x + 10, y + 10, x + 26, y + 26, 0xB0000000);
            context.drawCenteredTextWithShadow(client.textRenderer, Text.literal("X"), x + 18, y + 14, 0xFF5555);
        } else if (remaining > 0) {
            int overlayHeight = Math.max(1, Math.min(16, (int)(16F * (remaining / (float)Math.max(1, AbilityHudState.classSpecialCooldownMs)))));
            int overlayY = y + 10 + (16 - overlayHeight);
            context.drawTexture(COOLDOWN, x + 10, overlayY, 0, 16 - overlayHeight, 16, overlayHeight, 16, 16);
            int secs = (int)Math.ceil(remaining / 1000D);
            context.drawCenteredTextWithShadow(client.textRenderer, Text.literal(Integer.toString(secs)), x + 18, y + 14, 0xFFFFFF);
        }

        // Duration numbers use the same unobtrusive language as Shadow Technique:
        // gold = H-mode lifetime, aqua = current projected armament lifetime.
        long modeRemaining = AbilityHudState.classSpecialModeRemainingMs();
        long secondaryRemaining = AbilityHudState.classSpecialSecondaryRemainingMs();
        if (modeRemaining > 0L) {
            int secs = Math.max(0, (int)Math.ceil(modeRemaining / 1000D));
            context.drawTextWithShadow(client.textRenderer, Text.literal(Integer.toString(secs)), x + 21, y + 20, 0xFFE066);
        }
        if (secondaryRemaining > 0L) {
            int secs = Math.max(0, (int)Math.ceil(secondaryRemaining / 1000D));
            context.drawTextWithShadow(client.textRenderer, Text.literal(Integer.toString(secs)), x + 5, y + 20, 0x66E6FF);
        }

        String badge = AbilityHudState.classSpecialBadge();
        if (!badge.isBlank() && modeRemaining <= 0L && secondaryRemaining <= 0L) {
            context.drawTextWithShadow(client.textRenderer, Text.literal(badge), x + 21, y + 20, 0xFFE066);
        }
        context.drawCenteredTextWithShadow(client.textRenderer, SmoothClassesClient.classSpecialKey().getBoundKeyLocalizedText(), x + 18, y, 0xFFFFFF);
        context.draw();
        context.getMatrices().pop();
        RenderSystem.disableBlend();
    }

    private void renderAvengerSummonSlot(DrawContext context, MinecraftClient client, int x, int y) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        context.drawTexture(FRAME, x + 5, y + 6, 58, 22, 24, 24, 256, 256);
        context.drawTexture(AbilityHudState.avengerSummonIcon(), x + 10, y + 10, 0, 0, 16, 16, 16, 16);
        context.draw();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        context.getMatrices().push();
        context.getMatrices().translate(0, 0, 300);

        long remaining = AbilityHudState.avengerSummonRemainingMs();
        int charges = AbilityHudState.avengerSummonCharges;
        if (charges < AbilityHudState.avengerSummonMaxCharges && remaining > 0) {
            int overlayHeight = Math.max(1, Math.min(16, (int)(16F * (remaining / (float)Math.max(1, AbilityHudState.avengerSummonRechargeMs)))));
            int overlayY = y + 10 + (16 - overlayHeight);
            context.drawTexture(COOLDOWN, x + 10, overlayY, 0, 16 - overlayHeight, 16, overlayHeight, 16, 16);
            if (charges == 0) {
                int secs = (int)Math.ceil(remaining / 1000D);
                context.drawCenteredTextWithShadow(client.textRenderer, Text.literal(Integer.toString(secs)), x + 18, y + 14, 0xFFFFFF);
            }
        }
        // Charge badge remains visible while a recharge is running, so 1-2 stored casts are obvious.
        context.drawTextWithShadow(client.textRenderer, Text.literal(Integer.toString(charges)), x + 21, y + 20,
                charges > 0 ? 0xFFE066 : 0xFF5555);
        context.drawCenteredTextWithShadow(client.textRenderer, SmoothClassesClient.classSpecialKey().getBoundKeyLocalizedText(), x + 18, y, 0xFFFFFF);
        context.draw();
        context.getMatrices().pop();
        RenderSystem.disableBlend();
    }

}
