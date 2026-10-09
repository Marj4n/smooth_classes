package org.marj4n.smooth_classes.client.origin;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Lightweight render/UI mirror of the server-owned Origin state. */
public final class OriginClientState {
    private static final long BLOOD_SENSE_BASE_EFFECT_MS = 10_000L;
    private static final long BLOOD_SENSE_LORD_EFFECT_MS = 20_000L;
    private static final long BLOOD_SENSE_FLASH_MS = 1_250L;
    private static final long LORD_EVOLUTION_EFFECT_MS = 6_500L;
    private static final long LORD_EVOLUTION_FLASH_MS = 1_800L;

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
    private static long manBatDurationUntilMs;
    private static long manBatCooldownUntilMs;
    private static long bloodSenseCooldownUntilMs;
    private static long bloodSenseEffectUntilMs;
    private static long bloodSenseFlashUntilMs;
    private static long lordEvolutionEffectUntilMs;
    private static long lordEvolutionFlashUntilMs;
    private static boolean initialized;

    private static Map<String, Integer> progress = Map.of();
    private static Set<String> flags = Set.of();

    private OriginClientState() {}

    public static void sync(String origin, int bloodValue, int bloodCapacityValue, int sun, int wetness, int wing,
                            int instabilityValue, int soulValue, int bone, int carbon, int living,
                            Map<String, Integer> progressValues, Set<String> flagValues) {
        boolean wasBat = flags.contains("vampire.form.bat");
        boolean enteringBat = flagValues != null && flagValues.contains("vampire.form.bat") && !wasBat;
        boolean wasLord = flags.contains("vampire.evolution.lord");
        boolean isLord = flagValues != null && flagValues.contains("vampire.evolution.lord");
        int previousSunExposure = sunExposure;
        long nowMs = System.currentTimeMillis();
        long previousBloodSenseRemaining = Math.max(0L, bloodSenseCooldownUntilMs - nowMs);

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
        vampireBatCooldownUntilMs = nowMs + batTicks * 50L;
        manBatDurationUntilMs = nowMs + Math.max(0, progress.getOrDefault("ui.vampire.man_bat_duration_ticks", 0)) * 50L;
        manBatCooldownUntilMs = nowMs + Math.max(0, progress.getOrDefault("ui.vampire.man_bat_cooldown_ticks", 0)) * 50L;
        bloodSenseCooldownUntilMs = nowMs + Math.max(0, progress.getOrDefault("ui.vampire.blood_sense_cooldown_ticks", 0)) * 50L;

        long newBloodSenseRemaining = Math.max(0L, bloodSenseCooldownUntilMs - nowMs);
        boolean freshBloodSensePulse = originId.equals("vampire")
                && newBloodSenseRemaining > previousBloodSenseRemaining + 4_000L
                && newBloodSenseRemaining >= 8_500L;
        if (freshBloodSensePulse) {
            bloodSenseEffectUntilMs = nowMs + (isLord ? BLOOD_SENSE_LORD_EFFECT_MS : BLOOD_SENSE_BASE_EFFECT_MS);
            bloodSenseFlashUntilMs = nowMs + BLOOD_SENSE_FLASH_MS;
        }

        // Only play the local lord-evolution reveal when it happens in-session.
        if (initialized && originId.equals("vampire") && isLord && !wasLord) {
            lordEvolutionEffectUntilMs = nowMs + LORD_EVOLUTION_EFFECT_MS;
            lordEvolutionFlashUntilMs = nowMs + LORD_EVOLUTION_FLASH_MS;
        }
        initialized = true;
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

    public static long bloodSenseCooldownRemainingMs() {
        return Math.max(0L, bloodSenseCooldownUntilMs - System.currentTimeMillis());
    }

    public static long bloodSenseEffectRemainingMs() {
        return Math.max(0L, bloodSenseEffectUntilMs - System.currentTimeMillis());
    }

    public static long bloodSenseFlashRemainingMs() {
        return Math.max(0L, bloodSenseFlashUntilMs - System.currentTimeMillis());
    }

    public static long lordEvolutionEffectRemainingMs() {
        return Math.max(0L, lordEvolutionEffectUntilMs - System.currentTimeMillis());
    }

    public static long lordEvolutionFlashRemainingMs() {
        return Math.max(0L, lordEvolutionFlashUntilMs - System.currentTimeMillis());
    }

    public static long manBatDurationRemainingMs() {
        return Math.max(0L, manBatDurationUntilMs - System.currentTimeMillis());
    }

    public static long manBatCooldownRemainingMs() {
        return Math.max(0L, manBatCooldownUntilMs - System.currentTimeMillis());
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
        manBatDurationUntilMs = 0L;
        manBatCooldownUntilMs = 0L;
        bloodSenseCooldownUntilMs = 0L;
        bloodSenseEffectUntilMs = 0L;
        bloodSenseFlashUntilMs = 0L;
        lordEvolutionEffectUntilMs = 0L;
        lordEvolutionFlashUntilMs = 0L;
        initialized = false;
        progress = Map.of();
        flags = Set.of();
    }
}
