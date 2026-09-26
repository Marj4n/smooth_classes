package org.marj4n.smooth_classes.content.berserker.ability;

import net.minecraft.util.Identifier;
import org.marj4n.smooth_classes.SmoothClasses;
import org.marj4n.smooth_classes.api.ability.Ability;

public record BerserkerAbility(String path, String name, String description, int cooldownTicks) implements Ability {
    public BerserkerAbility(String path, String name, String description) { this(path, name, description, 0); }
    @Override public Identifier id() { return SmoothClasses.id("berserker/" + path); }
}
