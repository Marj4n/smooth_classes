package org.marj4n.smooth_classes.content.saber.runtime;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import org.marj4n.smooth_classes.runtime.InternalSpellRuntime;
import org.marj4n.smooth_classes.runtime.SkillFx;
import net.minecraft.util.Identifier;
import org.marj4n.smooth_classes.content.saber.SaberClass;
import org.marj4n.smooth_classes.content.saber.SaberContent;
import org.marj4n.smooth_classes.runtime.AbilityRuntime;
import org.marj4n.smooth_classes.runtime.CombatRuntime;
import org.marj4n.smooth_classes.runtime.ExecutionResult;
import org.marj4n.smooth_classes.runtime.ClassEffectRuntime;
import org.marj4n.smooth_classes.effects.SmoothEffects;

/** Talent-aware runtime plan builder for Saber. Actual Minecraft effects are executed by hooks/integrations. */
public final class SaberRuntime {
    private SaberRuntime() {}
    private static void require(ServerPlayerEntity player) {
        if (!AbilityRuntime.isClass(player, SaberClass.ID)) throw new IllegalStateException("Player is not Saber");
    }
    private static boolean has(ServerPlayerEntity player, Identifier talent) { return AbilityRuntime.hasTalent(player, talent); }

    public record ConsecrationPlan(boolean duration, boolean ward, boolean taunt, boolean mighty, boolean spellforged) {}
    public static ConsecrationPlan consecration(ServerPlayerEntity player) { require(player); return new ConsecrationPlan(has(player, SaberContent.CONSECRATION_DURATION.id()), has(player, SaberContent.CONSECRATION_WARD.id()), has(player, SaberContent.CONSECRATION_TAUNT.id()), has(player, SaberContent.CONSECRATION_MIGHTY.id()), has(player, SaberContent.CONSECRATION_SPELLFORGED.id())); }
    public record OnslaughtPlan(boolean heal, boolean defend, boolean mighty, boolean stun) {}
    public static OnslaughtPlan sacredOnslaught(ServerPlayerEntity player) { require(player); return new OnslaughtPlan(has(player, SaberContent.SACRED_ONSLAUGHT_HEAL.id()), has(player, SaberContent.SACRED_ONSLAUGHT_DEFEND.id()), has(player, SaberContent.SACRED_ONSLAUGHT_MIGHTY.id()), has(player, SaberContent.SACRED_ONSLAUGHT_STUN.id())); }
    public record AdjudicationPlan(boolean taunt, boolean mark, boolean effect, boolean exhaust, boolean mighty) {}
    public static AdjudicationPlan divineAdjudication(ServerPlayerEntity player) { require(player); return new AdjudicationPlan(has(player, SaberContent.DIVINE_ADJUDICATION_TAUNT.id()), has(player, SaberContent.DIVINE_ADJUDICATION_MARK.id()), has(player, SaberContent.DIVINE_ADJUDICATION_CHAIN.id()), has(player, SaberContent.DIVINE_ADJUDICATION_RECUPERATE.id()), has(player, SaberContent.DIVINE_ADJUDICATION_MIGHTY.id())); }


    public static ExecutionResult executeConsecration(ServerPlayerEntity player) {
        ConsecrationPlan plan = consecration(player);
        int duration = plan.duration() ? 500 : 250;
        ClassEffectRuntime.apply(player, SmoothEffects.CONSECRATION, duration, 0);
        return ExecutionResult.success(1, "consecration");
    }

    public static ExecutionResult executeSacredOnslaught(ServerPlayerEntity player) {
        OnslaughtPlan plan = sacredOnslaught(player);
        ClassEffectRuntime.apply(player, SmoothEffects.SACRED_ONSLAUGHT, 40, 0);
        if (plan.defend()) {
            ClassEffectRuntime.apply(player, SmoothEffects.GOLDEN_AEGIS, 200, 0);
            SkillFx.sound(player, "soundeffect_15", 0.5F, 1.1F);
        }
        if (plan.mighty()) ClassEffectRuntime.apply(player, SmoothEffects.MIGHT, 200, 2);
        return ExecutionResult.success(1, "sacred_onslaught");
    }

    public static ExecutionResult executeDivineAdjudication(ServerPlayerEntity player) {
        AdjudicationPlan plan=divineAdjudication(player);
        java.util.List<LivingEntity> enemies=CombatRuntime.nearbyEnemies(player,8.0D);
        if(enemies.isEmpty()) return ExecutionResult.failure("No hostile target within 8 blocks.");

        LivingEntity guaranteed=enemies.stream()
                .min(java.util.Comparator.comparingDouble(player::squaredDistanceTo)).orElse(enemies.get(0));
        int hits=0;
        for(LivingEntity target:enemies){
            boolean chosen=target==guaranteed || player.getRandom().nextInt(100)<35;
            if(!chosen) continue;
            if(InternalSpellRuntime.target(player,"smooth_classes:paladins_judgement",target,0.85F)) hits++;
            if(plan.mark()) target.addStatusEffect(new StatusEffectInstance(SmoothEffects.DEATH_MARK,240,0,false,false,true));
            if(plan.taunt()&&target instanceof MobEntity)
                target.addStatusEffect(new org.marj4n.smooth_classes.effects.SourceStatusEffectInstance(
                        SmoothEffects.TAUNTED,240,0,false,false,true,player));
        }
        if(plan.effect()) ClassEffectRuntime.apply(player,SmoothEffects.DIVINE_ADJUDICATION,400,0);
        return ExecutionResult.success(hits,"divine_adjudication");
    }

    /** Divine Adjudication: periodic AOE judgement, chance rolled per hostile. */
    public static void tick(ServerPlayerEntity player) {
        if(!player.hasStatusEffect(SmoothEffects.DIVINE_ADJUDICATION)||player.age%5!=0)return;
        AdjudicationPlan plan=divineAdjudication(player);
        boolean delivered=false;
        for(LivingEntity target:org.marj4n.smooth_classes.runtime.CombatRuntime.nearbyEnemies(player,5)) {
            if(player.getRandom().nextInt(100)<10) {
                delivered=InternalSpellRuntime.target(player,"smooth_classes:paladins_judgement",target,1F);
                break;
            }
        }
        if(delivered){
            if(plan.exhaust())decrement(player,SmoothEffects.EXHAUSTION,2);
            if(plan.mighty())increment(player,SmoothEffects.MIGHT,120,1,5);
        }
    }

    private static void increment(LivingEntity e, net.minecraft.entity.effect.StatusEffect fx,int duration,int amount,int max){
        StatusEffectInstance old=e.getStatusEffect(fx);
        int amp=old==null?Math.max(0,amount-1):Math.min(max-1,old.getAmplifier()+amount);
        e.addStatusEffect(new StatusEffectInstance(fx,duration,amp,false,false,true));
    }
    private static void decrement(LivingEntity e, net.minecraft.entity.effect.StatusEffect fx,int amount){
        StatusEffectInstance old=e.getStatusEffect(fx); if(old==null)return;
        int amp=old.getAmplifier()-amount;
        if(amp<0)e.removeStatusEffect(fx);
        else e.addStatusEffect(new StatusEffectInstance(fx,old.getDuration(),amp,false,false,true));
    }

}
