package org.marj4n.smooth_classes.content.caster.runtime;

import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.Identifier;
import org.marj4n.smooth_classes.content.caster.CasterClass;
import org.marj4n.smooth_classes.content.caster.CasterContent;
import org.marj4n.smooth_classes.integration.PuffishSkillsIntegration;
import org.marj4n.smooth_classes.integration.SimplySkillsNodeIds;
import org.marj4n.smooth_classes.runtime.AbilityRuntime;
import org.marj4n.smooth_classes.runtime.CombatRuntime;
import org.marj4n.smooth_classes.runtime.ExecutionResult;
import org.marj4n.smooth_classes.runtime.SpellPowerRuntime;
import org.marj4n.smooth_classes.runtime.InternalSpellRuntime;
import org.marj4n.smooth_classes.integration.OptionalCompatRuntime;

/** Talent-aware runtime plan builder for Caster. Actual Minecraft effects are executed by hooks/integrations. */
public final class CasterRuntime {
    private CasterRuntime() {}
    private static void require(ServerPlayerEntity player) {
        if (!AbilityRuntime.isClass(player, CasterClass.ID)) throw new IllegalStateException("Player is not Caster");
    }
    private static boolean has(ServerPlayerEntity player, Identifier talent) { return AbilityRuntime.hasTalent(player, talent); }

    public record MeteorPlan(boolean scorching, boolean explosive, boolean homing) {}
    public static MeteorPlan meteorShower(ServerPlayerEntity player) { require(player); return new MeteorPlan(has(player, CasterContent.METEOR_SHOWER_GREATER.id()), has(player, CasterContent.METEOR_SHOWER_WRATH.id()), has(player, CasterContent.METEOR_SHOWER_RENEWING_WRATH.id())); }
    public record IceCometPlan(boolean shatter, boolean frozen, boolean impact) {}
    public static IceCometPlan iceComet(ServerPlayerEntity player) { require(player); return new IceCometPlan(has(player, CasterContent.ICE_COMET_LEAP.id()), has(player, CasterContent.ICE_COMET_VOLLEY.id()), has(player, CasterContent.ICE_COMET_DAMAGE.id())); }
    public record StaticPlan(boolean overload, boolean chain, boolean orb) {}
    public static StaticPlan staticDischarge(ServerPlayerEntity player) { require(player); return new StaticPlan(has(player, CasterContent.STATIC_DISCHARGE_LIGHTNING_BALL.id()), has(player, CasterContent.STATIC_DISCHARGE_LEAP.id()), has(player, CasterContent.STATIC_DISCHARGE_LIGHTNING_ORB.id())); }


    public static ExecutionResult executeMeteorShower(ServerPlayerEntity player) {
        MeteorPlan plan=meteorShower(player);
        var center=aim(player,120);
        LivingEntity target=player.getWorld().getEntitiesByClass(LivingEntity.class,
                new net.minecraft.util.math.Box(center,center).expand(8),
                e->e!=player&&e.isAlive()&&OptionalCompatRuntime.canHarm(player,e)).stream().findFirst().orElse(null);
        if(target==null)return ExecutionResult.failure("No Meteor Shower target near the aimed position.");
        if(plan.explosive()) org.marj4n.smooth_classes.runtime.ClassEffectRuntime.apply(player,
                org.marj4n.smooth_classes.effects.SmoothEffects.METEORIC_WRATH,800,9);
        boolean cast=InternalSpellRuntime.target(player,
                plan.scorching()?"smooth_classes:fire_meteor_large":"smooth_classes:fire_meteor",target,1F);
        return cast?ExecutionResult.success(1,"meteor_shower"):ExecutionResult.failure("Meteor Shower spell unavailable.");
    }

