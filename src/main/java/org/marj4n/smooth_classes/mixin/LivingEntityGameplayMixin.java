package org.marj4n.smooth_classes.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import org.marj4n.smooth_classes.effects.SmoothEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Core rules that belong to every living entity, including nonplayers. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityGameplayMixin {
    @Inject(method = "canTarget(Lnet/minecraft/entity/LivingEntity;)Z", at = @At("HEAD"), cancellable = true)
    private void smooth_classes$ignoreStealth(LivingEntity target, CallbackInfoReturnable<Boolean> cir) {
        if (target.hasStatusEffect(SmoothEffects.STEALTH)) cir.setReturnValue(false);
    }

    @Inject(method = "isDead", at = @At("HEAD"), cancellable = true)
    private void smooth_classes$undying(CallbackInfoReturnable<Boolean> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity.getHealth() <= 0F && entity.hasStatusEffect(SmoothEffects.UNDYING)) {
            entity.setHealth(1F);
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "addStatusEffect(Lnet/minecraft/entity/effect/StatusEffectInstance;)Z",
            at = @At("HEAD"), cancellable = true)
    private void smooth_classes$barrierBlocksCrowdControl(StatusEffectInstance incoming,
                                                          CallbackInfoReturnable<Boolean> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (!entity.hasStatusEffect(SmoothEffects.BARRIER)) return;
        var effect = incoming.getEffectType();
        if (effect == SmoothEffects.IMMOBILIZE || effect == StatusEffects.SLOWNESS
                || effect == StatusEffects.BLINDNESS || effect == StatusEffects.LEVITATION
                || effect == StatusEffects.MINING_FATIGUE) cir.setReturnValue(false);
    }
}
