package org.marj4n.smooth_classes.content.avenger.runtime;

import net.minecraft.entity.Entity;

/** Per-summon sound preference; vanilla Silent is tracked to clients and saved with the entity. */
public final class AvengerSummonSilence {
    private static final String MUTED = "smooth_classes_avenger_muted";
    private static final String PREVIOUSLY_SILENT = "smooth_classes_avenger_previously_silent";

    private AvengerSummonSilence() {}

    public static boolean toggle(Entity summon) {
        if (summon.getCommandTags().contains(MUTED)) {
            restore(summon);
            return false;
        }
        if (summon.isSilent()) summon.addCommandTag(PREVIOUSLY_SILENT);
        summon.addCommandTag(MUTED);
        summon.setSilent(true);
        return true;
    }

    public static void restore(Entity summon) {
        if (!summon.getCommandTags().contains(MUTED)) return;
        summon.setSilent(summon.getCommandTags().contains(PREVIOUSLY_SILENT));
        summon.removeScoreboardTag(MUTED);
        summon.removeScoreboardTag(PREVIOUSLY_SILENT);
    }
}