    public static ExecutionResult executeIceComet(ServerPlayerEntity player) {
        IceCometPlan plan=iceComet(player);
        if(plan.shatter()) {
            CombatRuntime.launchBackward(player,3.0,1.3);
            CombatRuntime.buff(player,StatusEffects.SLOW_FALLING,180,0);
        }
        if(plan.frozen()) org.marj4n.smooth_classes.runtime.ClassEffectRuntime.apply(player,
                org.marj4n.smooth_classes.effects.SmoothEffects.FROST_VOLLEY,400,6);
        var center=aim(player,120);
        LivingEntity target=player.getWorld().getEntitiesByClass(LivingEntity.class,
                new net.minecraft.util.math.Box(center,center).expand(3),
                e->e!=player&&e.isAlive()&&OptionalCompatRuntime.canHarm(player,e)).stream().findFirst().orElse(null);
        String spell="ice_comet";
        if(PuffishSkillsIntegration.isSkillUnlocked(PuffishSkillsIntegration.CASTER,
                SimplySkillsNodeIds.wizardSpecialisationIceCometDamageThree,player))spell="ice_comet_large_three";
        else if(PuffishSkillsIntegration.isSkillUnlocked(PuffishSkillsIntegration.CASTER,
                SimplySkillsNodeIds.wizardSpecialisationIceCometDamageTwo,player))spell="ice_comet_large_two";
        else if(plan.impact())spell="ice_comet_large";
        boolean cast=target!=null?InternalSpellRuntime.target(player,"smooth_classes:"+spell,target,1F)
                :InternalSpellRuntime.dumbFire(player,"smooth_classes:"+spell,1F);
        return cast?ExecutionResult.success(target==null?0:1,"ice_comet"):ExecutionResult.failure("Ice Comet spell unavailable.");
    }

    public static ExecutionResult executeStaticDischarge(ServerPlayerEntity player) {
        require(player);
        if(has(player,CasterContent.STATIC_DISCHARGE_LIGHTNING_BALL.id())){
            boolean cast=org.marj4n.smooth_classes.runtime.InternalSpellRuntime.dumbFire(player,"smooth_classes:lightning_ball",3F);
            return ExecutionResult.success(cast?1:0,"static_discharge");
        }
        LivingEntity target=null; double best=Double.MAX_VALUE;
        net.minecraft.util.math.Vec3d look=player.getRotationVec(1F).normalize();
        for(LivingEntity e:CombatRuntime.nearbyEnemies(player,120)){
            net.minecraft.util.math.Vec3d to=e.getEyePos().subtract(player.getEyePos()); double dist=to.length();
            if(dist<=0||look.dotProduct(to.normalize())<0.94)continue; if(dist<best){best=dist;target=e;}
        }
        if(target==null)return ExecutionResult.success(0,"static_discharge");
        boolean cast=org.marj4n.smooth_classes.runtime.InternalSpellRuntime.target(player,"smooth_classes:static_discharge",target,3F);
        if(cast&&has(player,CasterContent.STATIC_DISCHARGE_LEAP.id())){
            int amp=12;
            if(PuffishSkillsIntegration.isSkillUnlocked(PuffishSkillsIntegration.CASTER, SimplySkillsNodeIds.wizardSpecialisationStaticDischargeLeapTwo,player))amp+=20;
            else if(PuffishSkillsIntegration.isSkillUnlocked(PuffishSkillsIntegration.CASTER,SimplySkillsNodeIds.wizardSpecialisationStaticDischargeLeapThree,player))amp+=40;
            target.addStatusEffect(new org.marj4n.smooth_classes.effects.SourceStatusEffectInstance(
                    org.marj4n.smooth_classes.effects.SmoothEffects.STATIC_CHARGE,1600,amp,false,false,true,player));
        }
        if (cast) onStaticChargeHit(player, target);
        return ExecutionResult.success(cast?1:0,"static_discharge");
    }

