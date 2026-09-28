package org.marj4n.smooth_classes.content.avenger.ability;

import net.minecraft.util.Identifier;
import org.marj4n.smooth_classes.SmoothClasses;
import org.marj4n.smooth_classes.api.ability.Ability;

public record AvengerAbility(String path, String name, String description, int cooldownTicks) implements Ability {
    @Override public Identifier id() { return SmoothClasses.id("avenger/" + path); }
}
