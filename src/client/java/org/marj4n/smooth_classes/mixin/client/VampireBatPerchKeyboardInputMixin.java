package org.marj4n.smooth_classes.mixin.client;

import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import org.marj4n.smooth_classes.client.origin.OriginClientState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Freeze controls *after* vanilla keyboard polling so WASD cannot override the roost. */
@Mixin(KeyboardInput.class)
public abstract class VampireBatPerchKeyboardInputMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void smooth_classes$lockPerchedBatKeyboard(boolean slowDown, float slowDownFactor, CallbackInfo ci) {
        if (!"vampire".equals(OriginClientState.originId)
                || !OriginClientState.hasFlag("vampire.form.bat")
                || !OriginClientState.hasFlag("vampire.bat.perched")) return;
        Input input = (Input) (Object) this;
        input.movementForward = 0.0F;
        input.movementSideways = 0.0F;
        input.pressingForward = false;
        input.pressingBack = false;
        input.pressingLeft = false;
        input.pressingRight = false;
        input.jumping = false;
        // input.sneaking stays true on Shift: this is the detach command.
    }
}
