package org.marj4n.smooth_classes.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.marj4n.smooth_classes.content.archer.runtime.ArcherSpecialRuntime;
import org.marj4n.smooth_classes.content.archer.runtime.PortalOfSovereigntyRuntime;

/** Non-collidable anchor entity for Portal of Sovereignty gates. */
public final class BladePortalEntity extends Entity {
    private static final TrackedData<Integer> CASTER = DataTracker.registerData(BladePortalEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> SIDE = DataTracker.registerData(BladePortalEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Float> OPENING = DataTracker.registerData(BladePortalEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> LOCAL_BACK = DataTracker.registerData(BladePortalEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> LOCAL_LATERAL = DataTracker.registerData(BladePortalEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> LOCAL_VERTICAL = DataTracker.registerData(BladePortalEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> LOCAL_SCALE = DataTracker.registerData(BladePortalEntity.class, TrackedDataHandlerRegistry.FLOAT);

    public BladePortalEntity(EntityType<?> type, World world) {
        super(type, world);
        setNoGravity(true);
    }

    @Override
    protected void initDataTracker() {
        dataTracker.startTracking(CASTER, -1);
        dataTracker.startTracking(SIDE, 0);
        dataTracker.startTracking(OPENING, 0F);
        dataTracker.startTracking(LOCAL_BACK, 1.55F);
        dataTracker.startTracking(LOCAL_LATERAL, 0F);
        dataTracker.startTracking(LOCAL_VERTICAL, 0.55F);
        dataTracker.startTracking(LOCAL_SCALE, 1F);
    }

    public void setCaster(LivingEntity caster) { dataTracker.set(CASTER, caster.getId()); }
    public void setSide(int side) { dataTracker.set(SIDE, Integer.compare(side, 0)); }
    public void setOpening(float value) { dataTracker.set(OPENING, Math.max(0F, Math.min(1F, value))); }
    public float opening(float delta) { return Math.min(1F, dataTracker.get(OPENING) + delta / 16F); }
    public int side() { return dataTracker.get(SIDE); }

    public void setPlacement(double back, double lateral, double vertical) {
        dataTracker.set(LOCAL_BACK, (float) back);
        dataTracker.set(LOCAL_LATERAL, (float) lateral);
        dataTracker.set(LOCAL_VERTICAL, (float) vertical);
    }

    public float localBack() { return dataTracker.get(LOCAL_BACK); }
    public float localLateral() { return dataTracker.get(LOCAL_LATERAL); }
    public float localVertical() { return dataTracker.get(LOCAL_VERTICAL); }
    public void setScale(float scale) { dataTracker.set(LOCAL_SCALE, Math.max(0.35F, Math.min(1.45F, scale))); }
    public float scale() { return dataTracker.get(LOCAL_SCALE); }

    public LivingEntity caster() {
        Entity e = getWorld().getEntityById(dataTracker.get(CASTER));
        return e instanceof LivingEntity living ? living : null;
    }

    public static Vec3d anchor(LivingEntity caster, Vec3d position, float yaw, int side) {
        return anchor(caster, position, yaw, side == 0 ? 1.55D : 0.72D, -side * 2.6D, 0.55D);
    }

    public static Vec3d anchor(LivingEntity caster, Vec3d position, float yaw,
                               double back, double lateral, double vertical) {
        double a = Math.toRadians(yaw);
        return position.add(Math.sin(a) * back + Math.cos(a) * lateral,
                caster.getStandingEyeHeight() + vertical,
                -Math.cos(a) * back + Math.sin(a) * lateral);
    }

    public void followCaster() {
        LivingEntity caster = caster();
        if (caster == null) return;
        setPosition(anchor(caster, caster.getPos(), caster.getYaw(), localBack(), localLateral(), localVertical()));
        setYaw(caster.getYaw());
        setPitch(caster.getPitch());
    }

    @Override
    public void tick() {
        super.tick();
        followCaster();
        if (!getWorld().isClient) {
            if (caster() == null || !caster().isAlive() || age > 100
                    || (!PortalOfSovereigntyRuntime.owns(this) && !ArcherSpecialRuntime.ownsPortal(this))) discard();
            return;
        }
    }

    @Override protected void writeCustomDataToNbt(NbtCompound nbt) {}
    @Override protected void readCustomDataFromNbt(NbtCompound nbt) { discard(); }
}
