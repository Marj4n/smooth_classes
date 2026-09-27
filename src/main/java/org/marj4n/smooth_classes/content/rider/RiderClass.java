package org.marj4n.smooth_classes.content.rider;

import java.util.List;
import net.minecraft.util.Identifier;
import org.marj4n.smooth_classes.SmoothClasses;
import org.marj4n.smooth_classes.api.classsystem.PlayerClass;

public final class RiderClass implements PlayerClass {
    public static final Identifier ID = SmoothClasses.id("rider");
    @Override public Identifier id() { return ID; }
    @Override public String name() { return "Rider"; }
    @Override public String description() { return "Mounted combat, conjured steeds, charges, auras, and blazing hoofprints."; }
    @Override public List<Identifier> abilityIds() { return RiderContent.abilities().stream().map(a -> a.id()).toList(); }
    @Override public List<Identifier> talentIds() { return RiderContent.talents().stream().map(t -> t.id()).toList(); }
}
