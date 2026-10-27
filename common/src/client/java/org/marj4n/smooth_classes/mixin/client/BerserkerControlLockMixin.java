package org.marj4n.smooth_classes.mixin.client;

import net.minecraft.client.input.Input;
import net.minecraft.client.network.ClientPlayerEntity;
import org.marj4n.smooth_classes.effects.SmoothEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Crimson Revenant is deliberately not player-steerable. Zero the local movement
 * input immediately before ClientPlayerEntity consumes it, while the server
 * remains responsible for the berserker's pursuit velocity and facing.
 */
@Mixin(ClientPlayerEntity.class)
public abstract class BerserkerControlLockMixin {
    @Shadow public Input input;

    @Inject(method = "tickMovement", at = @At("HEAD"))
    private void smooth_classes$lockCrimsonRevenantMovement(CallbackInfo ci) {
        ClientPlayerEntity player = (ClientPlayerEntity) (Object) this;
        boolean charging = player.hasStatusEffect(SmoothEffects.CRIMSON_REVENANT_CHARGE);
        boolean active = player.hasStatusEffect(SmoothEffects.CRIMSON_REVENANT);
        if (!charging && !active) return;

        input.movementForward = 0.0F;
        input.movementSideways = 0.0F;
        input.jumping = false;
        input.sneaking = false;
        // Charge roots the player. During frenzy, sprint is controlled by the AI/server
        // so the local client should visually remain in the sprinting state as well.
        player.setSprinting(active);
    }
}
