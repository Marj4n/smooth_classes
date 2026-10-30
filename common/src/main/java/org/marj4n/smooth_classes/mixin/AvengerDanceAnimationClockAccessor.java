package org.marj4n.smooth_classes.mixin;
import net.minecraft.entity.AnimationState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
/** Read-only server start clock; never advances or resets vanilla animation state. */
@Mixin(AnimationState.class)
public interface AvengerDanceAnimationClockAccessor {
    @Accessor("updatedAt") long smoothClasses$danceUpdatedAt();
}
