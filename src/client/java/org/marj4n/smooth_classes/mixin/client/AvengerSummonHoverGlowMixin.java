package org.marj4n.smooth_classes.mixin.client;

import net.minecraft.entity.Entity;
import org.marj4n.smooth_classes.client.AvengerSummonHoverGlow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Local-only vanilla outline, visible exclusively while aiming at an owned bound soul. */
@Mixin(Entity.class)
public abstract class AvengerSummonHoverGlowMixin {
    @Inject(method = "isGlowing", at = @At("HEAD"), cancellable = true)
    private void smooth_classes$boundSoulHoverOutline(CallbackInfoReturnable<Boolean> cir) {
        if (AvengerSummonHoverGlow.isHighlighted((Entity) (Object) this)) cir.setReturnValue(true);
    }
}
