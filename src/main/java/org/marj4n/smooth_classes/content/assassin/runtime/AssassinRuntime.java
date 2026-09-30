package org.marj4n.smooth_classes.content.assassin.runtime;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.marj4n.smooth_classes.runtime.SkillFx;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.Identifier;
import org.marj4n.smooth_classes.content.assassin.AssassinClass;
import org.marj4n.smooth_classes.content.assassin.AssassinContent;
import org.marj4n.smooth_classes.runtime.AbilityRuntime;
import org.marj4n.smooth_classes.runtime.CombatRuntime;
import org.marj4n.smooth_classes.runtime.ExecutionResult;
import org.marj4n.smooth_classes.runtime.ClassEffectRuntime;
import org.marj4n.smooth_classes.effects.SmoothEffects;

/** Talent-aware runtime plan builder for Assassin. Actual Minecraft effects are executed by hooks/integrations. */
public final class AssassinRuntime {
    private AssassinRuntime() {}
    private static void require(ServerPlayerEntity player) {
        if (!AbilityRuntime.isClass(player, AssassinClass.ID)) throw new IllegalStateException("Player is not Assassin");
    }
    private static boolean has(ServerPlayerEntity player, Identifier talent) { return AbilityRuntime.hasTalent(player, talent); }

    public record EvasionPlan(boolean mastery, boolean stealth) {}
    public static EvasionPlan evasion(ServerPlayerEntity player) { require(player); return new EvasionPlan(has(player, AssassinContent.EVASION_MASTERY.id()), has(player, AssassinContent.EVASION_BLADESTORM.id())); }
    public record PreparationPlan(boolean shadowstrike, boolean reset) {}
    public static PreparationPlan preparation(ServerPlayerEntity player) { require(player); return new PreparationPlan(has(player, AssassinContent.PREPARATION_SHADOWSTRIKE.id()), has(player, AssassinContent.PREPARATION_SHADOWSTRIKE_VAMPIRE.id())); }
    public record SiphoningPlan(boolean heal, boolean haste) {}
    public static SiphoningPlan siphoningStrikes(ServerPlayerEntity player) { require(player); return new SiphoningPlan(has(player, AssassinContent.SIPHONING_STRIKES_MIGHTY.id()), has(player, AssassinContent.SIPHONING_STRIKES_VANISH.id())); }


    public static ExecutionResult executeEvasion(ServerPlayerEntity player) {
        EvasionPlan plan = evasion(player);
        ClassEffectRuntime.apply(player, SmoothEffects.EVASION, 160, 0);
        if (has(player, AssassinContent.EVASION_FAN_OF_BLADES.id())) {
            int stacks = has(player, AssassinContent.EVASION_FAN_OF_BLADES_ASSAULT.id()) ? 18 : 9;
            ClassEffectRuntime.apply(player, SmoothEffects.FANOFBLADES, 500, stacks);
        }
        return ExecutionResult.success(1, "evasion");
    }

    public static ExecutionResult executePreparation(ServerPlayerEntity player) {
        require(player);
        return ShadowTechniqueRuntime.cast(player);
    }

    public static ExecutionResult executeSiphoningStrikes(ServerPlayerEntity player) {
        SiphoningPlan plan = siphoningStrikes(player);
        ClassEffectRuntime.apply(player, SmoothEffects.SIPHONING_STRIKES, 600, 10);
        if (plan.heal()) ClassEffectRuntime.apply(player, SmoothEffects.MIGHT, 600, 3);
        if (has(player, AssassinContent.SIPHONING_STRIKES_AURA.id()))
            ClassEffectRuntime.apply(player, SmoothEffects.IMMOBILIZING_AURA, 600, 0);
        return ExecutionResult.success(1, "siphoning_strikes");
    }

}
