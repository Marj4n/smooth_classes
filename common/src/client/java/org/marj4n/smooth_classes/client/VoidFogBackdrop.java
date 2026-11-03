package org.marj4n.smooth_classes.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.puffish.skillsmod.util.Bounds2i;

/** Approved void-fog concept, layered around the unchanged original blinking eye.
 * Viewport-space decoration only; does not move or replace any skill nodes. */
public final class VoidFogBackdrop {
    private static final Identifier BACK =
            Identifier.of("smooth_classes", "textures/backgrounds/decor/void_fog_approved.png");
    private static final Identifier VEIL =
            Identifier.of("smooth_classes", "textures/backgrounds/decor/void_fog_veil.png");
    private static final Identifier GLOW =
            Identifier.of("smooth_classes", "textures/backgrounds/decor/void_eye_glow.png");
    private static final int WIDTH = 1774;
    private static final int HEIGHT = 887;

    private VoidFogBackdrop() {}

    public static void draw(DrawContext context, Bounds2i bounds, long now) {
        layer(context, bounds, BACK, phase(now, 18000L), 1F);
    }

    public static void drawVeil(DrawContext context, Bounds2i bounds, long now) {
        layer(context, bounds, VEIL, -phase(now, 23000L) + 0.55, 150F / 255F);
    }

    /** Called in the original 64px eye's local matrix, immediately before its sprite. */
    public static void drawEyeGlow(DrawContext context, float eyeAlpha) {
        if (eyeAlpha <= 0.01F) return;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        try {
            RenderSystem.setShaderColor(1F, 1F, 1F, Math.min(1F, eyeAlpha));
            context.drawTexture(GLOW, -28, -7, 120, 78, 0F, 0F, 180, 116, 180, 116);
        } finally {
            // The caller restores its own eye tint and completes its blend scope.
            RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        }
    }

    private static double phase(long now, long period) {
        return Math.floorMod(now, period) * (Math.PI * 2D / period);
    }

    private static void layer(
            DrawContext context, Bounds2i bounds, Identifier texture, double phase, float alpha) {
        if (bounds.width() <= 0 || bounds.height() <= 0) return;
        float scale =
                Math.max(bounds.width() / (WIDTH * 0.94F), bounds.height() / (HEIGHT * 0.94F));
        float extraX = WIDTH * scale - bounds.width();
        float extraY = HEIGHT * scale - bounds.height();
        float x = bounds.min().x - extraX * (0.5F + 0.25F * (float) Math.sin(phase));
        float y = bounds.min().y - extraY * (0.5F + 0.25F * (float) Math.cos(phase));
        MatrixStack matrices = context.getMatrices();
        matrices.push();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        try {
            matrices.translate(x, y, 0F);
            matrices.scale(scale, scale, 1F);
            RenderSystem.setShaderColor(1F, 1F, 1F, alpha);
            context.drawTexture(texture, 0, 0, 0F, 0F, WIDTH, HEIGHT, WIDTH, HEIGHT);
        } finally {
            RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
            RenderSystem.disableBlend();
            matrices.pop();
        }
    }
}
