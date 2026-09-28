package org.marj4n.smooth_classes.entity;

import java.util.UUID;
import net.minecraft.entity.*;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.*;
import net.minecraft.world.*;
import org.marj4n.smooth_classes.runtime.BloodRainRuntime;

/** A fixed local storm; never changes the world's global weather. */
public final class BloodRainEntity extends Entity {
    private static final TrackedData<Integer> ASCENDANCY_TIER = DataTracker.registerData(
            BloodRainEntity.class, TrackedDataHandlerRegistry.INTEGER
    );

    public UUID owner;
    private long expires;
    private boolean empowered;
    private boolean transcendent;

    public BloodRainEntity(EntityType<?> type, World world) {
        super(type, world);
        setNoGravity(true);
    }

    public void configure(UUID id, int points) {
        owner = id;
        empowered = points >= 30;
        transcendent = points >= 60;
        syncTier();
        expires = getWorld().getTime() + (transcendent ? 3600 : 3000);
    }

    public int ascendancyTier() {
        return dataTracker.get(ASCENDANCY_TIER);
    }

    public double radius() {
        return ascendancyTier() >= 2 ? 96D : 64D;
    }

    public boolean inside(double dx, double dz) {
        double r = radius();
        return dx * dx + dz * dz <= r * r;
    }

    public static boolean exposed(LivingEntity e) {
        World world = e.getWorld();
        if (world.getDimension().hasCeiling()) return false;

        BlockPos head = BlockPos.ofFloored(e.getX(), e.getEyeY(), e.getZ());
        int topY = world.getTopY(Heightmap.Type.MOTION_BLOCKING, head.getX(), head.getZ());

        // A "sky dimension" for Blood Rain is any dimension without a hard ceiling.
        // We intentionally do not use World#isSkyVisible here: dimensions such as
        // The End have an open sky visually but do not use Overworld-style skylight.
        // Heightmap keeps caves/roofs protected while allowing End/custom sky worlds.
        return topY <= head.getY();
    }

    public boolean valid() {
        var server = getServer();
        if (server == null) return false;
        var p = owner == null ? null : server.getPlayerManager().getPlayer(owner);
        return getWorld().getTime() < expires
                && p != null
                && p.isAlive()
                && p.getWorld() == getWorld()
                && inside(p.getX() - getX(), p.getZ() - getZ());
    }

    @Override
    public void tick() {
        super.tick();
        if (!(getWorld() instanceof ServerWorld world)) return;
        BloodRainRuntime.track(this);
        if (isRemoved()) return;
        if (!valid()) {
            discard();
            return;
        }
        if (age % 10 != 0) return;

        double radius = radius();
        var box = new Box(
                getX() - radius, world.getBottomY(), getZ() - radius,
                getX() + radius, world.getTopY(), getZ() + radius
        );
        for (var e : world.getEntitiesByClass(LivingEntity.class, box, t -> t.isAlive() && !t.isSpectator())) {
            if (!inside(e.getX() - getX(), e.getZ() - getZ()) || !exposed(e)) continue;
            if (e.getUuid().equals(owner)) {
                effect(e, StatusEffects.REGENERATION, transcendent ? 3 : empowered ? 2 : 1);
                if (age % 20 == 0) {
                    e.addStatusEffect(new StatusEffectInstance(StatusEffects.SATURATION, 1, 2, false, false, true));
                }
                if (transcendent) {
                    effect(e, StatusEffects.RESISTANCE, 3);
                    effect(e, StatusEffects.STRENGTH, 2);
                } else if (empowered) {
                    effect(e, StatusEffects.RESISTANCE, 2);
                }
            } else if (isOwnedByCaster(e)) {
                continue;
            } else {
                effect(e, StatusEffects.WITHER, transcendent ? 3 : empowered ? 2 : 1);
                effect(e, StatusEffects.HUNGER, empowered ? 4 : 2);
                if (transcendent) {
                    effect(e, StatusEffects.WEAKNESS, 2);
                    effect(e, StatusEffects.SLOWNESS, 1);
                }
            }
        }
    }

    private boolean isOwnedByCaster(LivingEntity e) {
        if (owner == null) return false;
        if (e instanceof net.minecraft.entity.passive.TameableEntity tame
                && owner.equals(tame.getOwnerUuid())) return true;
        if (e instanceof net.minecraft.entity.passive.AbstractHorseEntity horse
                && owner.equals(horse.getOwnerUuid())) return true;
        if (e instanceof net.minecraft.entity.Ownable owned) {
            Entity master = owned.getOwner();
            if (master != null && owner.equals(master.getUuid())) return true;
            if (master instanceof net.minecraft.entity.passive.TameableEntity tame
                    && owner.equals(tame.getOwnerUuid())) return true;
        }
        return false;
    }

    private static void effect(LivingEntity e, StatusEffect fx, int amplifier) {
        int period = fx == StatusEffects.REGENERATION
                ? Math.max(1, 50 >> amplifier)
                : fx == StatusEffects.WITHER ? Math.max(1, 40 >> amplifier) : 20;
        int duration = period * 2 - (int) (e.getWorld().getTime() % period);
        e.addStatusEffect(new StatusEffectInstance(fx, duration, amplifier, false, true, true));
    }

    private void syncTier() {
        int tier = transcendent ? 2 : empowered ? 1 : 0;
        if (dataTracker.get(ASCENDANCY_TIER) != tier) {
            dataTracker.set(ASCENDANCY_TIER, tier);
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);
        if (!getWorld().isClient && reason.shouldDestroy()) BloodRainRuntime.ended(this);
    }

    @Override
    protected void initDataTracker() {
        dataTracker.startTracking(ASCENDANCY_TIER, 0);
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound n) {
        if (owner != null) n.putUuid("Owner", owner);
        n.putLong("Expires", expires);
        n.putBoolean("Empowered", empowered);
        n.putBoolean("Transcendent", transcendent);
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound n) {
        owner = n.containsUuid("Owner") ? n.getUuid("Owner") : null;
        expires = n.getLong("Expires");
        empowered = n.getBoolean("Empowered");
        transcendent = n.getBoolean("Transcendent");
        syncTier();
    }
}
