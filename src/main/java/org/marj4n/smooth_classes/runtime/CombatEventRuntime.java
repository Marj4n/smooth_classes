package org.marj4n.smooth_classes.runtime;

import org.marj4n.smooth_classes.config.SmoothBalance;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.spell_power.api.SpellSchool;
import java.util.List;
import org.marj4n.smooth_classes.effects.SmoothEffects;
import org.marj4n.smooth_classes.runtime.base.BasePathRuntime;
import org.marj4n.smooth_classes.runtime.classpass.ClassPassiveRuntime;
import org.marj4n.smooth_classes.content.ruler.runtime.RulerRuntime;
import org.marj4n.smooth_classes.integration.PuffishSkillsIntegration;
import org.marj4n.smooth_classes.integration.SkillNodeIds;
import org.marj4n.smooth_classes.integration.OptionalCompatRuntime;
import org.marj4n.smooth_classes.content.avenger.AvengerClass;
import org.marj4n.smooth_classes.content.avenger.runtime.AvengerMinionGameplay;

/**
 * Chapter 5 event router. One authoritative entry point per combat event keeps
 * base-path and class logic from firing twice through overlapping callbacks.
 */
public final class CombatEventRuntime {
    private CombatEventRuntime() {}


    /** Central Spell Engine cast route. */
    public static void onSpellCast(ServerPlayerEntity player, List<Entity> targets, SpellSchool school) {
        if (AbilityRuntime.isClass(player, AvengerClass.ID)) AvengerMinionGameplay.onSpellCast(player);
        BasePathRuntime.onSpellCast(player, targets, school);
        ClassPassiveRuntime.onSpellCast(player, targets, school);
        OptionalCompatRuntime.onSpellCast(player);
    }

    /** Spell-projectile lifecycle hook. Projectile-specific behavior is Chapter 7. */
    public static void onSpellProjectileTick(ServerPlayerEntity owner, Entity projectile) {
        // Deliberately centralized now; homing/artillery/orb behavior is owned by Chapter 7.
    }

    /** Spell-projectile impact hook. Ascendancy/projectile effects attach here in Chapters 6-7. */
    public static void onSpellProjectileHit(ServerPlayerEntity owner, Entity projectile, LivingEntity target) {
        RighteousHammerChargeRuntime.onTriggeredHit(owner,target);
        AscendancyRuntime.spellProjectileHit(owner, target);
    }

    public static void onMeleeAttack(ServerPlayerEntity player, Entity target) {
        if (!player.isAlive() || !target.isAttackable() || !(target instanceof LivingEntity living)) return;
        RighteousHammerChargeRuntime.onTriggeredHit(player,living);
        BasePathRuntime.onMeleeHit(player, living);
        ClassPassiveRuntime.onMeleeHit(player, living);
        RulerRuntime.onAnointedMeleeHit(player);

        // Elemental Surge Renewal extends the active surge by 3 ticks per melee hit.
        if (has(player, PuffishSkillsIntegration.FOREIGNER, SkillNodeIds.spellbladeSpecialisationElementalSurgeRenewal)
                && player.hasStatusEffect(SmoothEffects.ELEMENTAL_SURGE)) {
            int duration = player.getStatusEffect(SmoothEffects.ELEMENTAL_SURGE).getDuration();
            player.addStatusEffect(new StatusEffectInstance(SmoothEffects.ELEMENTAL_SURGE, duration + 3, 0, false, false, true));
        }
        // Bloodthirsty modifiers are melee-event procs.
        if (player.hasStatusEffect(SmoothEffects.BLOODTHIRSTY)) {
            if (has(player, PuffishSkillsIntegration.BERSERKER, SkillNodeIds.berserkerSpecialisationBloodthirstyTremor)
                    && player.getRandom().nextInt(100) < SmoothBalance.Berserker.bloodthirstyTremorChance)
                increment(player, SmoothEffects.EARTHSHAKER, 30, 1, 1);
            if (has(player, PuffishSkillsIntegration.BERSERKER, SkillNodeIds.berserkerSpecialisationBloodthirstyTireless)
                    && player.getRandom().nextInt(100) < SmoothBalance.Berserker.bloodthirstyTirelessChance)
                decrement(player, SmoothEffects.EXHAUSTION, 1);
        }
    }

