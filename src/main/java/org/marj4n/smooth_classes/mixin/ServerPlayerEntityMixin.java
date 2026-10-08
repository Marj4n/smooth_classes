package org.marj4n.smooth_classes.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.marj4n.smooth_classes.runtime.CombatEventRuntime;
import org.marj4n.smooth_classes.runtime.base.BasePathRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.marj4n.smooth_classes.effects.SmoothEffects;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin {
    @Unique private boolean smooth_classes$lancerPrepared;
    @Unique private float smooth_classes$lancerHealthBefore;
    @Unique private float smooth_classes$lancerAbsorptionBefore;
    @Unique private int smooth_classes$lancerTargetId = Integer.MIN_VALUE;


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
        var originState = org.marj4n.smooth_classes.origin.OriginRuntime.state(player);
        if (originState.origin() == org.marj4n.smooth_classes.origin.OriginType.VAMPIRE
                && originState.hasFlag("vampire.form.bat")
                && source.isOf(net.minecraft.entity.damage.DamageTypes.IN_WALL)) {
            // Tight-space travel form: the bat must never die just because a resize or
            // high-speed edge contact briefly overlaps a solid collision shape.
            cir.setReturnValue(false);
            return;
        }
        if (!CombatEventRuntime.onIncomingDamage(player, source, amount)) cir.setReturnValue(false);
    }

    @Inject(method="damage", at=@At("RETURN"))
    private void smooth_classes$afterDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) CombatEventRuntime.afterIncomingDamage((ServerPlayerEntity)(Object)this, source, amount);
    }

    @Inject(method="attack", at=@At("HEAD"), cancellable=true)
    private void smooth_classes$attack(Entity target, CallbackInfo ci) {
        ServerPlayerEntity player=(ServerPlayerEntity)(Object)this;
        if (org.marj4n.smooth_classes.origin.OriginSkillRuntime.blockVampireCombat(player)) {
            ci.cancel();
            return;
        }
        smooth_classes$lancerPrepared=org.marj4n.smooth_classes.content.lancer.runtime.LancerRuntime.prepareDirectSpearAttack(player);
        if(target instanceof net.minecraft.entity.LivingEntity living){
            smooth_classes$lancerTargetId=target.getId();
            smooth_classes$lancerHealthBefore=living.getHealth();
            smooth_classes$lancerAbsorptionBefore=living.getAbsorptionAmount();
        }else smooth_classes$lancerTargetId=Integer.MIN_VALUE;
        CombatEventRuntime.onMeleeAttack(player, target);
    }

    @Inject(method="attack", at=@At("RETURN"))
    private void smooth_classes$afterAttack(Entity target, CallbackInfo ci) {
        boolean landed=false;
        net.minecraft.entity.LivingEntity landedTarget=null;
        if(target instanceof net.minecraft.entity.LivingEntity living
                && target.getId()==smooth_classes$lancerTargetId){
            landed=living.getHealth()<smooth_classes$lancerHealthBefore
                    || living.getAbsorptionAmount()<smooth_classes$lancerAbsorptionBefore
                    || !living.isAlive();
            if (landed) landedTarget=living;
        }
        ServerPlayerEntity player=(ServerPlayerEntity)(Object)this;
        org.marj4n.smooth_classes.content.lancer.runtime.LancerRuntime.finishDirectSpearAttack(
                player, smooth_classes$lancerPrepared, smooth_classes$lancerPrepared && landed);
        if (landedTarget != null) CombatEventRuntime.afterMeleeAttack(player, landedTarget);
        smooth_classes$lancerPrepared=false;
        smooth_classes$lancerTargetId=Integer.MIN_VALUE;
    }


    @Inject(method="onDeath", at=@At("HEAD"))
    private void smooth_classes$death(DamageSource source, CallbackInfo ci) {
        CombatEventRuntime.onDeath((ServerPlayerEntity)(Object)this, source);
    }
    @Inject(method="copyFrom", at=@At("TAIL"))
    private void smooth_classes$copyOrigin(ServerPlayerEntity oldPlayer, boolean alive, CallbackInfo ci) {
        var self = (ServerPlayerEntity)(Object)this;
        var from = ((org.marj4n.smooth_classes.origin.OriginDataHolder) oldPlayer).smooth_classes$getOriginState();
        var to = ((org.marj4n.smooth_classes.origin.OriginDataHolder) self).smooth_classes$getOriginState();
        to.copyFrom(from);
    }

}
