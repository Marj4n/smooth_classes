package org.marj4n.smooth_classes.content.caster;

import java.util.List;
import net.minecraft.util.Identifier;
import org.marj4n.smooth_classes.SmoothClasses;
import org.marj4n.smooth_classes.api.classsystem.PlayerClass;

public final class CasterClass implements PlayerClass {
    public static final Identifier ID = SmoothClasses.id("caster");
    @Override public Identifier id() { return ID; }
    @Override public String name() { return "Caster"; }
    @Override public String description() { return "Elemental spellcasting, Elemental Attunement, and powerful area attacks."; }

    @Override public List<Identifier> abilityIds() { return CasterContent.abilities().stream().map(a -> a.id()).toList(); }
    @Override public List<Identifier> talentIds() { return CasterContent.talents().stream().map(t -> t.id()).toList(); }
}
