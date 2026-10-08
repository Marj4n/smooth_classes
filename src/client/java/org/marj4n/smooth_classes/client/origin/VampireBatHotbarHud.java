package org.marj4n.smooth_classes.client.origin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/** Locked vanilla inventory hotbar; visual only (server packet gates protect the actual world). */
public final class VampireBatHotbarHud {
    private VampireBatHotbarHud() { }

    public static void render(DrawContext context, float delta) {
        var client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden || client.currentScreen != null
                || !"vampire".equals(OriginClientState.originId)
                || !OriginClientState.hasFlag("vampire.form.bat")) return;
        int left = client.getWindow().getScaledWidth() / 2 - 91;
        int y = client.getWindow().getScaledHeight() - 22;
        context.getMatrices().push();
        context.getMatrices().translate(0, 0, 350);
        for (int i = 0; i < 9; i++) {
            int x = left + 3 + i * 20;
            context.fill(x, y + 3, x + 16, y + 19, 0xB0000000);
            context.drawCenteredTextWithShadow(client.textRenderer, net.minecraft.text.Text.literal("X"), x + 8, y + 7, 0xFFFF5555);
        }
        context.getMatrices().pop();
    }
}
