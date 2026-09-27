package org.marj4n.smooth_classes.content.ruler;

import java.util.List;
import org.marj4n.smooth_classes.api.ability.Ability;
import org.marj4n.smooth_classes.api.talent.Talent;
import org.marj4n.smooth_classes.content.ruler.ability.RulerAbility;
import org.marj4n.smooth_classes.content.ruler.talent.RulerTalent;
import org.marj4n.smooth_classes.registry.AbilityRegistry;
import org.marj4n.smooth_classes.registry.TalentRegistry;

/** Class abilities and talents; runtime behavior is implemented separately. */
public final class RulerContent {
    private RulerContent() {}

    public static final Ability SACRED_ORB = ability("sacred_orb", "Sacred Banner", 120 * 20);
    public static final Ability DIVINE_INTERVENTION = ability("divine_intervention", "Divine Intervention", 45 * 20);
    public static final Ability ANOINT_WEAPON = ability("anoint_weapon", "Anoint Weapon", 35 * 20);

    public static final Talent ALTRUISM = talent("altruism", "Altruism");
    public static final Talent MUTUAL_MENDING = talent("mutual_mending", "Mutual Mending");
    public static final Talent HEALING_WARD = talent("healing_ward", "Healing Ward");
    public static final Talent SACRED_ORB_SPEED = talent("sacred_orb_speed", "Banner Strength");
    public static final Talent SACRED_ORB_DEBUFFS = talent("sacred_orb_debuffs", "Banner Resistance");
    public static final Talent SACRED_ORB_BUFFS = talent("sacred_orb_buffs", "Banner Haste");
    public static final Talent DIVINE_INTERVENTION_FIRE_RESISTANCE = talent("divine_intervention_fire_resistance", "Divine Intervention Fire Resistance");
    public static final Talent DIVINE_INTERVENTION_MIGHT = talent("divine_intervention_might", "Divine Intervention Might");
    public static final Talent DIVINE_INTERVENTION_SPELLFORGED = talent("divine_intervention_spellforged", "Divine Intervention Spellforged");
    public static final Talent ANOINT_WEAPON_RESISTANCE = talent("anoint_weapon_resistance", "Anoint Weapon Resistance");
    public static final Talent ANOINT_WEAPON_UNDYING = talent("anoint_weapon_undying", "Anoint Weapon Undying");
    public static final Talent ANOINT_WEAPON_CLEANSE = talent("anoint_weapon_cleanse", "Anoint Weapon Cleanse");

    private static final List<Ability> ABILITIES = List.of(SACRED_ORB, DIVINE_INTERVENTION, ANOINT_WEAPON);
    private static final List<Talent> TALENTS = List.of(ALTRUISM, MUTUAL_MENDING, HEALING_WARD, SACRED_ORB_SPEED, SACRED_ORB_DEBUFFS, SACRED_ORB_BUFFS, DIVINE_INTERVENTION_FIRE_RESISTANCE, DIVINE_INTERVENTION_MIGHT, DIVINE_INTERVENTION_SPELLFORGED, ANOINT_WEAPON_RESISTANCE, ANOINT_WEAPON_UNDYING, ANOINT_WEAPON_CLEANSE);

    public static void register() {
        ABILITIES.forEach(AbilityRegistry::register);
        TALENTS.forEach(TalentRegistry::register);
    }
    public static List<Ability> abilities() { return ABILITIES; }
    public static List<Talent> talents() { return TALENTS; }
    private static Ability ability(String path, String name, int cooldownTicks) { return new RulerAbility(path, name, "Ruler signature ability.", cooldownTicks); }
    private static Talent talent(String path, String name) { return new RulerTalent(path, name, "Ruler talent node."); }
}
