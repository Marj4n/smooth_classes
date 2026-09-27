package org.marj4n.smooth_classes.content.berserker.runtime;

import net.minecraft.server.network.ServerPlayerEntity;
import org.marj4n.smooth_classes.runtime.SkillFx;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.Identifier;
import org.marj4n.smooth_classes.content.berserker.BerserkerClass;
import org.marj4n.smooth_classes.content.berserker.BerserkerContent;
import org.marj4n.smooth_classes.runtime.AbilityRuntime;
import org.marj4n.smooth_classes.runtime.CombatRuntime;
import org.marj4n.smooth_classes.runtime.ExecutionResult;
import org.marj4n.smooth_classes.runtime.ClassEffectRuntime;
import org.marj4n.smooth_classes.effects.SmoothEffects;

/** Talent-aware runtime plan builder for Berserker. Actual Minecraft effects are executed by hooks/integrations. */
public final class BerserkerRuntime {
    private BerserkerRuntime() {}
    private static void require(ServerPlayerEntity player) {
        if (!AbilityRuntime.isClass(player, BerserkerClass.ID)) throw new IllegalStateException("Player is not Berserker");
    }
    private static boolean has(ServerPlayerEntity player, Identifier talent) { return AbilityRuntime.hasTalent(player, talent); }

    public record RampagePlan(boolean bloodlust, boolean relentless, boolean leap) {}
    public static RampagePlan rampage(ServerPlayerEntity player) { require(player); return new RampagePlan(has(player, BerserkerContent.RAMPAGE_CHARGE.id()), has(player, BerserkerContent.RAMPAGE_CHARGE_RELENTLESS.id()), has(player, BerserkerContent.RAMPAGE_CHARGE_IMMOBILIZE.id())); }
    public record BloodthirstyPlan(boolean heal, boolean frenzy) {}
    public static BloodthirstyPlan bloodthirsty(ServerPlayerEntity player) { require(player); return new BloodthirstyPlan(has(player, BerserkerContent.BLOODTHIRSTY_MIGHTY.id()), has(player, BerserkerContent.BLOODTHIRSTY_TIRELESS.id())); }
    public record BerserkingPlan(boolean resistance, boolean speed) {}
    public static BerserkingPlan berserking(ServerPlayerEntity player) { require(player); return new BerserkingPlan(has(player, BerserkerContent.BERSERKING_LEAP.id()), has(player, BerserkerContent.BERSERKING_LEAP_PULL.id())); }


    public static ExecutionResult executeRampage(ServerPlayerEntity player) {
        RampagePlan plan = rampage(player);
        ClassEffectRuntime.apply(player, SmoothEffects.RAMPAGE, 250, 0);
        if (plan.bloodlust()) {
            ClassEffectRuntime.apply(player, SmoothEffects.BULLRUSH, 20, 0);
            SkillFx.sound(player, "soundeffect_15", 0.5F, 1.1F);
        }
        return ExecutionResult.success(1, "rampage");
    }

    public static ExecutionResult executeBloodthirsty(ServerPlayerEntity player) {
        BloodthirstyPlan plan = bloodthirsty(player);
        ClassEffectRuntime.apply(player, SmoothEffects.BLOODTHIRSTY, 400, 0);
        if (plan.heal()) ClassEffectRuntime.apply(player, SmoothEffects.MIGHT, 400, 0);
        return ExecutionResult.success(1, "bloodthirsty");
    }

    public static ExecutionResult executeBerserking(ServerPlayerEntity player) {
        BerserkingPlan plan = berserking(player);
        float sacrifice = player.getHealth() * 0.30F;
        player.damage(player.getDamageSources().generic(), sacrifice);
        ClassEffectRuntime.apply(player, SmoothEffects.BERSERKING, Math.max(20, (int)(sacrifice * 20F)), 0);
        if (plan.resistance()) {
            ClassEffectRuntime.apply(player, SmoothEffects.LEAPSLAM, 62, 0);
            SkillFx.sound(player, "soundeffect_15", 0.5F, 1.1F);
        }
        return ExecutionResult.success(1, "berserking");
    }

}
