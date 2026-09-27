package org.marj4n.smooth_classes.content.saber;

import java.util.List;
import net.minecraft.util.Identifier;
import org.marj4n.smooth_classes.SmoothClasses;
import org.marj4n.smooth_classes.api.classsystem.PlayerClass;

public final class SaberClass implements PlayerClass {
    public static final Identifier ID = SmoothClasses.id("saber");
    @Override public Identifier id() { return ID; }
    @Override public String name() { return "Saber"; }
    @Override public String description() { return "Holy melee combat, shields, and protective abilities."; }

    @Override public List<Identifier> abilityIds() { return SaberContent.abilities().stream().map(a -> a.id()).toList(); }
    @Override public List<Identifier> talentIds() { return SaberContent.talents().stream().map(t -> t.id()).toList(); }
}
