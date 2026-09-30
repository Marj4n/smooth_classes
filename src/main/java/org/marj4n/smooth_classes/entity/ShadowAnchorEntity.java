package org.marj4n.smooth_classes.entity;
import net.minecraft.entity.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;
import org.marj4n.smooth_classes.content.assassin.runtime.ShadowTechniqueRuntime;
public final class ShadowAnchorEntity extends Entity {
    public ShadowAnchorEntity(EntityType<?> type, World world) { super(type, world); setNoGravity(true); }
    protected void initDataTracker() {}
    protected void writeCustomDataToNbt(NbtCompound nbt) {}
    protected void readCustomDataFromNbt(NbtCompound nbt) { discard(); }
    public void tick() { super.tick(); if (!getWorld().isClient && !ShadowTechniqueRuntime.owns(this)) discard(); }
}
