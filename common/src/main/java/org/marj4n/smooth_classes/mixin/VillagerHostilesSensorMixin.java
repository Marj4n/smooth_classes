package org.marj4n.smooth_classes.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.sensor.VillagerHostilesSensor;
import org.marj4n.smooth_classes.content.avenger.runtime.AvengerReworkRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Villagers do not classify Death List summons as danger, even if the entity type is hostile. */
@Mixin(VillagerHostilesSensor.class)
public abstract class VillagerHostilesSensorMixin {
    @Inject(method = "matches", at = @At("HEAD"), cancellable = true)
    private void smooth_classes$ignoreAvengerSummons(LivingEntity villager, LivingEntity target,
                                                       CallbackInfoReturnable<Boolean> cir) {
        if (AvengerReworkRuntime.isAvengerSummon(target)) cir.setReturnValue(false);
    }
}
