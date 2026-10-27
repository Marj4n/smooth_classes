package org.marj4n.smooth_classes.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.spell_engine.client.gui.HudRenderHelper;
import org.marj4n.smooth_classes.client.origin.OriginClientState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Renders X over each Spell Engine slot at its actual configurable HUD coordinates. */
@Mixin(HudRenderHelper.SpellHotBarWidget.class)
public abstract class VampireBatSpellHotbarMixin {
    @Inject(method = "render", at = @At("TAIL"), remap = false)
    private static void smooth_classes$spellHotbarLocked(DrawContext context, int screenWidth, int screenHeight,
                                                          HudRenderHelper.SpellHotBarWidget.ViewModel model,
                                                          CallbackInfo ci) {
        if (!"vampire".equals(OriginClientState.originId)
                || !OriginClientState.hasFlag("vampire.form.bat") || model.isEmpty()) return;
        var area = HudRenderHelper.SpellHotBarWidget.lastRendered;
        if (area == null) return;
        var client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden) return;
        int count = model.spells().size();
        int left = (int) area.topLeft().x;
        int top = (int) area.topLeft().y;
        context.getMatrices().push();
        context.getMatrices().translate(0, 0, 350);
        for (int slot = 0; slot < count; slot++) {
            int x = left + 3 + slot * 20;
            int y = top + 3;
            context.fill(x, y, x + 16, y + 16, 0xB0000000);
            context.drawCenteredTextWithShadow(client.textRenderer, net.minecraft.text.Text.literal("X"), x + 8, y + 4, 0xFFFF5555);
        }
        context.getMatrices().pop();
    }
}
