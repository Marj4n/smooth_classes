package org.marj4n.smooth_classes.content.avenger.runtime;

import java.util.List;

/** Immutable server-side result of resolving Summoning Ritual talents. */
public record AvengerSummonPlan(
        List<AvengerMinionType> minions,
        boolean necroticFortification,
        boolean shadowAura
) {
    public AvengerSummonPlan {
        minions = List.copyOf(minions);
    }
}
