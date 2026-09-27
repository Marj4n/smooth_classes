package org.marj4n.smooth_classes.content.saber;

import java.util.List;
import org.marj4n.smooth_classes.api.ability.Ability;
import org.marj4n.smooth_classes.api.talent.Talent;
import org.marj4n.smooth_classes.content.saber.ability.SaberAbility;
import org.marj4n.smooth_classes.content.saber.talent.SaberTalent;
import org.marj4n.smooth_classes.registry.AbilityRegistry;
import org.marj4n.smooth_classes.registry.TalentRegistry;

/** Class abilities and talents; runtime behavior is implemented separately. */
public final class SaberContent {
    private SaberContent() {}

    public static final Ability CONSECRATION = ability("consecration", "Consecration", 30 * 20);
    public static final Ability SACRED_ONSLAUGHT = ability("sacred_onslaught", "Sacred Onslaught", 15 * 20);
    public static final Ability HEAVENSMITHS_CALL = ability("heavensmiths_call", "Heavensmiths Call", 55 * 20);

    public static final Talent AEGIS = talent("aegis", "Aegis");
    public static final Talent RETRIBUTION = talent("retribution", "Retribution");
    public static final Talent EXHAUSTIVE_RECOVERY = talent("exhaustive_recovery", "Exhaustive Recovery");
    public static final Talent CONSECRATION_DURATION = talent("consecration_duration", "Consecration Duration");
    public static final Talent CONSECRATION_WARD = talent("consecration_ward", "Consecration Ward");
    public static final Talent CONSECRATION_TAUNT = talent("consecration_taunt", "Consecration Taunt");
    public static final Talent CONSECRATION_MIGHTY = talent("consecration_mighty", "Consecration Mighty");
    public static final Talent CONSECRATION_SPELLFORGED = talent("consecration_spellforged", "Consecration Spellforged");
    public static final Talent SACRED_ONSLAUGHT_HEAL = talent("sacred_onslaught_heal", "Sacred Onslaught Heal");
    public static final Talent SACRED_ONSLAUGHT_DEFEND = talent("sacred_onslaught_defend", "Sacred Onslaught Defend");
    public static final Talent SACRED_ONSLAUGHT_MIGHTY = talent("sacred_onslaught_mighty", "Sacred Onslaught Mighty");
    public static final Talent SACRED_ONSLAUGHT_STUN = talent("sacred_onslaught_stun", "Sacred Onslaught Stun");
    public static final Talent HEAVENSMITHS_CALL_TAUNT = talent("heavensmiths_call_taunt", "Heavensmiths Call Taunt");
    public static final Talent HEAVENSMITHS_CALL_MARK = talent("heavensmiths_call_mark", "Heavensmiths Call Mark");
    public static final Talent HEAVENSMITHS_CALL_EFFECT = talent("heavensmiths_call_effect", "Heavensmiths Call Effect");
    public static final Talent HEAVENSMITHS_CALL_EXHAUST = talent("heavensmiths_call_exhaust", "Heavensmiths Call Exhaust");
    public static final Talent HEAVENSMITHS_CALL_MIGHTY = talent("heavensmiths_call_mighty", "Heavensmiths Call Mighty");

    private static final List<Ability> ABILITIES = List.of(CONSECRATION, SACRED_ONSLAUGHT, HEAVENSMITHS_CALL);
    private static final List<Talent> TALENTS = List.of(AEGIS, RETRIBUTION, EXHAUSTIVE_RECOVERY, CONSECRATION_DURATION, CONSECRATION_WARD, CONSECRATION_TAUNT, CONSECRATION_MIGHTY, CONSECRATION_SPELLFORGED, SACRED_ONSLAUGHT_HEAL, SACRED_ONSLAUGHT_DEFEND, SACRED_ONSLAUGHT_MIGHTY, SACRED_ONSLAUGHT_STUN, HEAVENSMITHS_CALL_TAUNT, HEAVENSMITHS_CALL_MARK, HEAVENSMITHS_CALL_EFFECT, HEAVENSMITHS_CALL_EXHAUST, HEAVENSMITHS_CALL_MIGHTY);

    public static void register() {
        ABILITIES.forEach(AbilityRegistry::register);
        TALENTS.forEach(TalentRegistry::register);
    }
    public static List<Ability> abilities() { return ABILITIES; }
    public static List<Talent> talents() { return TALENTS; }
    private static Ability ability(String path, String name, int cooldownTicks) { return new SaberAbility(path, name, "Saber signature ability.", cooldownTicks); }
    private static Talent talent(String path, String name) { return new SaberTalent(path, name, "Saber talent node."); }
}
