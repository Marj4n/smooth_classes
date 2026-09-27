package org.marj4n.smooth_classes.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.SkeletonHorseEntity;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.world.World;

/**
 * Standalone Rider Dread Steed.
 * Visual concept/assets are adapted from Ice and Fire Community Edition under LGPL-3.0.
 * Fluid walking behavior follows Eternal Nether / Bygone Nether's WitherSkeletonHorse.
 */
public final class RiderDreadSteedEntity extends SkeletonHorseEntity {
    public RiderDreadSteedEntity(EntityType<? extends SkeletonHorseEntity> type, World world) {
        super(type, world);
        var jump = getAttributeInstance(EntityAttributes.HORSE_JUMP_STRENGTH);
        if (jump != null) jump.setBaseValue(1.0D);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return AbstractHorseEntity.createBaseHorseAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 35.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.35D);
    }

    public boolean isRiderSurfaceWalking() {
        return isOnGround() && (isTouchingWater() || isInLava());
    }

    @Override
    public boolean doesRenderOnFire() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        RiderFluidWalkPhysics.tick(this, true, true);
        // Lava walking is an intended Rider ability; never show the vanilla
        // burning overlay while this mount is using it.
        if (isInLava() || isOnFire()) extinguish();
    }

    @Override
    public boolean canWalkOnFluid(FluidState state) {
        return state.isIn(FluidTags.WATER)
                || state.isIn(FluidTags.LAVA)
                || super.canWalkOnFluid(state);
    }
}
