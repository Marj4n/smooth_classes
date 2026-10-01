package org.marj4n.smooth_classes.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.marj4n.smooth_classes.SmoothClasses;

/** Ability HUD: signature (V), ascendancy (R), plus the current class-special H slot. */
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
        if (AbilityHudState.avengerSummonVisible) {
            renderAvengerSummonSlot(context, client, x + 44, y);
        } else if (AbilityHudState.riderMountVisible) {
            renderSlot(context, client, x + 44, y, "rider_mount", AbilityHudState.riderMountIcon(),
                    AbilityHudState.riderMountCooldownMs, AbilityHudState.riderMountRemainingMs(), SmoothClassesClient.riderMountKey());
        } else if (AbilityHudState.berserkerSpecialVisible) {
            renderSlot(context, client, x + 44, y, "crimson_revenant", AbilityHudState.berserkerSpecialIcon(),
                    AbilityHudState.berserkerSpecialCooldownMs, AbilityHudState.berserkerSpecialRemainingMs(), SmoothClassesClient.riderMountKey());
        }
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
                || ("agony".equals(ability) && AbilityHudState.whenOnHighActive)
                || ("rider_mount".equals(ability) && (AbilityHudState.riderMountActive
                || isLocalRiderMount(client)))
                || ("crimson_revenant".equals(ability) && (AbilityHudState.berserkerSpecialCharging
                || AbilityHudState.berserkerSpecialActive))) {
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
        context.drawCenteredTextWithShadow(client.textRenderer, SmoothClassesClient.riderMountKey().getBoundKeyLocalizedText(), x + 18, y, 0xFFFFFF);
        context.draw();
        context.getMatrices().pop();
        RenderSystem.disableBlend();
    }

    private static boolean isLocalRiderMount(MinecraftClient client) {
        if (client.player == null) return false;
        var vehicle = client.player.getVehicle();
        return vehicle instanceof org.marj4n.smooth_classes.entity.RiderHorseEntity
                || vehicle instanceof org.marj4n.smooth_classes.entity.RiderDreadSteedEntity
                || vehicle instanceof org.marj4n.smooth_classes.entity.RiderHippogryphEntity;
    }

}
