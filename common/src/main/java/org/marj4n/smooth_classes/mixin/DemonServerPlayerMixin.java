package org.marj4n.smooth_classes.mixin;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.entity.damage.DamageSource;
import org.marj4n.smooth_classes.origin.demon.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** ServerPlayerEntity overrides death without invoking LivingEntity.onDeath. */
@Mixin(ServerPlayerEntity.class)
public abstract class DemonServerPlayerMixin {
    @Inject(method="onDeath",at=@At("HEAD"))
    private void demon$playerDeath(DamageSource source,CallbackInfo ci){
        var player=(ServerPlayerEntity)(Object)this;
        ForbiddenBirth.finish(player,"death");
        if(DemonProgression.demon(player)){DemonDomain.end(player);DemonThrone.removeSeat(player);DemonPortals.cancel(player.getUuid());}
        DemonCurse.onDeath(player);
    }
}
