package org.marj4n.smooth_classes.client.origin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.marj4n.smooth_classes.SmoothClasses;
import org.marj4n.smooth_classes.origin.OriginType;

/** Vampire-specific HUD: Blood replaces hunger, Vampirism-style fang reticle, sunlight pain. */
public final class VampireHudRenderer {
    private static final Identifier VAMPIRISM_ICONS = SmoothClasses.id("textures/gui/origin/vampirism/icons.png");
    private static final Identifier SUN_GRADIENT = SmoothClasses.id("textures/gui/origin/sun/gradient.png");
    private static final Identifier SUN_RAYS = SmoothClasses.id("textures/gui/origin/sun/rays.png");
    private static final Identifier SUN_VEINS = SmoothClasses.id("textures/gui/origin/sun/veins.png");
    private static final Identifier[][] BLOOD = new Identifier[2][8];
    private static final long BLOOD_SENSE_BASE_MS = 10_000L;
    private static final long BLOOD_SENSE_LORD_MS = 20_000L;
    private static final long BLOOD_SENSE_FADE_OUT_MS = 1_600L;
    private static final long BLOOD_SENSE_FADE_IN_MS = 240L;
    private static final boolean DYNAMIC_BARS = FabricLoader.getInstance().isModLoaded("dynamic_resource_bars");
    private static final Identifier STAMINA_FRAME = new Identifier("dynamic_resource_bars", "textures/gui/stamina_foreground.png");
    private static final long LORD_EVOLUTION_MS = 6_500L;

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

        renderBloodSense(context, client, tickDelta);
        renderLordEvolution(context, client, tickDelta);

        // Survival resource HUD only. Creative/spectator intentionally have no Blood/Hunger bar.
        if (client.player.isCreative() || client.player.isSpectator()) {
            return;
        }

        if (DYNAMIC_BARS) renderModpackBloodBar(context, client, tickDelta);
        else renderBlood(context, client);
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

    /**
     * Dynamic RPG Resource Bars replaces vanilla hunger with a full-width FOOD stamina bar.
     * That bar is cancelled for Smooth Classes Vampires by the optional compat mixin.
     * Match the Smooth Odyssey 1.2.8 right-side bar anchor rather than drawing ten
     * vanilla-style icons which would overlap the resource-bar UI.
     */
    private static void renderModpackBloodBar(DrawContext context, MinecraftClient client, float tickDelta) {
        int width = context.getScaledWindowWidth();
        int height = context.getScaledWindowHeight();
        int capacity = Math.max(1, OriginClientState.bloodCapacity);
        float percentage = MathHelper.clamp(OriginClientState.blood / (float) capacity, 0.0F, 1.0F);
        // 1.20.1 Smooth Odyssey config: HUNGER anchor (+91, -40), total (-74, +4).
        int barX = width / 2 + 16;
        int barY = height - 37;
        int filled = Math.round(77.0F * percentage);
        context.fill(barX - 1, barY - 1, barX + 78, barY + 8, 0xFF10080E);
        context.fill(barX, barY, barX + 77, barY + 6, 0xFF34171F);
        if (filled > 0) {
            context.fill(barX, barY, barX + filled, barY + 6, 0xFF951629);
            context.fill(barX, barY, barX + filled, barY + 2, 0xFFC74256);
            context.fill(barX, barY + 5, barX + filled, barY + 6, 0xFF61101B);
        }
        // Tiny rising crimson bubbles inside the filled Blood bar. Position and
        // timing are deterministic from render ticks; no per-frame allocations or
        // randomness, and never draw above the current blood amount.
        if (filled > 4) {
            float ticks = client.player.age + tickDelta;
            for (int i = 0; i < 15; i++) {
                float phase = ticks * (0.046F + (i % 4) * 0.015F) + i * 8.83F;
                float travel = phase - (float)Math.floor(phase);
                int bubbleX = barX + 1 + (i * 37 + 13) % Math.max(1, filled - 2);
                int bubbleY = barY + 4 - (int)(travel * 4.0F);
                int alpha = (int)(MathHelper.clamp(
                        Math.min(travel * 4.0F, (1.0F - travel) * 4.0F), 0.0F, 1.0F) * 150.0F);
                if (bubbleX < barX + filled - 1 && bubbleY >= barY + 1 && bubbleY <= barY + 4) {
                    context.fill(bubbleX, bubbleY, bubbleX + 1, bubbleY + 1,
                            (alpha << 24) | 0xFFABB8);
                    if (i % 5 == 0 && bubbleX + 1 < barX + filled - 1) {
                        context.fill(bubbleX + 1, bubbleY + 1, bubbleX + 2, bubbleY + 2,
                                ((alpha / 2) << 24) | 0x6C0B23);
                    }
                }
            }
        }
        // The user-provided Bars.zip renders this 99x23 frame; preserve its style.
        RenderSystem.enableBlend();
        context.drawTexture(STAMINA_FRAME, barX - 9, barY - 9, 0, 0, 99, 23, 99, 23);
        RenderSystem.disableBlend();
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
        drawFullscreen(context, SUN_GRADIENT, width, height, 1.0F, 1.0F, 1.0F, glare * 0.72F);
        drawFullscreen(context, SUN_RAYS, width, height, 1.0F, 1.0F, 1.0F, glare * 0.62F);
        if (exposure > 0.50F) {
            float veins = (exposure - 0.50F) / 0.50F;
            drawFullscreen(context, SUN_VEINS, width, height, 1.0F, 1.0F, 1.0F, veins * 0.82F);
            int redAlpha = (int)(Math.min(0.38F, veins * 0.38F) * 255.0F);
            context.fill(0, 0, width, height, (redAlpha << 24) | 0x6B0000);
        }
    }

