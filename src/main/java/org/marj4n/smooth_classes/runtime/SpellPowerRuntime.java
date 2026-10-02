package org.marj4n.smooth_classes.runtime;

import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.spell_power.api.SpellPower;
import net.spell_power.api.SpellSchool;
import net.spell_power.api.SpellSchools;
import org.marj4n.smooth_classes.content.rider.runtime.RiderRuntime;

/**
 * Small boundary around Spell Power. Class code never needs to know the
 * external API shape, which keeps scaling readable.
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


    /**
     * Universal offensive scaling used by Ascendancy. It considers vanilla
     * Attack Damage plus every registered Spell Power school, including
     * physical/custom schools added by compatibility mods.
     */
    public static float strongest(ServerPlayerEntity player, double multiplier) {
        double strongest = Math.max(0.0D, player.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE));
        float mounted = RiderRuntime.mountedDamageMultiplier(player);
        for (SpellSchool school : SpellSchools.all()) {
            try {
                double scale = school.archetype == SpellSchool.Archetype.MAGIC ? mounted : 1.0D;
                double value = SpellPower.getSpellPower(school, player).randomValue() * scale;
                if (Double.isFinite(value)) strongest = Math.max(strongest, value);
            } catch (RuntimeException ignored) {
                // Optional schools are allowed to be registered without a player power source.
            }
        }
        return (float) Math.max(0.0D, strongest * multiplier);
    }


    public static float strongestMagic(ServerPlayerEntity player, double multiplier) {
        double strongest = 0.0D;
        for (SpellSchool school : SpellSchools.all()) {
            if (school.archetype != SpellSchool.Archetype.MAGIC) continue;
            try {
                double value = SpellPower.getSpellPower(school, player).randomValue();
                if (Double.isFinite(value)) strongest = Math.max(strongest, value);
            } catch (RuntimeException ignored) {
                // Optional schools may exist without a usable player attribute.
            }
        }
        return (float) Math.max(0.0D, strongest * multiplier * RiderRuntime.mountedDamageMultiplier(player));
    }

    public static double strongestBase(ServerPlayerEntity player) {
        double strongest = Math.max(0.0D, player.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE));
        float mounted = RiderRuntime.mountedDamageMultiplier(player);
        for (SpellSchool school : SpellSchools.all()) {
            try {
                double scale = school.archetype == SpellSchool.Archetype.MAGIC ? mounted : 1.0D;
                double value = SpellPower.getSpellPower(school, player).baseValue() * scale;
                if (Double.isFinite(value)) strongest = Math.max(strongest, value);
            } catch (RuntimeException ignored) {
                // See strongest(...).
            }
        }
        return strongest;
    }

    public static float scaled(SpellSchool school, ServerPlayerEntity player, double multiplier) {
        float value = (float) Math.max(0.0D, SpellPower.getSpellPower(school, player).randomValue() * multiplier);
        return value * RiderRuntime.mountedDamageMultiplier(player);
    }
}
