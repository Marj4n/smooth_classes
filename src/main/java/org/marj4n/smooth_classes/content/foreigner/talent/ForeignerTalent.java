package org.marj4n.smooth_classes.content.foreigner.talent;

import net.minecraft.util.Identifier;
import org.marj4n.smooth_classes.SmoothClasses;
import org.marj4n.smooth_classes.api.talent.Talent;

public record ForeignerTalent(String path, String name, String description, int maxRank) implements Talent {
    public ForeignerTalent { if (maxRank < 1) throw new IllegalArgumentException("maxRank must be at least 1"); }
    public ForeignerTalent(String path, String name, String description) { this(path, name, description, 1); }
    @Override public Identifier id() { return SmoothClasses.id("foreigner/" + path); }
}
