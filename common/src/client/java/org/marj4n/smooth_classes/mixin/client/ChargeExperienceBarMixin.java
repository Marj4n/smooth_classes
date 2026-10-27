package org.marj4n.smooth_classes.mixin.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.marj4n.smooth_classes.client.charge.ChargeHudState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * While Portal of Sovereignty or Arcane Slash is winding up, the normal XP bar is replaced with
 * Spell Engine's own cast-bar texture. Player XP/level values remain untouched.
 */
@Mixin(InGameHud.class)
public abstract class ChargeExperienceBarMixin {
    private static final Identifier CAST_BAR =
            new Identifier("spell_engine", "textures/hud/castbar.png");
    private static final int BAR_WIDTH = 182;
    private static final int BAR_HEIGHT = 5;

    @Inject(method = "renderExperienceBar", at = @At("HEAD"), cancellable = true)
    private void smooth_classes$renderChargeBar(DrawContext context, int x, CallbackInfo ci) {
        if (!ChargeHudState.active()) return;
        ci.cancel();

        int y = context.getScaledWindowHeight() - 29;
        int tint = ChargeHudState.tint();
        float red = ((tint >> 16) & 0xFF) / 255F;
        float green = ((tint >> 8) & 0xFF) / 255F;
        float blue = (tint & 0xFF) / 255F;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        context.setShaderColor(red, green, blue, 1F);

        // Spell Engine 1.10.7 castbar.png: top 5px = fill, bottom 5px = frame/background.
        context.drawTexture(CAST_BAR, x, y, 0F, 5F,
                BAR_WIDTH, BAR_HEIGHT, BAR_WIDTH, 10);

        int fill = MathHelper.clamp(Math.round(BAR_WIDTH * ChargeHudState.progress()), 0, BAR_WIDTH);
        if (fill > 0) {
            context.drawTexture(CAST_BAR, x, y, 0F, 0F,
                    fill, BAR_HEIGHT, BAR_WIDTH, 10);
        }

        context.setShaderColor(1F, 1F, 1F, 1F);
        RenderSystem.disableBlend();

        MinecraftClient client = MinecraftClient.getInstance();
        String label = ChargeHudState.label();
        int textX = context.getScaledWindowWidth() / 2 - client.textRenderer.getWidth(label) / 2;
        context.drawTextWithShadow(client.textRenderer, label, textX, y - 10, 0xFFFFFFFF);
    }
}
