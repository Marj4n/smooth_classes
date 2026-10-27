package org.marj4n.smooth_classes.mixin.client;

import net.minecraft.client.render.entity.feature.StuckArrowsFeatureRenderer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.marj4n.smooth_classes.origin.OriginRuntime;
import org.marj4n.smooth_classes.origin.OriginType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Bat Form replaces the humanoid player mesh with a vanilla-bat model.
 * Vanilla's stuck-arrow feature still samples attachment points from the hidden
 * humanoid model, which makes arrows appear one or two blocks above the bat and
 * falsely look like the collision box is still player-sized.
 *
 * The authoritative Bat Form collision remains 0.5 x 0.5 blocks. Hide the
 * incompatible humanoid stuck-arrow visual while transformed instead of
 * rendering it at bogus player-model coordinates.
 */
@Mixin(StuckArrowsFeatureRenderer.class)
public abstract class BatStuckArrowsFeatureMixin {
    @Inject(method = "getObjectCount", at = @At("HEAD"), cancellable = true)
    private void smooth_classes$hideHumanoidBatArrows(LivingEntity entity,
                                                       CallbackInfoReturnable<Integer> cir) {
        if (!(entity instanceof PlayerEntity player)) return;
        var state = OriginRuntime.state(player);
        if (state.origin() == OriginType.VAMPIRE && state.hasFlag("vampire.form.bat")) {
            cir.setReturnValue(0);
        }
    }
}
