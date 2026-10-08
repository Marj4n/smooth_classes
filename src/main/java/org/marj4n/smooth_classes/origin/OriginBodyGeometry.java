package org.marj4n.smooth_classes.origin;

/**
 * Single source of truth for Bat Form body geometry.
 * Collision, eye/camera height and third-person render alignment all read these
 * values so the visible bat and the body the player actually controls occupy
 * the same compact vertical space.
 */
public final class OriginBodyGeometry {
    private OriginBodyGeometry() {}

    public static final float BAT_WIDTH = 0.50F;
    public static final float BAT_HEIGHT = 0.50F;
    public static final float BAT_EYE_HEIGHT = 0.25F;
    public static final float BAT_RENDER_SCALE = 0.35F;

    /** Ground pose is user-approved and intentionally frozen. */
    public static final double BAT_GROUNDED_RENDER_Y = 1.22D;

    /**
     * Flight only lifts the mesh a small amount inside the same compact body.
     * The previous 0.88 anchor moved the visible bat far above its half-block
     * collision body, which caused TPV to clip ceilings while FPV remained below.
     */
    public static final double BAT_FLIGHT_VISUAL_LIFT = 0.14D;
    public static final double BAT_AIRBORNE_RENDER_Y =
            BAT_GROUNDED_RENDER_Y - BAT_FLIGHT_VISUAL_LIFT;
}
