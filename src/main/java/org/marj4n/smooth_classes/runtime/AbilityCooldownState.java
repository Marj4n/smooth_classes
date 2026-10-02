package org.marj4n.smooth_classes.runtime;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import net.minecraft.world.PersistentState;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Persistent server-owned cooldown ledger.
 *
 * Cooldowns are stored as real-world end timestamps instead of server ticks so they:
 * - survive logout/rejoin;
 * - survive dedicated-server and single-player restarts;
 * - never inherit a stale tick counter from another integrated-server instance;
 * - continue elapsing while the player is offline.
 */
public final class AbilityCooldownState extends PersistentState {
    private static final String SAVE_ID = "smooth_classes_ability_cooldowns";

    private record Entry(long readyAtMillis, long durationMillis) {}
    private final Map<UUID, Map<String, Entry>> players = new HashMap<>();

    public static AbilityCooldownState get(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager().getOrCreate(
                AbilityCooldownState::fromNbt,
                AbilityCooldownState::new,
                SAVE_ID
        );
    }

    public long remainingMillis(UUID player, Identifier ability, long nowMillis) {
        Map<String, Entry> map = players.get(player);
        if (map == null) return 0L;
        Entry entry = map.get(ability.toString());
        if (entry == null) return 0L;

        long remaining = entry.readyAtMillis() - nowMillis;
        if (remaining <= 0L) {
            map.remove(ability.toString());
            if (map.isEmpty()) players.remove(player);
            markDirty();
            return 0L;
        }

        // System clock corrections should never turn a normal skill into a 100s/1000s
        // cooldown. Clamp to the duration that was actually started and repair the entry.
        long maxExpected = Math.max(0L, entry.durationMillis());
        if (maxExpected > 0L && remaining > maxExpected + 1_000L) {
            remaining = maxExpected;
            map.put(ability.toString(), new Entry(nowMillis + remaining, maxExpected));
            markDirty();
        }
        return remaining;
    }

    public void start(UUID player, Identifier ability, long durationMillis, long nowMillis) {
        long duration = Math.max(0L, durationMillis);
        if (duration <= 0L) {
            clear(player, ability);
            return;
        }
        players.computeIfAbsent(player, ignored -> new HashMap<>())
                .put(ability.toString(), new Entry(nowMillis + duration, duration));
        markDirty();
    }

    public void clear(UUID player, Identifier ability) {
        Map<String, Entry> map = players.get(player);
        if (map == null) return;
        if (map.remove(ability.toString()) != null) markDirty();
        if (map.isEmpty()) players.remove(player);
    }

    public void clearPlayer(UUID player) {
        if (players.remove(player) != null) markDirty();
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtCompound all = new NbtCompound();
        for (Map.Entry<UUID, Map<String, Entry>> playerEntry : players.entrySet()) {
            NbtCompound abilities = new NbtCompound();
            for (Map.Entry<String, Entry> abilityEntry : playerEntry.getValue().entrySet()) {
                Entry entry = abilityEntry.getValue();
                NbtCompound value = new NbtCompound();
                value.putLong("ReadyAt", entry.readyAtMillis());
                value.putLong("Duration", entry.durationMillis());
                abilities.put(abilityEntry.getKey(), value);
            }
            all.put(playerEntry.getKey().toString(), abilities);
        }
        nbt.put("Players", all);
        return nbt;
    }

    public static AbilityCooldownState fromNbt(NbtCompound nbt) {
        AbilityCooldownState state = new AbilityCooldownState();
        NbtCompound all = nbt.getCompound("Players");
        for (String uuidKey : all.getKeys()) {
            try {
                UUID uuid = UUID.fromString(uuidKey);
                NbtCompound abilities = all.getCompound(uuidKey);
                Map<String, Entry> map = new HashMap<>();
                for (String ability : abilities.getKeys()) {
                    NbtCompound value = abilities.getCompound(ability);
                    if (!value.contains("ReadyAt", NbtElement.NUMBER_TYPE)) continue;
                    long readyAt = value.getLong("ReadyAt");
                    long duration = Math.max(0L, value.getLong("Duration"));
                    if (readyAt > 0L) map.put(ability, new Entry(readyAt, duration));
                }
                if (!map.isEmpty()) state.players.put(uuid, map);
            } catch (IllegalArgumentException ignored) {
                // Ignore malformed UUID keys without discarding the rest of the save.
            }
        }
        return state;
    }
}
