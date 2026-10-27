package org.marj4n.smooth_classes.runtime;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.spell_engine.entity.SpellProjectile;
import org.joml.Vector3f;
import org.marj4n.smooth_classes.registry.SmoothParticles;

/** Crescent visuals plus temporary damaging fire left on nearby surfaces. */
public final class ArcaneSlashVisuals {
    private static final DustParticleEffect CORE =
            new DustParticleEffect(new Vector3f(0.94F, 0.72F, 1F), 1.8F);
    private ArcaneSlashVisuals() {}

    public static void charge(ServerPlayerEntity player, int remaining) {
        ServerWorld world = player.getServerWorld();
        Vec3d forward = player.getRotationVec(1F);
        Vec3d right = new Vec3d(-forward.z, 0, forward.x).normalize();
        Vec3d hand = player.getEyePos().add(forward.multiply(0.65)).add(right.multiply(0.65)).add(0, -0.65, 0);
        world.spawnParticles(new DustParticleEffect(new Vector3f(.8F, .4F, 1F), .65F),
                hand.x, hand.y, hand.z, 2, .04, .08, .04, 0);
        world.spawnParticles(SmoothParticles.ARCANE_FLAME, hand.x, hand.y, hand.z,
                1, 0.03, 0.06, 0.03, 0.005);
        // Charge stays cosmetic: never ignite the caster's feet.
    }

    public static void projectile(SpellProjectile projectile, boolean empowered) {
        if (!(projectile.getWorld() instanceof ServerWorld world)) return;
        Vec3d forward = projectile.getVelocity().normalize();
        if (forward.lengthSquared() < 0.01) return;
        Vec3d right = forward.crossProduct(new Vec3d(0, 1, 0));
        if (right.lengthSquared() < 0.01) right = new Vec3d(1, 0, 0);
        right = right.normalize();
        Vec3d up = right.crossProduct(forward).normalize();
        double radius = empowered ? 3.5 : 2.6;
        // Curve in the forward/up plane, centered on the projectile trajectory.
        // Symmetric ribbons add thickness without pushing the slash to either side.
        for (int layer = -2; layer <= 2; layer++) {
            for (int i = 0; i <= 40; i++) {
                double angle = -Math.PI * 0.64 + i * Math.PI * 1.28 / 40;
                double r = radius - Math.abs(layer) * 0.08;
                Vec3d point = projectile.getPos()
                        .add(up.multiply(Math.sin(angle) * r))
                        .add(forward.multiply((Math.cos(angle) - 1.0) * r * 0.55))
                        .add(right.multiply(layer * 0.14));
                world.spawnParticles(layer == 0 ? CORE : SmoothParticles.ARCANE_FLAME,
                        point.x, point.y, point.z, 1, 0.035, 0.035, 0.035, 0);
            }
        }
        // Fill between projectile steps so the burning trail is continuous.
        for (int step = 0; step < 3; step++) {
            Vec3d center = projectile.getPos().subtract(projectile.getVelocity().multiply(step / 3.0));
            groundFlame(world, projectile, center);
        }
    }

    private static void groundFlame(ServerWorld world, Entity entity, Vec3d from) {
        var hit = world.raycast(new RaycastContext(from, from.add(0, -4, 0),
                RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, entity));
        if (hit.getType() != HitResult.Type.BLOCK
                || hit.getSide() != net.minecraft.util.math.Direction.UP) return;
        var pos = hit.getBlockPos().up();
        if (entity instanceof SpellProjectile projectile
                && projectile.getOwner() instanceof LivingEntity owner) {
            Vec3d horizontal = new Vec3d(projectile.getVelocity().x, 0,
                    projectile.getVelocity().z);
            if (horizontal.lengthSquared() < 0.0001) return;
            Vec3d offset = Vec3d.ofCenter(pos).subtract(owner.getPos());
            // Start at least two blocks ahead; block bounds must also clear the caster.
            if (offset.dotProduct(horizontal.normalize()) < 2.0
                    || owner.getBoundingBox().expand(0.75).intersects(
                            new net.minecraft.util.math.Box(pos))) return;
        }
        var fire = org.marj4n.smooth_classes.registry.SmoothBlocks.ARCANE_FIRE;
        var existing = world.getBlockState(pos);
        if (existing.isOf(fire)) return; // Never reset an existing fire's lifetime.
        if (!world.getFluidState(pos).isEmpty() || !existing.isReplaceable()) return;
        var state = fire.getDefaultState();
        if (!state.canPlaceAt(world, pos)) return;
        world.setBlockState(pos, state, net.minecraft.block.Block.NOTIFY_ALL);
    }
}
