package org.marj4n.smooth_classes.content.lancer;

import java.util.List;
import net.minecraft.util.Identifier;
import org.marj4n.smooth_classes.SmoothClasses;
import org.marj4n.smooth_classes.api.classsystem.PlayerClass;

public final class LancerClass implements PlayerClass {
    public static final Identifier ID = SmoothClasses.id("lancer");
    @Override public Identifier id() { return ID; }
    @Override public String name() { return "Lancer"; }
    @Override public String description() { return "Spear mastery, escalating Momentum, impalement chains, thrusts, and spear storms."; }
    @Override public List<Identifier> abilityIds() { return LancerContent.abilities().stream().map(a -> a.id()).toList(); }
    @Override public List<Identifier> talentIds() { return LancerContent.talents().stream().map(t -> t.id()).toList(); }
}
