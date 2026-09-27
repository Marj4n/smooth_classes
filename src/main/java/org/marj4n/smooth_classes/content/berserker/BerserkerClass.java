package org.marj4n.smooth_classes.content.berserker;

import java.util.List;
import net.minecraft.util.Identifier;
import org.marj4n.smooth_classes.SmoothClasses;
import org.marj4n.smooth_classes.api.classsystem.PlayerClass;

public final class BerserkerClass implements PlayerClass {
    public static final Identifier ID = SmoothClasses.id("berserker");
    @Override public Identifier id() { return ID; }
    @Override public String name() { return "Berserker"; }
    @Override public String description() { return "Aggressive melee combat, rage, and sustained pressure."; }

    @Override public List<Identifier> abilityIds() { return BerserkerContent.abilities().stream().map(a -> a.id()).toList(); }
    @Override public List<Identifier> talentIds() { return BerserkerContent.talents().stream().map(t -> t.id()).toList(); }
}
