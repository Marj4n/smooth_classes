package org.marj4n.smooth_classes.origin;

import net.minecraft.nbt.NbtCompound;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Persistent per-player Origin state, including milestone progress for the Origin tree. */
public final class OriginState {
    private String origin = "";
    private int blood = 100;
    private int sunExposure;
    private int wetnessTicks = 90 * 20;
    private int wingStaminaTicks = 6 * 20;
    private int instability;
    private int soul = 100;
    private int boneMass;
    private int carbonLayer;
    private int livingMass;
    private int lavaGraceTicks = 5 * 20;
    private long lastFeedTick;

    /** Generic counters/flags keep the evolution tracker data-driven and save-compatible. */
    private final Map<String, Integer> progress = new HashMap<>();
    private final Map<String, Long> longProgress = new HashMap<>();
    private final Set<String> flags = new HashSet<>();

    public String originId() { return origin; }
    public OriginType origin() { return OriginType.byId(origin).orElse(null); }
    public boolean hasOrigin() { return origin() != null; }

    public void setOrigin(OriginType type) {
        origin = type == null ? "" : type.id();
        resetResources();
    }

    public void clear() { setOrigin(null); }

    public int blood() { return blood; }
    public void blood(int value) { blood = clamp(value, 0, 200); }
    public int sunExposure() { return sunExposure; }
    public void sunExposure(int value) { sunExposure = clamp(value, 0, 100); }
    public int wetnessTicks() { return wetnessTicks; }
    public void wetnessTicks(int value) { wetnessTicks = Math.max(0, value); }
    public int wingStaminaTicks() { return wingStaminaTicks; }
    public void wingStaminaTicks(int value) { wingStaminaTicks = clamp(value, 0, 6 * 20); }
    public int instability() { return instability; }
    public void instability(int value) { instability = clamp(value, 0, 100); }
    public int soul() { return soul; }
    public void soul(int value) { soul = clamp(value, 0, 100); }
    public int boneMass() { return boneMass; }
    public void boneMass(int value) { boneMass = clamp(value, 0, 150); }
    public int carbonLayer() { return carbonLayer; }
    public void carbonLayer(int value) { carbonLayer = clamp(value, 0, 100); }
    public int livingMass() { return livingMass; }
    public void livingMass(int value) { livingMass = clamp(value, 0, 10); }
    public int lavaGraceTicks() { return lavaGraceTicks; }
    public void lavaGraceTicks(int value) { lavaGraceTicks = Math.max(0, value); }
    public long lastFeedTick() { return lastFeedTick; }
    public void lastFeedTick(long value) { lastFeedTick = value; }

    public int progress(String key) { return Math.max(0, progress.getOrDefault(key, 0)); }
    public void progress(String key, int value) {
        if (key == null || key.isBlank()) return;
        progress.put(key, Math.max(0, value));
    }
    public int addProgress(String key, int amount) {
        int value = Math.max(0, progress(key) + amount);
        progress(key, value);
        return value;
    }
    public long longProgress(String key) { return Math.max(0L, longProgress.getOrDefault(key, 0L)); }
    public void longProgress(String key, long value) {
        if (key == null || key.isBlank()) return;
        longProgress.put(key, Math.max(0L, value));
    }
    public boolean hasFlag(String key) { return key != null && flags.contains(key); }
    public boolean flag(String key) { return key != null && !key.isBlank() && flags.add(key); }
    public boolean unflag(String key) { return key != null && flags.remove(key); }
    public boolean toggleFlag(String key) {
        if (key == null || key.isBlank()) return false;
        if (flags.contains(key)) {
            flags.remove(key);
            return false;
        }
        flags.add(key);
        return true;
    }
    public int countFlags(String prefix) {
        if (prefix == null) return 0;
        int count = 0;
        for (String flag : flags) if (flag.startsWith(prefix)) count++;
        return count;
    }
    public Map<String, Integer> progressSnapshot() { return Collections.unmodifiableMap(new HashMap<>(progress)); }
    public Set<String> flagsSnapshot() { return Collections.unmodifiableSet(new HashSet<>(flags)); }

    public void sync(String originId, int bloodValue, int sunValue, int wetnessValue, int wingValue,
                     int instabilityValue, int soulValue, int boneValue, int carbonValue, int livingValue,
                     long lastFeedValue, Map<String, Integer> progressValues, Set<String> flagValues) {
        this.origin = originId == null ? "" : originId;
        this.blood = clamp(bloodValue, 0, 200);
        this.sunExposure = clamp(sunValue, 0, 100);
        this.wetnessTicks = Math.max(0, wetnessValue);
        this.wingStaminaTicks = clamp(wingValue, 0, 6 * 20);
        this.instability = clamp(instabilityValue, 0, 100);
        this.soul = clamp(soulValue, 0, 100);
        this.boneMass = clamp(boneValue, 0, 150);
        this.carbonLayer = clamp(carbonValue, 0, 100);
        this.livingMass = clamp(livingValue, 0, 10);
        this.lastFeedTick = Math.max(0L, lastFeedValue);
        this.progress.clear();
        this.longProgress.clear();
        if (progressValues != null) {
            for (var entry : progressValues.entrySet()) {
                if (entry.getKey() != null && !entry.getKey().isBlank()) {
                    this.progress.put(entry.getKey(), Math.max(0, entry.getValue()));
                }
            }
        }
        this.flags.clear();
        if (flagValues != null) {
            for (String flag : flagValues) {
                if (flag != null && !flag.isBlank()) this.flags.add(flag);
            }
        }
    }

