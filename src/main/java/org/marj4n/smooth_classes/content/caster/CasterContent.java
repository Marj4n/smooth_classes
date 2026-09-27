package org.marj4n.smooth_classes.content.caster;

import java.util.List;
import org.marj4n.smooth_classes.api.ability.Ability;
import org.marj4n.smooth_classes.api.talent.Talent;
import org.marj4n.smooth_classes.content.caster.ability.CasterAbility;
import org.marj4n.smooth_classes.content.caster.talent.CasterTalent;
import org.marj4n.smooth_classes.registry.AbilityRegistry;
import org.marj4n.smooth_classes.registry.TalentRegistry;

/** Class abilities and talents; runtime behavior is implemented separately. */
public final class CasterContent {
    private CasterContent() {}

    public static final Ability METEOR_SHOWER = ability("meteor_shower", "Meteor Shower", 40 * 20);
    public static final Ability ICE_COMET = ability("ice_comet", "Ice Comet", 30 * 20);
    public static final Ability STATIC_DISCHARGE = ability("static_discharge", "Static Discharge", 16 * 20);
    public static final Ability ARCANE_BOLT = ability("arcane_bolt", "Arcane Bolt", 35 * 20);

    public static final Talent SPELL_ECHO = talent("spell_echo", "Spell Echo");
    public static final Talent METEOR_SHOWER_GREATER = talent("meteor_shower_greater", "Meteor Shower Greater");
    public static final Talent METEOR_SHOWER_WRATH = talent("meteor_shower_wrath", "Meteor Shower Wrath");
    public static final Talent METEOR_SHOWER_RENEWING_WRATH = talent("meteor_shower_renewing_wrath", "Meteor Shower Renewing Wrath");
    public static final Talent ICE_COMET_LEAP = talent("ice_comet_leap", "Ice Comet Leap");
    public static final Talent ICE_COMET_VOLLEY = talent("ice_comet_volley", "Ice Comet Volley");
    public static final Talent ICE_COMET_DAMAGE = talent("ice_comet_damage", "Ice Comet Damage");
    public static final Talent STATIC_DISCHARGE_LIGHTNING_BALL = talent("static_discharge_lightning_ball", "Static Discharge Lightning Ball");
    public static final Talent STATIC_DISCHARGE_LEAP = talent("static_discharge_leap", "Static Discharge Leap");
    public static final Talent STATIC_DISCHARGE_SPEED = talent("static_discharge_speed", "Static Discharge Speed");
    public static final Talent STATIC_DISCHARGE_LIGHTNING_ORB = talent("static_discharge_lightning_orb", "Static Discharge Lightning Orb");
    public static final Talent STATIC_DISCHARGE_LIGHTNING_ORB_ON_HIT = talent("static_discharge_lightning_orb_on_hit", "Static Discharge Lightning Orb On Hit");
    public static final Talent ARCANE_BOLT_LESSER = talent("arcane_bolt_lesser", "Arcane Bolt Lesser");
    public static final Talent ARCANE_BOLT_VOLLEY = talent("arcane_bolt_volley", "Arcane Bolt Volley");
    public static final Talent ARCANE_BOLT_GREATER = talent("arcane_bolt_greater", "Arcane Bolt Greater");

    private static final List<Ability> ABILITIES = List.of(METEOR_SHOWER, ICE_COMET, STATIC_DISCHARGE, ARCANE_BOLT);
    private static final List<Talent> TALENTS = List.of(SPELL_ECHO, METEOR_SHOWER_GREATER, METEOR_SHOWER_WRATH, METEOR_SHOWER_RENEWING_WRATH, ICE_COMET_LEAP, ICE_COMET_VOLLEY, ICE_COMET_DAMAGE, STATIC_DISCHARGE_LIGHTNING_BALL, STATIC_DISCHARGE_LEAP, STATIC_DISCHARGE_SPEED, STATIC_DISCHARGE_LIGHTNING_ORB, STATIC_DISCHARGE_LIGHTNING_ORB_ON_HIT, ARCANE_BOLT_LESSER, ARCANE_BOLT_VOLLEY, ARCANE_BOLT_GREATER);

    public static void register() {
        ABILITIES.forEach(AbilityRegistry::register);
        TALENTS.forEach(TalentRegistry::register);
    }
    public static List<Ability> abilities() { return ABILITIES; }
    public static List<Talent> talents() { return TALENTS; }
    private static Ability ability(String path, String name, int cooldownTicks) { return new CasterAbility(path, name, "Caster signature ability.", cooldownTicks); }
    private static Talent talent(String path, String name) { return new CasterTalent(path, name, "Caster talent node."); }
}
