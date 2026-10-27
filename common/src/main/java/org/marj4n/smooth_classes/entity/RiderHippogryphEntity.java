package org.marj4n.smooth_classes.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.passive.HorseEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Standalone Smooth Classes Rider Hippogryph.
 *
 * Rider flight is intentionally modelled after the rider-flight controller used
 * by Saints & Dragons 1.20.1: smooth yaw following, throttle, sprint/CTRL boost,
 * pitch-directed flight, dive overdrive and momentum-preserving pull-up.
 */
public final class RiderHippogryphEntity extends HorseEntity {
    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0D);
    private static final float HALF_PI = (float) (Math.PI / 2.0D);
    private static final TrackedData<Boolean> FLYING = DataTracker.registerData(
            RiderHippogryphEntity.class, TrackedDataHandlerRegistry.BOOLEAN
    );
    private static final TrackedData<Boolean> BOOSTING = DataTracker.registerData(
            RiderHippogryphEntity.class, TrackedDataHandlerRegistry.BOOLEAN
    );

    private static final double BASE_FLIGHT_SPEED_MULT = 1.65D;
    private static final double BOOST_FLIGHT_SPEED_MULT = 2.65D;
    private static final double FLIGHT_ACCELERATION = 0.24D;
    private static final double DIVE_SPEED_MULTIPLIER = 2.05D;
    private static final double DIVE_ACCELERATION = 0.27D;
    private static final double STRAFE_POWER = 0.45D;
    private static final double NO_INPUT_DRAG = 0.055D;
    private static final double ASCEND_THRUST = 0.090D;
    private static final double DESCEND_THRUST = 0.125D;
    private static final double VERTICAL_SPEED_LIMIT = 0.85D;
    private static final double TAKEOFF_UPWARD = 0.52D;
    private static final int DIVE_EXIT_BOOST_HOLD_TICKS = 50;

    private boolean riderAscend;
    private boolean riderDescend;
    private int lastRiderInputAge = Integer.MIN_VALUE;
    private double riderFlightThrottle;
    private int diveBoostHoldTicks;

    // Client-friendly visual smoothing. These are derived from synced motion/yaw,
    // so they do not need extra packets.
    private float visualFlightPitchRadians;
    private float prevVisualHeadYawDegrees;
    private float visualHeadYawDegrees;
    private float prevVisualHeadPitchDegrees;
    private float visualHeadPitchDegrees;

    // Per-entity wing animation state. Keeping these on the entity avoids abrupt
    // pose changes when FLYING/BOOSTING flips between two ticks.
    private float prevWingFlightBlend;
    private float wingFlightBlend;
    private float prevWingBoostBlend;
    private float wingBoostBlend;
    private float prevWingFlapPhase;
    private float wingFlapPhase;
    private float wingFlapRate = 0.10F;

    public RiderHippogryphEntity(EntityType<? extends HorseEntity> type, World world) {
        super(type, world);
        var jump = getAttributeInstance(EntityAttributes.HORSE_JUMP_STRENGTH);
        if (jump != null) jump.setBaseValue(1.0D);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return AbstractHorseEntity.createBaseHorseAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 35.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.35D);
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(FLYING, false);
        this.dataTracker.startTracking(BOOSTING, false);
    }

    public boolean isRiderFlying() {
        return this.dataTracker.get(FLYING);
    }

    public boolean isRiderBoosting() {
        return this.dataTracker.get(BOOSTING);
    }

    public boolean isRiderSurfaceWalking() {
        return !isRiderFlying() && isOnGround() && (isTouchingWater() || isInLava());
    }


    public float getVisualFlightPitchRadians() {
        return visualFlightPitchRadians;
    }


    public float getVisualHeadYawRadians(float tickDelta) {
        float t = MathHelper.clamp(tickDelta, 0.0F, 1.0F);
        float delta = MathHelper.wrapDegrees(visualHeadYawDegrees - prevVisualHeadYawDegrees);
        return (prevVisualHeadYawDegrees + delta * t) * DEG_TO_RAD;
    }

    public float getVisualHeadPitchRadians(float tickDelta) {
        return MathHelper.lerp(MathHelper.clamp(tickDelta, 0.0F, 1.0F),
                prevVisualHeadPitchDegrees, visualHeadPitchDegrees) * DEG_TO_RAD;
    }

    public float getWingFlightBlend(float tickDelta) {
        return MathHelper.lerp(MathHelper.clamp(tickDelta, 0.0F, 1.0F), prevWingFlightBlend, wingFlightBlend);
    }

    public float getWingBoostBlend(float tickDelta) {
        return MathHelper.lerp(MathHelper.clamp(tickDelta, 0.0F, 1.0F), prevWingBoostBlend, wingBoostBlend);
    }

    public float getWingFlapPhase(float tickDelta) {
        return MathHelper.lerp(MathHelper.clamp(tickDelta, 0.0F, 1.0F), prevWingFlapPhase, wingFlapPhase);
    }

    @Override
    public boolean doesRenderOnFire() {
        return false;
    }

    private void setRiderFlying(boolean flying) {
        if (this.dataTracker.get(FLYING) != flying) this.dataTracker.set(FLYING, flying);
        setNoGravity(flying);
        if (!flying) {
            riderFlightThrottle = 0.0D;
            diveBoostHoldTicks = 0;
        }
        if (flying) fallDistance = 0.0F;
    }

    private void setRiderBoosting(boolean boosting) {
        if (this.dataTracker.get(BOOSTING) != boosting) this.dataTracker.set(BOOSTING, boosting);
    }

    /** Called by the server packet sent from the controlling Rider client. */
    public void acceptRiderFlightInput(ServerPlayerEntity player, boolean ascend, boolean descend, boolean boost) {
        if (getControllingPassenger() != player || !isSaddled()) return;

        boolean freshAscend = ascend && !this.riderAscend;
        this.riderAscend = ascend;
        this.riderDescend = descend;
        this.lastRiderInputAge = this.age;
        setRiderBoosting(boost);

        if (freshAscend && !isRiderFlying()) requestTakeoff();
    }

    private void requestTakeoff() {
        if (!isSaddled() || getControllingPassenger() == null) return;
        // Ground, water/lava surface and short falling recovery are all allowed.
        if (!isOnGround() && !isTouchingWater() && !isInLava() && fallDistance < 1.0F) return;

        setRiderFlying(true);
        Vec3d velocity = getVelocity();
        setVelocity(velocity.x, Math.max(velocity.y, TAKEOFF_UPWARD), velocity.z);
        velocityModified = true;
        fallDistance = 0.0F;
    }

    @Override
    public void tick() {
        super.tick();

        // These Rider evolutions are meant to stand on lava, not visually burn
        // while doing so. Clearing the fire flag on both logical sides also
        // removes the orange burning render overlay.
        if (isInLava() || isOnFire()) extinguish();

        LivingEntity rider = getControllingPassenger();
        if (rider == null || !isSaddled()) {
            riderAscend = false;
            riderDescend = false;
            setRiderBoosting(false);
            setRiderFlying(false);
            RiderFluidWalkPhysics.tick(this, true, true);
            tickFlightVisuals(false);
            tickWingAnimationState();
            return;
        }

        // Input packets are state-change + heartbeat packets. If one disappears,
        // fail safe instead of leaving boost/ascend stuck forever.
        if (!getWorld().isClient() && age - lastRiderInputAge > 10) {
            riderAscend = false;
            riderDescend = false;
            setRiderBoosting(false);
        }

        if (!isRiderFlying()) {
            RiderFluidWalkPhysics.tick(this, true, true);
        } else {
            setNoGravity(true);
            fallDistance = 0.0F;
            rider.fallDistance = 0.0F;

            // Only a solid landing exits flight. Fluid surfaces are handled by
            // RiderFluidWalkPhysics once flight state is cleared explicitly.
            if (isOnGround() && !isTouchingWater() && !isInLava() && getVelocity().y <= 0.0D) {
                setRiderFlying(false);
            }
        }

        tickFlightVisuals(isRiderFlying());
        tickWingAnimationState();
    }

    /**
     * Keep vanilla horse-jump as an alternate takeoff path. The client flight
     * packet is the primary path, but this keeps vanilla horse charging usable.
     */
    @Override
    protected void jump(float strength, Vec3d movementInput) {
        super.jump(strength, movementInput);
        if (getControllingPassenger() instanceof PlayerEntity && isSaddled()) {
            setRiderFlying(true);
            Vec3d v = getVelocity();
            setVelocity(v.x, Math.max(v.y, TAKEOFF_UPWARD), v.z);
            velocityModified = true;
            fallDistance = 0.0F;
        }
    }

    @Override
    protected Vec3d getControlledMovementInput(PlayerEntity controllingPlayer, Vec3d movementInput) {
        if (!isRiderFlying()) return super.getControlledMovementInput(controllingPlayer, movementInput);

        float sideways = controllingPlayer.sidewaysSpeed * 0.30F;
        float forward = controllingPlayer.forwardSpeed * 0.80F;
        if (forward < 0.0F) forward *= 0.50F;
        return new Vec3d(sideways, 0.0D, forward);
    }

    @Override
    protected void tickControlled(PlayerEntity controllingPlayer, Vec3d movementInput) {
        super.tickControlled(controllingPlayer, movementInput);

        if (!isRiderFlying()) return;

        // Simple steering only: the Hippogryph turns toward the rider camera.
        // A/D remains normal strafe input and never adds carve/bank/roll.
        float yawDiff = MathHelper.wrapDegrees(controllingPlayer.getYaw() - getYaw());
        float yawFollow = isRiderBoosting() ? 0.14F : 0.18F;
        float nextYaw = getYaw() + yawDiff * yawFollow;
        setYaw(nextYaw);
        setBodyYaw(nextYaw);
        setHeadYaw(nextYaw);
    }

    @Override
    public void travel(Vec3d movementInput) {
        if (isLogicalSideForUpdatingMovement()
                && isRiderFlying()
                && getControllingPassenger() instanceof PlayerEntity rider) {
            flyWithRider(rider, movementInput);
            return;
        }
        super.travel(movementInput);
    }

    private void flyWithRider(PlayerEntity rider, Vec3d input) {
        double forwardInput = input.z;
        double strafeInput = input.x;
        boolean hasInput = Math.abs(forwardInput) > 0.01D || Math.abs(strafeInput) > 0.01D;

        float pitchRadians = resolveRiderPitchRadians(rider, forwardInput);
        double diveIntensity = diveIntensity(pitchRadians);
        boolean diving = forwardInput > 0.01D && diveIntensity > 0.0D;

        double movementSpeed = Math.max(0.20D, getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED));
        double baseSpeed = movementSpeed * BASE_FLIGHT_SPEED_MULT;
        double sprintSpeed = movementSpeed * BOOST_FLIGHT_SPEED_MULT;
        double flightSpeed = tickThrottle(hasInput, forwardInput, diveIntensity, baseSpeed, sprintSpeed);

        float yawRadians = getYaw() * DEG_TO_RAD;
        double forwardXZ = Math.cos(pitchRadians);
        double forwardX = -Math.sin(yawRadians) * forwardXZ;
        double forwardY = -Math.sin(pitchRadians);
        double forwardZ = Math.cos(yawRadians) * forwardXZ;
        double rightX = Math.cos(yawRadians);
        double rightZ = Math.sin(yawRadians);

        double targetX = forwardX * forwardInput + rightX * strafeInput * STRAFE_POWER;
        double targetY = forwardY * forwardInput * 1.35D;
        double targetZ = forwardZ * forwardInput + rightZ * strafeInput * STRAFE_POWER;
        double length = Math.sqrt(targetX * targetX + targetY * targetY + targetZ * targetZ);

        Vec3d current = getVelocity();
        Vec3d velocity;
        boolean preservingDivePullUp = false;

        if (hasInput && length > 0.01D) {
            Vec3d targetDirection = new Vec3d(targetX / length, targetY / length, targetZ / length);
            preservingDivePullUp = !diving
                    && forwardInput > 0.01D
                    && targetDirection.y > 0.01D
                    && diveBoostHoldTicks > 0;

            if (preservingDivePullUp) {
                double maxDiveSpeed = Math.max(baseSpeed, sprintSpeed) * DIVE_SPEED_MULTIPLIER;
                double preserved = MathHelper.clamp(Math.max(current.length(), flightSpeed), flightSpeed, maxDiveSpeed);
                velocity = steerPreservingSpeed(current, targetDirection, preserved, FLIGHT_ACCELERATION, yawRadians);
            } else {
                Vec3d targetVelocity = targetDirection.multiply(flightSpeed);
                double acceleration = diving ? DIVE_ACCELERATION : FLIGHT_ACCELERATION;
                velocity = new Vec3d(
                        MathHelper.lerp(acceleration, current.x, targetVelocity.x),
                        MathHelper.lerp(acceleration, current.y, targetVelocity.y),
                        MathHelper.lerp(acceleration, current.z, targetVelocity.z)
                );
            }
        } else {
            velocity = current.multiply(1.0D - NO_INPUT_DRAG);
            if (velocity.lengthSquared() < 0.0001D) velocity = Vec3d.ZERO;
        }

        double vertical = velocity.y;
        if (!diving) {
            if (riderAscend && !riderDescend) vertical += ASCEND_THRUST;
            else if (riderDescend && !riderAscend) vertical -= DESCEND_THRUST;
        }

        double verticalLimit = preservingDivePullUp
                ? Math.max(VERTICAL_SPEED_LIMIT, flightSpeed)
                : VERTICAL_SPEED_LIMIT;
        vertical = MathHelper.clamp(vertical, -Math.max(VERTICAL_SPEED_LIMIT, diving ? flightSpeed : 0.0D), verticalLimit);
        velocity = new Vec3d(velocity.x, vertical, velocity.z);

        move(MovementType.SELF, velocity);
        setVelocity(velocity);
        velocityModified = true;
        fallDistance = 0.0F;
        rider.fallDistance = 0.0F;
    }

    private float resolveRiderPitchRadians(PlayerEntity rider, double forwardInput) {
        float pitch = MathHelper.clamp(rider.getPitch(), -75.0F, 75.0F) * DEG_TO_RAD;

        // Space/shift can curve the mount vertically without requiring the camera
        // to stare straight up/down. With W held we bias 45 degrees; hovering uses
        // a steeper 80-degree bias, matching the design of Saints & Dragons.
        if (riderAscend != riderDescend) {
            float biasDeg = forwardInput > 0.01D ? 45.0F : 80.0F;
            float bias = biasDeg * DEG_TO_RAD * (riderAscend ? -1.0F : 1.0F);
            if (Math.signum(pitch) != Math.signum(bias) && Math.abs(pitch) >= Math.abs(bias)) {
                pitch = 0.0F;
            } else {
                pitch += bias;
            }
        }
        return MathHelper.clamp(pitch, -HALF_PI, HALF_PI);
    }

    private double tickThrottle(boolean hasInput, double forwardInput, double diveIntensity,
                                double baseSpeed, double sprintSpeed) {
        double baseTarget = isRiderBoosting() ? sprintSpeed : baseSpeed;
        double maxOverdrive = Math.max(baseTarget, sprintSpeed) * DIVE_SPEED_MULTIPLIER;
        double throttle = MathHelper.clamp(riderFlightThrottle, 0.0D, maxOverdrive);
        boolean forwardActive = forwardInput > 0.01D;
        boolean diving = forwardActive && diveIntensity > 0.0D;

        if (hasInput) {
            if (throttle <= 0.0D) throttle = baseTarget;
            else if (throttle < baseTarget) throttle = MathHelper.lerp(FLIGHT_ACCELERATION, throttle, baseTarget);
        } else {
            riderFlightThrottle = Math.max(0.0D, throttle - 0.035D);
            return riderFlightThrottle;
        }

        if (diving) {
            diveBoostHoldTicks = DIVE_EXIT_BOOST_HOLD_TICKS;
            double situationalCap = MathHelper.lerp(diveIntensity, baseTarget, maxOverdrive);
            double pitchGain = baseTarget * DIVE_ACCELERATION * 0.16D * diveIntensity;
            if (throttle < situationalCap) throttle = Math.min(situationalCap, throttle + pitchGain);
        } else if (throttle > baseTarget) {
            if (forwardActive && diveBoostHoldTicks > 0) {
                diveBoostHoldTicks--;
            } else {
                throttle = Math.max(baseTarget, throttle - baseTarget * 0.035D);
            }
        } else if (!isRiderBoosting() && throttle > 0.0D) {
            throttle = Math.max(0.0D, throttle - baseTarget * 0.025D);
        }

        riderFlightThrottle = MathHelper.clamp(throttle, 0.0D, maxOverdrive);
        return riderFlightThrottle;
    }

    private static double diveIntensity(float pitchRadians) {
        double pitchDegrees = Math.toDegrees(pitchRadians);
        double normalized = (pitchDegrees - 30.0D) / (70.0D - 30.0D);
        return MathHelper.clamp(normalized, 0.0D, 1.0D);
    }

    private static Vec3d steerPreservingSpeed(Vec3d current, Vec3d targetDirection,
                                               double speed, double turnFraction, float yawRadians) {
        Vec3d to = targetDirection.normalize();
        if (current.lengthSquared() < 1.0E-8D) return to.multiply(speed);

        Vec3d from = current.normalize();
        double dot = MathHelper.clamp(from.dotProduct(to), -1.0D, 1.0D);
        if (dot > 0.9999D) return to.multiply(speed);

        Vec3d axis = from.crossProduct(to);
        if (axis.lengthSquared() < 1.0E-8D) {
            Vec3d horizontalForward = new Vec3d(-Math.sin(yawRadians), 0.0D, Math.cos(yawRadians));
            axis = from.crossProduct(horizontalForward);
            if (axis.lengthSquared() < 1.0E-8D) axis = new Vec3d(1.0D, 0.0D, 0.0D);
        }
        axis = axis.normalize();

        double turnRadians = Math.acos(dot) * MathHelper.clamp(turnFraction, 0.0D, 1.0D);
        double cos = Math.cos(turnRadians);
        double sin = Math.sin(turnRadians);
        Vec3d turned = from.multiply(cos)
                .add(axis.crossProduct(from).multiply(sin))
                .add(axis.multiply(axis.dotProduct(from) * (1.0D - cos)));
        return turned.normalize().multiply(speed);
    }

    private void tickFlightVisuals(boolean flying) {
        LivingEntity rider = getControllingPassenger();

        prevVisualHeadYawDegrees = visualHeadYawDegrees;
        prevVisualHeadPitchDegrees = visualHeadPitchDegrees;

        float targetHeadYaw = 0.0F;
        float targetHeadPitch = 0.0F;
        if (rider instanceof PlayerEntity player) {
            targetHeadYaw = MathHelper.clamp(MathHelper.wrapDegrees(player.getYaw() - getYaw()), -28.0F, 28.0F);
            targetHeadPitch = MathHelper.clamp(player.getPitch(), -24.0F, 24.0F);
        }

        // Keep only the useful first-person head damping. No visual bank, no
        // body carve, no rider/camera roll.
        float headYawDelta = MathHelper.wrapDegrees(targetHeadYaw - visualHeadYawDegrees);
        visualHeadYawDegrees += headYawDelta * (flying ? 0.12F : 0.16F);
        visualHeadPitchDegrees = MathHelper.lerp(flying ? 0.11F : 0.15F,
                visualHeadPitchDegrees, targetHeadPitch);

        if (!flying) {
            visualFlightPitchRadians = MathHelper.lerp(0.20F, visualFlightPitchRadians, 0.0F);
            return;
        }

        Vec3d v = getVelocity();
        double horizontal = Math.sqrt(v.x * v.x + v.z * v.z);
        float targetPitch = 0.0F;
        if (v.lengthSquared() > 0.0036D) {
            targetPitch = (float) Math.atan2(v.y, Math.max(0.08D, horizontal));
            targetPitch = MathHelper.clamp(targetPitch, -HALF_PI, HALF_PI);
        }
        visualFlightPitchRadians = MathHelper.lerp(0.24F, visualFlightPitchRadians, targetPitch);
    }

    private void tickWingAnimationState() {
        prevWingFlightBlend = wingFlightBlend;
        prevWingBoostBlend = wingBoostBlend;
        prevWingFlapPhase = wingFlapPhase;

        float flightTarget = isRiderFlying() ? 1.0F : 0.0F;
        float flightResponse = flightTarget > wingFlightBlend ? 0.115F : 0.070F;
        wingFlightBlend = MathHelper.lerp(flightResponse, wingFlightBlend, flightTarget);
        if (Math.abs(wingFlightBlend - flightTarget) < 0.002F) wingFlightBlend = flightTarget;

        float boostTarget = isRiderFlying() && isRiderBoosting() ? 1.0F : 0.0F;
        float boostResponse = boostTarget > wingBoostBlend ? 0.070F : 0.060F;
        wingBoostBlend = MathHelper.lerp(boostResponse, wingBoostBlend, boostTarget);
        if (Math.abs(wingBoostBlend - boostTarget) < 0.002F) wingBoostBlend = boostTarget;

        // Flap cadence itself accelerates/decelerates instead of switching phase
        // formulas instantly. Boost smoothly bleeds into a mostly-static jet glide.
        float targetRate;
        if (wingFlightBlend < 0.02F) {
            targetRate = 0.08F;
        } else {
            targetRate = MathHelper.lerp(wingBoostBlend, 0.34F, 0.115F);
        }
        wingFlapRate = MathHelper.lerp(0.12F, wingFlapRate, targetRate);
        wingFlapPhase += wingFlapRate;
        if (wingFlapPhase > ((float) (Math.PI * 2.0D)) * 512.0F) {
            wingFlapPhase -= ((float) (Math.PI * 2.0D)) * 512.0F;
            prevWingFlapPhase -= ((float) (Math.PI * 2.0D)) * 512.0F;
        }
    }

    @Override
    public boolean canWalkOnFluid(FluidState state) {
        return state.isIn(FluidTags.WATER)
                || state.isIn(FluidTags.LAVA)
                || super.canWalkOnFluid(state);
    }

    @Override
    public boolean handleFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource) {
        return false;
    }
}
