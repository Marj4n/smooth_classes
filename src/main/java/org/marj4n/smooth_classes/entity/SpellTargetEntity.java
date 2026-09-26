package org.marj4n.smooth_classes.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;

/** Short lived invisible target for spells delivered to a block position. */
public final class SpellTargetEntity extends Entity {
    public SpellTargetEntity(EntityType<?> type, World world) { super(type, world); }
    @Override public void baseTick() {
        super.baseTick();
        setNoGravity(true);
        if (!getWorld().isClient && age > 120) discard();
    }
    @Override public boolean shouldRender(double distance) { return false; }
    @Override protected void initDataTracker() { }
    @Override protected void readCustomDataFromNbt(NbtCompound nbt) { }
    @Override protected void writeCustomDataToNbt(NbtCompound nbt) { }
}
