package org.marj4n.smooth_classes.content.archer;

import java.util.List;
import org.marj4n.smooth_classes.api.ability.Ability;
import org.marj4n.smooth_classes.api.talent.Talent;
import org.marj4n.smooth_classes.content.archer.ability.ArcherAbility;
import org.marj4n.smooth_classes.content.archer.talent.ArcherTalent;
import org.marj4n.smooth_classes.registry.AbilityRegistry;
import org.marj4n.smooth_classes.registry.TalentRegistry;

/** Baseline content ported from SimplySkills Ranger; runtime behavior is implemented separately. */
public final class ArcherContent {
    private ArcherContent() {}

    public static final Ability ARROW_RAIN = ability("arrow_rain", "Arrow Rain", 14 * 20);
    public static final Ability DISENGAGE = ability("disengage", "Disengage", 15 * 20);
    public static final Ability ELEMENTAL_ARROWS = ability("elemental_arrows", "Elemental Arrows", 40 * 20);

    public static final Talent REVEAL = talent("reveal", "Reveal");
    public static final Talent TAMER = talent("tamer", "Tamer");
    public static final Talent BONDED = talent("bonded", "Bonded");
    public static final Talent TRAINED = talent("trained", "Trained");
    public static final Talent INCOGNITO = talent("incognito", "Incognito");
    public static final Talent ARROW_RAIN_ELEMENTAL = talent("arrow_rain_elemental", "Arrow Rain Elemental");
    public static final Talent ARROW_RAIN_ELEMENTAL_ARTILLERY = talent("arrow_rain_elemental_artillery", "Arrow Rain Elemental Artillery");
    public static final Talent ARROW_RAIN_EXPLOSIVE = talent("arrow_rain_explosive", "Arrow Rain Explosive");
    public static final Talent ARROW_RAIN_VOLLEY = talent("arrow_rain_volley", "Arrow Rain Volley");
    public static final Talent ARROW_RAIN_RADIUS = talent("arrow_rain_radius", "Arrow Rain Radius");
    public static final Talent DISENGAGE_RECUPERATE = talent("disengage_recuperate", "Disengage Recuperate");
    public static final Talent DISENGAGE_EXPLOITATION = talent("disengage_exploitation", "Disengage Exploitation");
    public static final Talent DISENGAGE_MARKSMAN = talent("disengage_marksman", "Disengage Marksman");
    public static final Talent ELEMENTAL_ARROWS_FROST_ATTUNED = talent("elemental_arrows_frost_attuned", "Elemental Arrows Frost Attuned");
    public static final Talent ELEMENTAL_ARROWS_FIRE_ATTUNED = talent("elemental_arrows_fire_attuned", "Elemental Arrows Fire Attuned");
    public static final Talent ELEMENTAL_ARROWS_LIGHTNING_ATTUNED = talent("elemental_arrows_lightning_attuned", "Elemental Arrows Lightning Attuned");
    public static final Talent ELEMENTAL_ARROWS_RADIUS = talent("elemental_arrows_radius", "Elemental Arrows Radius");
    public static final Talent ELEMENTAL_ARROWS_STACKS = talent("elemental_arrows_stacks", "Elemental Arrows Stacks");
    public static final Talent ELEMENTAL_ARROWS_RENEWAL = talent("elemental_arrows_renewal", "Elemental Arrows Renewal");

    private static final List<Ability> ABILITIES = List.of(ARROW_RAIN, DISENGAGE, ELEMENTAL_ARROWS);
    private static final List<Talent> TALENTS = List.of(REVEAL, TAMER, BONDED, TRAINED, INCOGNITO, ARROW_RAIN_ELEMENTAL, ARROW_RAIN_ELEMENTAL_ARTILLERY, ARROW_RAIN_EXPLOSIVE, ARROW_RAIN_VOLLEY, ARROW_RAIN_RADIUS, DISENGAGE_RECUPERATE, DISENGAGE_EXPLOITATION, DISENGAGE_MARKSMAN, ELEMENTAL_ARROWS_FROST_ATTUNED, ELEMENTAL_ARROWS_FIRE_ATTUNED, ELEMENTAL_ARROWS_LIGHTNING_ATTUNED, ELEMENTAL_ARROWS_RADIUS, ELEMENTAL_ARROWS_STACKS, ELEMENTAL_ARROWS_RENEWAL);

    public static void register() {
        ABILITIES.forEach(AbilityRegistry::register);
        TALENTS.forEach(TalentRegistry::register);
    }
    public static List<Ability> abilities() { return ABILITIES; }
    public static List<Talent> talents() { return TALENTS; }
    private static Ability ability(String path, String name, int cooldownTicks) { return new ArcherAbility(path, name, "Ported Ranger signature ability.", cooldownTicks); }
    private static Talent talent(String path, String name) { return new ArcherTalent(path, name, "Ported Ranger talent node."); }
}
