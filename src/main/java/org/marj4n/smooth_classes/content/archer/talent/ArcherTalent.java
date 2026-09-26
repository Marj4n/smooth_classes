package org.marj4n.smooth_classes.content.archer.talent;

import net.minecraft.util.Identifier;
import org.marj4n.smooth_classes.SmoothClasses;
import org.marj4n.smooth_classes.api.talent.Talent;

public record ArcherTalent(String path, String name, String description, int maxRank) implements Talent {
    public ArcherTalent { if (maxRank < 1) throw new IllegalArgumentException("maxRank must be at least 1"); }
    public ArcherTalent(String path, String name, String description) { this(path, name, description, 1); }
    @Override public Identifier id() { return SmoothClasses.id("archer/" + path); }
}
