package org.marj4n.smooth_classes.api.classsystem;

import net.minecraft.util.Identifier;
import java.util.List;

public interface PlayerClass {
    Identifier id();
    String name();
    String description();
    Identifier legacySource();
    default List<Identifier> abilityIds() { return List.of(); }
    default List<Identifier> talentIds() { return List.of(); }
}
