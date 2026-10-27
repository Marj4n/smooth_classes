package org.marj4n.smooth_classes.content.avenger.talent;

import net.minecraft.util.Identifier;
import org.marj4n.smooth_classes.SmoothClasses;
import org.marj4n.smooth_classes.api.talent.Talent;

/** A compact immutable talent definition owned by the Avenger class. */
public record AvengerTalent(String path, String name, String description, int maxRank) implements Talent {
    public AvengerTalent {
        if (maxRank < 1) throw new IllegalArgumentException("maxRank must be at least 1");
    }

    public AvengerTalent(String path, String name, String description) {
        this(path, name, description, 1);
    }

    @Override public Identifier id() { return SmoothClasses.id("avenger/" + path); }
}
