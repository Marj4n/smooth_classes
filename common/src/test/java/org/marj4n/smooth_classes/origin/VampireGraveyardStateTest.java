package org.marj4n.smooth_classes.origin;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class VampireGraveyardStateTest {
    @Test void allCandidateFootprintsAreDistinctAndClearOfTheChurch() {
        Set<String> positions = new HashSet<>();
        for (int i = 0; i < VampireGraveyardLayout.MAX_CANDIDATES; i++) {
            var offset = VampireGraveyardLayout.offset(i);
            assertTrue(Math.max(Math.abs(offset.x()), Math.abs(offset.z())) >= 50);
            assertEquals(0, offset.x() % 10); assertEquals(0, offset.z() % 10);
            assertTrue(positions.add(offset.x() + ":" + offset.z()), "Duplicate grave at index " + i);
        }
        assertThrows(IllegalArgumentException.class, () -> VampireGraveyardLayout.offset(-1));
        assertThrows(IllegalArgumentException.class, () -> VampireGraveyardLayout.offset(VampireGraveyardLayout.MAX_CANDIDATES));
    }

    @Test void worldHasOnlyOneChurchAndRejectsAnotherLocation() {
        var state = new VampireGraveyardState();
        var origin = new BlockPos(512, 58, -512);
        state.establishChurch(origin); state.establishChurch(origin);
        assertThrows(IllegalStateException.class, () -> state.establishChurch(origin.add(100, 0, 0)));
        assertEquals(origin, state.church());
    }

    @Test void joiningPlayersReceiveSeparatePlotsAndCannotClaimSomeoneElsesGrave() {
        var state = new VampireGraveyardState();
        var first = state.reserve(new BlockPos(0, 60, 0), 10L, false);
        var second = state.reserve(new BlockPos(10, 60, 0), 20L, false);
        UUID a = UUID.randomUUID(), b = UUID.randomUUID();
        assertSame(first, state.claim(a, first)); state.finish(first);
        assertThrows(IllegalStateException.class, () -> state.claim(b, first));
        assertSame(second, state.claim(b, second));
        assertEquals(2, state.claimedCount()); assertEquals(0, state.availableCount());
    }

    @Test void originResetDeathAndReconnectDoNotAllocateAnotherGrave() {
        var state = new VampireGraveyardState();
        var first = state.reserve(new BlockPos(0, 60, 0), 10L, false);
        var spare = state.reserve(new BlockPos(10, 60, 0), 20L, false);
        UUID player = UUID.randomUUID();
        state.claim(player, first); state.finish(first);
        assertSame(first, state.claim(player, spare));
        assertTrue(spare.available()); assertEquals(1, state.claimedCount());
    }

    @Test void changedOrLegacyPlotsAreNeverAllocatedToNewPlayers() {
        var state = new VampireGraveyardState();
        var legacy = state.reserve(new BlockPos(0, 60, 0), 10L, true);
        state.block(legacy);
        assertFalse(legacy.available());
        assertThrows(IllegalStateException.class, () -> state.claim(UUID.randomUUID(), legacy));
        assertThrows(IllegalArgumentException.class, () -> state.reserve(new BlockPos(7, 60, 7), 0L, false));
    }

    @Test void restartRoundTripPreservesChurchOwnersUsedLootAndCandidateCursor() {
        var state = new VampireGraveyardState();
        var church = new BlockPos(-123456, 58, 234567);
        state.establishChurch(church);
        List<UUID> players = new ArrayList<>();
        for (int i = 0; i < 256; i++) {
            int candidate = state.takeCandidate();
            var offset = VampireGraveyardLayout.offset(candidate);
            var plot = state.reserve(church.add(offset.x(), 0, offset.z()), Long.MIN_VALUE + i, false);
            UUID owner = UUID.randomUUID(); players.add(owner);
            state.claim(owner, plot);
            if (i != 255) state.finish(plot); // An interrupted construction remains reserved, never reused.
        }
        var blocked = state.reserve(church.add(0, 0, 0), 123L, true); state.block(blocked);
        var restored = VampireGraveyardState.fromNbt(state.writeNbt(new NbtCompound()));
        assertEquals(church, restored.church()); assertEquals(256, restored.claimedCount());
        assertEquals(256, restored.nextCandidate()); assertEquals(0, restored.availableCount());
        for (int i = 0; i < players.size(); i++) {
            var plot = restored.ownedBy(players.get(i));
            assertNotNull(plot); assertEquals(Long.MIN_VALUE + i, plot.fingerprint());
            assertEquals(i != 255, plot.built());
        }
        assertEquals(256, restored.takeCandidate());
    }

    @Test void missingStateAndInvalidCursorHaveSafeDefaults() {
        var empty = VampireGraveyardState.fromNbt(new NbtCompound());
        assertFalse(empty.hasChurch()); assertEquals(0, empty.claimedCount());
        NbtCompound tag = new NbtCompound(); tag.putInt("NextCandidate", Integer.MAX_VALUE);
        var exhausted = VampireGraveyardState.fromNbt(tag);
        assertEquals(-1, exhausted.takeCandidate());
    }
}
