package org.marj4n.smooth_classes.content.ruler.runtime;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.spell_engine.fx.SpellEngineParticles;
import org.marj4n.smooth_classes.runtime.InternalSpellRuntime;
import org.marj4n.smooth_classes.runtime.SpellPowerRuntime;
import org.marj4n.smooth_classes.runtime.SkillFx;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.Identifier;
import org.marj4n.smooth_classes.content.ruler.RulerClass;
import org.marj4n.smooth_classes.content.ruler.RulerContent;
import org.marj4n.smooth_classes.runtime.AbilityRuntime;
import org.marj4n.smooth_classes.runtime.ExecutionResult;
import org.marj4n.smooth_classes.runtime.ClassEffectRuntime;
import org.marj4n.smooth_classes.effects.SmoothEffects;

/** Talent-aware runtime plan builder for Ruler. Actual Minecraft effects are executed by hooks/integrations. */
public final class RulerRuntime {
    private RulerRuntime() {}
    private static void require(ServerPlayerEntity player) {
        if (!AbilityRuntime.isClass(player, RulerClass.ID)) throw new IllegalStateException("Player is not Ruler");
    }
    private static boolean has(ServerPlayerEntity player, Identifier talent) { return AbilityRuntime.hasTalent(player, talent); }

    public record SacredOrbPlan(boolean speed, boolean debuffs, boolean buffs) {}
    public static SacredOrbPlan sacredOrb(ServerPlayerEntity player) { require(player); return new SacredOrbPlan(has(player, RulerContent.SACRED_ORB_SPEED.id()), has(player, RulerContent.SACRED_ORB_DEBUFFS.id()), has(player, RulerContent.SACRED_ORB_BUFFS.id())); }
    public record DivinePlan(boolean sanctuary, boolean might, boolean spellforged) {}
    public static DivinePlan divineIntervention(ServerPlayerEntity player) { require(player); return new DivinePlan(has(player, RulerContent.DIVINE_INTERVENTION_SANCTUARY.id()), has(player, RulerContent.DIVINE_INTERVENTION_MIGHT.id()), has(player, RulerContent.DIVINE_INTERVENTION_SPELLFORGED.id())); }
    public record AnointPlan(boolean resistance, boolean undying, boolean cleanse) {}
    public static AnointPlan anointWeapon(ServerPlayerEntity player) { require(player); return new AnointPlan(has(player, RulerContent.ANOINT_WEAPON_RESISTANCE.id()), has(player, RulerContent.ANOINT_WEAPON_UNDYING.id()), has(player, RulerContent.ANOINT_WEAPON_CLEANSE.id())); }


    public static ExecutionResult executeSacredOrb(ServerPlayerEntity player) {
        require(player);
        return SacredBannerRuntime.cast(player);
    }

    public static ExecutionResult executeDivineIntervention(ServerPlayerEntity player) {
        DivinePlan plan=divineIntervention(player);
        var eye=player.getEyePos();var look=player.getRotationVec(1F).normalize();
        LivingEntity aimed=null;double nearest=25;
        for(LivingEntity e:player.getWorld().getEntitiesByClass(LivingEntity.class,
                player.getBoundingBox().expand(25), e->e!=player&&e.isAlive()&&isAlly(e,player))){
            var to=e.getEyePos().subtract(eye);double along=to.dotProduct(look);
            if(along>0&&along<nearest&&to.subtract(look.multiply(along)).lengthSquared()<2.25){aimed=e;nearest=along;}
        }
        var end=eye.add(look.multiply(25));
        var hit=player.getWorld().raycast(new net.minecraft.world.RaycastContext(eye,end,
                net.minecraft.world.RaycastContext.ShapeType.COLLIDER,
                net.minecraft.world.RaycastContext.FluidHandling.NONE,player));
        var center=aimed!=null?aimed.getPos():hit.getType()==net.minecraft.util.hit.HitResult.Type.MISS?end:hit.getPos();
        LivingEntity target=null;
        for (LivingEntity candidate : player.getWorld().getEntitiesByClass(LivingEntity.class,
                new net.minecraft.util.math.Box(center,center).expand(3),
                e->e!=player&&e.isAlive()&&isAlly(e,player))) {
            target=candidate;
            break;
        }
        if(target==null)return ExecutionResult.failure("No allied Divine Intervention target at the aimed position.");
        boolean cast=InternalSpellRuntime.target(player,"smooth_classes:divine_intervention",target,1F);
        if(!cast)return ExecutionResult.failure("Divine Intervention spell unavailable.");
        if(plan.sanctuary()) stack(target,StatusEffects.RESISTANCE,240,2,4);
        if(plan.might())stack(target,SmoothEffects.MIGHT,240,3,10);
        if(plan.spellforged())stack(target,SmoothEffects.SPELLFORGED,240,3,10);
        target.addStatusEffect(new StatusEffectInstance(SmoothEffects.DIVINE_RAY,30,0,false,false,false));
        org.marj4n.smooth_classes.runtime.DivineRayLightRuntime.illuminate(target);
        return ExecutionResult.success(1,"divine_intervention");
    }

