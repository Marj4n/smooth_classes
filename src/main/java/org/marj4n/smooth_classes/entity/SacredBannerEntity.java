package org.marj4n.smooth_classes.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;
import java.util.UUID;
import org.marj4n.smooth_classes.content.ruler.runtime.SacredBannerRuntime;

public final class SacredBannerEntity extends Entity {
    public UUID owner;
    public int buffs;
    private long expiresAt;
    public SacredBannerEntity(EntityType<?> type, World world) { super(type, world); setNoGravity(true); }
    public void configure(UUID owner, int buffs) {
        this.owner = owner; this.buffs = buffs;
        expiresAt = getWorld().getTime() + 1200;
    }
    public boolean expired() { return getWorld().getTime() >= expiresAt; }
    @Override public void remove(RemovalReason reason) {
        super.remove(reason);
        if (!getWorld().isClient && reason.shouldDestroy()) SacredBannerRuntime.ended(this);
    }
    @Override public void tick() {
        super.tick();
        if (!getWorld().isClient) {
            SacredBannerRuntime.track(this);
            if (isRemoved()) return;
            var caster = owner == null ? null : getServer().getPlayerManager().getPlayer(owner);
            if (caster == null || !caster.isAlive() || caster.getWorld()!=getWorld()
                    || caster.squaredDistanceTo(this)>400 || expired()) { discard(); return; }
            if (age % 10 == 0) {
                var from = getPos().add(0, 0.15, 0);
                var ground = getWorld().raycast(new net.minecraft.world.RaycastContext(from,
                        from.add(0, -0.35, 0), net.minecraft.world.RaycastContext.ShapeType.COLLIDER,
                        net.minecraft.world.RaycastContext.FluidHandling.NONE, this));
                if (ground.getType() != net.minecraft.util.hit.HitResult.Type.BLOCK) {
                    discard();
                    return;
                }
            }

        }
    }
    @Override protected void initDataTracker() {}
    @Override protected void readCustomDataFromNbt(NbtCompound nbt) {
        owner = nbt.containsUuid("Owner") ? nbt.getUuid("Owner") : null;
        buffs = nbt.getInt("Buffs"); expiresAt = nbt.getLong("ExpiresAt");
    }
    @Override protected void writeCustomDataToNbt(NbtCompound nbt) {
        if (owner != null) nbt.putUuid("Owner", owner);
        nbt.putInt("Buffs", buffs); nbt.putLong("ExpiresAt", expiresAt);
    }
}
