package org.marj4n.smooth_classes.mixin;
import net.minecraft.entity.boss.WitherEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.marj4n.smooth_classes.content.avenger.dance.AvengerDanceRuntime;
/** Observe the real final projectile path, including untargeted side-head shots; never cancel it. */
@Mixin(WitherEntity.class)
public abstract class AvengerDanceWitherAttackMixin {
    @Inject(method="shootSkullAt(IDDDZ)V",at=@At("HEAD"))
    private void attacking(int head,double x,double y,double z,boolean charged,CallbackInfo ci){AvengerDanceRuntime.interrupt((WitherEntity)(Object)this,10);}
}
