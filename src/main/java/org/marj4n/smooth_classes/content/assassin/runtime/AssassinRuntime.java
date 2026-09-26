package org.marj4n.smooth_classes.content.assassin.runtime;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.marj4n.smooth_classes.runtime.ContinuedFx;
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
        PreparationPlan plan = preparation(player);
        ClassEffectRuntime.apply(player, SmoothEffects.STEALTH, 80, 0);
        CombatRuntime.buff(player, StatusEffects.SPEED, 80, 2);
        ContinuedFx.sound(player, "soundeffect_39", 0.6F, 1.6F);
        if (has(player, AssassinContent.PREPARATION_SHADOWSTRIKE_SHIELD.id())) {
            ClassEffectRuntime.apply(player, SmoothEffects.BARRIER, 20, 0);
            player.removeStatusEffect(SmoothEffects.REVEALED);
        }
        if (plan.shadowstrike()) {
            int dashRange=8;
            Vec3d start=player.getPos();
            Vec3d look=player.getRotationVec(1F).normalize();
            Vec3d end=start.add(look.multiply(dashRange));
            Box corridor=new Box(start,end).expand(3);
            float damage=(float)player.getAttributeValue(
                    net.minecraft.entity.attribute.EntityAttributes.GENERIC_ATTACK_DAMAGE)*3F;
            for(LivingEntity target:player.getWorld().getEntitiesByClass(
                    LivingEntity.class,corridor,e->e!=player&&e.isAlive())){
                if(target instanceof PlayerEntity other && !player.shouldDamagePlayer(other))continue;
                if(target.isTeammate(player))continue;
                target.timeUntilRegen=0;
                target.damage(player.getDamageSources().playerAttack(player),damage);
                target.timeUntilRegen=0;
                if(has(player,AssassinContent.PREPARATION_SHADOWSTRIKE_VAMPIRE.id()))
                    target.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                            SmoothEffects.DEATH_MARK,120,0,false,false,true));
            }
            double actualRange=player.isOnGround()?dashRange:dashRange/5.0D;
            Vec3d velocity=look.multiply(actualRange);
            player.setVelocity(velocity.x,0,velocity.z);
            player.velocityModified=true;
            ContinuedFx.sound(player,"soundeffect_15",0.6F,1.3F);
        }
        return ExecutionResult.success(1, "preparation");
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
