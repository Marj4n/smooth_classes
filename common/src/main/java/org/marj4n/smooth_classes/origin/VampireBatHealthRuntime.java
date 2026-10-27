package org.marj4n.smooth_classes.origin;

import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Bat Form's temporary maximum-health penalty must not permanently erase hearts.
 * Store the real pre-transform HP and the HP after the temporary cap is applied,
 * then restore the difference on exit while retaining real damage/healing suffered
 * inside the form. The snapshot lives in OriginState so relogs preserve it.
 */
public final class VampireBatHealthRuntime {
    private static final String ACTIVE = "vampire.bat_health_snapshot_active";
    private static final String ORIGINAL_HP = "vampire.bat_health_original_bits";
    private static final String CAPPED_HP = "vampire.bat_health_capped_bits";

    private VampireBatHealthRuntime() {}

    public static void enter(ServerPlayerEntity player, OriginState state) {
        if (state.hasFlag(ACTIVE)) return;
        state.longProgress(ORIGINAL_HP, Float.floatToIntBits(player.getHealth()));
        state.flag(ACTIVE);

        // Apply the reduced max-health attribute NOW, rather than waiting up to 10 ticks.
        OriginRuntime.refreshVampireAttributes(player);
        state.longProgress(CAPPED_HP, Float.floatToIntBits(player.getHealth()));
    }

    /** Call AFTER removing vampire.form.bat, so normal humanoid HP is available. */
    public static void exit(ServerPlayerEntity player, OriginState state) {
        boolean hadSnapshot = state.hasFlag(ACTIVE);
        float original = Float.intBitsToFloat((int) state.longProgress(ORIGINAL_HP));
        float capped = Float.intBitsToFloat((int) state.longProgress(CAPPED_HP));
        float ending = player.getHealth();
        discard(state);
        OriginRuntime.refreshVampireAttributes(player);

        // Never resurrect the player or generate free HP when old saves lack a snapshot.
        if (!hadSnapshot || !player.isAlive() || ending <= 0.0F
                || !Float.isFinite(original) || !Float.isFinite(capped)
                || original <= 0.0F || capped <= 0.0F) return;

        // Example: enter with 15 HP -> bat is capped to 7 HP -> suffer 2 real damage.
        // Exit with 15 + (5 - 7) = 13 HP, not 5 and not a free full heal.
        float restored = original + (ending - capped);
        player.setHealth(Math.max(0.1F, Math.min(player.getMaxHealth(), restored)));
    }

    /** Drop snapshots on death (not a form exit) or when abandoning an invalid form. */
    public static void discard(OriginState state) {
        state.unflag(ACTIVE);
        state.longProgress(ORIGINAL_HP, 0L);
        state.longProgress(CAPPED_HP, 0L);
    }
}
