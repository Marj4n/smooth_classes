package org.marj4n.smooth_classes.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.marj4n.smooth_classes.runtime.CombatEventRuntime;
import org.marj4n.smooth_classes.runtime.base.BasePathRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.marj4n.smooth_classes.effects.SmoothEffects;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin {

    @Inject(method="tickFallStartPos", at=@At("HEAD"))
    private void smooth_classes$fallTick(CallbackInfo ci) {
        BasePathRuntime.onFallTick((ServerPlayerEntity)(Object)this);
    }

    @ModifyVariable(method="damage", at=@At("HEAD"), argsOnly=true)
    private float smooth_classes$modifyIncomingDamage(float amount) {
        ServerPlayerEntity player=(ServerPlayerEntity)(Object)this;
        if(player.hasStatusEffect(SmoothEffects.BONE_ARMOR))
            amount*=org.marj4n.smooth_classes.runtime.AscendancyBalance.boneMultiplier(org.marj4n.smooth_classes.runtime.AscendancyRuntime.points(player));
        if (player.hasStatusEffect(SmoothEffects.RAGE)) {
            float modifier=1F + player.getStatusEffect(SmoothEffects.RAGE).getAmplifier()/200F;
            return amount * modifier;
        }
        return amount;
    }

    @Inject(method="damage", at=@At("HEAD"), cancellable=true)
    private void smooth_classes$damage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        ServerPlayerEntity player=(ServerPlayerEntity)(Object)this;
        if (!CombatEventRuntime.onIncomingDamage(player, source, amount)) cir.setReturnValue(false);
    }

    @Inject(method="damage", at=@At("RETURN"))
    private void smooth_classes$afterDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) CombatEventRuntime.afterIncomingDamage((ServerPlayerEntity)(Object)this, source, amount);
    }

    @Inject(method="attack", at=@At("HEAD"))
    private void smooth_classes$attack(Entity target, CallbackInfo ci) {
        CombatEventRuntime.onMeleeAttack((ServerPlayerEntity)(Object)this, target);
    }


    @Inject(method="onDeath", at=@At("HEAD"))
    private void smooth_classes$death(DamageSource source, CallbackInfo ci) {
        CombatEventRuntime.onDeath((ServerPlayerEntity)(Object)this, source);
    }
}
