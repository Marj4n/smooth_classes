package org.marj4n.smooth_classes.content.ruler.talent;

import net.minecraft.util.Identifier;
import org.marj4n.smooth_classes.SmoothClasses;
import org.marj4n.smooth_classes.api.talent.Talent;

public record RulerTalent(String path, String name, String description, int maxRank) implements Talent {
    public RulerTalent { if (maxRank < 1) throw new IllegalArgumentException("maxRank must be at least 1"); }
    public RulerTalent(String path, String name, String description) { this(path, name, description, 1); }
    @Override public Identifier id() { return SmoothClasses.id("ruler/" + path); }
}
