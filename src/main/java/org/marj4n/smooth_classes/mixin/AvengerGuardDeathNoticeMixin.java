package org.marj4n.smooth_classes.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import org.marj4n.smooth_classes.content.avenger.runtime.AvengerReworkRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A concise action-bar warning distinguishes combat death from natural despawn. */
@Mixin(LivingEntity.class)
public abstract class AvengerGuardDeathNoticeMixin {
    @Inject(method = "onDeath", at = @At("HEAD"))
    private void smooth_classes$notifyGuardDeath(DamageSource source, CallbackInfo ci) {
        AvengerReworkRuntime.notifyGuardDeath((LivingEntity) (Object) this);
    }
}
