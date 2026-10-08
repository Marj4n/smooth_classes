package org.marj4n.smooth_classes.mixin;

import net.minecraft.entity.player.HungerManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.marj4n.smooth_classes.origin.OriginRuntime;
import org.marj4n.smooth_classes.origin.OriginType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Vampire nutrition belongs exclusively to OriginState.blood, not to the
 * invisible vanilla hunger/saturation meter. Other Origins are untouched.
 */
@Mixin(HungerManager.class)
public abstract class VampireHungerManagerMixin {
    @Shadow private float exhaustion;
    @Shadow private int foodTickTimer;

    @Inject(method = "update", at = @At("HEAD"), cancellable = true)
    private void smooth_classes$bloodReplacesHunger(PlayerEntity player, CallbackInfo ci) {
        if (!(player instanceof ServerPlayerEntity)
                || OriginRuntime.state(player).origin() != OriginType.VAMPIRE) return;
        // Flush exhaustion/timers accrued during vampire gameplay; otherwise
        // they would consume regular food when the player switches Origins.
        this.exhaustion = 0.0F;
        this.foodTickTimer = 0;
        ci.cancel();
    }
}
