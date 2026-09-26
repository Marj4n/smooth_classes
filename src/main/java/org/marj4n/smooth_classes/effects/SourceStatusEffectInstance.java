package org.marj4n.smooth_classes.effects;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;

/**
 * Source-aware effect instance used by curse/taunt mechanics.
 * Mirrors Continued's SimplyStatusEffectInstance without depending on Continued.
 */
public final class SourceStatusEffectInstance extends StatusEffectInstance {
    private final LivingEntity sourceEntity;

    public SourceStatusEffectInstance(StatusEffect type, int duration, int amplifier,
                                      boolean ambient, boolean showParticles, boolean showIcon,
                                      LivingEntity sourceEntity) {
        super(type, duration, amplifier, ambient, showParticles, showIcon);
        this.sourceEntity = sourceEntity;
    }

    public LivingEntity getSourceEntity() {
        return sourceEntity;
    }
}
