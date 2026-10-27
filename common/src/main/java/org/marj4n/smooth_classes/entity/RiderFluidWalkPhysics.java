package org.marj4n.smooth_classes.entity;

import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * Fluid-surface horse physics adapted from Eternal Nether / Bygone Nether's
 * WitherSkeletonHorse floatHorse() behavior.
 *
 * The important part is not only canWalkOnFluid(): once the horse reaches the
 * top fluid block it is explicitly treated as on-ground. While submerged it
 * receives a small buoyancy impulse until it reaches that surface.
 */
public final class RiderFluidWalkPhysics {
    private RiderFluidWalkPhysics() {}

    public static void tick(AbstractHorseEntity horse, boolean water, boolean lava) {
        boolean inWater = water && horse.isTouchingWater();
        boolean inLava = lava && horse.isInLava();
        if (!inWater && !inLava) return;

        BlockPos pos = horse.getBlockPos();
        FluidState state = horse.getWorld().getFluidState(pos);

        // getBlockPos() can already point at the block just above the liquid when
        // the feet are exactly on the surface. Check one block down as well.
        if (!matches(state, water, lava)) {
            BlockPos down = pos.down();
            FluidState downState = horse.getWorld().getFluidState(down);
            if (matches(downState, water, lava)) {
                pos = down;
                state = downState;
            }
        }

        if (!matches(state, water, lava)) return;

        FluidState above = horse.getWorld().getFluidState(pos.up());
        double height = state.getHeight(horse.getWorld(), pos);
        if (height <= 0.0D) height = 1.0D;
        double surfaceY = pos.getY() + height;

        // Eternal Nether's floatHorse() marks the horse as grounded when it has
        // reached the top fluid block. That makes ridden horse movement use the
        // normal grounded branch instead of swimming movement.
        boolean topFluidBlock = !matches(above, water, lava);
        boolean feetAtSurface = horse.getBoundingBox().minY >= surfaceY - 0.42D;

        if (topFluidBlock && feetAtSurface) {
            horse.setOnGround(true);
            horse.fallDistance = 0.0F;

            Vec3d v = horse.getVelocity();
            if (v.y < 0.0D) {
                horse.setVelocity(v.x, 0.0D, v.z);
                horse.velocityModified = true;
            }
            return;
        }

        // Same idea as Eternal Nether's:
        // velocity = velocity * 0.5 + (0, 0.05, 0)
        Vec3d v = horse.getVelocity();
        horse.setVelocity(v.multiply(0.5D).add(0.0D, 0.05D, 0.0D));
        horse.velocityModified = true;
        horse.fallDistance = 0.0F;
    }

    public static boolean matches(FluidState state, boolean water, boolean lava) {
        return (water && state.isIn(FluidTags.WATER))
                || (lava && state.isIn(FluidTags.LAVA));
    }
}
