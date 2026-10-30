package org.marj4n.smooth_classes.mixin;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import org.marj4n.smooth_classes.origin.demon.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(MobEntity.class)
public abstract class DemonMobMixin implements DemonCommandGoal.Access {
    @Shadow @Final protected net.minecraft.entity.ai.goal.GoalSelector goalSelector;
    @Override public net.minecraft.entity.ai.goal.GoalSelector smooth_classes$demonGoals(){return goalSelector;}
    @Unique private net.minecraft.nbt.NbtCompound demon$conversionData;
    @Inject(method="convertTo(Lnet/minecraft/entity/EntityType;Z)Lnet/minecraft/entity/mob/MobEntity;",at=@At("HEAD"))
    private void demon$beforeConvert(net.minecraft.entity.EntityType<?> type,boolean keep,CallbackInfoReturnable<MobEntity> cir){
        var self=(MobEntity)(Object)this;var data=DemonEntityData.of(self);
        if(self.getWorld().isClient||data.commander==null&&data.curseOwner==null&&!DemonMobIdentity.nativeMob(self))return;
        demon$conversionData=data.write();
        if(DemonMobIdentity.nativeMob(self)){demon$conversionData.putBoolean("BirthRecorded",true);demon$conversionData.putBoolean("NativeBirth",true);}
        data.transforming=true;
    }
    @Inject(method="convertTo(Lnet/minecraft/entity/EntityType;Z)Lnet/minecraft/entity/mob/MobEntity;",at=@At("RETURN"))
    private void demon$afterConvert(net.minecraft.entity.EntityType<?> type,boolean keep,CallbackInfoReturnable<MobEntity> cir){
        var self=(MobEntity)(Object)this;var data=DemonEntityData.of(self);data.transforming=false;
        if(demon$conversionData==null)return;
        var converted=cir.getReturnValue();
        if(converted!=null){
            DemonEntityData.of(converted).read(demon$conversionData);
            var token=DemonEntityData.of(converted).curseOwner;
            if(token!=null&&self.getServer()!=null){var ledger=DemonWorldState.get(self.getServer());var cast=ledger.casts.get(token);if(cast!=null&&self.getUuid().equals(cast.root)){cast.root=converted.getUuid();ledger.markDirty();}}
            data.curseOwner=null;DemonArmy.loaded(converted);
        }
        demon$conversionData=null;
    }
    @Inject(method="setTarget",at=@At("HEAD"),cancellable=true)
    private void demon$target(LivingEntity target,CallbackInfo ci){if(target!=null&&(DemonArmy.passiveOrder((MobEntity)(Object)this)||DemonMobIdentity.ignore((MobEntity)(Object)this,target)))ci.cancel();}
    @Inject(method="tick",at=@At("TAIL"))
    private void demon$maintain(CallbackInfo ci){
        var m=(MobEntity)(Object)this;if(m.getWorld().isClient)return;
        boolean passive=DemonArmy.passiveOrder(m);
        if(m.getTarget()!=null&&(passive||DemonMobIdentity.ignore(m,m.getTarget())))m.setTarget(null);
        var brain=m.getBrain();
        if(brain.hasMemoryModule(net.minecraft.entity.ai.brain.MemoryModuleType.ATTACK_TARGET)){
            var target=brain.getOptionalMemory(net.minecraft.entity.ai.brain.MemoryModuleType.ATTACK_TARGET);
            if(target.isPresent()&&(passive||DemonMobIdentity.ignore(m,target.get()))){
                brain.forget(net.minecraft.entity.ai.brain.MemoryModuleType.ATTACK_TARGET);
                if(brain.hasMemoryModule(net.minecraft.entity.ai.brain.MemoryModuleType.ANGRY_AT))brain.forget(net.minecraft.entity.ai.brain.MemoryModuleType.ANGRY_AT);
            }
        }
    }
}
