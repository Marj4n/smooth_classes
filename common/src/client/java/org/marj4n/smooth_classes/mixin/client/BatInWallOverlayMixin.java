package org.marj4n.smooth_classes.mixin.client;

import net.minecraft.block.BlockState;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.entity.player.PlayerEntity;
import org.marj4n.smooth_classes.origin.OriginRuntime;
import org.marj4n.smooth_classes.origin.OriginType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Bat Form uses a real half-block body and a quarter-block eye height.
 * Vanilla's in-wall overlay samples several points around the player's eye and
 * can briefly report a solid block while a tiny bat is legitimately moving
 * through a one-block opening or jumping under a low ceiling.  That produces
 * the black "inside block" flash even though collision has not placed the bat
 * inside a solid block.
 *
 * Only the client-side overlay probe is suppressed.  Actual collision remains
 * fully authoritative, so Bat Form still cannot move through solid blocks and
 * this does not grant noclip or disable real collision handling.
 */
@Mixin(InGameOverlayRenderer.class)
public abstract class BatInWallOverlayMixin {

    @Inject(method = "getInWallBlockState", at = @At("HEAD"), cancellable = true)
    private static void smooth_classes$skipFalseBatInWallOverlay(
            PlayerEntity player,
            CallbackInfoReturnable<BlockState> cir) {
        var state = OriginRuntime.state(player);
        if (state.origin() == OriginType.VAMPIRE && state.hasFlag("vampire.form.bat")) {
            cir.setReturnValue(null);
        }
    }
}
