package org.marj4n.smooth_classes.content.assassin;

import java.util.List;
import net.minecraft.util.Identifier;
import org.marj4n.smooth_classes.SmoothClasses;
import org.marj4n.smooth_classes.api.classsystem.PlayerClass;

public final class AssassinClass implements PlayerClass {
    public static final Identifier ID = SmoothClasses.id("assassin");
    @Override public Identifier id() { return ID; }
    @Override public String name() { return "Assassin"; }
    @Override public String description() { return "Smooth Classes successor to SimplySkills Rogue."; }
    @Override public Identifier legacySource() { return new Identifier("simplyskills", "rogue"); }
    @Override public List<Identifier> abilityIds() { return AssassinContent.abilities().stream().map(a -> a.id()).toList(); }
    @Override public List<Identifier> talentIds() { return AssassinContent.talents().stream().map(t -> t.id()).toList(); }
}