    private static void renderBloodSense(DrawContext context, MinecraftClient client, float tickDelta) {
        long remaining = OriginClientState.bloodSenseEffectRemainingMs();
        if (remaining <= 0L || client.player == null) return;

        int width = context.getScaledWindowWidth();
        int height = context.getScaledWindowHeight();
        long duration = OriginClientState.hasFlag("vampire.evolution.lord") ? BLOOD_SENSE_LORD_MS : BLOOD_SENSE_BASE_MS;
        float life = 1.0F - MathHelper.clamp(remaining / (float) duration, 0.0F, 1.0F);
        float pulse = 0.55F + 0.45F * MathHelper.sin((client.player.age + tickDelta) * 0.42F + life * 9.0F);
        // Ease the overlay out in its final 1.6s, instead of one-frame disappearance.
        float fadeOut = MathHelper.clamp(remaining / (float) BLOOD_SENSE_FADE_OUT_MS, 0.0F, 1.0F);
        float fadeIn = MathHelper.clamp((duration - remaining) / (float) BLOOD_SENSE_FADE_IN_MS, 0.0F, 1.0F);
        fadeOut = fadeOut * fadeOut * (3.0F - 2.0F * fadeOut);
        fadeIn = fadeIn * fadeIn * (3.0F - 2.0F * fadeIn);
        float opacity = fadeIn * fadeOut;

        drawFullscreen(context, SUN_GRADIENT, width, height, 0.62F, 0.08F, 0.12F, (0.18F + pulse * 0.10F) * opacity);
        drawFullscreen(context, SUN_RAYS, width, height, 0.94F, 0.14F, 0.18F, (0.10F + pulse * 0.10F) * opacity);
        drawFullscreen(context, SUN_VEINS, width, height, 0.86F, 0.12F, 0.16F, (0.06F + pulse * 0.08F) * opacity);

        long flashRemaining = OriginClientState.bloodSenseFlashRemainingMs();
        if (flashRemaining > 0L) {
            float flash = MathHelper.clamp(flashRemaining / 1250.0F, 0.0F, 1.0F);
            int alpha = (int)(Math.min(0.26F, flash * 0.26F) * 255.0F);
            context.fill(0, 0, width, height, (alpha << 24) | 0xA00016);
        }

        MobEntity target = nearestTrackedMob(client, OriginClientState.hasFlag("vampire.evolution.lord") ? 40.0D : 20.0D);
        if (target != null) {
            drawDirectionTrail(context, client, tickDelta, target, width, height, pulse, opacity);
        }

        int labelAlpha = (int)((0.35F + pulse * 0.35F) * opacity * 255.0F);
        int color = (labelAlpha << 24) | 0xFF6872;
        String label = "Blood Sense";
        int textWidth = client.textRenderer.getWidth(label);
        context.drawTextWithShadow(client.textRenderer, label, width / 2 - textWidth / 2, height / 2 + 26, color);
    }

