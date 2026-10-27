package org.marj4n.smooth_classes.mixin.client;

import net.minecraft.client.input.Input;
import net.minecraft.client.network.ClientPlayerEntity;
import org.marj4n.smooth_classes.client.origin.OriginClientState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Stop walking/flying controls while upside-down. Sneak is left intact to release. */
@Mixin(ClientPlayerEntity.class)
public abstract class VampireBatPerchControlMixin {
    @Shadow public Input input;

    @Inject(method = "tickMovement", at = @At("HEAD"))
    private void smooth_classes$freezeCeilingPerch(CallbackInfo ci) {
        if (!"vampire".equals(OriginClientState.originId)
                || !OriginClientState.hasFlag("vampire.form.bat")
                || !OriginClientState.hasFlag("vampire.bat.perched")) return;
        ClientPlayerEntity player = (ClientPlayerEntity) (Object) this;
        input.movementForward = 0.0F;
        input.movementSideways = 0.0F;
        input.jumping = false;
        // Keep sneaking input. Shift detaches the bat server-side.
        player.setSprinting(false);
        player.setVelocity(0.0D, 0.0D, 0.0D);
    }
}
