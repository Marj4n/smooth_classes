package org.marj4n.smooth_classes.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.WardenEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.marj4n.smooth_classes.effects.SmoothEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Core rules that belong to every living entity, including nonplayers. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityGameplayMixin {
    @org.spongepowered.asm.mixin.injection.ModifyVariable(method = "damage", at = @At("HEAD"), argsOnly = true)
    private float smooth_classes$shadowDamage(float amount, DamageSource source, float original) {
        amount = org.marj4n.smooth_classes.content.assassin.runtime.ShadowTechniqueRuntime.empower(source, amount);
        if (source.getSource() instanceof PersistentProjectileEntity
                && source.getAttacker() instanceof ServerPlayerEntity attacker
                && attacker.hasStatusEffect(SmoothEffects.MARKSMANSHIP)) {
            StatusEffectInstance marksmanship = attacker.getStatusEffect(SmoothEffects.MARKSMANSHIP);
            if (marksmanship != null) amount *= 1.0F + 0.10F * (marksmanship.getAmplifier() + 1);
        }
        return amount;
    }
    @Inject(method = "damage", at = @At("RETURN"))
    private void smooth_classes$confirmedHit(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() || amount <= 0) return;
        org.marj4n.smooth_classes.content.assassin.runtime.ShadowTechniqueRuntime.confirmedHit(source);
        org.marj4n.smooth_classes.runtime.BloodRainRuntime.confirmedHit((LivingEntity)(Object)this, source);
    }

    @Inject(method = "canTarget(Lnet/minecraft/entity/LivingEntity;)Z", at = @At("HEAD"), cancellable = true)
    private void smooth_classes$filterTargets(LivingEntity target, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (target.hasStatusEffect(SmoothEffects.STEALTH)
                || org.marj4n.smooth_classes.content.avenger.runtime.AvengerReworkRuntime
                .shouldIgnoreSummonTarget(self, target)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "isDead", at = @At("HEAD"), cancellable = true)
    private void smooth_classes$undying(CallbackInfoReturnable<Boolean> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity.getHealth() <= 0F && entity.hasStatusEffect(SmoothEffects.UNDYING)) {
            entity.setHealth(1F);
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void smooth_classes$riderChargeProtection(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (org.marj4n.smooth_classes.content.rider.runtime.RiderRuntime.isChargeProtected(entity)
                || org.marj4n.smooth_classes.content.avenger.runtime.AvengerReworkRuntime.shouldCancelFriendlyDamage(entity, source)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "onDeath", at = @At("HEAD"))
    private void smooth_classes$creditAvengerSummonKill(DamageSource source, CallbackInfo ci) {
        org.marj4n.smooth_classes.content.avenger.runtime.AvengerReworkRuntime.onSummonKilledOther(
                (LivingEntity) (Object) this, source);
    }

    @Inject(method = "addStatusEffect(Lnet/minecraft/entity/effect/StatusEffectInstance;)Z",
            at = @At("HEAD"), cancellable = true)
    private void smooth_classes$barrierBlocksCrowdControl(StatusEffectInstance incoming,
                                                          CallbackInfoReturnable<Boolean> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (smooth_classes$blockDarknessOrBarrierCrowdControl(entity, incoming, null)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "addStatusEffect(Lnet/minecraft/entity/effect/StatusEffectInstance;Lnet/minecraft/entity/Entity;)Z",
            at = @At("HEAD"), cancellable = true)
    private void smooth_classes$blockSourcedWardenDarkness(StatusEffectInstance incoming,
                                                           net.minecraft.entity.Entity source,
                                                           CallbackInfoReturnable<Boolean> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (smooth_classes$blockDarknessOrBarrierCrowdControl(entity, incoming, source)) {
            cir.setReturnValue(false);
        }
    }

    @org.spongepowered.asm.mixin.Unique
    private static boolean smooth_classes$blockDarknessOrBarrierCrowdControl(
            LivingEntity entity,
            StatusEffectInstance incoming,
            net.minecraft.entity.Entity source) {
        var effect = incoming.getEffectType();
        if (effect == StatusEffects.DARKNESS) {
            if (source instanceof WardenEntity
                    && org.marj4n.smooth_classes.content.avenger.runtime.AvengerReworkRuntime
                    .shouldBlockSummonedWardenDarknessFrom(entity, source)) {
                return true;
            }
            // Fallback for vanilla/modded call paths that omit the source entity.
            if (source == null
                    && org.marj4n.smooth_classes.content.avenger.runtime.AvengerReworkRuntime
                    .shouldBlockSummonedWardenDarkness(entity)) {
                return true;
            }
        }
        if (!entity.hasStatusEffect(SmoothEffects.BARRIER)) return false;
        return effect == SmoothEffects.IMMOBILIZE || effect == StatusEffects.SLOWNESS
                || effect == StatusEffects.BLINDNESS || effect == StatusEffects.LEVITATION
                || effect == StatusEffects.MINING_FATIGUE;
    }
}