    /** @return false when the incoming hit is consumed/cancelled. */
    public static boolean onIncomingDamage(ServerPlayerEntity player, DamageSource source, float amount) {
        if (!player.isAlive()) return true;

        if (!AscendancyRuntime.incomingDamage(player, source, amount)) return false;

        // Barrier consumes one stack and completely absorbs the hit.
        if (player.hasStatusEffect(SmoothEffects.BARRIER)) {
            decrement(player, SmoothEffects.BARRIER, 1);
            return false;
        }
        // Ghostwalk is an invulnerability movement state in this runtime.
        if (player.hasStatusEffect(SmoothEffects.GHOSTWALK)) return false;

        if (!ClassPassiveRuntime.allowDamage(player)) return false;

        if (AbilityRuntime.isClass(player, AvengerClass.ID)) AvengerMinionGameplay.onIncomingDamage(player);

        BasePathRuntime.onDamaged(player);
        LivingEntity attacker = source.getAttacker() instanceof LivingEntity living ? living : null;
        ClassPassiveRuntime.onDamaged(player, attacker);
        RulerRuntime.onDamaged(player);

        // Path of Strength / Berserker rage-on-damage state.
        if (player.hasStatusEffect(SmoothEffects.RAGE)) {
            // Existing rage is preserved; taking damage refreshes it below.
        }
        return true;
    }

    public static void afterIncomingDamage(ServerPlayerEntity player, DamageSource source, float amount) {
        if(amount>0)AscendancyRuntime.boneArmorHit(player);
        // Berserker path gains one Rage stack whenever real damage lands.
        if (has(player, PuffishSkillsIntegration.TREE, SkillNodeIds.berserkerPath))
            increment(player, SmoothEffects.RAGE, 300, 1, 99);
    }

    public static void onKilledOther(ServerPlayerEntity player, ServerWorld world, LivingEntity victim) {
        org.marj4n.smooth_classes.content.lancer.runtime.LancerRuntime.onKill(player, victim);
        // Renewal effects are intentionally event-based, matching the
        // PlayerEntity#onKilledOther hook rather than polling.
        if (has(player, PuffishSkillsIntegration.BERSERKER, SkillNodeIds.berserkerSpecialisationBloodthirsty)
                && player.hasStatusEffect(SmoothEffects.BLOODTHIRSTY))
            player.heal(player.getMaxHealth() * SmoothBalance.Berserker.bloodthirstyHealPercent);
        if (has(player, PuffishSkillsIntegration.ARCHER, SkillNodeIds.rangerSpecialisationElementalArrowsRenewal)
                && player.hasStatusEffect(SmoothEffects.ELEMENTAL_ARROWS))
            player.addStatusEffect(new StatusEffectInstance(SmoothEffects.ELEMENTAL_ARROWS, 500,
                    Math.max(0, player.getStatusEffect(SmoothEffects.ELEMENTAL_ARROWS).getAmplifier()), false, false, true));
        if (has(player, PuffishSkillsIntegration.ASSASSIN, SkillNodeIds.rogueSpecialisationEvasionFanOfBladesRenewal))
            player.addStatusEffect(new StatusEffectInstance(SmoothEffects.FANOFBLADES, 500, 1, false, false, true));
    }

    public static void onDeath(ServerPlayerEntity player, DamageSource source) {
        org.marj4n.smooth_classes.content.lancer.runtime.LancerRuntime.onDeath(player);
        player.setInvisible(false);
    }

    private static boolean has(ServerPlayerEntity player, net.minecraft.util.Identifier category, String skill) {
        return PuffishSkillsIntegration.isSkillUnlocked(category, skill, player);
    }

    private static void increment(LivingEntity e, net.minecraft.entity.effect.StatusEffect effect, int duration, int amount, int max) {
        StatusEffectInstance old=e.getStatusEffect(effect);
        int amp=old==null?Math.max(0,amount-1):Math.min(max-1,old.getAmplifier()+amount);
        e.addStatusEffect(new StatusEffectInstance(effect,duration,amp,false,false,true));
    }

    private static void decrement(LivingEntity e, net.minecraft.entity.effect.StatusEffect effect, int amount) {
        StatusEffectInstance old=e.getStatusEffect(effect);
        if(old==null)return;
        int amp=old.getAmplifier()-amount;
        if(amp<0)e.removeStatusEffect(effect);
        else e.addStatusEffect(new StatusEffectInstance(effect,old.getDuration(),amp,false,false,true));
    }
}
