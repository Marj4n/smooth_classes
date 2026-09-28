package org.marj4n.smooth_classes.content.lancer;

import java.util.List;
import org.marj4n.smooth_classes.api.ability.Ability;
import org.marj4n.smooth_classes.api.talent.Talent;
import org.marj4n.smooth_classes.content.lancer.ability.LancerAbility;
import org.marj4n.smooth_classes.content.lancer.talent.LancerTalent;
import org.marj4n.smooth_classes.registry.AbilityRegistry;
import org.marj4n.smooth_classes.registry.TalentRegistry;

public final class LancerContent {
    private LancerContent() {}

    public static final Ability IMPALING_VOLLEY = ability("impaling_volley", "Impaling Volley", 28 * 20);
    public static final Ability DRAGON_THRUST = ability("dragon_thrust", "Dragon Thrust", 14 * 20);
    public static final Ability SPEARSTORM = ability("spearstorm", "Spearstorm", 20 * 20);

    public static final Talent VOLLEY_FIVE = talent("volley_five", "Fivefold Formation");
    public static final Talent VOLLEY_SEVEN = talent("volley_seven", "Sevenfold Formation");
    public static final Talent VOLLEY_NINE = talent("volley_nine", "Ninefold Formation");

    public static final Talent THRUST_REACH = talent("thrust_reach", "Long Thrust");
    public static final Talent THRUST_PIN = talent("thrust_pin", "Pinning Point");
    public static final Talent THRUST_BREAKER = talent("thrust_breaker", "Titan Breaker");

    public static final Talent STORM_RADIUS = talent("storm_radius", "Wide Spearstorm");
    public static final Talent STORM_PULSES = talent("storm_pulses", "Relentless Spearstorm");
    public static final Talent STORM_VORTEX = talent("storm_vortex", "Vortex Formation");

    public static final Talent MOMENTUM_TRAINING_I = talent("momentum_training_i", "Momentum Training I");
    public static final Talent MOMENTUM_TRAINING_II = talent("momentum_training_ii", "Momentum Training II");
    public static final Talent EXECUTION_RHYTHM = talent("execution_rhythm", "Execution Rhythm");
    public static final Talent BATTLE_RECOVERY_I = talent("battle_recovery_i", "Battle Recovery I");
    public static final Talent BATTLE_RECOVERY_II = talent("battle_recovery_ii", "Battle Recovery II");
    public static final Talent FLEET_I = talent("fleet_i", "Fleet Lancer I");
    public static final Talent FLEET_II = talent("fleet_ii", "Fleet Lancer II");
    public static final Talent IRON_GRIP = talent("iron_grip", "Iron Grip");
    public static final Talent PERFECT_MOMENTUM = talent("perfect_momentum", "Perfect Momentum");

    private static final List<Ability> ABILITIES = List.of(IMPALING_VOLLEY, DRAGON_THRUST, SPEARSTORM);
    private static final List<Talent> TALENTS = List.of(
            VOLLEY_FIVE, VOLLEY_SEVEN, VOLLEY_NINE,
            THRUST_REACH, THRUST_PIN, THRUST_BREAKER,
            STORM_RADIUS, STORM_PULSES, STORM_VORTEX,
            MOMENTUM_TRAINING_I, MOMENTUM_TRAINING_II, EXECUTION_RHYTHM,
            BATTLE_RECOVERY_I, BATTLE_RECOVERY_II,
            FLEET_I, FLEET_II, IRON_GRIP, PERFECT_MOMENTUM
    );

    public static void register() {
        ABILITIES.forEach(AbilityRegistry::register);
        TALENTS.forEach(TalentRegistry::register);
    }
    public static List<Ability> abilities() { return ABILITIES; }
    public static List<Talent> talents() { return TALENTS; }
    private static Ability ability(String path, String name, int cooldownTicks) {
        return new LancerAbility(path, name, "Lancer signature ability.", cooldownTicks);
    }
    private static Talent talent(String path, String name) {
        return new LancerTalent(path, name, "Lancer talent node.");
    }
}
