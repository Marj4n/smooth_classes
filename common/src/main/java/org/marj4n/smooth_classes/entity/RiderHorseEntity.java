package org.marj4n.smooth_classes.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.passive.HorseEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.world.World;

/** Smooth Classes' base Rider horse. */
public final class RiderHorseEntity extends HorseEntity {
    private static final TrackedData<Boolean> WATER_STRIDE = DataTracker.registerData(
            RiderHorseEntity.class, TrackedDataHandlerRegistry.BOOLEAN
    );

    public RiderHorseEntity(EntityType<? extends HorseEntity> type, World world) {
        super(type, world);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return AbstractHorseEntity.createBaseHorseAttributes();
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(WATER_STRIDE, false);
    }

    public void setWaterStride(boolean enabled) {
        if (this.dataTracker.get(WATER_STRIDE) != enabled) {
            this.dataTracker.set(WATER_STRIDE, enabled);
        }
    }

    public boolean hasWaterStride() {
        return this.dataTracker.get(WATER_STRIDE);
    }

    public boolean isRiderSurfaceWalking() {
        return hasWaterStride() && isOnGround() && isTouchingWater();
    }

    @Override
    public void tick() {
        super.tick();
        if (hasWaterStride()) {
            RiderFluidWalkPhysics.tick(this, true, false);
        }
    }

    @Override
    public boolean canWalkOnFluid(FluidState state) {
        return (hasWaterStride() && state.isIn(FluidTags.WATER))
                || super.canWalkOnFluid(state);
    }
}
