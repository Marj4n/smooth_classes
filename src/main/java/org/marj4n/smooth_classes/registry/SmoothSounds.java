package org.marj4n.smooth_classes.registry;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import org.marj4n.smooth_classes.SmoothClasses;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Registers every bundled Smooth Classes sound under smooth_classes. */
public final class SmoothSounds {
    private static final Map<String, SoundEvent> EVENTS = new LinkedHashMap<>();

    public static final Identifier SKILL_UNLOCK_ID = SmoothClasses.id("fx_ui_unlock");
    public static final Identifier ABILITY_USE_ID = SmoothClasses.id("soundeffect_7");
    public static final Identifier ABILITY_BLOCKED_ID = SmoothClasses.id("gong_warbly");
    public static SoundEvent SKILL_UNLOCK;
    public static SoundEvent ABILITY_USE;
    public static SoundEvent ABILITY_BLOCKED;

    private SmoothSounds() {}

    public static void register() {
        register("fx_skill_backstab");
        register("fx_ui_unlock");
        register("fx_ui_unlock2");
        register("fx_ui_unlock3");
        register("soundeffect_6");
        register("soundeffect_7");
        register("soundeffect_8");
        register("soundeffect_9");
        register("soundeffect_10");
        register("soundeffect_11");
        register("soundeffect_12");
        register("soundeffect_13");
        register("soundeffect_14");
        register("soundeffect_15");
        register("soundeffect_16");
        register("soundeffect_17");
        register("soundeffect_18");
        register("soundeffect_19");
        register("soundeffect_20");
        register("soundeffect_21");
        register("soundeffect_22");
        register("soundeffect_23");
        register("soundeffect_24");
        register("soundeffect_25");
        register("soundeffect_26");
        register("soundeffect_27");
        register("soundeffect_28");
        register("soundeffect_29");
        register("soundeffect_30");
        register("soundeffect_31");
        register("soundeffect_32");
        register("soundeffect_33");
        register("soundeffect_34");
        register("soundeffect_35");
        register("soundeffect_36");
        register("soundeffect_37");
        register("soundeffect_38");
        register("soundeffect_39");
        register("soundeffect_40");
        register("soundeffect_41");
        register("soundeffect_42");
        register("soundeffect_43");
        register("soundeffect_44");
        register("soundeffect_45");
        register("soundeffect_46");
        register("soundeffect_47");
        register("soundeffect_48");
        register("spell_energy");
        register("spell_earth_punch");
        register("spell_misc_01");
        register("spell_fireball");
        register("spell_slash");
        register("spell_slash_02");
        register("spell_arcane_hit");
        register("spell_arcane_cast");
        register("spell_gain_barrier");
        register("spell_arcane_nova");
        register("spell_radiant_cast");
        register("spell_radiant_hit");
        register("spell_radiant_expire");
        register("spell_celestial_cast");
        register("spell_celestial_hit");
        register("spell_thunder_cast");
        register("spell_fire_cast");
        register("spell_lightning_cast");
        register("activate_tower_beacon");
        register("ui_replenish_01");
        register("gong_warbly");
        register("magic_shamanic_power_12");
        register("place_stone_06");
        register("place_stone_07");
        register("place_stone_08");
        register("place_stone_09");
        register("place_stone_10");
        register("slash_02");
        register("damage_03");
        register("object_impact_thud_repeat");
        register("object_impact_thud");
        register("hit_03");
        register("magic_shamanic_voice_20");
        register("magic_shamanic_spell_01");
        register("magic_shamanic_spell_02");
        register("magic_shamanic_spell_03");
        register("magic_shamanic_spell_04");
        register("dark_whirlwind_whoosh");
        register("energy_charge");
        register("maw");
        register("activate_plinth_01");
        SKILL_UNLOCK = get("fx_ui_unlock");
        ABILITY_USE = get("soundeffect_7");
        ABILITY_BLOCKED = get("gong_warbly");
    }

    private static SoundEvent register(String path) {
        Identifier id = SmoothClasses.id(path);
        SoundEvent event = SoundEvent.of(id);
        Registry.register(Registries.SOUND_EVENT, id, event);
        EVENTS.put(path, event);
        return event;
    }

    public static SoundEvent get(String path) { return EVENTS.get(path); }
    public static Map<String, SoundEvent> all() { return Collections.unmodifiableMap(EVENTS); }
}
