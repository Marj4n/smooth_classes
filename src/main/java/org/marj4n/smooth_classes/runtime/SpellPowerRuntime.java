package org.marj4n.smooth_classes.runtime;

import net.minecraft.server.network.ServerPlayerEntity;
import net.spell_power.api.SpellPower;
import net.spell_power.api.SpellSchool;
import net.spell_power.api.SpellSchools;

/**
 * Small boundary around Spell Power. Class code never needs to know the
 * external API shape, which keeps Continued-style scaling readable.
 */
public final class SpellPowerRuntime {
    private SpellPowerRuntime() {}

    public static float arcane(ServerPlayerEntity player, double multiplier) { return scaled(SpellSchools.ARCANE, player, multiplier); }
    public static float fire(ServerPlayerEntity player, double multiplier) { return scaled(SpellSchools.FIRE, player, multiplier); }
    public static float frost(ServerPlayerEntity player, double multiplier) { return scaled(SpellSchools.FROST, player, multiplier); }
    public static float lightning(ServerPlayerEntity player, double multiplier) { return scaled(SpellSchools.LIGHTNING, player, multiplier); }
    public static float healing(ServerPlayerEntity player, double multiplier) { return scaled(SpellSchools.HEALING, player, multiplier); }
    public static float soul(ServerPlayerEntity player, double multiplier) { return scaled(SpellSchools.SOUL, player, multiplier); }
    public static double soulBase(ServerPlayerEntity player) { return Math.max(0.0D, SpellPower.getSpellPower(SpellSchools.SOUL, player).baseValue()); }

    public static float scaled(SpellSchool school, ServerPlayerEntity player, double multiplier) {
        return (float) Math.max(0.0D, SpellPower.getSpellPower(school, player).randomValue() * multiplier);
    }
}
