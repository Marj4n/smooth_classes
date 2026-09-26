package org.marj4n.smooth_classes.content.avenger;

import net.minecraft.util.Identifier;
import org.marj4n.smooth_classes.SmoothClasses;
import org.marj4n.smooth_classes.api.classsystem.PlayerClass;

import java.util.List;

public final class AvengerClass implements PlayerClass {
    public static final Identifier ID = SmoothClasses.id("avenger");
    @Override public Identifier id() { return ID; }
    @Override public String name() { return "Avenger"; }
    @Override public String description() { return "Necromancy, summons, soul and shadow-oriented combat."; }
    @Override public Identifier legacySource() { return new Identifier("simplyskills", "necromancer"); }

    @Override
    public List<Identifier> abilityIds() {
        return List.of(AvengerContent.SUMMONING_RITUAL.id());
    }

    @Override
    public List<Identifier> talentIds() {
        return AvengerContent.talents().stream().map(talent -> talent.id()).toList();
    }
}
