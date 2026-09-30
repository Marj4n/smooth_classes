package org.marj4n.smooth_classes.client.charge;

import net.minecraft.util.math.MathHelper;

/**
 * Client-only mirror for Smooth Classes' short hold-to-charge casts.
 *
 * The server owns the real cast and cooldown state. This class only interpolates
 * the progress shown over the vanilla XP bar; it never changes player XP.
 */
public final class ChargeHudState {
    public static final String UNLIMITED_BLADE_WORKS = "unlimited_blade_works";
    public static final String ARCANE_SLASH = "arcane_slash";

    private static boolean active;
    private static String ability = "";
    private static int elapsedTicks;
    private static int totalTicks = 16;
    private static long syncedAtNanos;

    private ChargeHudState() {}

    public static void sync(boolean newActive, boolean completed, String newAbility, int elapsed, int total) {
        if (!newActive) {
            active = false;
            ability = "";
            elapsedTicks = completed ? Math.max(1, total) : 0;
            totalTicks = Math.max(1, total);
            syncedAtNanos = 0L;
            return;
        }

        active = true;
        ability = newAbility == null ? "" : newAbility;
        elapsedTicks = Math.max(0, elapsed);
        totalTicks = Math.max(1, total);
        syncedAtNanos = System.nanoTime();
    }

    public static void reset() {
        active = false;
        ability = "";
        elapsedTicks = 0;
        totalTicks = 16;
        syncedAtNanos = 0L;
    }

    public static boolean active() {
        return active;
    }

    public static boolean active(String queriedAbility) {
        return active && ability.equals(queriedAbility);
    }

    public static String ability() {
        return ability;
    }

    /** Smoothly fills between the server's start packet and completion packet. */
    public static float progress() {
        if (!active) return 0F;
        double localTicks = syncedAtNanos == 0L ? 0D
                : Math.max(0D, (System.nanoTime() - syncedAtNanos) / 50_000_000D);
        return MathHelper.clamp((float) ((elapsedTicks + localTicks) / totalTicks), 0F, 1F);
    }

    public static int tint() {
        return UNLIMITED_BLADE_WORKS.equals(ability) ? 0xF4D27C : 0xB784FF;
    }

    public static String label() {
        return UNLIMITED_BLADE_WORKS.equals(ability) ? "Portal of Sovereignty" : "Arcane Slash";
    }
}
