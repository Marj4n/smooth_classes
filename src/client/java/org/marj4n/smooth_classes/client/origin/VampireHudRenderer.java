package org.marj4n.smooth_classes.client.origin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import org.marj4n.smooth_classes.SmoothClasses;
import org.marj4n.smooth_classes.origin.OriginType;

/** Vampire-specific HUD: Blood replaces hunger, Vampirism-style fang reticle, sunlight pain. */
public final class VampireHudRenderer {
    private static final Identifier VAMPIRISM_ICONS = SmoothClasses.id("textures/gui/origin/vampirism/icons.png");
    private static final Identifier SUN_GRADIENT = SmoothClasses.id("textures/gui/origin/sun/gradient.png");
    private static final Identifier SUN_RAYS = SmoothClasses.id("textures/gui/origin/sun/rays.png");
    private static final Identifier SUN_VEINS = SmoothClasses.id("textures/gui/origin/sun/veins.png");
    private static final Identifier[][] BLOOD = new Identifier[2][8];

    static {
        for (int family = 0; family < 2; family++) {
            for (int i = 0; i < 8; i++) {
                String prefix = family == 1 ? "hunger/" : "";
                BLOOD[family][i] = SmoothClasses.id("textures/gui/origin/blood/" + prefix + "blood_" + i + ".png");
            }
        }
    }

    private VampireHudRenderer() {}

    public static void render(DrawContext context, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        OriginType origin = OriginType.byId(OriginClientState.originId).orElse(null);
        if (origin != OriginType.VAMPIRE) return;

        // Survival resource HUD only. Creative/spectator intentionally have no Blood/Hunger bar.
        if (client.player.isCreative() || client.player.isSpectator()) {
            return;
        }

        renderBlood(context, client);
        renderBiteReticle(context, client);
        renderSunPain(context, client);
    }

    private static void renderBlood(DrawContext context, MinecraftClient client) {
        int width = context.getScaledWindowWidth();
        int height = context.getScaledWindowHeight();
        int capacity = Math.max(1, OriginClientState.bloodCapacity);
        float normalized = Math.max(0.0F, Math.min(1.0F, OriginClientState.blood / (float)capacity));
        float totalIcons = normalized * 10.0F;
        int y = height - 39;
        boolean starving = OriginClientState.blood <= Math.max(5, capacity / 10);

        for (int i = 0; i < 10; i++) {
            float fill = Math.max(0.0F, Math.min(1.0F, totalIcons - i));
            int stage = fill <= 0.0F ? 0 : Math.min(7, Math.max(1, Math.round(fill * 7.0F)));
            int x = width / 2 + 91 - i * 8 - 9;
            Identifier tex = BLOOD[starving ? 1 : 0][stage];
            context.drawTexture(tex, x, y, 0.0F, 0.0F, 9, 9, 9, 9);
        }
    }

    private static void renderBiteReticle(DrawContext context, MinecraftClient client) {
        var state = org.marj4n.smooth_classes.origin.OriginRuntime.state(client.player);
        if (state.hasFlag("vampire.form.bat") || state.hasFlag("vampire.form.man_bat")) return;
        if (!client.player.getMainHandStack().isEmpty()) return;
        if (!(client.crosshairTarget instanceof EntityHitResult hit)) return;
        if (!(hit.getEntity() instanceof LivingEntity living) || !living.isAlive() || living == client.player) return;
        if (!org.marj4n.smooth_classes.origin.VampireBloodReserve.isFeedable(living)) return;
        if (client.player.squaredDistanceTo(living) > 12.25D) return;

        int width = context.getScaledWindowWidth();
        int height = context.getScaledWindowHeight();
        int x = width / 2 - 8;
        int y = height / 2 - 4;
        float progress = VampireFeedClient.isFeeding() ? VampireFeedClient.progress() : 0.0F;

        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1F, 1F, 1F, 0.70F);
        // Same fang region/layout used by Vampirism's HUD: u=27,v=0,size=16x10.
        context.drawTexture(VAMPIRISM_ICONS, x, y, 27, 0, 16, 10, 256, 256);
        if (progress > 0.0F) {
            int filled = Math.max(1, Math.min(10, (int)(10.0F * progress)));
            RenderSystem.setShaderColor(0.75F, 0.05F, 0.05F, 0.92F);
            context.drawTexture(VAMPIRISM_ICONS, x, y + (10 - filled), 27, 10 - filled, 16, filled, 256, 256);
        }
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        RenderSystem.disableBlend();

        // Show the mob's SERVER-TRACKED blood reserve, not health-based fake pips.
        // DataTracker delivers updates to nearby clients whenever feeding consumes a portion.
        int pips = org.marj4n.smooth_classes.origin.VampireBloodReserve.available(living);
        int startX = width / 2 - 22;
        int py = height / 2 + 11;
        for (int i = 0; i < 5; i++) {
            Identifier icon = BLOOD[0][i < pips ? 7 : 0];
            context.drawTexture(icon, startX + i * 9, py, 0.0F, 0.0F, 9, 9, 9, 9);
        }
    }

    private static void renderSunPain(DrawContext context, MinecraftClient client) {
        float exposure = Math.max(0.0F, Math.min(1.0F, OriginClientState.visualSunExposure() / 100.0F));
        if (exposure <= 0.01F) return;

        int width = context.getScaledWindowWidth();
        int height = context.getScaledWindowHeight();
        float glare = Math.min(1.0F, exposure * 1.7F);
        drawFullscreen(context, SUN_GRADIENT, width, height, glare * 0.72F);
        drawFullscreen(context, SUN_RAYS, width, height, glare * 0.62F);
        if (exposure > 0.50F) {
            float veins = (exposure - 0.50F) / 0.50F;
            drawFullscreen(context, SUN_VEINS, width, height, veins * 0.82F);
            int redAlpha = (int)(Math.min(0.38F, veins * 0.38F) * 255.0F);
            context.fill(0, 0, width, height, (redAlpha << 24) | 0x6B0000);
        }
    }

    private static void drawFullscreen(DrawContext context, Identifier texture, int width, int height, float alpha) {
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, Math.max(0.0F, Math.min(1.0F, alpha)));
        context.getMatrices().push();
        context.getMatrices().scale(width / 256.0F, height / 256.0F, 1.0F);
        context.drawTexture(texture, 0, 0, 0.0F, 0.0F, 256, 256, 256, 256);
        context.getMatrices().pop();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }
}
