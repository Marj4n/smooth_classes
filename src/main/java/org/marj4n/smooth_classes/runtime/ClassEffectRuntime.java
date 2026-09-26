package org.marj4n.smooth_classes.runtime;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.server.network.ServerPlayerEntity;

/** Shared application point for class-specific effects. */
public final class ClassEffectRuntime {
    private ClassEffectRuntime() {}
    public static void apply(ServerPlayerEntity player, StatusEffect effect, int ticks, int amplifier) {
        player.addStatusEffect(new StatusEffectInstance(effect, ticks, amplifier, false, true, true));
    }
}
