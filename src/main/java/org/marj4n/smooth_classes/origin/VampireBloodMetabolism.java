package org.marj4n.smooth_classes.origin;

import net.minecraft.world.Difficulty;

/** Starvation is driven by the player's Blood reserve, never hidden food. */
public final class VampireBloodMetabolism {
    private VampireBloodMetabolism() {}

    public static boolean canStarve(int blood, Difficulty difficulty, float currentHealth) {
        if (blood > 0 || difficulty == Difficulty.PEACEFUL || currentHealth <= 0.0F) return false;
        if (difficulty == Difficulty.HARD) return true;
        if (difficulty == Difficulty.NORMAL) return currentHealth > 1.0F;
        return currentHealth > 10.0F; // Easy: never below 5 hearts.
    }
}
