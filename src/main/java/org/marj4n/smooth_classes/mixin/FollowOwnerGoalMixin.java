package org.marj4n.smooth_classes.mixin;

import net.minecraft.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.entity.passive.TameableEntity;
import org.marj4n.smooth_classes.entity.DreadglareEntity;
import org.marj4n.smooth_classes.entity.GreaterDreadglareEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Continued keeps Dreadglares close to their owner without unnecessary teleporting. */
@Mixin(FollowOwnerGoal.class)
public abstract class FollowOwnerGoalMixin {
    @Shadow private TameableEntity tameable;

    @Inject(method = "tryTeleport", at = @At("HEAD"), cancellable = true)
    private void smoothClasses$nearbyDreadglare(CallbackInfo ci) {
        if (tameable != null && !tameable.getWorld().isClient()
                && (tameable instanceof DreadglareEntity || tameable instanceof GreaterDreadglareEntity)
                && tameable.getOwner() != null && tameable.squaredDistanceTo(tameable.getOwner()) < 576)
            ci.cancel();
    }
}
