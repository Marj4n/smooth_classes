package org.marj4n.smooth_classes.runtime;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.marj4n.smooth_classes.SmoothClasses;
import org.marj4n.smooth_classes.registry.SmoothEntities;
import java.util.UUID;

public final class TormentRuntime {
    public static final int DURATION=3600;
    private TormentRuntime() {}
    public static long now(ServerWorld world) { return world.getServer().getOverworld().getTime(); }
    public static final float BURN_COEFFICIENT = .35F;
    public static final float BLAST_COEFFICIENT = .7F;
    public static float scaledDamage(double power, float coefficient) {
        return (float)Math.max(0D, power * coefficient);
    }
    public static void hurt(LivingEntity target, UUID owner, float snapshotDamage, float coefficient) {
        var world=(ServerWorld)target.getWorld();
        var caster=owner==null?null:world.getServer().getPlayerManager().getPlayer(owner);
        // Online casters use current equipment, buffs, spell crit and target vulnerability on every hit.
        // Offline casters retain their saved non-critical cast power until the original expiry.
        float damage = snapshotDamage;
        if(caster!=null) {
            var power=net.spell_power.api.SpellPower.getSpellPower(net.spell_power.api.SpellSchools.FIRE,caster);
            var vulnerability=net.spell_power.api.SpellPower.getVulnerability(target,net.spell_power.api.SpellSchools.FIRE);
            damage=scaledDamage(power.randomValue(vulnerability),coefficient);
        }
        if(damage<=0)return;
        target.damage(new TormentDamageSource(world,caster),damage);
    }
    public static ExecutionResult cast(ServerPlayerEntity p) {
        var world=p.getServerWorld();
        Vec3d eye=p.getEyePos(), end=eye.add(p.getRotationVec(1).multiply(24));
        var hit=world.raycast(new RaycastContext(eye,end,RaycastContext.ShapeType.COLLIDER,RaycastContext.FluidHandling.NONE,p));
        Vec3d point=hit.getPos(); double nearest=eye.squaredDistanceTo(point);
        // The first living target under the crosshair wins, with solid blocks occluding it.
        for(var target:world.getEntitiesByClass(LivingEntity.class,p.getBoundingBox().stretch(end.subtract(eye)).expand(1),e->e!=p&&e.isAlive()&&!e.isSpectator())) {
            var intersection=target.getBoundingBox().expand(.15).raycast(eye,point);
            if(intersection.isPresent() && eye.squaredDistanceTo(intersection.get())<nearest) {
                nearest=eye.squaredDistanceTo(intersection.get());point=intersection.get();
            }
        }
        Vec3d from=point.add(0,2,0);
        var floor=world.raycast(new RaycastContext(from,point.add(0,-12,0),RaycastContext.ShapeType.COLLIDER,RaycastContext.FluidHandling.NONE,p));
        if(floor.getType()!=HitResult.Type.BLOCK) return ExecutionResult.failure("Aim at a target or ground within 24 blocks.");
        var field=SmoothEntities.TORMENT_FIELD.create(world);
        if(field==null)return ExecutionResult.failure("Cannot summon Torment here.");
        field.setPosition(floor.getPos().add(0,.04,0));
        field.configure(p.getUuid(),now(world),scaledDamage(net.spell_power.api.SpellPower.getSpellPower(net.spell_power.api.SpellSchools.FIRE,p).baseValue(),BURN_COEFFICIENT),scaledDamage(net.spell_power.api.SpellPower.getSpellPower(net.spell_power.api.SpellSchools.FIRE,p).baseValue(),BLAST_COEFFICIENT));
        if(!world.spawnEntity(field))return ExecutionResult.failure("Cannot summon Torment here.");
        return ExecutionResult.success(1,"torment");
    }
}
