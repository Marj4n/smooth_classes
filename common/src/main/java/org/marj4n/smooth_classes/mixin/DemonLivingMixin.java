package org.marj4n.smooth_classes.mixin;

import net.minecraft.entity.*;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import org.marj4n.smooth_classes.origin.*;
import org.marj4n.smooth_classes.origin.demon.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(LivingEntity.class)
public abstract class DemonLivingMixin implements DemonEntityData.Holder {
    @Unique private final DemonEntityData smooth_classes$demonData=new DemonEntityData();
    @Override public DemonEntityData smooth_classes$demon(){return smooth_classes$demonData;}
    @Inject(method="writeCustomDataToNbt",at=@At("TAIL"))
    private void demon$write(NbtCompound n,CallbackInfo ci){var d=smooth_classes$demonData;if(d.birthRecorded||d.commander!=null||d.curseOwner!=null||d.provokedBy!=null)n.put("SmoothClassesDemon",d.write());}
    @Inject(method="readCustomDataFromNbt",at=@At("TAIL"))
    private void demon$read(NbtCompound n,CallbackInfo ci){if(n.contains("SmoothClassesDemon"))smooth_classes$demonData.read(n.getCompound("SmoothClassesDemon"));}
    @Inject(method="tick",at=@At("TAIL"))
    private void demon$tick(CallbackInfo ci){if(smooth_classes$demonData.curseOwner!=null)DemonCurse.tick((LivingEntity)(Object)this);}
    @Inject(method="damage",at=@At("HEAD"),cancellable=true)
    private void demon$damage(DamageSource source,float amount,CallbackInfoReturnable<Boolean> cir){
        LivingEntity self=(LivingEntity)(Object)this;
        if(self.getWorld().isClient)return;
        if(DemonArmy.friendly(self,source)){cir.setReturnValue(false);return;}
        if(self instanceof ServerPlayerEntity p&&DemonProgression.demon(p)){
            if(DemonBiology.protectFire(p,source)||OriginRuntime.state(p).progress("demon.protection")>0&&!source.isOf(net.minecraft.entity.damage.DamageTypes.GENERIC_KILL))cir.setReturnValue(false);
            else if(source.getAttacker() instanceof MobEntity m&&DemonMobIdentity.ignore(m,p))cir.setReturnValue(false);
        }
    }
    @ModifyVariable(method="damage",at=@At("HEAD"),argsOnly=true)
    private float demon$outgoing(float amount,DamageSource source,float ignored){return DemonCurse.outgoing(source,amount);}
    @Inject(method="damage",at=@At("RETURN"))
    private void demon$provoked(DamageSource source,float amount,CallbackInfoReturnable<Boolean> cir){
        if(cir.getReturnValueZ()&&source.getAttacker() instanceof ServerPlayerEntity p&&DemonProgression.demon(p)&&DemonMobIdentity.nativeMob((LivingEntity)(Object)this)){smooth_classes$demonData.provokedBy=p.getUuid();smooth_classes$demonData.provoked.add(p.getUuid());}
    }
    @Inject(method="tryUseTotem",at=@At("HEAD"),cancellable=true)
    private void demon$fatal(DamageSource source,CallbackInfoReturnable<Boolean> cir){
        if((Object)this instanceof ServerPlayerEntity p&&!source.isOf(net.minecraft.entity.damage.DamageTypes.GENERIC_KILL)&&DemonImmortality.rescue(p,source))cir.setReturnValue(true);
    }
    @Inject(method="onDeath",at=@At("HEAD"))
    private void demon$death(DamageSource source,CallbackInfo ci){
        var self=(LivingEntity)(Object)this;
        if(self instanceof ServerPlayerEntity dead)DemonKillStreak.reset(dead);
        if(self instanceof ServerPlayerEntity dead&&DemonProgression.demon(dead)){DemonDomain.end(dead);DemonThrone.removeSeat(dead);DemonPortals.cancel(dead.getUuid());}
        if(!self.getWorld().isClient&&source.getAttacker() instanceof ServerPlayerEntity p)DemonProgression.onKill(p,self);
        else if(!self.getWorld().isClient&&smooth_classes$demonData.damageCredit!=null)DemonProgression.recordKill(smooth_classes$demonData.damageCredit,self);
        DemonCurse.onDeath(self);
    }
    @Inject(method="onDeath",at=@At("TAIL"))
    private void demon$confirmedStreak(DamageSource source,CallbackInfo ci){DemonKillStreak.confirmedDeath((LivingEntity)(Object)this,source);}
    @Inject(method="canTarget(Lnet/minecraft/entity/LivingEntity;)Z",at=@At("HEAD"),cancellable=true)
    private void demon$respect(LivingEntity target,CallbackInfoReturnable<Boolean> cir){if((Object)this instanceof MobEntity m&&DemonMobIdentity.ignore(m,target))cir.setReturnValue(false);}
}
