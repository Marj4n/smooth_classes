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
            renderPageToggle(context, client, x + 81, y);
            renderOriginSlot(context, client, x, y, 0, SmoothClassesClient.signatureKey());
            renderOriginSlot(context, client, x + 22, y, 1, SmoothClassesClient.ascendancyKey());
            renderOriginSlot(context, client, x + 44, y, 2, SmoothClassesClient.classSpecialKey());
            return;
        }

        renderPageToggle(context, client, x + 81, y);
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


    /** A compact keycap after H; never overlaps neighboring ability slots. */
    private void renderPageToggle(DrawContext context, MinecraftClient client, int x, int y) {
        String binding = SmoothClassesClient.abilityPageKey() == null ? "Tab"
                : SmoothClassesClient.abilityPageKey().getBoundKeyLocalizedText().getString();
        // Display the actual configured key, while keeping the UI free of class/origin labels.
        String text = binding.equalsIgnoreCase("key.keyboard.tab") ? "Tab" : binding;
        int width = Math.max(24, client.textRenderer.getWidth(text) + 12);
        int top = y + 9;
        int left = x;
        int border = AbilityPageState.isOriginPage() ? 0xFFE89ADB : 0xFF91B9DA;
        context.fill(left + 1, top + 2, left + width + 2, top + 19, 0x60000000);
        context.fill(left, top, left + width, top + 17, border);
        context.fill(left + 1, top + 1, left + width - 1, top + 16, 0xE7191826);
        context.fill(left + 4, top + 3, left + width - 4, top + 4, border);
        context.drawCenteredTextWithShadow(client.textRenderer, Text.literal(text),
                left + width / 2, top + 6, 0xFFF7F3FF);
    }

    /** Tiny Vampire Bat Form cannot cast other skills. Matches Sacred Banner's X overlay. */
    private boolean tinyBatLocked() {
        return org.marj4n.smooth_classes.origin.OriginType.VAMPIRE.id().equals(OriginClientState.originId)
                && OriginClientState.hasFlag("vampire.form.bat");
    }

    private void renderBatLockedOverlay(DrawContext context, MinecraftClient client, int x, int y) {
        context.fill(x + 10, y + 10, x + 26, y + 26, 0xB0000000);
        context.drawCenteredTextWithShadow(client.textRenderer, Text.literal("X"), x + 18, y + 14, 0xFF5555);
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
        if (tinyBatLocked() && slot != 0) {
            // Only Origin slot 0 returns the Bat to a humanoid form.
            // The other two Origin slots are locked.
            renderBatLockedOverlay(context, client, x, y);
            context.drawCenteredTextWithShadow(client.textRenderer, key.getBoundKeyLocalizedText(), x + 18, y, 0xFFFFFF);
            context.draw();
            context.getMatrices().pop();
            RenderSystem.disableBlend();
            return;
        }
        long originCooldownMs = 0L;
        int originCooldownTotalMs = 0;
        if (slot == 0 && org.marj4n.smooth_classes.origin.OriginType.VAMPIRE.id().equals(OriginClientState.originId)) {
            originCooldownMs = OriginClientState.vampireBatCooldownRemainingMs();
            originCooldownTotalMs = 10_000;
        }
        if (slot == 1 && org.marj4n.smooth_classes.origin.OriginType.VAMPIRE.id().equals(OriginClientState.originId)) {
            if (OriginClientState.hasFlag("vampire.form.man_bat")) {
                long remaining = OriginClientState.manBatDurationRemainingMs();
                int seconds = (int)Math.ceil(remaining / 1000D);
                // Centered exactly like a cooldown countdown; pink means the form is active.
                renderActiveDuration(context, client, x, y, seconds);
            } else {
                originCooldownMs = OriginClientState.manBatCooldownRemainingMs();
                originCooldownTotalMs = 30_000;
            }
        }
        if (slot == 2 && org.marj4n.smooth_classes.origin.OriginType.VAMPIRE.id().equals(OriginClientState.originId)) {
            originCooldownMs = OriginClientState.bloodSenseCooldownRemainingMs();
            originCooldownTotalMs = 20_000;
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
        if (tinyBatLocked()) {
            renderBatLockedOverlay(context, client, x, y);
        } else if ("preparation".equals(ability) && AbilityHudState.shadowActive) {
            long shadowRemaining = AbilityHudState.shadowRemainingMs();
            int secs = Math.max(0, (int)Math.ceil(shadowRemaining / 1000D));
            // Active duration uses the same centered alignment as regular cooldowns.
            renderActiveDuration(context, client, x, y, secs);
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

    /** Shared timer alignment: white = cooldown, pink = skill/form active duration. */
    private void renderActiveDuration(DrawContext context, MinecraftClient client, int x, int y, int seconds) {
        context.drawCenteredTextWithShadow(client.textRenderer,
                Text.literal(Integer.toString(Math.max(0, seconds))), x + 18, y + 14, 0xDDA0FF);
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
        if (tinyBatLocked()) {
            renderBatLockedOverlay(context, client, x, y);
        } else if (AbilityHudState.classSpecialActive) {
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
        if (!tinyBatLocked() && modeRemaining > 0L) {
            int secs = Math.max(0, (int)Math.ceil(modeRemaining / 1000D));
            context.drawTextWithShadow(client.textRenderer, Text.literal(Integer.toString(secs)), x + 21, y + 20, 0xFFE066);
        }
        if (!tinyBatLocked() && secondaryRemaining > 0L) {
            int secs = Math.max(0, (int)Math.ceil(secondaryRemaining / 1000D));
            context.drawTextWithShadow(client.textRenderer, Text.literal(Integer.toString(secs)), x + 5, y + 20, 0x66E6FF);
        }

        String badge = AbilityHudState.classSpecialBadge();
        if (!tinyBatLocked() && !badge.isBlank() && modeRemaining <= 0L && secondaryRemaining <= 0L) {
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
        if (tinyBatLocked()) {
            renderBatLockedOverlay(context, client, x, y);
        } else if (charges < AbilityHudState.avengerSummonMaxCharges && remaining > 0) {
            int overlayHeight = Math.max(1, Math.min(16, (int)(16F * (remaining / (float)Math.max(1, AbilityHudState.avengerSummonRechargeMs)))));
            int overlayY = y + 10 + (16 - overlayHeight);
            context.drawTexture(COOLDOWN, x + 10, overlayY, 0, 16 - overlayHeight, 16, overlayHeight, 16, 16);
            if (charges == 0) {
                int secs = (int)Math.ceil(remaining / 1000D);
                context.drawCenteredTextWithShadow(client.textRenderer, Text.literal(Integer.toString(secs)), x + 18, y + 14, 0xFFFFFF);
            }
        }
        // Charge badge remains visible while a recharge is running, so 1-2 stored casts are obvious.
        if (!tinyBatLocked()) context.drawTextWithShadow(client.textRenderer, Text.literal(Integer.toString(charges)), x + 21, y + 20,
                charges > 0 ? 0xFFE066 : 0xFF5555);
        context.drawCenteredTextWithShadow(client.textRenderer, SmoothClassesClient.classSpecialKey().getBoundKeyLocalizedText(), x + 18, y, 0xFFFFFF);
        context.draw();
        context.getMatrices().pop();
        RenderSystem.disableBlend();
    }

}
