package org.marj4n.smooth_classes.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import org.marj4n.smooth_classes.origin.OriginRuntime;
import org.marj4n.smooth_classes.origin.OriginType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class ManBatNoFallDamageMixin {
    @Inject(method = "handleFallDamage", at = @At("HEAD"), cancellable = true)
    private void smooth_classes$manBatNoFall(float fallDistance, float damageMultiplier,
                                              DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        if ((Object)this instanceof PlayerEntity player) {
            var state = OriginRuntime.state(player);
            if (state.origin() == OriginType.VAMPIRE && state.hasFlag("vampire.form.man_bat")) {
                cir.setReturnValue(false);
            }
        }
    }
}
