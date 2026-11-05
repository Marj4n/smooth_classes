package org.marj4n.smooth_classes.entity;

import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * Minecraft binding for the Smooth Classes surface-motion policy.
 * Historical Eternal Nether / Bygone Nether reference is recorded in THIRD_PARTY_NOTICES.md.
 * This behavior-preserving reimplementation does not assert clean-room authorship or legal clearance.
 */
public final class RiderFluidWalkPhysics {
    private RiderFluidWalkPhysics() {}

    public static void tick(AbstractHorseEntity horse, boolean water, boolean lava) {
        if (!(water && horse.isTouchingWater()) && !(lava && horse.isInLava())) return;
        Sample sample = supportingFluid(horse, water, lava);
        if (sample == null) return;

        var world = horse.getWorld();
        boolean column = matches(world.getFluidState(sample.cell().up()), water, lava);
        double surface = RiderSurfaceMotion.top(sample.cell().getY(),
                sample.fluid().getHeight(world, sample.cell()));
        var mode = RiderSurfaceMotion.mode(true, column, horse.getBoundingBox().minY,
                surface);

        if (mode == RiderSurfaceMotion.Mode.SUPPORTED) horse.setOnGround(true);
        // Keep the supported branch's fall-distance reset before its velocity write.
        if (mode == RiderSurfaceMotion.Mode.SUPPORTED) horse.fallDistance = 0.0F;
        Vec3d velocity = horse.getVelocity();
        if (RiderSurfaceMotion.replacesVelocity(mode, velocity.y)) {
            horse.setVelocity(
                    RiderSurfaceMotion.horizontal(mode, velocity.x),
                    RiderSurfaceMotion.vertical(mode, velocity.y),
                    RiderSurfaceMotion.horizontal(mode, velocity.z));
            horse.velocityModified = true;
        }
        if (mode == RiderSurfaceMotion.Mode.RISING) horse.fallDistance = 0.0F;
    }

    private record Sample(BlockPos cell, FluidState fluid) {}

    private static Sample supportingFluid(AbstractHorseEntity horse, boolean water, boolean lava) {
        BlockPos feet = horse.getBlockPos();
        var world = horse.getWorld();
        FluidState current = world.getFluidState(feet);
        if (matches(current, water, lava)) return new Sample(feet, current);
        BlockPos below = feet.down();
        FluidState lower = world.getFluidState(below);
        return matches(lower, water, lava) ? new Sample(below, lower) : null;
    }

    public static boolean matches(FluidState state, boolean water, boolean lava) {
        return (water && state.isIn(FluidTags.WATER)) || (lava && state.isIn(FluidTags.LAVA));
    }
}
