package org.marj4n.smooth_classes.origin;

/**
 * On death/respawn only the player's progression is persistent. Active forms
 * and their visual/flight/temporary-health state belong to the old entity.
 * Clear these before Minecraft copies player NBT and before re-synchronizing.
 */
public final class VampireFormRespawnRuntime {
    private VampireFormRespawnRuntime() {}

    public static void clear(OriginState state) {
        if (state.origin() != OriginType.VAMPIRE) return;
        state.unflag("vampire.form.bat");
        state.unflag("vampire.form.man_bat");
        VampireBatHealthRuntime.discard(state);
        state.longProgress("vampire.man_bat_end_at", 0L);
        state.longProgress("vampire.man_bat_next_drain", 0L);
        state.longProgress("vampire.man_bat_jump_ready_at", 0L);
        state.longProgress("vampire.man_bat_previous_fly_speed", 0L);
    }
}
