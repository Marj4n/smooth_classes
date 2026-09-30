package org.marj4n.smooth_classes.mixin;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.LivingEntity;
import org.marj4n.smooth_classes.content.archer.runtime.BladeDamageContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnchantmentHelper.class)
public abstract class BladeLootingMixin {
    @Inject(method = "getLooting", at = @At("HEAD"), cancellable = true)
    private static void smoothClasses$projectedLooting(LivingEntity entity, CallbackInfoReturnable<Integer> cir) {
        BladeDamageContext.Hit hit = BladeDamageContext.current();
        if (hit != null && hit.caster() == entity)
            cir.setReturnValue(EnchantmentHelper.getLevel(Enchantments.LOOTING, hit.weapon()));
    }
}
