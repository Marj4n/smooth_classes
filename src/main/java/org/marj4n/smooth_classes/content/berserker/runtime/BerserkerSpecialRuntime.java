package org.marj4n.smooth_classes.content.berserker.runtime;

import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;
import org.marj4n.smooth_classes.SmoothClasses;
import org.marj4n.smooth_classes.content.berserker.BerserkerClass;
import org.marj4n.smooth_classes.content.berserker.BerserkerContent;
import org.marj4n.smooth_classes.effects.SmoothEffects;
import org.marj4n.smooth_classes.network.SmoothClassesNetworking;
import org.marj4n.smooth_classes.runtime.AbilityCooldowns;
import org.marj4n.smooth_classes.runtime.AbilityRuntime;
import org.marj4n.smooth_classes.runtime.ClassEffectRuntime;
import org.marj4n.smooth_classes.runtime.ExecutionResult;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Berserker H-special.
 *
 * Hold H to complete a short roar/charge. Releasing early applies a fixed 4s
 * cancellation cooldown. Once completed, Crimson Revenant takes movement control
 * away from the player and hunts nearby non-player living mobs for 30s.
 */
public final class BerserkerSpecialRuntime {
    public static final Identifier SPECIAL_ID = SmoothClasses.id("crimson_revenant");
    public static final String HUD_ABILITY = "crimson_revenant";
    public static final int CHARGE_TICKS = 20;
    private static final int BASE_DURATION_TICKS = 30 * 20;
    private static final int BASE_COOLDOWN_TICKS = 90 * 20;
    private static final int CANCEL_COOLDOWN_TICKS = 4 * 20;
    private static final int HOLD_HEARTBEAT_GRACE = 12;

    private static final Map<UUID, Integer> HELD_AT = new HashMap<>();
    private static final Map<UUID, Integer> CHARGE_STARTED_AT = new HashMap<>();
    private static final Map<UUID, Long> CANCEL_COOLDOWN_UNTIL = new HashMap<>();
    private static final Set<UUID> CHARGING = new HashSet<>();
    /** Short-lived side preference keeps obstacle avoidance from jittering left/right every tick. */
    private static final Map<UUID, Integer> AVOID_SIDE = new HashMap<>();
    private static final Map<UUID, Integer> AVOID_UNTIL = new HashMap<>();

    private static final DustParticleEffect SMALL_RED =
            new DustParticleEffect(new Vector3f(1.0F, 0.10F, 0.10F), 1.15F);
    private static final DustParticleEffect LARGE_RED =
            new DustParticleEffect(new Vector3f(0.82F, 0.0F, 0.0F), 2.0F);

    private BerserkerSpecialRuntime() {}

    /** Receives the client hold heartbeat. */
    public static void hold(ServerPlayerEntity player, boolean held) {
        if (held) {
            HELD_AT.put(player.getUuid(), player.getServer().getTicks());
            return;
        }
        HELD_AT.remove(player.getUuid());
        if (CHARGING.contains(player.getUuid())) cancelCharge(player, true);
    }

    public static boolean isHeld(ServerPlayerEntity player) {
        Integer tick = HELD_AT.get(player.getUuid());
        return tick != null && player.getServer().getTicks() - tick <= HOLD_HEARTBEAT_GRACE;
    }

    public static boolean isCharging(ServerPlayerEntity player) {
        return CHARGING.contains(player.getUuid());
    }

    public static boolean isActive(ServerPlayerEntity player) {
        return player.hasStatusEffect(SmoothEffects.CRIMSON_REVENANT);
    }

    public static int cooldownTotalTicks(ServerPlayerEntity player) {
        return AbilityCooldowns.adjustedTicks(player, BASE_COOLDOWN_TICKS);
    }

