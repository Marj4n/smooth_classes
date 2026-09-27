package org.marj4n.smooth_classes.effects;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.attribute.AttributeContainer;

/**
 * Smooth Classes effect instance. The registry stays lightweight while
 * EffectBehaviorRuntime owns server-side parity behavior for every effect ID.
 */
public final class SmoothStatusEffect extends StatusEffect {
    private final String id;

    public SmoothStatusEffect(String id, StatusEffectCategory category, int color) {
        super(category, color);
        this.id = id;
    }

    @Override
    public void applyUpdateEffect(LivingEntity entity, int amplifier) {
        EffectBehaviorRuntime.tick(id, entity, amplifier);
        super.applyUpdateEffect(entity, amplifier);
    }

    @Override
    public void onApplied(LivingEntity entity, AttributeContainer attributes, int amplifier) {
        EffectBehaviorRuntime.applied(id, entity, amplifier);
        super.onApplied(entity, attributes, amplifier);
    }

    @Override
    public void onRemoved(LivingEntity entity, AttributeContainer attributes, int amplifier) {
        EffectBehaviorRuntime.removed(id, entity, amplifier);
        super.onRemoved(entity, attributes, amplifier);
    }

    @Override
    public boolean canApplyUpdateEffect(int duration, int amplifier) {
        return EffectBehaviorRuntime.hasTickBehavior(id);
    }
}