    public void copyFrom(OriginState other) {
        this.origin = other.origin;
        this.blood = other.blood;
        this.sunExposure = other.sunExposure;
        this.wetnessTicks = other.wetnessTicks;
        this.wingStaminaTicks = other.wingStaminaTicks;
        this.instability = other.instability;
        this.soul = other.soul;
        this.boneMass = other.boneMass;
        this.carbonLayer = other.carbonLayer;
        this.livingMass = other.livingMass;
        this.lavaGraceTicks = other.lavaGraceTicks;
        this.lastFeedTick = other.lastFeedTick;
        this.progress.clear();
        this.progress.putAll(other.progress);
        this.longProgress.clear();
        this.longProgress.putAll(other.longProgress);
        this.flags.clear();
        this.flags.addAll(other.flags);
    }

    public void write(NbtCompound nbt) {
        nbt.putString("Origin", origin);
        nbt.putInt("Blood", blood);
        nbt.putInt("SunExposure", sunExposure);
        nbt.putInt("Wetness", wetnessTicks);
        nbt.putInt("WingStamina", wingStaminaTicks);
        nbt.putInt("Instability", instability);
        nbt.putInt("Soul", soul);
        nbt.putInt("BoneMass", boneMass);
        nbt.putInt("CarbonLayer", carbonLayer);
        nbt.putInt("LivingMass", livingMass);
        nbt.putInt("LavaGrace", lavaGraceTicks);
        nbt.putLong("LastFeedTick", lastFeedTick);

        NbtCompound progressNbt = new NbtCompound();
        for (var entry : progress.entrySet()) progressNbt.putInt(entry.getKey(), entry.getValue());
        nbt.put("OriginProgress", progressNbt);

        NbtCompound longProgressNbt = new NbtCompound();
        for (var entry : longProgress.entrySet()) longProgressNbt.putLong(entry.getKey(), entry.getValue());
        nbt.put("OriginLongProgress", longProgressNbt);

        NbtCompound flagNbt = new NbtCompound();
        for (String flag : flags) flagNbt.putBoolean(flag, true);
        nbt.put("OriginFlags", flagNbt);
    }

    public void read(NbtCompound nbt) {
        origin = nbt.getString("Origin");
        blood = nbt.contains("Blood") ? nbt.getInt("Blood") : 100;
        sunExposure = nbt.getInt("SunExposure");
        wetnessTicks = nbt.contains("Wetness") ? nbt.getInt("Wetness") : 90 * 20;
        wingStaminaTicks = nbt.contains("WingStamina") ? nbt.getInt("WingStamina") : 6 * 20;
        instability = nbt.getInt("Instability");
        soul = nbt.contains("Soul") ? nbt.getInt("Soul") : 100;
        boneMass = nbt.getInt("BoneMass");
        carbonLayer = nbt.getInt("CarbonLayer");
        livingMass = nbt.getInt("LivingMass");
        lavaGraceTicks = nbt.contains("LavaGrace") ? nbt.getInt("LavaGrace") : 5 * 20;
        lastFeedTick = nbt.getLong("LastFeedTick");

        progress.clear();
        NbtCompound progressNbt = nbt.getCompound("OriginProgress");
        for (String key : progressNbt.getKeys()) progress.put(key, Math.max(0, progressNbt.getInt(key)));

        longProgress.clear();
        NbtCompound longProgressNbt = nbt.getCompound("OriginLongProgress");
        for (String key : longProgressNbt.getKeys()) longProgress.put(key, Math.max(0L, longProgressNbt.getLong(key)));

        flags.clear();
        NbtCompound flagNbt = nbt.getCompound("OriginFlags");
        for (String key : flagNbt.getKeys()) if (flagNbt.getBoolean(key)) flags.add(key);
    }

    private void resetResources() {
        blood = 100;
        sunExposure = 0;
        wetnessTicks = 90 * 20;
        wingStaminaTicks = 6 * 20;
        instability = 0;
        soul = 100;
        boneMass = 0;
        carbonLayer = 0;
        livingMass = 0;
        lavaGraceTicks = 5 * 20;
        lastFeedTick = 0L;
        progress.clear();
        longProgress.clear();
        flags.clear();
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
