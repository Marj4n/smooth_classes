package org.marj4n.smooth_classes.content.rider;

import java.util.List;
import org.marj4n.smooth_classes.api.ability.Ability;
import org.marj4n.smooth_classes.api.talent.Talent;
import org.marj4n.smooth_classes.content.rider.ability.RiderAbility;
import org.marj4n.smooth_classes.content.rider.talent.RiderTalent;
import org.marj4n.smooth_classes.registry.AbilityRegistry;
import org.marj4n.smooth_classes.registry.TalentRegistry;

public final class RiderContent {
    private RiderContent() {}

    public static final Ability CHARGE = ability("charge", "Rider's Charge", 12 * 20);
    public static final Ability WAR_AURA = ability("war_aura", "War Aura", 24 * 20);
    public static final Ability BLAZING_HOOVES = ability("blazing_hooves", "Blazing Hooves", 20 * 20);

    public static final Talent CHARGE_REACH = talent("charge_reach", "Long Rein");
    public static final Talent CHARGE_IMPACT = talent("charge_impact", "Lance Impact");
    public static final Talent CHARGE_PHASE = talent("charge_phase", "Phantom Gallop");
    public static final Talent AURA_RESISTANCE = talent("aura_resistance", "Iron Escort");
    public static final Talent AURA_STRENGTH = talent("aura_strength", "War Cry");
    public static final Talent AURA_HARM = talent("aura_harm", "Dread Presence");
    public static final Talent FIRE_DAMAGE = talent("fire_damage", "Scorching Hooves");
    public static final Talent FIRE_DURATION = talent("fire_duration", "Endless Cinders");
    public static final Talent FIRE_RADIUS = talent("fire_radius", "Wide Trail");

    public static final Talent WATER_STRIDE = talent("water_stride", "Water Stride");
    public static final Talent DREAD_STEED = talent("dread_steed", "Dread Steed");
    public static final Talent HIPPOGRYPH = talent("hippogryph", "White Hippogryph");

    public static final Talent HEALTH_I = talent("health_i", "Mount Vitality I");
    public static final Talent HEALTH_II = talent("health_ii", "Mount Vitality II");
    public static final Talent HEALTH_III = talent("health_iii", "Mount Vitality III");
    public static final Talent SPEED_I = talent("speed_i", "Mount Speed I");
    public static final Talent SPEED_II = talent("speed_ii", "Mount Speed II");
    public static final Talent SPEED_III = talent("speed_iii", "Mount Speed III");
    public static final Talent ARMOR_I = talent("armor_i", "Mount Armor I");
    public static final Talent ARMOR_II = talent("armor_ii", "Mount Armor II");
    public static final Talent JUMP_I = talent("jump_i", "Mount Jump I");
    public static final Talent JUMP_II = talent("jump_ii", "Mount Jump II");
    public static final Talent RECOVERY_I = talent("recovery_i", "Mount Recovery I");
    public static final Talent RECOVERY_II = talent("recovery_ii", "Mount Recovery II");

    private static final List<Ability> ABILITIES = List.of(CHARGE, WAR_AURA, BLAZING_HOOVES);
    private static final List<Talent> TALENTS = List.of(
            CHARGE_REACH, CHARGE_IMPACT, CHARGE_PHASE,
            AURA_RESISTANCE, AURA_STRENGTH, AURA_HARM,
            FIRE_DAMAGE, FIRE_DURATION, FIRE_RADIUS,
            WATER_STRIDE, DREAD_STEED, HIPPOGRYPH,
            HEALTH_I, HEALTH_II, HEALTH_III,
            SPEED_I, SPEED_II, SPEED_III,
            ARMOR_I, ARMOR_II, JUMP_I, JUMP_II, RECOVERY_I, RECOVERY_II
    );

    public static void register() {
        ABILITIES.forEach(AbilityRegistry::register);
        TALENTS.forEach(TalentRegistry::register);
    }
    public static List<Ability> abilities() { return ABILITIES; }
    public static List<Talent> talents() { return TALENTS; }
    private static Ability ability(String path, String name, int cooldownTicks) { return new RiderAbility(path, name, "Rider signature ability.", cooldownTicks); }
    private static Talent talent(String path, String name) { return new RiderTalent(path, name, "Rider mount talent."); }
}