    public static int hudCooldownTotalTicks(ServerPlayerEntity player) {
        long now = player.getServerWorld().getTime();
        Long cancelUntil = CANCEL_COOLDOWN_UNTIL.get(player.getUuid());
        if (cancelUntil != null) {
            if (cancelUntil > now) return CANCEL_COOLDOWN_TICKS;
            CANCEL_COOLDOWN_UNTIL.remove(player.getUuid());
        }
        return cooldownTotalTicks(player);
    }

    public static long cooldownRemainingTicks(ServerPlayerEntity player) {
        return AbilityCooldowns.remainingTicks(player, SPECIAL_ID);
    }

    /** Starts the charge after the client's hold=true packet has arrived. */
    public static ExecutionResult activate(ServerPlayerEntity player) {
        if (!AbilityRuntime.isClass(player, BerserkerClass.ID)) {
            return ExecutionResult.failure("Only Berserker can use Crimson Revenant.");
        }
        if (isActive(player)) return ExecutionResult.failure("Crimson Revenant is already active.");
        if (isCharging(player)) return ExecutionResult.failure("Crimson Revenant is already charging.");
        if (!isHeld(player)) return ExecutionResult.failure("Hold H until Crimson Revenant finishes charging.");

        long remaining = cooldownRemainingTicks(player);
        if (remaining > 0) {
            return ExecutionResult.failure("Crimson Revenant can be used again in "
                    + (int) Math.ceil(remaining / 20.0D) + "s.");
        }

        UUID id = player.getUuid();
        CANCEL_COOLDOWN_UNTIL.remove(id);
        CHARGING.add(id);
        CHARGE_STARTED_AT.put(id, player.getServer().getTicks());
        // Long safety duration. The runtime removes it on success/cancel.
        ClassEffectRuntime.apply(player, SmoothEffects.CRIMSON_REVENANT_CHARGE, CHARGE_TICKS + 40, 0);
        player.setSprinting(false);
        player.setVelocity(0.0D, player.getVelocity().y, 0.0D);
        player.velocityModified = true;
        SmoothClassesNetworking.sendChargeState(player, true, false, HUD_ABILITY, 0, CHARGE_TICKS);
        player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.ENTITY_RAVAGER_ROAR,
                SoundCategory.PLAYERS, 1.15F, 0.78F);
        return ExecutionResult.success(1, "Charging Crimson Revenant");
    }

    public static void tickCharge(ServerPlayerEntity player) {
        UUID id = player.getUuid();
        if (!CHARGING.contains(id)) return;

        if (!player.isAlive() || player.isSpectator()
                || !AbilityRuntime.isClass(player, BerserkerClass.ID)
                || !isHeld(player)) {
            cancelCharge(player, true);
            return;
        }

        player.setSprinting(false);
        player.setVelocity(0.0D, player.getVelocity().y, 0.0D);
        player.velocityModified = true;

        Integer started = CHARGE_STARTED_AT.get(id);
        if (started == null) {
            cancelCharge(player, true);
            return;
        }
        int elapsed = Math.max(0, player.getServer().getTicks() - started);
        chargeParticles(player, elapsed);
        if (elapsed >= CHARGE_TICKS) beginFrenzy(player);
    }

    /** Called if some other mechanic removes the charge marker. */
    public static void chargeEffectRemoved(ServerPlayerEntity player) {
        if (CHARGING.contains(player.getUuid())) cancelCharge(player, true);
    }

    private static void cancelCharge(ServerPlayerEntity player, boolean applyCooldown) {
        UUID id = player.getUuid();
        if (!CHARGING.remove(id)) return;
        CHARGE_STARTED_AT.remove(id);
        player.removeStatusEffect(SmoothEffects.CRIMSON_REVENANT_CHARGE);
        SmoothClassesNetworking.sendChargeState(player, false, false, HUD_ABILITY, 0, CHARGE_TICKS);
        if (applyCooldown) {
            AbilityCooldowns.start(player, SPECIAL_ID, CANCEL_COOLDOWN_TICKS);
            CANCEL_COOLDOWN_UNTIL.put(id, player.getServerWorld().getTime() + CANCEL_COOLDOWN_TICKS);
        }
        player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.BLOCK_FIRE_EXTINGUISH,
                SoundCategory.PLAYERS, 0.55F, 0.72F);
        SmoothClassesNetworking.sendAbilityState(player);
    }

    private static void beginFrenzy(ServerPlayerEntity player) {
        UUID id = player.getUuid();
        if (!CHARGING.remove(id)) return;
        CHARGE_STARTED_AT.remove(id);
        CANCEL_COOLDOWN_UNTIL.remove(id);
        player.removeStatusEffect(SmoothEffects.CRIMSON_REVENANT_CHARGE);
        SmoothClassesNetworking.sendChargeState(player, false, true, HUD_ABILITY, CHARGE_TICKS, CHARGE_TICKS);
        AbilityCooldowns.start(player, SPECIAL_ID, cooldownTotalTicks(player));

        int duration = BASE_DURATION_TICKS;
        if (AbilityRuntime.hasTalent(player, BerserkerContent.CRIMSON_REVENANT_ENDURING.id())) duration += 8 * 20;
        if (AbilityRuntime.hasTalent(player, BerserkerContent.CRIMSON_REVENANT_MASSACRE.id())) duration += 4 * 20;

        ClassEffectRuntime.apply(player, SmoothEffects.CRIMSON_REVENANT, duration, 0);
        player.heal(Math.min(12.0F, player.getMaxHealth() * 0.30F));
        player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.ENTITY_WARDEN_ROAR,
                SoundCategory.PLAYERS, 1.0F, 1.10F);

        if (player.getWorld() instanceof ServerWorld world) {
            world.spawnParticles(LARGE_RED, player.getX(), player.getBodyY(0.65D), player.getZ(),
                    54, 0.85D, 0.65D, 0.85D, 0.015D);
            world.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, player.getX(), player.getBodyY(0.7D), player.getZ(),
                    28, 0.65D, 0.55D, 0.65D, 0.01D);
        }
        SmoothClassesNetworking.sendAbilityState(player);
    }

    public static void tickFrenzy(ServerPlayerEntity player) {
        var effect = player.getStatusEffect(SmoothEffects.CRIMSON_REVENANT);
        if (effect == null || !(player.getWorld() instanceof ServerWorld world)) return;

        boolean fortress = AbilityRuntime.hasTalent(player, BerserkerContent.CRIMSON_REVENANT_FORTRESS.id());
        boolean massacre = AbilityRuntime.hasTalent(player, BerserkerContent.CRIMSON_REVENANT_MASSACRE.id());
        double searchRange = massacre ? 24.0D : 18.0D;
        double pursuitSpeed = massacre ? 0.54D : 0.44D;
        int attackInterval = massacre ? 4 : 6;
        double strikeRange = massacre ? 4.25D : 3.35D;

        // Keep the defensive layer alive without reapplying it twenty times per second.
        if (player.age % 10 == 0) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 30, fortress ? 1 : 0,
                    false, false, true));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 30, 1,
                    false, false, true));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 30, massacre ? 1 : 0,
                    false, false, true));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 40, fortress ? 2 : 1,
                    false, false, true));
        }

        spectralEmbers(world, player, effect.getDuration());

        LivingEntity target = nearestVictim(player, searchRange);
        if (target == null) {
            player.setSprinting(false);
            player.setVelocity(0.0D, player.getVelocity().y, 0.0D);
            player.velocityModified = true;
            return;
        }

        steerToward(player, target, pursuitSpeed);
        if (player.age % attackInterval == 0 && player.squaredDistanceTo(target) <= strikeRange * strikeRange) {
            savageStrike(player, target, massacre ? 7.0F : 4.5F, massacre ? 3.2D : 2.6D);
        }
    }

    public static void activeEffectRemoved(ServerPlayerEntity player) {
        AVOID_SIDE.remove(player.getUuid());
        AVOID_UNTIL.remove(player.getUuid());
        if (!player.getWorld().isClient()) {
            player.setSprinting(false);
            player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.ENTITY_RAVAGER_ROAR,
                    SoundCategory.PLAYERS, 0.55F, 0.82F);
            SmoothClassesNetworking.sendAbilityState(player);
        }
    }

    private static void chargeParticles(ServerPlayerEntity player, int elapsed) {
        if (!(player.getWorld() instanceof ServerWorld world) || player.age % 2 != 0) return;
        float progress = Math.min(1.0F, elapsed / (float) CHARGE_TICKS);
        int count = 2 + (int) (progress * 4);
        for (int i = 0; i < count; i++) {
            double side = ((i & 1) == 0 ? -1.0D : 1.0D) * (0.25D + progress * 0.35D);
            double y = player.getY() + 0.15D + (i % 3) * 0.48D + progress * 0.35D;
            double z = player.getZ() + 0.18D + (i % 2) * 0.14D;
            world.spawnParticles(progress < 0.55F ? SMALL_RED : LARGE_RED,
                    player.getX() + side, y, z, 1, 0.015D, 0.03D, 0.015D, 0.0D);
            if (i == 0) world.spawnParticles(ParticleTypes.SMOKE,
                    player.getX(), y, player.getZ(), 1, 0.03D, 0.04D, 0.03D, 0.0D);
        }
    }

    /** Tiny embers only; the giant skeletal renderer is the actual aura now. */
    private static void spectralEmbers(ServerWorld world, ServerPlayerEntity player, int remaining) {
        if (player.age % 6 != 0) return;
        int count = remaining <= 40 ? 1 : 3;
        for (int i = 0; i < count; i++) {
            double x = player.getX() + (player.getRandom().nextDouble() - 0.5D) * 1.5D;
            double y = player.getY() + 0.4D + player.getRandom().nextDouble() * 2.4D;
            double z = player.getZ() + (player.getRandom().nextDouble() - 0.5D) * 1.5D;
            world.spawnParticles(LARGE_RED, x, y, z, 1, 0.01D, 0.02D, 0.01D, 0.0D);
        }
    }

    private static LivingEntity nearestVictim(ServerPlayerEntity player, double range) {
        Box box = player.getBoundingBox().expand(range, 8.0D, range);
        return player.getWorld().getEntitiesByClass(LivingEntity.class, box,
                        candidate -> isVictim(player, candidate)
                                && player.squaredDistanceTo(candidate) <= range * range)
                .stream()
                .min(Comparator.comparingDouble(player::squaredDistanceTo))
                .orElse(null);
    }

    /** Friendly mobs, pets, summons and hostile mobs are all valid. Players are not. */
    private static boolean isVictim(ServerPlayerEntity berserker, LivingEntity candidate) {
        if (candidate == null || candidate == berserker || !candidate.isAlive()) return false;
        if (candidate instanceof ArmorStandEntity || candidate instanceof PlayerEntity) return false;
        return !candidate.isRemoved();
    }

    private static void steerToward(ServerPlayerEntity player, LivingEntity target, double speed) {
        Vec3d toTarget = target.getPos().subtract(player.getPos());
        Vec3d desired = new Vec3d(toTarget.x, 0.0D, toTarget.z);
        double horizontalLength = desired.length();
        if (horizontalLength <= 0.0001D) return;
        desired = desired.multiply(1.0D / horizontalLength);

        // Crimson Revenant is always running under AI control; the player never needs
        // to press sprint manually.
        player.setSprinting(true);

        boolean directBlocked = !horizontalClear(player, desired, 0.85D, 0.0D)
                || !horizontalClear(player, desired, 1.35D, 0.0D);
        boolean jumpable = directBlocked && canStepOver(player, desired);

        Vec3d move = desired;
        if (directBlocked && !jumpable) {
            move = avoidanceDirection(player, desired);
        }

        double vertical = player.getVelocity().y;
        player.setVelocity(move.x * speed, vertical, move.z * speed);
        player.velocityModified = true;

        // Auto-jump only for a low obstacle with clear head room. Tall walls/trees are
        // handled by side-routing instead of repeatedly jumping into them.
        if (player.isOnGround() && (jumpable || (toTarget.y > 0.85D && horizontalClear(player, move, 0.7D, 1.0D)))) {
            player.setVelocity(player.getVelocity().x, 0.46D, player.getVelocity().z);
            player.velocityModified = true;
        }

        player.lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, target.getEyePos());
        player.headYaw = player.getYaw();
        player.bodyYaw = player.getYaw();
    }

    /** True when the player's collision box can occupy a point along this horizontal direction. */
    private static boolean horizontalClear(ServerPlayerEntity player, Vec3d direction,
                                           double distance, double yOffset) {
        if (direction.lengthSquared() < 0.0001D) return true;
        Vec3d dir = new Vec3d(direction.x, 0.0D, direction.z).normalize();
        Box shifted = player.getBoundingBox().offset(dir.x * distance, yOffset, dir.z * distance);
        return player.getServerWorld().getWorldBorder().contains(shifted)
                && player.getWorld().isSpaceEmpty(player, shifted);
    }

    /**
     * A one-block-ish obstacle is jumpable when the low probe is blocked but the same
     * forward space is free one block higher. Fences/tall walls normally fail this test.
     */
    private static boolean canStepOver(ServerPlayerEntity player, Vec3d direction) {
        if (!player.isOnGround()) return false;
        boolean lowBlocked = !horizontalClear(player, direction, 0.72D, 0.0D);
        if (!lowBlocked) return false;
        return horizontalClear(player, direction, 0.72D, 1.05D)
                && horizontalClear(player, direction, 1.12D, 1.05D);
    }

    /**
     * Cheap player pathing: when the direct line is blocked, pick one side and keep
     * sliding around the obstacle for a short window. This is intentionally stateful so
     * trees and long walls do not make the berserker flip left/right and get stuck.
     */
    private static Vec3d avoidanceDirection(ServerPlayerEntity player, Vec3d desired) {
        UUID id = player.getUuid();
        int now = player.getServer().getTicks();
        int preferred = AVOID_SIDE.getOrDefault(id, 0);
        int until = AVOID_UNTIL.getOrDefault(id, 0);

        if (preferred == 0 || now > until) {
            double left = bestSideScore(player, desired, -1);
            double right = bestSideScore(player, desired, 1);
            preferred = right > left ? 1 : -1;
            AVOID_SIDE.put(id, preferred);
            AVOID_UNTIL.put(id, now + 28);
        }

        Vec3d best = bestSideDirection(player, desired, preferred);
        if (best != null) return best;

        // If the chosen side becomes a dead end, swap once rather than staying pinned.
        preferred = -preferred;
        AVOID_SIDE.put(id, preferred);
        AVOID_UNTIL.put(id, now + 28);
        best = bestSideDirection(player, desired, preferred);
        if (best != null) return best;

        // Last resort: back/diagonal movement gives the next tick room to re-evaluate.
        Vec3d reverse = rotateHorizontal(desired, 145.0D * preferred);
        return horizontalClear(player, reverse, 0.65D, 0.0D) ? reverse : desired.multiply(-0.35D);
    }

    private static double bestSideScore(ServerPlayerEntity player, Vec3d desired, int side) {
        Vec3d best = bestSideDirection(player, desired, side);
        if (best == null) return -1000.0D;
        double clearance = clearanceDistance(player, best);
        double alignment = Math.max(-1.0D, Math.min(1.0D, best.dotProduct(desired)));
        return clearance * 2.0D + alignment;
    }

    private static Vec3d bestSideDirection(ServerPlayerEntity player, Vec3d desired, int side) {
        double[] angles = {25.0D, 42.5D, 60.0D, 77.5D, 95.0D, 115.0D};
        Vec3d best = null;
        double bestScore = -1000.0D;
        for (double angle : angles) {
            Vec3d candidate = rotateHorizontal(desired, angle * side);
            double clearance = clearanceDistance(player, candidate);
            if (clearance < 0.55D) continue;
            double alignment = candidate.dotProduct(desired);
            // Prefer open directions while still making progress around the target.
            double score = clearance * 2.4D + alignment * 0.8D - angle * 0.002D;
            if (score > bestScore) {
                bestScore = score;
                best = candidate;
            }
        }
        return best;
    }

    private static double clearanceDistance(ServerPlayerEntity player, Vec3d direction) {
        double[] probes = {0.45D, 0.75D, 1.05D, 1.40D, 1.80D, 2.25D};
        double clear = 0.0D;
        for (double probe : probes) {
            if (!horizontalClear(player, direction, probe, 0.0D)) break;
            clear = probe;
        }
        return clear;
    }

    private static Vec3d rotateHorizontal(Vec3d direction, double degrees) {
        double radians = Math.toRadians(degrees);
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        return new Vec3d(direction.x * cos - direction.z * sin,
                0.0D,
                direction.x * sin + direction.z * cos).normalize();
    }

    private static void savageStrike(ServerPlayerEntity player, LivingEntity primary,
                                     float bonusDamage, double splashRadius) {
        if (!(player.getWorld() instanceof ServerWorld world)) return;

        player.swingHand(Hand.MAIN_HAND, true);
        player.attack(primary);
        primary.timeUntilRegen = 0;
        primary.damage(player.getDamageSources().playerAttack(player), bonusDamage);
        primary.timeUntilRegen = 0;

        List<LivingEntity> nearby = world.getEntitiesByClass(LivingEntity.class,
                primary.getBoundingBox().expand(splashRadius),
                candidate -> isVictim(player, candidate) && candidate != primary);
        for (LivingEntity victim : nearby) {
            victim.timeUntilRegen = 0;
            victim.damage(player.getDamageSources().playerAttack(player), bonusDamage * 0.6F);
            victim.timeUntilRegen = 0;
            Vec3d away = victim.getPos().subtract(player.getPos());
            if (away.lengthSquared() > 0.01D) {
                Vec3d knock = away.normalize().multiply(0.55D);
                victim.addVelocity(knock.x, 0.22D, knock.z);
                victim.velocityModified = true;
            }
        }

        world.spawnParticles(ParticleTypes.SWEEP_ATTACK,
                primary.getX(), primary.getBodyY(0.5D), primary.getZ(),
                1, 0.0D, 0.0D, 0.0D, 0.0D);
        world.playSound(null, primary.getBlockPos(), SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP,
                SoundCategory.PLAYERS, 0.85F, 0.72F);
    }

    public static void disconnect(ServerPlayerEntity player) {
        UUID id = player.getUuid();
        boolean wasCharging = CHARGING.remove(id);
        CHARGE_STARTED_AT.remove(id);
        HELD_AT.remove(id);
        AVOID_SIDE.remove(id);
        AVOID_UNTIL.remove(id);
        if (wasCharging) {
            AbilityCooldowns.start(player, SPECIAL_ID, CANCEL_COOLDOWN_TICKS);
            CANCEL_COOLDOWN_UNTIL.put(id, player.getServerWorld().getTime() + CANCEL_COOLDOWN_TICKS);
        }
        player.removeStatusEffect(SmoothEffects.CRIMSON_REVENANT_CHARGE);
    }

    public static void clear() {
        HELD_AT.clear();
        CHARGE_STARTED_AT.clear();
        CANCEL_COOLDOWN_UNTIL.clear();
        CHARGING.clear();
        AVOID_SIDE.clear();
        AVOID_UNTIL.clear();
    }
}
