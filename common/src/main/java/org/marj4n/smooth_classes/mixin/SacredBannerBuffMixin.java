package org.marj4n.smooth_classes.mixin;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffectInstance;
import org.marj4n.smooth_classes.content.ruler.runtime.SacredBannerRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(LivingEntity.class)
public abstract class SacredBannerBuffMixin {
    @Inject(method="addStatusEffect(Lnet/minecraft/entity/effect/StatusEffectInstance;Lnet/minecraft/entity/Entity;)Z",
            at=@At("HEAD"),cancellable=true)
    private void smooth$bannerBaseline(StatusEffectInstance effect, Entity source, CallbackInfoReturnable<Boolean> cir) {
        Boolean result=SacredBannerRuntime.externalEffect((LivingEntity)(Object)this,effect);
        if(result!=null)cir.setReturnValue(result);
    }
}
