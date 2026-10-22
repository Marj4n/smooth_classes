package org.marj4n.smooth_classes.mixin.client;

import org.marj4n.smooth_classes.client.origin.OriginClientState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Optional client-only compatibility: custom Vampire Blood replaces FOOD stamina. */
@Mixin(targets = "dev.muon.dynamic_resource_bars.render.StaminaBarRenderer", remap = false)
public abstract class DynamicResourceBarsVampireMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private static void smooth_classes$useVampireBlood(CallbackInfo ci) {
        if ("vampire".equals(OriginClientState.originId)) ci.cancel();
    }
}
