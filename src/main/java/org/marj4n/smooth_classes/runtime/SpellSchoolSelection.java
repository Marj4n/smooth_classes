package org.marj4n.smooth_classes.runtime;

import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.Formatting;
import net.spell_power.api.SpellSchool;
import net.spell_power.api.SpellSchools;

/** Shared six-school selector used by Caster and Foreigner class specials. */
public final class SpellSchoolSelection {
    public static final int ARCANE = 0;
    public static final int FIRE = 1;
    public static final int FROST = 2;
    public static final int WIND = 3;
    public static final int WATER = 4;
    public static final int EARTH = 5;
    public static final int SIZE = 6;

    private static final String[] NAMES = {"Arcane", "Fire", "Frost", "Wind", "Water", "Earth"};
    private static final String[] IDS = {
            "spell_power:arcane", "spell_power:fire", "spell_power:frost",
            "spell_power:air", "spell_power:water", "spell_power:earth"
    };

    private SpellSchoolSelection() {}

    public static SpellSchool school(int index) {
        return switch (Math.floorMod(index, SIZE)) {
            case ARCANE -> SpellSchools.ARCANE;
            case FIRE -> SpellSchools.FIRE;
            case FROST -> SpellSchools.FROST;
            case WIND -> SpellSchools.getSchool("spell_power:air");
            case WATER -> SpellSchools.getSchool("spell_power:water");
            case EARTH -> SpellSchools.getSchool("spell_power:earth");
            default -> null;
        };
    }

    public static String name(int index) {
        return NAMES[Math.floorMod(index, SIZE)];
    }

    public static String id(int index) {
        return IDS[Math.floorMod(index, SIZE)];
    }

    public static int index(SpellSchool school) {
        if (school == null || school.id == null) return -1;
        String id = school.id.toString();
        for (int i = 0; i < IDS.length; i++) if (IDS[i].equals(id)) return i;
        return -1;
    }

    public static Formatting color(int index) {
        return switch (Math.floorMod(index, SIZE)) {
            case FIRE -> Formatting.RED;
            case FROST -> Formatting.AQUA;
            case WIND -> Formatting.WHITE;
            case WATER -> Formatting.BLUE;
            case EARTH -> Formatting.GOLD;
            default -> Formatting.LIGHT_PURPLE;
        };
    }

    public static ParticleEffect particle(int index) {
        return switch (Math.floorMod(index, SIZE)) {
            case FIRE -> ParticleTypes.FLAME;
            case FROST -> ParticleTypes.SNOWFLAKE;
            case WIND -> ParticleTypes.CLOUD;
            case WATER -> ParticleTypes.SPLASH;
            case EARTH -> ParticleTypes.ASH;
            default -> ParticleTypes.ENCHANT;
        };
    }
}
