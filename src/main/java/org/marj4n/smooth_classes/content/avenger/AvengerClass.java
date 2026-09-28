package org.marj4n.smooth_classes.content.avenger;

import net.minecraft.util.Identifier;
import org.marj4n.smooth_classes.SmoothClasses;
import org.marj4n.smooth_classes.api.classsystem.PlayerClass;

import java.util.List;

public final class AvengerClass implements PlayerClass {
    public static final Identifier ID = SmoothClasses.id("avenger");
    @Override public Identifier id() { return ID; }
    @Override public String name() { return "Avenger"; }
    @Override public String description() { return "From corpses we arise: persistent Death List souls, vanilla summons, and soul magic."; }


    @Override
    public List<Identifier> abilityIds() {
        return List.of(AvengerContent.CURTAIN_CALL.id(), AvengerContent.ENDLESS_DEVOUR.id());
    }

    @Override
    public List<Identifier> talentIds() {
        return AvengerContent.talents().stream().map(talent -> talent.id()).toList();
    }
}
