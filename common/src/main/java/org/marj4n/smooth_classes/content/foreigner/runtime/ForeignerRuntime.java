package org.marj4n.smooth_classes.content.foreigner.runtime;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.Identifier;
import org.marj4n.smooth_classes.content.foreigner.ForeignerClass;
import org.marj4n.smooth_classes.content.foreigner.ForeignerContent;
import org.marj4n.smooth_classes.runtime.AbilityRuntime;
import org.marj4n.smooth_classes.runtime.CombatRuntime;
import org.marj4n.smooth_classes.runtime.ExecutionResult;
import org.marj4n.smooth_classes.runtime.ClassEffectRuntime;
import org.marj4n.smooth_classes.effects.SmoothEffects;

/** Talent-aware runtime plan builder for Foreigner. Actual Minecraft effects are executed by hooks/integrations. */
public final class ForeignerRuntime {
    private ForeignerRuntime() {}
    private static void require(ServerPlayerEntity player) {
        if (!AbilityRuntime.isClass(player, ForeignerClass.ID)) throw new IllegalStateException("Player is not Foreigner");
    }
    private static boolean has(ServerPlayerEntity player, Identifier talent) { return AbilityRuntime.hasTalent(player, talent); }

    public record SurgePlan(boolean radius, boolean frequency, boolean power, boolean renewal) {}
    public static SurgePlan elementalSurge(ServerPlayerEntity player) {
        require(player);
        return new SurgePlan(has(player, ForeignerContent.ELEMENTAL_SURGE_RADIUS.id()),
                has(player, ForeignerContent.ELEMENTAL_SURGE_FREQUENCY.id()),
                has(player, ForeignerContent.ELEMENTAL_SURGE_POWER.id()),
                has(player, ForeignerContent.ELEMENTAL_SURGE_RENEWAL.id()));
    }
    public record ImpactPlan(boolean magnet, boolean resistance) {}
    public static ImpactPlan elementalImpact(ServerPlayerEntity player) { require(player); return new ImpactPlan(has(player, ForeignerContent.ELEMENTAL_IMPACT_MAGNET.id()), has(player, ForeignerContent.ELEMENTAL_IMPACT_RESISTANCE.id())); }
    public record SpellweaverPlan(boolean haste, boolean regeneration) {}
    public static SpellweaverPlan spellweaver(ServerPlayerEntity player) { require(player); return new SpellweaverPlan(has(player, ForeignerContent.SPELLWEAVER_HASTE.id()), has(player, ForeignerContent.SPELLWEAVER_REGENERATION.id())); }


    public static ExecutionResult executeElementalSurge(ServerPlayerEntity player) {
        require(player);
        ClassEffectRuntime.apply(player, SmoothEffects.ELEMENTAL_SURGE, 300, 0);
        return ExecutionResult.success(1, "elemental_surge");
    }

    public static ExecutionResult executeElementalImpact(ServerPlayerEntity player) {
        ImpactPlan plan = elementalImpact(player);
        ClassEffectRuntime.apply(player, SmoothEffects.ELEMENTAL_IMPACT, 20, 0);
        if (plan.resistance()) CombatRuntime.buff(player, StatusEffects.RESISTANCE,35,2);
        return ExecutionResult.success(1, "elemental_impact");
    }

    public static ExecutionResult executeSpellweaver(ServerPlayerEntity player) {
        require(player);
        ClassEffectRuntime.apply(player, SmoothEffects.SPELLWEAVER, 600, 19);
        return ExecutionResult.success(1, "spellweaver");
    }

}
