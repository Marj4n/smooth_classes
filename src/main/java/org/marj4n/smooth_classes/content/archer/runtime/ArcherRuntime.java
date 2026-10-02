package org.marj4n.smooth_classes.content.archer.runtime;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import org.marj4n.smooth_classes.entity.ArrowRainEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import org.marj4n.smooth_classes.content.archer.ArcherClass;
import org.marj4n.smooth_classes.content.archer.ArcherContent;
import org.marj4n.smooth_classes.runtime.AbilityRuntime;
import org.marj4n.smooth_classes.runtime.CombatRuntime;
import org.marj4n.smooth_classes.runtime.ExecutionResult;
import org.marj4n.smooth_classes.runtime.ClassEffectRuntime;
import org.marj4n.smooth_classes.effects.SmoothEffects;
import org.marj4n.smooth_classes.integration.PuffishSkillsIntegration;
import org.marj4n.smooth_classes.integration.SkillNodeIds;
import org.marj4n.smooth_classes.runtime.InternalSpellRuntime;
import org.marj4n.smooth_classes.integration.OptionalCompatRuntime;

/** Talent-aware runtime plan builder for Archer. Actual Minecraft effects are executed by hooks/integrations. */
public final class ArcherRuntime {
    private ArcherRuntime() {}
    private static void require(ServerPlayerEntity player) {
        if (!AbilityRuntime.isClass(player, ArcherClass.ID)) throw new IllegalStateException("Player is not Archer");
    }
    private static boolean has(ServerPlayerEntity player, Identifier talent) { return AbilityRuntime.hasTalent(player, talent); }

    /** Bow release consumes one Elemental Arrows stack. The school always rotates freely; upgrades improve the shot instead of locking an element. */
    public static boolean fireElementalArrows(ServerPlayerEntity player) {
        if (!player.hasStatusEffect(SmoothEffects.ELEMENTAL_ARROWS)) return false;
        String spell = switch (player.getRandom().nextInt(3)) {
            case 0 -> "fire_arrow_rain";
            case 1 -> "lightning_arrow_rain";
            default -> "frost_arrow_rain";
        };
        float power = has(player, ArcherContent.ELEMENTAL_ARROWS_OVERCHARGE.id()) ? 1.10F : 1.0F;
        boolean convergence = has(player, ArcherContent.ELEMENTAL_ARROWS_CONVERGENCE.id());
        boolean split = has(player, ArcherContent.ELEMENTAL_ARROWS_SPLIT_VOLLEY.id());
        int radius = 4;
        if (PuffishSkillsIntegration.isSkillUnlocked(PuffishSkillsIntegration.ARCHER,
                SkillNodeIds.rangerSpecialisationElementalArrowsRadiusThree, player)) radius += 6;
        else if (PuffishSkillsIntegration.isSkillUnlocked(PuffishSkillsIntegration.ARCHER,
                SkillNodeIds.rangerSpecialisationElementalArrowsRadiusTwo, player)) radius += 4;
        else if (PuffishSkillsIntegration.isSkillUnlocked(PuffishSkillsIntegration.ARCHER,
                SkillNodeIds.rangerSpecialisationElementalArrowsRadiusOne, player)) radius += 2;
        Vec3d center = aimedPosition(player, 120);
        var box = new net.minecraft.util.math.Box(center, center).expand(radius);
        var targets = player.getWorld().getEntitiesByClass(LivingEntity.class, box,
                e -> e.isAlive() && e != player && OptionalCompatRuntime.canHarm(player, e));
        int count = targets.size() == 1 ? (convergence ? 7 : 6) : 1;
        for (LivingEntity target : targets) {
            for (int i = 0; i < count; i++)
                InternalSpellRuntime.target(player, "smooth_classes:" + spell, target, power);
            if (targets.size() > 1 && split)
                InternalSpellRuntime.target(player, "smooth_classes:" + spell, target, power * 0.55F);
        }
        if (targets.isEmpty()) InternalSpellRuntime.dumbFire(player, "smooth_classes:" + spell, power);
        consume(player, SmoothEffects.ELEMENTAL_ARROWS);
        return true;
    }

