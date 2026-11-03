package org.marj4n.smooth_classes.client.effects;

import net.minecraft.entity.LivingEntity;
import net.spell_engine.api.effect.CustomParticleStatusEffect;
import net.spell_engine.api.spell.fx.ParticleGroup;
import net.spell_engine.api.spell.fx.ParticleGroupBuilder;
import net.spell_engine.fx.ParticleHelper;
import org.marj4n.smooth_classes.effects.SmoothEffects;

public final class SkillParticleEmitter implements CustomParticleStatusEffect.Spawner {
    public enum Profile {
        BARRIER("spell_engine:magic_arcane", .4F),
        UNDYING("spell_engine:magic_holy", .7F),
        RAGE("minecraft:crimson_spore", .03F),
        EVASION("minecraft:mycelium", .7F);

        final String particle;
        final float extent;

        Profile(String particle, float extent) {
            this.particle = particle;
            this.extent = extent;
        }

        public float count(float base, int amplifier) {
            return switch (this) {
                case BARRIER -> base * (float) (1 + amplifier * .15);
                case UNDYING -> base * (amplifier + 1);
                case RAGE -> base + Math.max(amplifier / 10, 1);
                case EVASION -> base * (amplifier + 25);
            };
        }

        public boolean visible(float height, boolean stealthed) {
            return this != BARRIER || (height > 1 && !stealthed);
        }
    }

    private final Profile profile;
    private final int baseCount;
    private final ParticleGroup template;

    public SkillParticleEmitter(Profile profile, int baseCount) {
        this.profile = profile;
        this.baseCount = baseCount;
        template = ParticleGroupBuilder.of(profile.particle).batch(batch -> batch
                .shape(ParticleGroup.Shape.PIPE).anchor(ParticleGroup.Anchor.GROUND)
                .extent(profile.extent).speed(.1F).count(baseCount));
    }

    @Override
    public void spawnParticles(LivingEntity entity, int amplifier) {
        if (!profile.visible(entity.getHeight(), entity.hasStatusEffect(SmoothEffects.STEALTH))) return;
        var emission = template.copy();
        emission.batch.count = profile.count(baseCount, amplifier);
        ParticleHelper.play(entity.getWorld(), entity, emission);
    }
}
