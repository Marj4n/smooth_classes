package org.marj4n.smooth_classes.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.marj4n.smooth_classes.runtime.CombatEventRuntime;
import org.marj4n.smooth_classes.runtime.base.BasePathRuntime;
import org.marj4n.smooth_classes.runtime.classpass.ClassPassiveRuntime;
import org.marj4n.smooth_classes.runtime.AscendancyRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {
    @Inject(method="onKilledOther", at=@At("HEAD"))
    private void smooth_classes$killedOther(ServerWorld world, LivingEntity other, CallbackInfoReturnable<Boolean> cir) {
        if ((Object)this instanceof ServerPlayerEntity player) {
            CombatEventRuntime.onKilledOther(player, world, other);
        }
    }

    @Inject(method="takeShieldHit", at=@At("HEAD"))
    private void smooth_classes$takeShieldHit(LivingEntity attacker, CallbackInfo ci) {
        if ((Object)this instanceof ServerPlayerEntity player) {
            BasePathRuntime.onShieldHit(player, attacker);
            ClassPassiveRuntime.onShieldHit(player, attacker);
            AscendancyRuntime.shieldHit(player);
        }
    }
}
