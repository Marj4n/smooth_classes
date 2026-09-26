package org.marj4n.smooth_classes.client.effects;

import net.minecraft.entity.LivingEntity;
import net.spell_engine.api.effect.CustomParticleStatusEffect;
import net.spell_engine.api.spell.fx.ParticleGroup;
import net.spell_engine.api.spell.fx.ParticleGroupBuilder;
import net.spell_engine.fx.ParticleHelper;


public class UndyingParticles implements CustomParticleStatusEffect.Spawner {

    private final ParticleGroup particles;

    public UndyingParticles(int particleCount) {
        this.particles = ParticleGroupBuilder.of("spell_engine:magic_holy")
                .batch(batch -> batch
                        .count(particleCount)
                        .shape(ParticleGroup.Shape.PIPE)
                        .anchor(ParticleGroup.Anchor.GROUND)
                        .speed(0.1F)
                        .extent(0.7F));
    }

    @Override
    public void spawnParticles(LivingEntity livingEntity, int amplifier) {
        var scaled = particles.copy();
        scaled.batch.count *= (amplifier + 1);
        ParticleHelper.play(livingEntity.getWorld(), livingEntity, scaled);
    }
}
