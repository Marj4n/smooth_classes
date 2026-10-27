package org.marj4n.smooth_classes.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityGroup;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.pathing.BirdNavigation;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.entity.ai.goal.AttackWithOwnerGoal;
import net.minecraft.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.TrackOwnerAttackerGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.EntityView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.marj4n.smooth_classes.content.avenger.runtime.AvengerMinionGameplay;

/**
 * Shared owner/lifecycle/AI contract for every Avenger summon.
 *
 * Puffish Skills remains authoritative for talents. This entity only owns
 * physical minion behaviour and delegates talent effects to AvengerMinionGameplay.
 */
public abstract class AvengerMinionEntity extends TameableEntity {
    public static final int LIFESPAN_TICKS = 2400;


    protected AvengerMinionEntity(EntityType<? extends TameableEntity> type, World world) {
        super(type, world);
        this.moveControl = new DirectionalFlightMoveControl(this, 1, true);
        setNoGravity(true);
    }

    @Override
    protected void initGoals() {
        goalSelector.add(1, new FollowOwnerGoal(this, 1.0D, this instanceof WraithEntity ? 8.0F : 20.0F, this instanceof WraithEntity ? 12.0F : 2.0F, true));
        if (!(this instanceof WraithEntity)) goalSelector.add(3, new MeleeAttackGoal(this, 1.0D, true));
        goalSelector.add(4, new WanderAroundFarGoal(this, 1.0D));

        if (!(this instanceof WraithEntity)) {
            targetSelector.add(1, new TrackOwnerAttackerGoal(this));
            targetSelector.add(2, new AttackWithOwnerGoal(this));
            targetSelector.add(3, new RevengeGoal(this));
            targetSelector.add(4, new ActiveTargetGoal<>(this, HostileEntity.class, false));
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (!hasNoGravity()) {
            this.moveControl = new DirectionalFlightMoveControl(this, 1, true);
        setNoGravity(true);
        }

        if (getWorld().isClient()) {
            return;
        }

        Entity ownerEntity = getOwner();

        if (age > LIFESPAN_TICKS || (age > 120 && (ownerEntity == null || !ownerEntity.isAlive()))) {
            discard();
            return;
        }

        if (ownerEntity instanceof ServerPlayerEntity owner) {
            AvengerMinionGameplay.tickMinion(owner, this);
            AvengerMinionGameplay.trySpecialAttack(owner, this);
        }
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        Entity attacker = source.getAttacker();
        if (attacker != null && attacker.equals(getOwner())) {
            return false;
        }
        return super.damage(source, amount);
    }

    @Override
    public boolean tryAttack(Entity target) {
        if (getMoveControl() instanceof DirectionalFlightMoveControl flight) flight.onAttack();
        if (target.equals(getOwner())) {
            return false;
        }

        target.timeUntilRegen = 0;
        boolean success = super.tryAttack(target);

        if (success && !getWorld().isClient() && getOwner() instanceof ServerPlayerEntity owner) {
            AvengerMinionGameplay.onMinionAttack(owner, this, target);
        }

        return success;
    }

    @Override
    public void onDeath(DamageSource source) {
        if (!getWorld().isClient() && getOwner() instanceof ServerPlayerEntity owner) {
            AvengerMinionGameplay.onMinionDeath(owner, this);
        }
        super.onDeath(source);
    }

    @Override
    public EntityGroup getGroup() {
        return EntityGroup.UNDEAD;
    }

    @Override
    public boolean handleFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource) {
        return false;
    }


    @Override public void tickMovement() {
        super.tickMovement();
        Vec3d v=getVelocity();
        if(!v.equals(Vec3d.ZERO)){
            float yaw=(float)(MathHelper.atan2(v.z,v.x)*(180.0/Math.PI))-90F;
            float pitch=(float)(-(MathHelper.atan2(v.y,Math.sqrt(v.x*v.x+v.z*v.z))*(180.0/Math.PI)));
            setYaw(yaw); bodyYaw=yaw; setPitch(pitch);
        }
    }

    @Override protected EntityNavigation createNavigation(World world) {
        BirdNavigation nav=new BirdNavigation(this,world);
        nav.setCanPathThroughDoors(false); nav.setCanSwim(false); nav.setCanEnterOpenDoors(false); return nav;
    }

    public abstract double healthMultiplier();
    public abstract double attackMultiplier();
    public abstract boolean isGreater();

    @Nullable
    @Override
    public PassiveEntity createChild(ServerWorld world, PassiveEntity entity) {
        return null;
    }

    // Yarn 1.20.1 Tameable bridge.
    @Override
    public EntityView method_48926() {
        return getWorld();
    }
}
