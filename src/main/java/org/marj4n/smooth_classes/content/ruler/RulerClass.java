package org.marj4n.smooth_classes.content.ruler;

import java.util.List;
import net.minecraft.util.Identifier;
import org.marj4n.smooth_classes.SmoothClasses;
import org.marj4n.smooth_classes.api.classsystem.PlayerClass;

public final class RulerClass implements PlayerClass {
    public static final Identifier ID = SmoothClasses.id("ruler");
    @Override public Identifier id() { return ID; }
    @Override public String name() { return "Ruler"; }
    @Override public String description() { return "Smooth Classes successor to SimplySkills Cleric."; }
    @Override public Identifier legacySource() { return new Identifier("simplyskills", "cleric"); }
    @Override public List<Identifier> abilityIds() { return RulerContent.abilities().stream().map(a -> a.id()).toList(); }
    @Override public List<Identifier> talentIds() { return RulerContent.talents().stream().map(t -> t.id()).toList(); }
}
