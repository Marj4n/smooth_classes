package org.marj4n.smooth_classes.client.origin;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Lightweight render/UI mirror of the server-owned Origin state. */
public final class OriginClientState {
    public static String originId = "";
    public static int blood;
    public static int bloodCapacity = 100;
    public static int sunExposure;
    private static float visualSunExposure;
    private static long visualSunLastNanos;
    public static int wetnessTicks;
    public static int wingStaminaTicks;
    public static int instability;
    public static int soul;
    public static int boneMass;
    public static int carbonLayer;
    public static int livingMass;
    private static long vampireBatCooldownUntilMs;

    private static Map<String, Integer> progress = Map.of();
    private static Set<String> flags = Set.of();

    private OriginClientState() {}

    public static void sync(String origin, int bloodValue, int bloodCapacityValue, int sun, int wetness, int wing,
                            int instabilityValue, int soulValue, int bone, int carbon, int living,
                            Map<String, Integer> progressValues, Set<String> flagValues) {
        boolean wasBat = flags.contains("vampire.form.bat");
        boolean enteringBat = flagValues != null && flagValues.contains("vampire.form.bat") && !wasBat;
        int previousSunExposure = sunExposure;

        originId = origin == null ? "" : origin;
        blood = bloodValue;
        bloodCapacity = Math.max(1, bloodCapacityValue);
        sunExposure = sun;

        // The server correctly clears sunlight exposure immediately when Bat Form starts,
        // but the visual burn should linger and fade instead of popping off in one frame.
        if (enteringBat) {
            visualSunExposure = Math.max(visualSunExposure, previousSunExposure);
            visualSunLastNanos = System.nanoTime();
        } else if (sunExposure > visualSunExposure) {
            // Sun pain should appear responsively while exposure is building.
            visualSunExposure = sunExposure;
        }
        wetnessTicks = wetness;
        wingStaminaTicks = wing;
        instability = instabilityValue;
        soul = soulValue;
        boneMass = bone;
        carbonLayer = carbon;
        livingMass = living;
        progress = Collections.unmodifiableMap(new HashMap<>(progressValues == null ? Map.of() : progressValues));
        flags = Collections.unmodifiableSet(new HashSet<>(flagValues == null ? Set.of() : flagValues));
        int batTicks = Math.max(0, progress.getOrDefault("ui.vampire.bat_cooldown_ticks", 0));
        vampireBatCooldownUntilMs = System.currentTimeMillis() + batTicks * 50L;
    }


    public static float visualSunExposure() {
        long now = System.nanoTime();
        if (visualSunLastNanos == 0L) visualSunLastNanos = now;
        float dt = Math.min(0.10F, Math.max(0.0F, (now - visualSunLastNanos) / 1_000_000_000.0F));
        visualSunLastNanos = now;

        float target = Math.max(0.0F, Math.min(100.0F, sunExposure));
        if (visualSunExposure < target) {
            visualSunExposure = target;
        } else if (visualSunExposure > target) {
            // About 1.4 seconds from full exposure to clear: visible enough to feel like
            // the sunlight is leaving the eyes, but short enough to keep Bat travel snappy.
            visualSunExposure = Math.max(target, visualSunExposure - (72.0F * dt));
        }
        return visualSunExposure;
    }

    public static long vampireBatCooldownRemainingMs() {
        return Math.max(0L, vampireBatCooldownUntilMs - System.currentTimeMillis());
    }

    public static int progress(String key) {
        return Math.max(0, progress.getOrDefault(key, 0));
    }

    public static boolean hasFlag(String key) {
        return flags.contains(key);
    }

    public static int countFlags(String prefix) {
        int count = 0;
        for (String flag : flags) if (flag.startsWith(prefix)) count++;
        return count;
    }

    public static Map<String, Integer> progressSnapshot() { return progress; }
    public static Set<String> flagsSnapshot() { return flags; }

    public static void reset() {
        originId = "";
        blood = sunExposure = wetnessTicks = wingStaminaTicks = instability = soul = boneMass = carbonLayer = livingMass = 0;
        visualSunExposure = 0.0F;
        visualSunLastNanos = 0L;
        bloodCapacity = 100;
        vampireBatCooldownUntilMs = 0L;
        progress = Map.of();
        flags = Set.of();
    }
}
