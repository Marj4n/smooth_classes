package org.marj4n.smooth_classes.client.effects;

import net.minecraft.entity.LivingEntity;
import net.spell_engine.api.effect.CustomParticleStatusEffect;
import net.spell_engine.api.spell.fx.ParticleGroup;
import net.spell_engine.api.spell.fx.ParticleGroupBuilder;
import net.spell_engine.fx.ParticleHelper;
import org.marj4n.smooth_classes.effects.SmoothEffects;

public class BarrierParticles implements CustomParticleStatusEffect.Spawner {

    private final ParticleGroup particles;

    public BarrierParticles(int particleCount) {
        this.particles = ParticleGroupBuilder.of("spell_engine:magic_arcane")
                .batch(batch -> batch
                        .count(particleCount)
                        .shape(ParticleGroup.Shape.PIPE)
                        .anchor(ParticleGroup.Anchor.GROUND)
                        .speed(0.1F)
                        .extent(0.4F));
    }

    @Override
    public void spawnParticles(LivingEntity livingEntity, int amplifier) {
        if (livingEntity.getHeight() > 1 && !livingEntity.hasStatusEffect(SmoothEffects.STEALTH)) {
            var scaled = particles.copy();
            scaled.batch.count *= (float) ((amplifier * 0.15) + 1);
            ParticleHelper.play(livingEntity.getWorld(), livingEntity, scaled);
        }
    }
}
