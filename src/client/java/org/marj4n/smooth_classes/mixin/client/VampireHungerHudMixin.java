package org.marj4n.smooth_classes.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.util.Identifier;
import org.marj4n.smooth_classes.client.origin.OriginClientState;
import org.marj4n.smooth_classes.origin.OriginType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Completely removes vanilla hunger-meat icons for Vampire; Blood HUD owns that slot. */
@Mixin(InGameHud.class)
public abstract class VampireHungerHudMixin {
    @Redirect(
            method = "renderStatusBars",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawTexture(Lnet/minecraft/util/Identifier;IIIIII)V"),
            require = 0
    )
    private void smooth_classes$hideVampireFoodIcons(DrawContext context, Identifier texture,
                                                       int x, int y, int u, int v, int width, int height) {
        MinecraftClient client = MinecraftClient.getInstance();
        OriginType origin = OriginType.byId(OriginClientState.originId).orElse(null);
        // In 1.20.1 vanilla food icons live on v=27 of textures/gui/icons.png.
        if (client.player != null && origin == OriginType.VAMPIRE && v == 27 && width == 9 && height == 9) {
            return;
        }
        context.drawTexture(texture, x, y, u, v, width, height);
    }
}
