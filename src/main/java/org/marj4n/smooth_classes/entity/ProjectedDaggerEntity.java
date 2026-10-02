package org.marj4n.smooth_classes.entity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Ownable;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.marj4n.smooth_classes.content.archer.runtime.BladeDamageContext;
import org.marj4n.smooth_classes.integration.OptionalCompatRuntime;

/** A temporary, uncollectable copy of the main-hand dagger; no inventory item is consumed. */
public final class ProjectedDaggerEntity extends ProjectileEntity {
    public static final double PORTAL_EMERGENCE_DEPTH = 0.01D;
    public static final int PORTAL_EMERGENCE_TICKS = 3;
    private static final TrackedData<ItemStack> WEAPON = DataTracker.registerData(ProjectedDaggerEntity.class, TrackedDataHandlerRegistry.ITEM_STACK);
    private final Set<UUID> victims = new HashSet<>();
    private float attackDamage;
    private float multiplier = 1;
    private double remaining = 27;
    private int maxVictims = 1;
    public ProjectedDaggerEntity(EntityType<? extends ProjectedDaggerEntity> type, World world) {
        super(type, world);
        setNoGravity(true);
    }
    @Override protected void initDataTracker() { dataTracker.startTracking(WEAPON, ItemStack.EMPTY); }
    public ItemStack weapon() { return dataTracker.get(WEAPON); }
    public void configure(ServerPlayerEntity caster, ItemStack weapon, float damage, float multiplier, double range, int pierce) {
        setOwner(caster);
        ItemStack copy = weapon.copy();
        copy.setCount(1);
        dataTracker.set(WEAPON, copy);
        attackDamage = Math.max(0, damage);
        this.multiplier = multiplier;
        remaining = range;
        maxVictims = pierce;
    }
    public void faceVelocity() {
        Vec3d v = getVelocity();
        setYaw((float)Math.toDegrees(Math.atan2(v.x, v.z)));
        setPitch((float)Math.toDegrees(Math.atan2(v.y, v.horizontalLength())));
        prevYaw = getYaw();
        prevPitch = getPitch();
    }
    public static boolean validTarget(LivingEntity target, ServerPlayerEntity caster) {
        if (!target.isAlive() || target.isSpectator()) return false;
        if (target == caster || target == caster.getVehicle() || caster.isConnectedThroughVehicle(target)) return false;
        if (target instanceof Ownable owned && owned.getOwner() == caster) return false;
        return OptionalCompatRuntime.canHarm(caster, target);
    }
    private Vec3d movementStep(Vec3d velocity) {
        if (age > PORTAL_EMERGENCE_TICKS || velocity.lengthSquared() < 1.0E-8D) return velocity;
        // The blade now starts almost inside the portal mouth instead of deep behind it,
        // then eases outward for a few ticks so it feels like it is being expelled
        // from within the gate rather than spawned in open air.
        double distance = switch (age) {
            case 1 -> 0.10D;
            case 2 -> 0.16D;
            default -> 0.24D;
        };
        return velocity.normalize().multiply(Math.min(distance, velocity.length()));
    }

    public double visualTrailLength(float delta) {
        if (age <= PORTAL_EMERGENCE_TICKS)
            return 0D;
        return Math.min(1.35D, getVelocity().length() * Math.max(0.25D, age - PORTAL_EMERGENCE_TICKS + delta));
    }

    @Override public void tick() {
        super.tick();
        Vec3d start = getPos(), velocity = getVelocity();
        Vec3d step = movementStep(velocity);
        Vec3d end = start.add(step);
        if (getWorld().isClient) {
            setPosition(end);
            faceVelocity();
            return;
        }
        if (!(getOwner() instanceof ServerPlayerEntity caster) || !caster.isAlive() || caster.isRemoved()
                || caster.getWorld() != getWorld() || age > 50 || remaining <= 0 || weapon().isEmpty()) {
            discard(); return;
        }
        if (step.lengthSquared() > remaining * remaining) end = start.add(step.normalize().multiply(remaining));

        // During the reveal phase the dagger is still inside the magic portal. Do
        // not let blocks/entities behind the caster eat the projectile before its
        // tip has visibly crossed the portal surface.
        if (age <= PORTAL_EMERGENCE_TICKS) {
            remaining -= start.distanceTo(end);
            setPosition(end);
            faceVelocity();
            return;
        }

        var block = getWorld().raycast(new RaycastContext(start, end, RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE, this));
        boolean hitBlock = block.getType() != HitResult.Type.MISS;
        if (hitBlock) end = block.getPos();
        record Contact(LivingEntity entity, Vec3d point) {}
        var contacts = new ArrayList<Contact>();
        for (LivingEntity target : getWorld().getEntitiesByClass(LivingEntity.class,
                new Box(start, end).expand(.3), e -> validTarget(e, caster) && !victims.contains(e.getUuid()))) {
            Box hitbox = target.getBoundingBox().expand(.15);
            var hit = hitbox.contains(start) ? java.util.Optional.of(start) : hitbox.raycast(start, end);
            hit.ifPresent(point -> contacts.add(new Contact(target, point)));
        }
        contacts.sort(Comparator.comparingDouble(c -> start.squaredDistanceTo(c.point())));
        for (Contact hit : contacts) {
            victims.add(hit.entity().getUuid());
            strike(caster, hit.entity(), hit.point());
            if (victims.size() >= maxVictims) { setPosition(hit.point()); discard(); return; }
        }
        remaining -= start.distanceTo(end);
        setPosition(end);
        if (hitBlock || remaining <= .001) discard();
    }
    private void strike(ServerPlayerEntity caster, LivingEntity target, Vec3d point) {
        ItemStack weapon = weapon();
        float damage = (attackDamage + EnchantmentHelper.getAttackDamage(weapon, target.getGroup())) * multiplier;
        int fire = EnchantmentHelper.getLevel(Enchantments.FIRE_ASPECT, weapon);
        int oldFireTicks = target.getFireTicks();
        int oldRegen = target.timeUntilRegen;
        Vec3d oldVelocity = target.getVelocity();
        BladeDamageContext.Hit previous = BladeDamageContext.enter(caster, weapon);
        boolean landed;
        try {
            target.timeUntilRegen = 0;
            if (fire > 0 && !target.isOnFire()) target.setOnFireFor(1);
            landed = target.damage(caster.getDamageSources().playerAttack(caster), damage);
            if (landed) {
                target.setVelocity(oldVelocity);
                int knockback = EnchantmentHelper.getLevel(Enchantments.KNOCKBACK, weapon);
                if (knockback > 0) target.takeKnockback(knockback * .3, -getVelocity().x, -getVelocity().z);
                if (fire > 0) target.setOnFireFor(fire * 4);
                // Standard enchantment hooks also include modded enchantments implementing these callbacks.
                EnchantmentHelper.get(weapon).forEach((enchantment, level) -> enchantment.onTargetDamaged(caster, target, level));
                EnchantmentHelper.onUserDamaged(target, caster);
                // Run item-specific successful-hit behavior on the disposable copy, never the real dagger.
                weapon.copy().postHit(target, caster);
            } else if (fire > 0) {
                target.setFireTicks(oldFireTicks);
            }
        } finally {
            target.timeUntilRegen = Math.max(oldRegen, target.timeUntilRegen);
            BladeDamageContext.restore(previous);
        }
        if (landed) ((ServerWorld)getWorld()).spawnParticles(ParticleTypes.CRIT,
                point.x, point.y, point.z, 5, .12, .12, .12, .05);
    }
    @Override protected void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        // Transient spell projectiles must not resume after chunk reload or restart.
    }
    @Override protected void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        discard();
    }
}
