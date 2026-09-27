package org.marj4n.smooth_classes.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.server.network.ServerPlayerEntity;
import org.marj4n.smooth_classes.runtime.WhenOnHighRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class WhenOnHighEffectGuardMixin {
    @Inject(method="addStatusEffect(Lnet/minecraft/entity/effect/StatusEffectInstance;Lnet/minecraft/entity/Entity;)Z",at=@At("HEAD"),cancellable=true)
    private void smooth$guard(StatusEffectInstance effect,Entity source,CallbackInfoReturnable<Boolean> cir){
        if((Object)this instanceof ServerPlayerEntity p&&WhenOnHighRuntime.active(p)&&!effect.getEffectType().isBeneficial())cir.setReturnValue(false);
    }
}