    /** Marksman uses the physical snipe asset at bow release. */
    public static boolean fireMarksman(ServerPlayerEntity player) {
        if (!player.hasStatusEffect(SmoothEffects.MARKSMAN)) return false;
        LivingEntity target = aimedEnemy(player, 120);
        if (target != null) InternalSpellRuntime.target(player, "smooth_classes:physical_bow_snipe", target, 1F);
        else InternalSpellRuntime.dumbFire(player, "smooth_classes:physical_bow_snipe", 1F);
        consume(player, SmoothEffects.MARKSMAN);
        return true;
    }

    private static void consume(ServerPlayerEntity player, net.minecraft.entity.effect.StatusEffect effect) {
        var old = player.getStatusEffect(effect);
        if (old == null) return;
        if (old.getAmplifier() <= 0) player.removeStatusEffect(effect);
        else player.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(effect,
                old.getDuration(), old.getAmplifier() - 1, false, false, true));
    }

    private static Vec3d aimedPosition(ServerPlayerEntity player, double range) {
        Vec3d eye = player.getEyePos();
        Vec3d end = eye.add(player.getRotationVec(1F).normalize().multiply(range));
        var hit = player.getWorld().raycast(new net.minecraft.world.RaycastContext(eye, end,
                net.minecraft.world.RaycastContext.ShapeType.COLLIDER,
                net.minecraft.world.RaycastContext.FluidHandling.NONE, player));
        LivingEntity entity = aimedEnemy(player, range);
        return entity != null ? entity.getPos() : hit.getType() == net.minecraft.util.hit.HitResult.Type.MISS ? end : hit.getPos();
    }

    private static LivingEntity aimedEnemy(ServerPlayerEntity player, double range) {
        Vec3d eye = player.getEyePos();
        Vec3d look = player.getRotationVec(1F).normalize();
        LivingEntity nearest = null;
        double best = range;
        for (LivingEntity candidate : CombatRuntime.nearbyEnemies(player, range)) {
            Vec3d to = candidate.getEyePos().subtract(eye);
            double along = to.dotProduct(look);
            if (along <= 0 || along >= best || to.subtract(look.multiply(along)).lengthSquared() > 2.25) continue;
            best = along;
            nearest = candidate;
        }
        return nearest;
    }

    public record ArrowRainPlan(boolean elemental, boolean artillery, boolean explosive, boolean volley, boolean radius) {}
    public static ArrowRainPlan arrowRain(ServerPlayerEntity player) { require(player); return new ArrowRainPlan(has(player, ArcherContent.ARROW_RAIN_ELEMENTAL.id()), has(player, ArcherContent.ARROW_RAIN_ELEMENTAL_ARTILLERY.id()), has(player, ArcherContent.ARROW_RAIN_EXPLOSIVE.id()), has(player, ArcherContent.ARROW_RAIN_VOLLEY.id()), has(player, ArcherContent.ARROW_RAIN_RADIUS.id())); }
    public static ExecutionResult executeArrowRain(ServerPlayerEntity player) {
        require(player);
        ClassEffectRuntime.apply(player, SmoothEffects.ARROW_RAIN, 600, 0);
        return ExecutionResult.success(1, "arrow_rain");
    }

    /** Arrow Rain is consumed by a fully drawn bow release, not passively over time. */
    public static boolean fireArrowRain(ServerPlayerEntity player) {
        if(!AbilityRuntime.isClass(player,ArcherClass.ID)||!player.hasStatusEffect(SmoothEffects.ARROW_RAIN))return false;
        ArrowRainPlan plan=arrowRain(player);
        int radius=3;
        if(PuffishSkillsIntegration.isSkillUnlocked(PuffishSkillsIntegration.ARCHER,SkillNodeIds.rangerSpecialisationArrowRainRadiusThree,player))radius+=3;
        else if(PuffishSkillsIntegration.isSkillUnlocked(PuffishSkillsIntegration.ARCHER,SkillNodeIds.rangerSpecialisationArrowRainRadiusTwo,player))radius+=2;
        else if(PuffishSkillsIntegration.isSkillUnlocked(PuffishSkillsIntegration.ARCHER,SkillNodeIds.rangerSpecialisationArrowRainRadiusOne,player))radius+=1;
        int volleys=2;
        if(PuffishSkillsIntegration.isSkillUnlocked(PuffishSkillsIntegration.ARCHER,SkillNodeIds.rangerSpecialisationArrowRainVolleyThree,player))volleys+=3;
        else if(PuffishSkillsIntegration.isSkillUnlocked(PuffishSkillsIntegration.ARCHER,SkillNodeIds.rangerSpecialisationArrowRainVolleyTwo,player))volleys+=2;
        else if(PuffishSkillsIntegration.isSkillUnlocked(PuffishSkillsIntegration.ARCHER,SkillNodeIds.rangerSpecialisationArrowRainVolleyOne,player))volleys+=1;
        int density=25;
        int range=64;
        Vec3d eye=player.getEyePos();
        Vec3d look=player.getRotationVec(1F).normalize();
        Vec3d center=eye.add(look.multiply(range));
        // Prefer a terrain hit when the ray reaches one.
        net.minecraft.util.hit.BlockHitResult hit=player.getWorld().raycast(new net.minecraft.world.RaycastContext(
                eye,center,net.minecraft.world.RaycastContext.ShapeType.COLLIDER,
                net.minecraft.world.RaycastContext.FluidHandling.NONE,player));
        if(hit.getType()!=net.minecraft.util.hit.HitResult.Type.MISS)center=hit.getPos();

        ServerWorld world=player.getServerWorld();
        int limiter=0;
        boolean pointBlank = false;
        Vec3d forward = player.getRotationVec(1F);
        for (LivingEntity enemy : CombatRuntime.nearbyEnemies(player,3)) {
            if (enemy.getPos().subtract(player.getPos()).dotProduct(forward) > 0) {
                pointBlank = true;
                break;
            }
        }
        int elementalCap=pointBlank?4:30;
        for(int x=-radius+1;x<=radius;x++)for(int z=-radius+1;z<=radius;z++)for(int i=0;i<volleys;i++){
            if(player.getRandom().nextInt(100)>=density)continue;
            BlockPos pos=BlockPos.ofFloored(center.x+x,center.y+25+player.getRandom().nextInt(15)*volleys+1,center.z+z);
            if(!world.getBlockState(pos).isAir())continue;
            ArrowEntity arrow=new ArrowRainEntity(world,pos.getX()+0.5,pos.getY(),pos.getZ()+0.5);
            arrow.setOwner(player);
            arrow.pickupType=PersistentProjectileEntity.PickupPermission.CREATIVE_ONLY;
            arrow.setVelocity(0,-0.5,0);
            world.spawnEntity(arrow);
            if(plan.elemental()){
                arrow.addEffect(new net.minecraft.entity.effect.StatusEffectInstance(StatusEffects.SLOWNESS));
                if(limiter<elementalCap){
                    String spell=null;
                    if(player.getRandom().nextInt(100)<5) spell="fire_arrow_rain";
                    else if(player.getRandom().nextInt(100)<15) spell="frost_arrow_rain";
                    else if(player.getRandom().nextInt(100)<25) spell="lightning_arrow_rain";
                    if(spell!=null){
                        InternalSpellRuntime.target(player,"smooth_classes:"+spell,arrow,1F);
                        arrow.setInvisible(true);
                        limiter++;
                    }
                }
            }
        }
        net.minecraft.entity.effect.StatusEffectInstance fx=player.getStatusEffect(SmoothEffects.ARROW_RAIN);
        if(fx!=null){
            int amp=fx.getAmplifier()-1;
            if(amp<0)player.removeStatusEffect(SmoothEffects.ARROW_RAIN);
            else player.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                    SmoothEffects.ARROW_RAIN,fx.getDuration(),amp,false,false,true));
        }
        return true;
    }

    public static ExecutionResult executePortalOfSovereignty(ServerPlayerEntity player) {
        require(player);
        return PortalOfSovereigntyRuntime.cast(player);
    }

    public static ExecutionResult executeElementalArrows(ServerPlayerEntity player) {
        require(player);
        int stacks=4;
        if(PuffishSkillsIntegration.isSkillUnlocked(PuffishSkillsIntegration.ARCHER,SkillNodeIds.rangerSpecialisationElementalArrowsStacksThree,player))stacks+=3;
        else if(PuffishSkillsIntegration.isSkillUnlocked(PuffishSkillsIntegration.ARCHER,SkillNodeIds.rangerSpecialisationElementalArrowsStacksTwo,player))stacks+=2;
        else if(PuffishSkillsIntegration.isSkillUnlocked(PuffishSkillsIntegration.ARCHER,SkillNodeIds.rangerSpecialisationElementalArrowsStacksOne,player))stacks+=1;
        ClassEffectRuntime.apply(player, SmoothEffects.ELEMENTAL_ARROWS, 600, stacks);
        return ExecutionResult.success(1, "elemental_arrows");
    }

}
