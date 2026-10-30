package org.marj4n.smooth_classes.origin;

/** Stable square rings around the church. Plot shells are 7x7, spaced 10 blocks apart. */
public final class VampireGraveyardLayout {
    public static final int PLOT_SPACING = 10;
    public static final int MAX_CANDIDATES = 8192;
    private VampireGraveyardLayout() {}
    public record Offset(int x, int z) {}

    public static Offset offset(int index) {
        if (index < 0 || index >= MAX_CANDIDATES) throw new IllegalArgumentException("Invalid grave candidate: " + index);
        int ring = 5;
        while (index >= 8 * ring) index -= 8 * ring++;
        int edge = 2 * ring;
        int x, z;
        if (index < edge) { x = -ring + index; z = -ring; }
        else if (index < edge * 2) { x = ring; z = -ring + index - edge; }
        else if (index < edge * 3) { x = ring - (index - edge * 2); z = ring; }
        else { x = -ring; z = ring - (index - edge * 3); }
        return new Offset(x * PLOT_SPACING, z * PLOT_SPACING);
    }
}
