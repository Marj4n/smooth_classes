package org.marj4n.smooth_classes.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import org.marj4n.smooth_classes.runtime.RighteousHammerChargeRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PersistentProjectileEntity.class)
public abstract class ArrowHammerImpactMixin {
    @Inject(method="onEntityHit",at=@At("HEAD"))
    private void smooth_classes$arrowHammer(EntityHitResult hit,CallbackInfo ci) {
        PersistentProjectileEntity projectile=(PersistentProjectileEntity)(Object)this;
        if (!projectile.getWorld().isClient() && projectile.getOwner() instanceof ServerPlayerEntity player
                && hit.getEntity() instanceof LivingEntity target)
            RighteousHammerChargeRuntime.onTriggeredHit(player,target);
    }
}
