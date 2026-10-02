package org.marj4n.smooth_classes.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Pure visual entity used by Lancer Impaling Volley.
 *
 * Unlike TridentEntity this stores and syncs the exact ItemStack the player was
 * holding, allowing the client renderer to draw modded spear/lance models.
 */
public final class LancerImpaleEntity extends Entity {
    private static final TrackedData<ItemStack> STACK =
            DataTracker.registerData(LancerImpaleEntity.class, TrackedDataHandlerRegistry.ITEM_STACK);
    private static final TrackedData<Boolean> GROUND_SPIKE =
            DataTracker.registerData(LancerImpaleEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

    public LancerImpaleEntity(EntityType<?> type, World world) {
        super(type, world);
        setNoGravity(true);
    }

    @Override
    protected void initDataTracker() {
        dataTracker.startTracking(STACK, ItemStack.EMPTY);
        dataTracker.startTracking(GROUND_SPIKE, false);
    }

    public ItemStack getVisualStack() {
        return dataTracker.get(STACK);
    }

    public void setVisualStack(ItemStack stack) {
        ItemStack visual = stack == null ? ItemStack.EMPTY : stack.copy();
        if (!visual.isEmpty()) visual.setCount(1);
        dataTracker.set(STACK, visual);
    }

    public void setGroundSpike(boolean groundSpike) { dataTracker.set(GROUND_SPIKE, groundSpike); }
    public boolean isGroundSpike() { return dataTracker.get(GROUND_SPIKE); }

    @Override
    public void tick() {
        super.tick();
        setNoGravity(true);
        setVelocity(Vec3d.ZERO);
        if (!getWorld().isClient && age > (isGroundSpike() ? 34 : 40)) discard();
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        ItemStack stack = getVisualStack();
        if (!stack.isEmpty()) {
            nbt.put("VisualStack", stack.writeNbt(new NbtCompound()));
        }
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        if (nbt.contains("VisualStack")) {
            setVisualStack(ItemStack.fromNbt(nbt.getCompound("VisualStack")));
        }
    }
}
