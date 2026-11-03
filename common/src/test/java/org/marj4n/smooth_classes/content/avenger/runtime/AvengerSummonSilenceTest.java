package org.marj4n.smooth_classes.content.avenger.runtime;

import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.nbt.NbtCompound;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AvengerSummonSilenceTest {
    @BeforeAll
    static void bootstrap() {
        SharedConstants.createGameVersion();
        Bootstrap.initialize();
    }

    private static ItemEntity entity() {
        return new ItemEntity(EntityType.ITEM, null);
    }

    @Test
    void repeatedTogglesRestoreSoundAndPreserveOwnershipTags() {
        var summon = entity();
        summon.addCommandTag("smooth_classes_avenger_owner_test");
        for (int i = 0; i < 3; i++) {
            assertTrue(AvengerSummonSilence.toggle(summon));
            assertTrue(summon.isSilent());
            assertFalse(AvengerSummonSilence.toggle(summon));
            assertFalse(summon.isSilent());
            assertEquals(1, summon.getCommandTags().size());
            assertTrue(summon.getCommandTags().contains("smooth_classes_avenger_owner_test"));
        }
    }

    @Test
    void unbindingRestoresAnOriginallySilentEntity() {
        var summon = entity();
        summon.setSilent(true);
        assertTrue(AvengerSummonSilence.toggle(summon));
        AvengerSummonSilence.restore(summon);
        assertTrue(summon.isSilent());
        assertTrue(summon.getCommandTags().isEmpty());
    }

    @Test
    void restoreDoesNotChangeAnEntityThatWasNeverMuted() {
        var summon = entity();
        summon.setSilent(true);
        summon.addCommandTag("other_mod_state");
        AvengerSummonSilence.restore(summon);
        assertTrue(summon.isSilent());
        assertEquals(1, summon.getCommandTags().size());
        summon.setSilent(false);
        AvengerSummonSilence.restore(summon);
        assertFalse(summon.isSilent());
    }

    @Test
    void muteIsStoredInVanillaEntityNbt() {
        var summon = entity();
        AvengerSummonSilence.toggle(summon);
        NbtCompound saved = summon.writeNbt(new NbtCompound());
        assertTrue(saved.getBoolean("Silent"));
        assertFalse(saved.getList("Tags", 8).isEmpty());
        AvengerSummonSilence.restore(summon);
        NbtCompound restored = summon.writeNbt(new NbtCompound());
        assertFalse(restored.getBoolean("Silent"));
        assertTrue(restored.getList("Tags", 8).isEmpty());
    }

    @Test
    void hoverHighlightIsOffBeforeTheClientStarts() {
        assertFalse(
                org.marj4n.smooth_classes.client.AvengerSummonHoverGlow.isHighlighted(entity()));
    }
}
