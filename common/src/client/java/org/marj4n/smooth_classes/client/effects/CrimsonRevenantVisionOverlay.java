package org.marj4n.smooth_classes.client.effects;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import org.marj4n.smooth_classes.SmoothClasses;
import org.marj4n.smooth_classes.content.berserker.runtime.BerserkerSpecialRuntime;
import org.marj4n.smooth_classes.effects.SmoothEffects;

/**
 * First-person Crimson Revenant vision effect.
 * Adds a red pulsing tint and semi-organic vein overlays while charging or frenzied.
 */
public final class CrimsonRevenantVisionOverlay {
    private static final Identifier TINT = SmoothClasses.id("textures/gui/crimson_revenant_tint.png");
    private static final Identifier VEINS = SmoothClasses.id("textures/gui/crimson_revenant_veins.png");

    private CrimsonRevenantVisionOverlay() {}

    public static void render(DrawContext context, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden || !client.options.getPerspective().isFirstPerson()) return;

        var active = client.player.getStatusEffect(SmoothEffects.CRIMSON_REVENANT);
        var charge = client.player.getStatusEffect(SmoothEffects.CRIMSON_REVENANT_CHARGE);
        if (active == null && charge == null) return;

        int width = client.getWindow().getScaledWidth();
        int height = client.getWindow().getScaledHeight();
        float time = client.player.age + tickDelta;
        float pulse = 0.5F + 0.5F * (float) Math.sin(time * 0.28F);

        float tintAlpha;
        float veinAlpha;
        if (active != null) {
            float endFade = active.getDuration() <= 40 ? Math.max(0.35F, active.getDuration() / 40.0F) : 1.0F;
            tintAlpha = (0.10F + pulse * 0.05F) * endFade;
            veinAlpha = (0.24F + pulse * 0.18F) * endFade;
        } else {
            int elapsed = Math.max(0, (BerserkerSpecialRuntime.CHARGE_TICKS + 40) - charge.getDuration());
            float progress = Math.min(1.0F, elapsed / (float) BerserkerSpecialRuntime.CHARGE_TICKS);
            tintAlpha = 0.04F + progress * 0.08F + pulse * 0.03F;
            veinAlpha = 0.10F + progress * 0.18F + pulse * 0.08F;
        }

        drawFull(context, TINT, width, height, 0.95F, 0.12F, 0.12F, tintAlpha);
        drawFull(context, VEINS, width, height, 1.0F, 0.22F, 0.22F, veinAlpha);
    }

    private static void drawFull(DrawContext context, Identifier texture, int width, int height,
                                 float r, float g, float b, float a) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(r, g, b, a);
        context.drawTexture(texture, 0, 0, 0, 0, width, height, 512, 512);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }
}
