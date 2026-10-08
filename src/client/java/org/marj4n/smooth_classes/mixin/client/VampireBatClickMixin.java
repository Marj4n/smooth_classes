package org.marj4n.smooth_classes.mixin.client;

import net.minecraft.client.MinecraftClient;
import org.marj4n.smooth_classes.client.origin.OriginClientState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Suppress vanilla client attack, block-breaking hold, and use input immediately. */
@Mixin(MinecraftClient.class)
public abstract class VampireBatClickMixin {
    private static boolean bat() {
        return "vampire".equals(OriginClientState.originId)
                && OriginClientState.hasFlag("vampire.form.bat");
    }

    @Inject(method = "doAttack", at = @At("HEAD"), cancellable = true)
    private void smooth_classes$noBatAttack(CallbackInfoReturnable<Boolean> cir) {
        if (bat()) cir.setReturnValue(false);
    }

    @Inject(method = "doItemUse", at = @At("HEAD"), cancellable = true)
    private void smooth_classes$noBatUse(CallbackInfo ci) {
        if (bat()) ci.cancel();
    }

    @Inject(method = "handleBlockBreaking", at = @At("HEAD"), cancellable = true)
    private void smooth_classes$noBatMining(boolean breaking, CallbackInfo ci) {
        if (bat()) ci.cancel();
    }
}
