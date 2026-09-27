package org.marj4n.smooth_classes.content.foreigner;

import java.util.List;
import net.minecraft.util.Identifier;
import org.marj4n.smooth_classes.SmoothClasses;
import org.marj4n.smooth_classes.api.classsystem.PlayerClass;

public final class ForeignerClass implements PlayerClass {
    public static final Identifier ID = SmoothClasses.id("foreigner");
    @Override public Identifier id() { return ID; }
    @Override public String name() { return "Foreigner"; }
    @Override public String description() { return "Weapon combat enhanced by elemental magic."; }

    @Override public List<Identifier> abilityIds() { return ForeignerContent.abilities().stream().map(a -> a.id()).toList(); }
    @Override public List<Identifier> talentIds() { return ForeignerContent.talents().stream().map(t -> t.id()).toList(); }
}