    private static void renderLordEvolution(DrawContext context, MinecraftClient client, float tickDelta) {
        long remaining = OriginClientState.lordEvolutionEffectRemainingMs();
        if (remaining <= 0L || client.player == null) return;

        int width = context.getScaledWindowWidth();
        int height = context.getScaledWindowHeight();
        float life = 1.0F - MathHelper.clamp(remaining / (float) LORD_EVOLUTION_MS, 0.0F, 1.0F);
        float fadeIn = Math.min(1.0F, life * 3.4F);
        float fadeOut = Math.min(1.0F, remaining / 1600.0F);
        float envelope = fadeIn * fadeOut;
        float pulse = 0.65F + 0.35F * MathHelper.sin((client.player.age + tickDelta) * 0.50F + 0.8F);

        int darkAlpha = (int)(Math.min(0.58F, envelope * 0.58F) * 255.0F);
        context.fill(0, 0, width, height, (darkAlpha << 24) | 0x120006);
        drawFullscreen(context, SUN_GRADIENT, width, height, 0.72F, 0.05F, 0.12F, envelope * 0.30F);
        drawFullscreen(context, SUN_RAYS, width, height, 0.94F, 0.16F, 0.22F, envelope * (0.16F + pulse * 0.16F));
        drawFullscreen(context, SUN_VEINS, width, height, 0.92F, 0.10F, 0.18F, envelope * (0.10F + pulse * 0.10F));

        long flashRemaining = OriginClientState.lordEvolutionFlashRemainingMs();
        if (flashRemaining > 0L) {
            float flash = MathHelper.clamp(flashRemaining / 1800.0F, 0.0F, 1.0F);
            int alpha = (int)(Math.min(0.32F, flash * 0.32F) * 255.0F);
            context.fill(0, 0, width, height, (alpha << 24) | 0xE0141E);
        }

        String title = "Vampire Lord";
        String subtitle = "Crimson Nobility Ascended";
        int titleWidth = client.textRenderer.getWidth(title);
        int subtitleWidth = client.textRenderer.getWidth(subtitle);
        int baseY = height / 2 - 34 - (int)((1.0F - fadeIn) * 14.0F);
        int titleColor = (((int)(Math.min(1.0F, envelope) * 255.0F)) << 24) | 0xFFE0E7;
        int subtitleColor = (((int)(Math.min(0.92F, envelope * 0.92F) * 255.0F)) << 24) | 0xFF9DAA;
        context.drawTextWithShadow(client.textRenderer, title, width / 2 - titleWidth / 2, baseY, titleColor);
        context.drawTextWithShadow(client.textRenderer, subtitle, width / 2 - subtitleWidth / 2, baseY + 14, subtitleColor);
    }

    private static MobEntity nearestTrackedMob(MinecraftClient client, double radius) {
        if (client.player == null || client.world == null) return null;
        double bestDistance = radius * radius;
        MobEntity best = null;
        for (MobEntity mob : client.world.getEntitiesByClass(MobEntity.class,
                client.player.getBoundingBox().expand(radius), MobEntity::isAlive)) {
            double dist = mob.squaredDistanceTo(client.player);
            if (dist > bestDistance) continue;
            if (!mob.isGlowing() && client.player.age % 4 != 0) continue;
            bestDistance = dist;
            best = mob;
        }
        return best;
    }

    private static void drawDirectionTrail(DrawContext context, MinecraftClient client, float tickDelta,
                                           MobEntity target, int width, int height, float pulse, float opacity) {
        Vec3d camera = client.player.getCameraPosVec(tickDelta);
        Vec3d toTarget = target.getPos().add(0.0D, target.getHeight() * 0.55D, 0.0D).subtract(camera);
        double horizontal = Math.sqrt(toTarget.x * toTarget.x + toTarget.z * toTarget.z);
        if (horizontal < 0.0001D) return;

        float yawToTarget = (float)(Math.atan2(toTarget.z, toTarget.x) * 180.0D / Math.PI) - 90.0F;
        float pitchToTarget = (float)(-(Math.atan2(toTarget.y, horizontal) * 180.0D / Math.PI));
        float deltaYaw = MathHelper.wrapDegrees(yawToTarget - client.player.getYaw(tickDelta));
        float deltaPitch = MathHelper.wrapDegrees(pitchToTarget - client.player.getPitch(tickDelta));
        float dirX = MathHelper.clamp(deltaYaw / 75.0F, -1.0F, 1.0F);
        float dirY = MathHelper.clamp(deltaPitch / 55.0F, -1.0F, 1.0F);

        int centerX = width / 2;
        int centerY = height / 2;
        for (int i = 0; i < 5; i++) {
            float step = 0.22F + i * 0.17F;
            int px = centerX + Math.round(dirX * (34.0F + i * 22.0F));
            int py = centerY + Math.round(dirY * (22.0F + i * 14.0F));
            int size = 2 + i;
            int alpha = (int)((0.12F + (1.0F - step) * 0.18F + pulse * 0.10F) * opacity * 255.0F);
            int color = (alpha << 24) | 0xFF3644;
            context.fill(px - size, py - 1, px + size, py + 1, color);
            context.fill(px - 1, py - size, px + 1, py + size, color);
        }
    }

    private static void drawFullscreen(DrawContext context, Identifier texture, int width, int height,
                                       float red, float green, float blue, float alpha) {
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(red, green, blue, Math.max(0.0F, Math.min(1.0F, alpha)));
        context.getMatrices().push();
        context.getMatrices().scale(width / 256.0F, height / 256.0F, 1.0F);
        context.drawTexture(texture, 0, 0, 0.0F, 0.0F, 256, 256, 256, 256);
        context.getMatrices().pop();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }
}
