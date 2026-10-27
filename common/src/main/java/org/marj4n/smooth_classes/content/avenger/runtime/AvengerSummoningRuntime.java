package org.marj4n.smooth_classes.content.avenger.runtime;

import net.minecraft.server.network.ServerPlayerEntity;
import org.marj4n.smooth_classes.content.avenger.AvengerContent;
import org.marj4n.smooth_classes.runtime.AbilityRuntime;

import java.util.ArrayList;
import java.util.List;

/** Resolves Summoning Ritual directly from authoritative Puffish Skills unlock state. */
public final class AvengerSummoningRuntime {
    private AvengerSummoningRuntime() {}

    public static int getMinionLimit(ServerPlayerEntity player) {
        int legionRanks = Math.min(4, Math.max(0,
                AbilityRuntime.talentRank(player, AvengerContent.UNDEAD_LEGION.id())));
        return 1 + legionRanks;
    }

    public static AvengerSummonPlan resolve(ServerPlayerEntity player) {
        int count = getMinionLimit(player);
        List<AvengerMinionType> result = new ArrayList<>(count);

        if (AbilityRuntime.hasTalent(player, AvengerContent.GREATER_DREADGLARE.id())) {
            result.add(AvengerMinionType.GREATER_DREADGLARE);
            return plan(player, result);
        }

        boolean canSummonWraith = AbilityRuntime.hasTalent(player, AvengerContent.SUMMON_WRAITH.id());
        boolean wraithLegion = AbilityRuntime.hasTalent(player, AvengerContent.WRAITH_LEGION.id());

        for (int i = 0; i < count; i++) {
            if (!canSummonWraith) {
                result.add(AvengerMinionType.DREADGLARE);
                continue;
            }
            int chance = wraithLegion ? 5 : player.getRandom().nextInt(100);
            result.add(chance < 50 ? AvengerMinionType.WRAITH : AvengerMinionType.DREADGLARE);
        }

        return plan(player, result);
    }

    private static AvengerSummonPlan plan(ServerPlayerEntity player, List<AvengerMinionType> minions) {
        return new AvengerSummonPlan(
                minions,
                AbilityRuntime.hasTalent(player, AvengerContent.NECROTIC_FORTIFICATION.id()),
                AbilityRuntime.hasTalent(player, AvengerContent.SHADOW_AURA.id())
        );
    }
}
