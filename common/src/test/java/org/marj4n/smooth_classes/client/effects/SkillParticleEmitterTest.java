package org.marj4n.smooth_classes.client.effects;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class SkillParticleEmitterTest {
    @ParameterizedTest
    @CsvSource({
        "BARRIER,1,0,1", "BARRIER,1,3,1.45", "BARRIER,1,10,2.5",
        "UNDYING,2,0,2", "UNDYING,2,3,8", "UNDYING,2,-1,0",
        "RAGE,1,0,2", "RAGE,1,19,2", "RAGE,1,20,3", "RAGE,1,-20,2",
        "EVASION,1,0,25", "EVASION,1,3,28", "EVASION,1,-26,-1"
    })
    void preservesEmissionCounts(SkillParticleEmitter.Profile profile, float base, int amplifier,
                                  float expected) {
        assertEquals(expected, profile.count(base, amplifier), .00001F);
        assertEquals(expected, profile.count(base, amplifier), .00001F, "count must not accumulate");
    }

    @Test
    void onlyBarrierIsHiddenByStealthOrShortEntity() {
        for (var profile : SkillParticleEmitter.Profile.values()) {
            boolean barrier = profile == SkillParticleEmitter.Profile.BARRIER;
            assertEquals(!barrier, profile.visible(1F, false));
            assertEquals(!barrier, profile.visible(1.8F, true));
            assertEquals(!barrier, profile.visible(.5F, true));
            assertTrue(profile.visible(1.01F, false));
        }
    }

    @Test
    void retainsParticleAssetsAndSpread() {
        assertEquals("spell_engine:magic_arcane", SkillParticleEmitter.Profile.BARRIER.particle);
        assertEquals("spell_engine:magic_holy", SkillParticleEmitter.Profile.UNDYING.particle);
        assertEquals("minecraft:crimson_spore", SkillParticleEmitter.Profile.RAGE.particle);
        assertEquals("minecraft:mycelium", SkillParticleEmitter.Profile.EVASION.particle);
        assertEquals(.4F, SkillParticleEmitter.Profile.BARRIER.extent);
        assertEquals(.7F, SkillParticleEmitter.Profile.UNDYING.extent);
        assertEquals(.03F, SkillParticleEmitter.Profile.RAGE.extent);
        assertEquals(.7F, SkillParticleEmitter.Profile.EVASION.extent);
    }
}
