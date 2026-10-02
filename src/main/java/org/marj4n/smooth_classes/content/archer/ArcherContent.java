package org.marj4n.smooth_classes.content.archer;

import java.util.List;
import org.marj4n.smooth_classes.api.ability.Ability;
import org.marj4n.smooth_classes.api.talent.Talent;
import org.marj4n.smooth_classes.content.archer.ability.ArcherAbility;
import org.marj4n.smooth_classes.content.archer.talent.ArcherTalent;
import org.marj4n.smooth_classes.registry.AbilityRegistry;
import org.marj4n.smooth_classes.registry.TalentRegistry;

/** Class abilities and talents; runtime behavior is implemented separately. */
public final class ArcherContent {
    private ArcherContent() {}

    public static final Ability ARROW_RAIN = ability("arrow_rain", "Arrow Rain", 14 * 20);
    public static final Ability PORTAL_OF_SOVEREIGNTY = ability("unlimited_blade_works", "Portal of Sovereignty", 30 * 20);
    public static final Ability ELEMENTAL_ARROWS = ability("elemental_arrows", "Elemental Arrows", 40 * 20);

    public static final Talent TREASURY_EXPANSION_I = talent("treasury_expansion_i", "Treasury Expansion I");
    public static final Talent TREASURY_EXPANSION_II = talent("treasury_expansion_ii", "Treasury Expansion II");
    public static final Talent TREASURY_EXPANSION_III = talent("treasury_expansion_iii", "Treasury Expansion III");
    public static final Talent TREASURY_EXPANSION_IV = talent("treasury_expansion_iv", "Treasury Expansion IV");
    public static final Talent ROYAL_DRAW = talent("royal_draw", "Royal Draw");
    public static final Talent ARROW_RAIN_ELEMENTAL = talent("arrow_rain_elemental", "Arrow Rain Elemental");
    public static final Talent ARROW_RAIN_ELEMENTAL_ARTILLERY = talent("arrow_rain_elemental_artillery", "Arrow Rain Elemental Artillery");
    public static final Talent ARROW_RAIN_EXPLOSIVE = talent("arrow_rain_explosive", "Arrow Rain Explosive");
    public static final Talent ARROW_RAIN_VOLLEY = talent("arrow_rain_volley", "Arrow Rain Volley");
    public static final Talent ARROW_RAIN_RADIUS = talent("arrow_rain_radius", "Arrow Rain Radius");
    public static final Talent PORTAL_ENDLESS_ARSENAL = talent("portal_endless_arsenal", "Endless Arsenal");
    public static final Talent PORTAL_PIERCING_BLADES = talent("portal_piercing_blades", "Piercing Blades");
    public static final Talent PORTAL_PERFECT_PROJECTION = talent("portal_perfect_projection", "Perfect Projection");
    public static final Talent ELEMENTAL_ARROWS_CONVERGENCE = talent("elemental_arrows_convergence", "Convergence");
    public static final Talent ELEMENTAL_ARROWS_OVERCHARGE = talent("elemental_arrows_overcharge", "Overcharged Fletching");
    public static final Talent ELEMENTAL_ARROWS_SPLIT_VOLLEY = talent("elemental_arrows_split_volley", "Split Trajectory");
    public static final Talent ELEMENTAL_ARROWS_RADIUS = talent("elemental_arrows_radius", "Elemental Arrows Radius");
    public static final Talent ELEMENTAL_ARROWS_STACKS = talent("elemental_arrows_stacks", "Elemental Arrows Stacks");
    public static final Talent ELEMENTAL_ARROWS_RENEWAL = talent("elemental_arrows_renewal", "Elemental Arrows Renewal");

    private static final List<Ability> ABILITIES = List.of(ARROW_RAIN, PORTAL_OF_SOVEREIGNTY, ELEMENTAL_ARROWS);
    private static final List<Talent> TALENTS = List.of(TREASURY_EXPANSION_I, TREASURY_EXPANSION_II, TREASURY_EXPANSION_III, TREASURY_EXPANSION_IV, ROYAL_DRAW, ARROW_RAIN_ELEMENTAL, ARROW_RAIN_ELEMENTAL_ARTILLERY, ARROW_RAIN_EXPLOSIVE, ARROW_RAIN_VOLLEY, ARROW_RAIN_RADIUS, PORTAL_ENDLESS_ARSENAL, PORTAL_PIERCING_BLADES, PORTAL_PERFECT_PROJECTION, ELEMENTAL_ARROWS_CONVERGENCE, ELEMENTAL_ARROWS_OVERCHARGE, ELEMENTAL_ARROWS_SPLIT_VOLLEY, ELEMENTAL_ARROWS_RADIUS, ELEMENTAL_ARROWS_STACKS, ELEMENTAL_ARROWS_RENEWAL);

    public static void register() {
        ABILITIES.forEach(AbilityRegistry::register);
        TALENTS.forEach(TalentRegistry::register);
    }
    public static List<Ability> abilities() { return ABILITIES; }
    public static List<Talent> talents() { return TALENTS; }
    private static Ability ability(String path, String name, int cooldownTicks) { return new ArcherAbility(path, name, "Archer signature ability.", cooldownTicks); }
    private static Talent talent(String path, String name) { return new ArcherTalent(path, name, "Archer talent node."); }
}