    private static boolean isAlly(LivingEntity entity,ServerPlayerEntity player) {
        return entity.isTeammate(player) || entity instanceof net.minecraft.entity.passive.TameableEntity tame
                && tame.isOwner(player);
    }

    private static void stack(LivingEntity entity, net.minecraft.entity.effect.StatusEffect effect,
                              int duration, int amount, int cap){
        var old=entity.getStatusEffect(effect);
        int amplifier=old==null?amount-1:Math.min(cap-1,old.getAmplifier()+amount);
        entity.addStatusEffect(new StatusEffectInstance(effect,duration,amplifier,false,false,true));
    }

    public static ExecutionResult executeAnointWeapon(ServerPlayerEntity player) {
        require(player);
        ClassEffectRuntime.apply(player,SmoothEffects.ANOINTED,400,0);
        return ExecutionResult.success(1,"anoint_weapon");
    }

    public static void onAnointedMeleeHit(ServerPlayerEntity player) {
        if (!player.hasStatusEffect(SmoothEffects.ANOINTED)) return;
        java.util.List<net.minecraft.entity.Entity> allies=new java.util.ArrayList<>();
        java.util.List<LivingEntity> hostiles=new java.util.ArrayList<>();
        for(LivingEntity e:player.getWorld().getEntitiesByClass(LivingEntity.class, player.getBoundingBox().expand(4),e->e!=player&&e.isAlive())){
            if(player.isTeammate(e)) allies.add(e); else hostiles.add(e);
        }
        if(!allies.isEmpty()) InternalSpellRuntime.cast(player,"smooth_classes:paladins_flash_heal",allies,1F);
        if(!hostiles.isEmpty()){
            float amount=SpellPowerRuntime.healing(player,2.2)/hostiles.size();
            for(LivingEntity target:hostiles){
                target.timeUntilRegen=0; target.damage(player.getDamageSources().indirectMagic(player,player),amount); target.timeUntilRegen=0;
                if(target instanceof MobEntity mob&&mob.isUndead()) target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS,40,1,false,false,true));
                for(int i=6;i>0;i--){
                    SkillFx.particle(target,SpellEngineParticles.magic_holy.type(),0.1,0.1+i,0.2);
                    SkillFx.particle(target,SpellEngineParticles.magic_holy.type(),0.2,0.2+i,0.1);
                    SkillFx.particle(target,SpellEngineParticles.magic_holy.type(),0.1,0.2*i,0.2);
                }
            }
        }
        if(has(player,RulerContent.ANOINT_WEAPON_RESISTANCE.id())){
            StatusEffectInstance cur=player.getStatusEffect(StatusEffects.RESISTANCE);
            int amp=cur==null?1:Math.min(2,cur.getAmplifier()+1);
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE,40,amp,false,false,true));
        }
    }

    public static void onDamaged(ServerPlayerEntity player) {
        if(!has(player,RulerContent.ANOINT_WEAPON_UNDYING.id()))return;
        float hp=(player.getHealth()/player.getMaxHealth())*100F;
        if(hp<30F&&player.getRandom().nextInt(100)<15) ClassEffectRuntime.apply(player,SmoothEffects.UNDYING,120,0);
    }

}
