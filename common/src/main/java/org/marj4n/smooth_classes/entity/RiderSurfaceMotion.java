package org.marj4n.smooth_classes.entity;

/** Smooth Classes surface-motion policy, separated from Minecraft entity/world access. */
public final class RiderSurfaceMotion {
    private RiderSurfaceMotion() {}

    public enum Mode { UNCHANGED, SUPPORTED, RISING }

    /** Select a response without touching position, collision, AI, or entity state. */
    public static Mode mode(boolean contact, boolean columnContinues, double floorY, double topY) {
        if (!contact) return Mode.UNCHANGED;
        return !columnContinues && floorY >= topY - 0.42D ? Mode.SUPPORTED : Mode.RISING;
    }

    public static double top(int cellY, double fluidHeight) {
        return cellY + (fluidHeight <= 0.0D ? 1.0D : fluidHeight);
    }

    public static boolean replacesVelocity(Mode mode, double verticalSpeed) {
        return mode == Mode.RISING || (mode == Mode.SUPPORTED && verticalSpeed < 0.0D);
    }

    public static double horizontal(Mode mode, double speed) {
        // Retain the vector addition's signed-zero behavior on horizontal axes too.
        return mode == Mode.RISING ? speed * 0.5D + 0.0D : speed;
    }

    public static double vertical(Mode mode, double speed) {
        return switch (mode) {
            case UNCHANGED -> speed;
            case SUPPORTED -> speed < 0.0D ? 0.0D : speed;
            case RISING -> speed * 0.5D + 0.05D;
        };
    }
}
