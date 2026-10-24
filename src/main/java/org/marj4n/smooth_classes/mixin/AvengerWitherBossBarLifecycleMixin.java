package org.marj4n.smooth_classes.mixin;

import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.marj4n.smooth_classes.content.avenger.runtime.AvengerReworkRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * WitherEntity recreates its vanilla boss bar on world load and resumes
 * tracking when players rejoin. Stay has NoAI, so mobTick is NOT a reliable
 * place to hide it; use WitherEntity.tickMovement and tracking instead.
 */
@Mixin(WitherEntity.class)
public abstract class AvengerWitherBossBarLifecycleMixin {
    @Inject(method = "onStartedTrackingBy", at = @At("HEAD"))
    private void smoothClasses$hideBeforeTracking(ServerPlayerEntity player, CallbackInfo ci) {
        smoothClasses$syncBossBar();
    }

    @Inject(method = "tickMovement", at = @At("HEAD"))
    private void smoothClasses$hideOnMovementTick(CallbackInfo ci) {
        smoothClasses$syncBossBar();
    }

    private void smoothClasses$syncBossBar() {
        WitherEntity wither = (WitherEntity) (Object) this;
        if (wither.getWorld().isClient) return;
        if (AvengerReworkRuntime.isAvengerSummon(wither)) {
            var bar = ((AvengerWitherBossBarAccessor) wither).smoothClasses$getBossBar();
            if (bar.isVisible()) bar.setVisible(false);
        }
    }
}
