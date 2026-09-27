package org.marj4n.smooth_classes.content.assassin;

import java.util.List;
import org.marj4n.smooth_classes.api.ability.Ability;
import org.marj4n.smooth_classes.api.talent.Talent;
import org.marj4n.smooth_classes.content.assassin.ability.AssassinAbility;
import org.marj4n.smooth_classes.content.assassin.talent.AssassinTalent;
import org.marj4n.smooth_classes.registry.AbilityRegistry;
import org.marj4n.smooth_classes.registry.TalentRegistry;

/** Class abilities and talents; runtime behavior is implemented separately. */
public final class AssassinContent {
    private AssassinContent() {}

    public static final Ability EVASION = ability("evasion", "Evasion", 25 * 20);
    public static final Ability PREPARATION = ability("preparation", "Preparation", 15 * 20);
    public static final Ability SIPHONING_STRIKES = ability("siphoning_strikes", "Siphoning Strikes", 25 * 20);

    public static final Talent BACKSTAB = talent("backstab", "Backstab");
    public static final Talent SMOKE_BOMB = talent("smoke_bomb", "Smoke Bomb");
    public static final Talent OPPORTUNISTIC_MASTERY = talent("opportunistic_mastery", "Opportunistic Mastery");
    public static final Talent OPPORTUNISTIC_MASTERY_PROFICIENT = talent("opportunistic_mastery_proficient", "Opportunistic Mastery Proficient");
    public static final Talent OPPORTUNISTIC_MASTERY_SKILLED = talent("opportunistic_mastery_skilled", "Opportunistic Mastery Skilled");
    public static final Talent EVASION_MASTERY = talent("evasion_mastery", "Evasion Mastery");
    public static final Talent EVASION_MASTERY_PROFICIENT = talent("evasion_mastery_proficient", "Evasion Mastery Proficient");
    public static final Talent EVASION_MASTERY_SKILLED = talent("evasion_mastery_skilled", "Evasion Mastery Skilled");
    public static final Talent EXPLOITATION = talent("exploitation", "Exploitation");
    public static final Talent RECOVERY = talent("recovery", "Recovery");
    public static final Talent SHADOW_VEIL = talent("shadow_veil", "Shadow Veil");
    public static final Talent DEFLECTION = talent("deflection", "Deflection");
    public static final Talent FLEETFOOTED = talent("fleetfooted", "Fleetfooted");
    public static final Talent EVASION_FAN_OF_BLADES = talent("evasion_fan_of_blades", "Evasion Fan Of Blades");
    public static final Talent EVASION_FAN_OF_BLADES_DISENCHANTMENT = talent("evasion_fan_of_blades_disenchantment", "Evasion Fan Of Blades Disenchantment");
    public static final Talent EVASION_FAN_OF_BLADES_ASSAULT = talent("evasion_fan_of_blades_assault", "Evasion Fan Of Blades Assault");
    public static final Talent EVASION_FAN_OF_BLADES_RENEWAL = talent("evasion_fan_of_blades_renewal", "Evasion Fan Of Blades Renewal");
    public static final Talent EVASION_BLADESTORM = talent("evasion_bladestorm", "Evasion Bladestorm");
    public static final Talent EVASION_BLADESTORM_SIPHON = talent("evasion_bladestorm_siphon", "Evasion Bladestorm Siphon");
    public static final Talent PREPARATION_SHADOWSTRIKE = talent("preparation_shadowstrike", "Preparation Shadowstrike");
    public static final Talent PREPARATION_SHADOWSTRIKE_VAMPIRE = talent("preparation_shadowstrike_vampire", "Preparation Shadowstrike Vampire");
    public static final Talent PREPARATION_SHADOWSTRIKE_SHIELD = talent("preparation_shadowstrike_shield", "Preparation Shadowstrike Shield");
    public static final Talent SIPHONING_STRIKES_MIGHTY = talent("siphoning_strikes_mighty", "Siphoning Strikes Mighty");
    public static final Talent SIPHONING_STRIKES_AURA = talent("siphoning_strikes_aura", "Siphoning Strikes Aura");
    public static final Talent SIPHONING_STRIKES_VANISH = talent("siphoning_strikes_vanish", "Siphoning Strikes Vanish");

    private static final List<Ability> ABILITIES = List.of(EVASION, PREPARATION, SIPHONING_STRIKES);
    private static final List<Talent> TALENTS = List.of(BACKSTAB, SMOKE_BOMB, OPPORTUNISTIC_MASTERY, OPPORTUNISTIC_MASTERY_PROFICIENT, OPPORTUNISTIC_MASTERY_SKILLED, EVASION_MASTERY, EVASION_MASTERY_PROFICIENT, EVASION_MASTERY_SKILLED, EXPLOITATION, RECOVERY, SHADOW_VEIL, DEFLECTION, FLEETFOOTED, EVASION_FAN_OF_BLADES, EVASION_FAN_OF_BLADES_DISENCHANTMENT, EVASION_FAN_OF_BLADES_ASSAULT, EVASION_FAN_OF_BLADES_RENEWAL, EVASION_BLADESTORM, EVASION_BLADESTORM_SIPHON, PREPARATION_SHADOWSTRIKE, PREPARATION_SHADOWSTRIKE_VAMPIRE, PREPARATION_SHADOWSTRIKE_SHIELD, SIPHONING_STRIKES_MIGHTY, SIPHONING_STRIKES_AURA, SIPHONING_STRIKES_VANISH);

    public static void register() {
        ABILITIES.forEach(AbilityRegistry::register);
        TALENTS.forEach(TalentRegistry::register);
    }
    public static List<Ability> abilities() { return ABILITIES; }
    public static List<Talent> talents() { return TALENTS; }
    private static Ability ability(String path, String name, int cooldownTicks) { return new AssassinAbility(path, name, "Assassin signature ability.", cooldownTicks); }
    private static Talent talent(String path, String name) { return new AssassinTalent(path, name, "Assassin talent node."); }
}
