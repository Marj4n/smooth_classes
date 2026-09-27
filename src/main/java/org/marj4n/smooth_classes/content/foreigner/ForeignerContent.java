package org.marj4n.smooth_classes.content.foreigner;

import java.util.List;
import org.marj4n.smooth_classes.api.ability.Ability;
import org.marj4n.smooth_classes.api.talent.Talent;
import org.marj4n.smooth_classes.content.foreigner.ability.ForeignerAbility;
import org.marj4n.smooth_classes.content.foreigner.talent.ForeignerTalent;
import org.marj4n.smooth_classes.registry.AbilityRegistry;
import org.marj4n.smooth_classes.registry.TalentRegistry;

/** Class abilities and talents; runtime behavior is implemented separately. */
public final class ForeignerContent {
    private ForeignerContent() {}

    public static final Ability ELEMENTAL_SURGE = ability("elemental_surge", "Elemental Surge", 30 * 20);
    public static final Ability ELEMENTAL_IMPACT = ability("elemental_impact", "Elemental Impact", 25 * 20);
    public static final Ability SPELLWEAVER = ability("spellweaver", "Spellweaver", 40 * 20);

    public static final Talent WEAPON_EXPERT = talent("weapon_expert", "Weapon Expert");
    public static final Talent SPELLWEAVING = talent("spellweaving", "Spellweaving");
    public static final Talent ELEMENTAL_SURGE_NO_FROST = talent("elemental_surge_no_frost", "Elemental Surge No Frost");
    public static final Talent ELEMENTAL_SURGE_NO_FIRE = talent("elemental_surge_no_fire", "Elemental Surge No Fire");
    public static final Talent ELEMENTAL_SURGE_RENEWAL = talent("elemental_surge_renewal", "Elemental Surge Renewal");
    public static final Talent ELEMENTAL_SURGE_NO_LIGHTNING = talent("elemental_surge_no_lightning", "Elemental Surge No Lightning");
    public static final Talent ELEMENTAL_IMPACT_MAGNET = talent("elemental_impact_magnet", "Elemental Impact Magnet");
    public static final Talent ELEMENTAL_IMPACT_RESISTANCE = talent("elemental_impact_resistance", "Elemental Impact Resistance");
    public static final Talent SPELLWEAVER_HASTE = talent("spellweaver_haste", "Spellweaver Haste");
    public static final Talent SPELLWEAVER_REGENERATION = talent("spellweaver_regeneration", "Spellweaver Regeneration");

    private static final List<Ability> ABILITIES = List.of(ELEMENTAL_SURGE, ELEMENTAL_IMPACT, SPELLWEAVER);
    private static final List<Talent> TALENTS = List.of(WEAPON_EXPERT, SPELLWEAVING, ELEMENTAL_SURGE_NO_FROST, ELEMENTAL_SURGE_NO_FIRE, ELEMENTAL_SURGE_RENEWAL, ELEMENTAL_SURGE_NO_LIGHTNING, ELEMENTAL_IMPACT_MAGNET, ELEMENTAL_IMPACT_RESISTANCE, SPELLWEAVER_HASTE, SPELLWEAVER_REGENERATION);

    public static void register() {
        ABILITIES.forEach(AbilityRegistry::register);
        TALENTS.forEach(TalentRegistry::register);
    }
    public static List<Ability> abilities() { return ABILITIES; }
    public static List<Talent> talents() { return TALENTS; }
    private static Ability ability(String path, String name, int cooldownTicks) { return new ForeignerAbility(path, name, "Foreigner signature ability.", cooldownTicks); }
    private static Talent talent(String path, String name) { return new ForeignerTalent(path, name, "Foreigner talent node."); }
}
