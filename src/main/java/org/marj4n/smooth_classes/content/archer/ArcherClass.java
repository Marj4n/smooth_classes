package org.marj4n.smooth_classes.content.archer;

import java.util.List;
import net.minecraft.util.Identifier;
import org.marj4n.smooth_classes.SmoothClasses;
import org.marj4n.smooth_classes.api.classsystem.PlayerClass;

public final class ArcherClass implements PlayerClass {
    public static final Identifier ID = SmoothClasses.id("archer");
    @Override public Identifier id() { return ID; }
    @Override public String name() { return "Archer"; }
    @Override public String description() { return "Ranged combat, precision shots, and elemental arrows."; }

    @Override public List<Identifier> abilityIds() { return ArcherContent.abilities().stream().map(a -> a.id()).toList(); }
    @Override public List<Identifier> talentIds() { return ArcherContent.talents().stream().map(t -> t.id()).toList(); }
}
