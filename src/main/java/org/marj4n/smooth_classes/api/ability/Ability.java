package org.marj4n.smooth_classes.api.ability;

import net.minecraft.util.Identifier;

public interface Ability {
    Identifier id();
    String name();
    String description();
    default int cooldownTicks() { return 0; }
}
