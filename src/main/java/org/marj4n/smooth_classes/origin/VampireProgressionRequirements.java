package org.marj4n.smooth_classes.origin;

/**
 * Single source of truth for Vampire objective thresholds.
 * Used by server unlocks and client tracking so they cannot disagree.
 * Counters are lifetime milestones, not spendable Origin XP.
 */
public final class VampireProgressionRequirements {
    private VampireProgressionRequirements() { }

    public static final int BAT_KILLS = 20;
    public static final int MAN_BAT_HUMANOID_BLOOD = 200;
    public static final int MAN_BAT_NIGHT_KILLS = 50;
    public static final int BLOOD_SENSE_LIFETIME = 750;
    public static final int BLOOD_FLASK_LIFETIME = 1000;
    public static final int NOBILITY_LIFETIME = 1500;
    public static final int SUN_TOLERANCE_NIGHT_KILLS = 125;
    public static final int LORD_LIFETIME = 3000;
}
