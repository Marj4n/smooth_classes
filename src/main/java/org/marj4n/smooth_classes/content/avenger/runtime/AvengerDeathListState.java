package org.marj4n.smooth_classes.content.avenger.runtime;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Persistent, server-owned Avenger soul ledger.
 *
 * This deliberately lives in overworld PersistentState instead of player inventory NBT:
 * it survives death, logout, dimension changes and server restarts while staying keyed to
 * the player's UUID. The physical Death List book is only a view of this data.
 */
public final class AvengerDeathListState extends PersistentState {
    public static final String SAVE_ID = "smooth_classes_avenger_death_list";
    public static final int MAX_SUMMON_CHARGES = 3;
    public static final long RECHARGE_MILLIS = 20_000L;

    private final Map<UUID, PlayerLedger> players = new HashMap<>();

    public static AvengerDeathListState get(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager().getOrCreate(
                AvengerDeathListState::fromNbt,
                AvengerDeathListState::new,
                SAVE_ID
        );
    }

    public PlayerLedger ledger(UUID player) {
        return players.computeIfAbsent(player, ignored -> new PlayerLedger());
    }

    public PlayerLedger ledgerIfPresent(UUID player) {
        return players.get(player);
    }

    public boolean hasAwakened(UUID player) {
        PlayerLedger ledger = players.get(player);
        return ledger != null && ledger.awakened;
    }

    public void awaken(UUID player) {
        PlayerLedger ledger = ledger(player);
        if (!ledger.awakened) {
            ledger.awakened = true;
            markDirty();
        }
    }

    public void recordKill(UUID player, String entityId) {
        PlayerLedger ledger = ledger(player);
        ledger.awakened = true;
        ledger.kills.merge(entityId, 1, Integer::sum);
        ledger.souls.merge(entityId, 1, Integer::sum);
        markDirty();
    }

    public int available(UUID player, String entityId) {
        PlayerLedger ledger = players.get(player);
        return ledger == null ? 0 : Math.max(0, ledger.souls.getOrDefault(entityId, 0));
    }

    public int kills(UUID player, String entityId) {
        PlayerLedger ledger = players.get(player);
        return ledger == null ? 0 : Math.max(0, ledger.kills.getOrDefault(entityId, 0));
    }

    public void restoreSoul(UUID player, String entityId) {
        PlayerLedger ledger = ledger(player);
        ledger.awakened = true;
        ledger.souls.merge(entityId, 1, Integer::sum);
        markDirty();
    }

    public boolean consumeSoul(UUID player, String entityId) {
        PlayerLedger ledger = ledger(player);
        int count = ledger.souls.getOrDefault(entityId, 0);
        if (count <= 0) return false;
        if (count == 1) ledger.souls.remove(entityId);
        else ledger.souls.put(entityId, count - 1);
        markDirty();
        return true;
    }

    public ChargeSnapshot refreshCharges(UUID player, long nowMillis) {
        PlayerLedger ledger = ledger(player);
        boolean changed = false;
        ledger.charges = Math.max(0, Math.min(MAX_SUMMON_CHARGES, ledger.charges));

        if (ledger.charges >= MAX_SUMMON_CHARGES) {
            if (ledger.nextChargeAtMillis != 0L) {
                ledger.nextChargeAtMillis = 0L;
                changed = true;
            }
        } else {
            if (ledger.nextChargeAtMillis <= 0L) {
                ledger.nextChargeAtMillis = nowMillis + RECHARGE_MILLIS;
                changed = true;
            }
            while (ledger.charges < MAX_SUMMON_CHARGES && nowMillis >= ledger.nextChargeAtMillis) {
                ledger.charges++;
                changed = true;
                if (ledger.charges < MAX_SUMMON_CHARGES) ledger.nextChargeAtMillis += RECHARGE_MILLIS;
                else ledger.nextChargeAtMillis = 0L;
            }
        }
        if (changed) markDirty();
        long remaining = ledger.charges >= MAX_SUMMON_CHARGES || ledger.nextChargeAtMillis <= 0L
                ? 0L : Math.max(0L, ledger.nextChargeAtMillis - nowMillis);
        return new ChargeSnapshot(ledger.charges, remaining);
    }

    public boolean consumeCharge(UUID player, long nowMillis) {
        ChargeSnapshot snapshot = refreshCharges(player, nowMillis);
        if (snapshot.charges() <= 0) return false;
        PlayerLedger ledger = ledger(player);
        boolean wasFull = ledger.charges >= MAX_SUMMON_CHARGES;
        ledger.charges--;
        if (wasFull || ledger.nextChargeAtMillis <= 0L) ledger.nextChargeAtMillis = nowMillis + RECHARGE_MILLIS;
        markDirty();
        return true;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtCompound all = new NbtCompound();
        for (Map.Entry<UUID, PlayerLedger> entry : players.entrySet()) {
            PlayerLedger ledger = entry.getValue();
            NbtCompound p = new NbtCompound();
            p.putBoolean("Awakened", ledger.awakened);
            p.putInt("Charges", ledger.charges);
            p.putLong("NextChargeAt", ledger.nextChargeAtMillis);
            p.put("Kills", writeCounts(ledger.kills));
            p.put("Souls", writeCounts(ledger.souls));
            all.put(entry.getKey().toString(), p);
        }
        nbt.put("Players", all);
        return nbt;
    }

    public static AvengerDeathListState fromNbt(NbtCompound nbt) {
        AvengerDeathListState state = new AvengerDeathListState();
        NbtCompound all = nbt.getCompound("Players");
        for (String key : all.getKeys()) {
            try {
                UUID uuid = UUID.fromString(key);
                NbtCompound p = all.getCompound(key);
                PlayerLedger ledger = new PlayerLedger();
                ledger.awakened = p.getBoolean("Awakened");
                ledger.charges = p.contains("Charges", NbtElement.NUMBER_TYPE)
                        ? Math.max(0, Math.min(MAX_SUMMON_CHARGES, p.getInt("Charges")))
                        : MAX_SUMMON_CHARGES;
                ledger.nextChargeAtMillis = p.getLong("NextChargeAt");
                readCounts(p.getCompound("Kills"), ledger.kills);
                readCounts(p.getCompound("Souls"), ledger.souls);
                state.players.put(uuid, ledger);
            } catch (IllegalArgumentException ignored) {
                // Ignore corrupt/foreign UUID keys without throwing away the rest of the ledger.
            }
        }
        return state;
    }

    private static NbtCompound writeCounts(Map<String, Integer> counts) {
        NbtCompound out = new NbtCompound();
        counts.forEach((id, count) -> {
            if (count != null && count > 0) out.putInt(id, count);
        });
        return out;
    }

    private static void readCounts(NbtCompound in, Map<String, Integer> out) {
        for (String id : in.getKeys()) {
            int count = in.getInt(id);
            if (count > 0) out.put(id, count);
        }
    }

    public static final class PlayerLedger {
        private boolean awakened;
        private int charges = MAX_SUMMON_CHARGES;
        private long nextChargeAtMillis;
        private final Map<String, Integer> kills = new HashMap<>();
        private final Map<String, Integer> souls = new HashMap<>();

        public boolean awakened() { return awakened; }
        public Map<String, Integer> kills() { return Collections.unmodifiableMap(kills); }
        public Map<String, Integer> souls() { return Collections.unmodifiableMap(souls); }
        public int charges() { return charges; }
        public long nextChargeAtMillis() { return nextChargeAtMillis; }
    }

    public record ChargeSnapshot(int charges, long remainingMillis) {}
}
