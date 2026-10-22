package org.marj4n.smooth_classes.client.origin.appearance;

/**
 * Mirrored Man-Bat wing rig angles, kept independent of Minecraft so they can
 * be tested against one another without a rendering engine.
 *
 * Imported VSB wing meshes are mirrored, but the original idle animation
 * contains a one-sided left-wing anchor track. That track is intentionally
 * suppressed by VsbFiguraAvatarRenderer; both wings use this single driver.
 */
public final class ManBatWingSymmetry {
    private ManBatWingSymmetry() {}

    public record WingRotation(float pitch, float yaw, float roll) {}

    public static WingRotation rotation(boolean left, float open, float beat, float dive) {
        float pitch = -9F - 20F * open - 8F * dive;
        float spread = 20F + 42F * open;
        float flap = 18F * open + 37F * beat;
        return new WingRotation(pitch, left ? -spread : spread, left ? flap : -flap);
    }
}
