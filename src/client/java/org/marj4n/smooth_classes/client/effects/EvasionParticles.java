package org.marj4n.smooth_classes.client.effects;

import net.minecraft.entity.LivingEntity;
import net.spell_engine.api.effect.CustomParticleStatusEffect;
import net.spell_engine.api.spell.fx.ParticleGroup;
import net.spell_engine.api.spell.fx.ParticleGroupBuilder;
import net.spell_engine.fx.ParticleHelper;


public class EvasionParticles implements CustomParticleStatusEffect.Spawner {

    private final ParticleGroup particles;

    public EvasionParticles(int particleCount) {
        this.particles = ParticleGroupBuilder.of("minecraft:mycelium")
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
        scaled.batch.count *= (amplifier + 25);
        ParticleHelper.play(livingEntity.getWorld(), livingEntity, scaled);
    }
}