    /** Continued's on-hit speed and lightning-orb rolls, shared by the first hit and every leap. */
    public static void onStaticChargeHit(ServerPlayerEntity owner, LivingEntity target) {
        if (owner.getWorld() instanceof net.minecraft.server.world.ServerWorld world) {
            net.minecraft.entity.LightningEntity flash=net.minecraft.entity.EntityType.LIGHTNING_BOLT.create(world);
            if (flash!=null) {
                flash.setCosmetic(true);
                flash.refreshPositionAndAngles(target.getX(),target.getY(),target.getZ(),0,0);
                world.spawnEntity(flash);
            }
            world.spawnParticles(net.minecraft.particle.ParticleTypes.ELECTRIC_SPARK,
                    target.getX(),target.getBodyY(.5),target.getZ(),30,.4,.9,.4,.08);
        }
        int chance = 5;
        if (PuffishSkillsIntegration.isSkillUnlocked(PuffishSkillsIntegration.CASTER,
                SimplySkillsNodeIds.wizardSpecialisationStaticDischargeSpeedThree, owner)) chance += 10;
        else if (PuffishSkillsIntegration.isSkillUnlocked(PuffishSkillsIntegration.CASTER,
                SimplySkillsNodeIds.wizardSpecialisationStaticDischargeSpeedTwo, owner)) chance += 5;
        if (has(owner, CasterContent.STATIC_DISCHARGE_SPEED.id()) && owner.getRandom().nextInt(100) < chance) {
            net.minecraft.entity.effect.StatusEffectInstance old = owner.getStatusEffect(StatusEffects.SPEED);
            int amp = old == null ? 0 : Math.min(2, old.getAmplifier() + 1);
            owner.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(StatusEffects.SPEED, 400, amp, false, false, true));
        }
        if (has(owner, CasterContent.STATIC_DISCHARGE_LIGHTNING_ORB.id())
                && owner.getRandom().nextInt(100) < chance / 2)
            org.marj4n.smooth_classes.runtime.InternalSpellRuntime.target(owner,
                    "smooth_classes:lightning_ball_homing", target, 3F);
    }

    public static ExecutionResult executeArcaneBolt(ServerPlayerEntity player) {
        require(player);
        if(has(player,CasterContent.ARCANE_BOLT_VOLLEY.id()))
            org.marj4n.smooth_classes.runtime.ClassEffectRuntime.apply(player,
                    org.marj4n.smooth_classes.effects.SmoothEffects.ARCANE_VOLLEY,400,9);
        String spell=has(player,CasterContent.ARCANE_BOLT_LESSER.id())?"arcane_bolt_expanding"
                :has(player,CasterContent.ARCANE_BOLT_GREATER.id())?"arcane_bolt_greater":"arcane_bolt";
        LivingEntity target=aimedEnemy(player,120);
        boolean cast=target!=null?InternalSpellRuntime.target(player,"smooth_classes:"+spell,target,1F)
                :InternalSpellRuntime.dumbFire(player,"smooth_classes:"+spell,1F);
        return cast?ExecutionResult.success(target==null?0:1,"arcane_bolt"):ExecutionResult.failure("Arcane Bolt spell unavailable.");
    }

    private static net.minecraft.util.math.Vec3d aim(ServerPlayerEntity player,double range){
        LivingEntity target=aimedEnemy(player,range);
        if(target!=null)return target.getPos();
        var eye=player.getEyePos();var end=eye.add(player.getRotationVec(1F).multiply(range));
        var hit=player.getWorld().raycast(new net.minecraft.world.RaycastContext(eye,end,
                net.minecraft.world.RaycastContext.ShapeType.COLLIDER,
                net.minecraft.world.RaycastContext.FluidHandling.NONE,player));
        return hit.getType()==net.minecraft.util.hit.HitResult.Type.MISS?end:hit.getPos();
    }
    private static LivingEntity aimedEnemy(ServerPlayerEntity player,double range){
        var eye=player.getEyePos();var look=player.getRotationVec(1F).normalize();
        LivingEntity best=null;double bestDist=range;
        for(LivingEntity e:CombatRuntime.nearbyEnemies(player,range)){
            var to=e.getEyePos().subtract(eye);double along=to.dotProduct(look);
            if(along>0&&along<bestDist&&to.subtract(look.multiply(along)).lengthSquared()<2.25){best=e;bestDist=along;}
        }
        return best;
    }

}
