package org.marj4n.smooth_classes.api.talent;

import net.minecraft.util.Identifier;

public interface Talent {
    Identifier id();
    String name();
    String description();
    default int maxRank() { return 1; }
}
