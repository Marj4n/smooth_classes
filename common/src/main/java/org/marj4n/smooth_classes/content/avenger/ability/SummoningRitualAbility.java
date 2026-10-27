package org.marj4n.smooth_classes.content.avenger.ability;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.marj4n.smooth_classes.SmoothClasses;
import org.marj4n.smooth_classes.api.ability.Ability;
import org.marj4n.smooth_classes.content.avenger.runtime.AvengerSummonPlan;
import org.marj4n.smooth_classes.content.avenger.runtime.AvengerSummoningRuntime;

public final class SummoningRitualAbility implements Ability {
    public static final Identifier ID = SmoothClasses.id("avenger/summoning_ritual");

    @Override public Identifier id() { return ID; }
    @Override public String name() { return "Summoning Ritual"; }
    @Override public int cooldownTicks() { return 120 * 20; }

    @Override public String description() {
        return "Summons Avenger minions. Talents modify minion type, count and inherited combat attributes.";
    }

    /** Server-side resolution. Puffish Skills is the authoritative talent state. */
    public AvengerSummonPlan resolveSummons(ServerPlayerEntity player) {
        return AvengerSummoningRuntime.resolve(player);
    }
}
