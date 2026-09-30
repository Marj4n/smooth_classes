package org.marj4n.smooth_classes.content.rider.runtime;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Ownable;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.passive.HorseColor;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.marj4n.smooth_classes.SmoothClasses;
import org.marj4n.smooth_classes.config.SmoothBalance;
import org.marj4n.smooth_classes.content.rider.RiderClass;
import org.marj4n.smooth_classes.content.rider.RiderContent;
import org.marj4n.smooth_classes.entity.RiderHorseEntity;
import org.marj4n.smooth_classes.runtime.AbilityCooldowns;
import org.marj4n.smooth_classes.runtime.AbilityRuntime;
import org.marj4n.smooth_classes.runtime.ExecutionResult;
import org.marj4n.smooth_classes.runtime.SpellPowerRuntime;
import org.marj4n.smooth_classes.registry.SmoothEntities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Server-owned Rider mount and signature runtime. Puffish Skills remains authoritative for unlocks. */
public final class RiderRuntime {
    private static final Identifier SUMMON_COOLDOWN = SmoothClasses.id("rider_mount_summon");
    private static final UUID MOUNT_ATTACK_MODIFIER = UUID.fromString("7a62ea1a-c747-4ea1-b56f-0d3859de01ad");

    public static int summonCooldownTicks(ServerPlayerEntity player) {
        return Math.max(1, SmoothBalance.Rider.summonCooldown * 20);
    }

    public static long summonRemainingTicks(ServerPlayerEntity player) {
        return AbilityCooldowns.remainingTicks(player, SUMMON_COOLDOWN);
    }

    public static boolean hasActiveMount(ServerPlayerEntity player) {
        MountSession session = MOUNTS.get(player.getUuid());
        return session != null && valid(session.mount);
    }

    public static boolean isMountedDamageBoostActive(ServerPlayerEntity player) {
        if (!AbilityRuntime.isClass(player, RiderClass.ID)) return false;
        MountSession session = MOUNTS.get(player.getUuid());
        return session != null && valid(session.mount) && player.getVehicle() == session.mount;
    }

    public static float mountedDamageMultiplier(ServerPlayerEntity player) {
        return isMountedDamageBoostActive(player) ? 2.0F : 1.0F;
    }

    private static final Map<UUID, MountSession> MOUNTS = new HashMap<>();
    private static final Set<UUID> CHARGE_PROTECTED = new HashSet<>();

    private RiderRuntime() {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (MOUNTS.isEmpty()) return;
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) tickPlayer(player);
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> cleanup(handler.player.getUuid(), true));
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            for (UUID id : new ArrayList<>(MOUNTS.keySet())) cleanup(id, true);
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            MOUNTS.clear();
            CHARGE_PROTECTED.clear();
        });
    }

    public static ExecutionResult summonMount(ServerPlayerEntity player) {
        if (!AbilityRuntime.isClass(player, RiderClass.ID)) return ExecutionResult.failure("You are not a Rider.");

        ServerWorld world = player.getServerWorld();
        MountSession old = MOUNTS.get(player.getUuid());
        if (old != null && valid(old.mount)) {
            int desiredTier = desiredMountTier(player);
            int currentTier = mountTier(old.mount);
            if (desiredTier > currentTier) {
                if (evolveMount(player, old, world)) {
                    return ExecutionResult.success(1, describeTier(desiredTier) + " evolved");
                }
                cleanup(player.getUuid(), true);
            } else {
                return ExecutionResult.failure("Your Rider mount is already summoned.");
            }
        }

        long remaining = AbilityCooldowns.remainingTicks(player, SUMMON_COOLDOWN);
        if (remaining > 0) return ExecutionResult.failure("Mount summon cooldown " + String.format(java.util.Locale.ROOT, "%.1f", remaining / 20D) + "s");

        LivingEntity mount = createBestMount(player, world);
        if (mount == null) return ExecutionResult.failure("Could not create Rider mount.");

        Vec3d spawn = summonPosition(player);
        mount.refreshPositionAndAngles(spawn.x, player.getY(), spawn.z, player.getYaw(), 0F);
        if (!world.isSpaceEmpty(mount)) mount.refreshPositionAndAngles(player.getX(), player.getY(), player.getZ(), player.getYaw(), 0F);
        if (!world.spawnEntity(mount)) return ExecutionResult.failure("Could not spawn Rider mount.");

        applyMountStats(player, mount);
        MountSession session = new MountSession(player.getUuid(), mount);
        MOUNTS.put(player.getUuid(), session);
        puff(world, mount.getPos(), false);
        world.playSound(null, mount.getBlockPos(), SoundEvents.ENTITY_HORSE_AMBIENT, SoundCategory.PLAYERS, .65F, 1.15F);
        AbilityCooldowns.start(player, SUMMON_COOLDOWN, SmoothBalance.Rider.summonCooldown * 20);
        return ExecutionResult.success(1, describeTier(mountTier(mount)) + " summoned");
    }

    public static ExecutionResult executeCharge(ServerPlayerEntity player) {
        MountSession session = mountedSession(player);
        if (session == null) return ExecutionResult.failure("Mount your Rider steed first.");
        if (session.chargeTicks > 0) return ExecutionResult.failure("Rider's Charge is already active.");

        Vec3d initialDirection = player.getRotationVec(1F);
        session.chargeDir = initialDirection.lengthSquared() < 1.0E-6D
                ? horizontal(initialDirection)
                : initialDirection.normalize();
        session.chargeTicks = has(player, RiderContent.CHARGE_REACH) ? SmoothBalance.Rider.chargeLongTicks : SmoothBalance.Rider.chargeTicks;
        session.phase = has(player, RiderContent.CHARGE_PHASE);
        session.impact = has(player, RiderContent.CHARGE_IMPACT);
        session.chargeHits.clear();
        session.chargeOriginalNoGravity = session.mount.hasNoGravity();
        session.mount.setNoGravity(true);
        session.mount.fallDistance = 0.0F;
        if (session.phase) {
            CHARGE_PROTECTED.add(player.getUuid());
            CHARGE_PROTECTED.add(session.mount.getUuid());
        }
        wind(player.getServerWorld(), session.mount.getPos());
        player.getServerWorld().playSound(null, session.mount.getBlockPos(), SoundEvents.ITEM_TRIDENT_RIPTIDE_2, SoundCategory.PLAYERS, .8F, 1.0F);
        return ExecutionResult.success(1, "Rider's Charge");
    }

    public static ExecutionResult executeWarAura(ServerPlayerEntity player) {
        MountSession session = mountedSession(player);
        if (session == null) return ExecutionResult.failure("Mount your Rider steed first.");
        session.auraTicks = SmoothBalance.Rider.auraDuration;
        session.auraResistance = has(player, RiderContent.AURA_RESISTANCE) ? 1 : 0;
        session.auraStrength = has(player, RiderContent.AURA_STRENGTH) ? 1 : 0;
        session.auraHarm = has(player, RiderContent.AURA_HARM) ? 1 : 0;
        return ExecutionResult.success(1, "War Aura");
    }

    public static ExecutionResult executeBlazingHooves(ServerPlayerEntity player) {
        MountSession session = mountedSession(player);
        if (session == null) return ExecutionResult.failure("Mount your Rider steed first.");
        session.fireTicks = has(player, RiderContent.FIRE_DURATION) ? SmoothBalance.Rider.fireLongDuration : SmoothBalance.Rider.fireDuration;
        session.fireDamage = has(player, RiderContent.FIRE_DAMAGE);
        session.fireRadius = has(player, RiderContent.FIRE_RADIUS);
        return ExecutionResult.success(1, "Blazing Hooves");
    }

    public static boolean isChargeProtected(LivingEntity entity) {
        return CHARGE_PROTECTED.contains(entity.getUuid());
    }

    private static void tickPlayer(ServerPlayerEntity player) {
        MountSession s = MOUNTS.get(player.getUuid());
        if (s == null) return;
        if (!valid(s.mount) || !player.isAlive() || player.getWorld() != s.mount.getWorld()
                || !AbilityRuntime.isClass(player, RiderClass.ID)) {
            cleanup(player.getUuid(), true);
            return;
        }

        // Keep the base Rider horse's Water Stride flag synced every tick.
        // DataTracker mirrors it to the client, so ridden movement is calculated
        // natively at land speed on both sides instead of being corrected afterward.
        if (s.mount instanceof RiderHorseEntity riderHorse) {
            riderHorse.setWaterStride(has(player, RiderContent.WATER_STRIDE));
        }

        boolean riding = player.getVehicle() == s.mount;
        if (riding) {
            s.everMounted = true;
            applyMountedDamageBuff(player);
        } else {
            removeMountedDamageBuff(player);
            if (s.everMounted) {
                cleanup(player.getUuid(), true);
                return;
            }
        }

        if (player.age % 20 == 0) {
            // Auto-upgrade idle/not-yet-mounted mounts when the player just unlocked an evolution.
            if (!riding && desiredMountTier(player) > mountTier(s.mount) && evolveMount(player, s, player.getServerWorld())) {
                s = MOUNTS.get(player.getUuid());
                if (s == null) return;
            }
            applyMountStats(player, s.mount);
            int recovery = rank(player, RiderContent.RECOVERY_I, RiderContent.RECOVERY_II);
            if (recovery > 0 && s.mount.getHealth() < s.mount.getMaxHealth()) s.mount.heal(recovery);
        }

        if (!riding) return;
        fluidTraversal(player, s.mount);
        if (s.chargeTicks > 0) tickCharge(player, s);
        if (s.auraTicks > 0) tickAura(player, s);
        if (s.fireTicks > 0) tickFire(player, s);
    }

    private static void tickCharge(ServerPlayerEntity player, MountSession s) {
        LivingEntity mount = s.mount;
        ServerWorld world = player.getServerWorld();
        s.chargeTicks--;

        // Rider's Charge now behaves like steerable riptide. The look vector is
        // sampled every tick, then smoothly blended so the charge can arc upward,
        // downward, or sideways instead of being locked to the initial X/Z line.
        Vec3d requested = player.getRotationVec(1F);
        if (requested.lengthSquared() > 1.0E-6D) {
            requested = requested.normalize();
            double steer = s.impact ? 0.24D : 0.30D;
            Vec3d blended = s.chargeDir.multiply(1.0D - steer).add(requested.multiply(steer));
            if (blended.lengthSquared() > 1.0E-6D) s.chargeDir = blended.normalize();
        }

        double speed = s.impact ? SmoothBalance.Rider.chargeImpactSpeed : SmoothBalance.Rider.chargeSpeed;
        Vec3d step = s.chargeDir.multiply(speed);
        Box next = mount.getBoundingBox().offset(step);

        mount.setNoGravity(true);
        mount.fallDistance = 0.0F;
        player.fallDistance = 0.0F;

        if (!world.isSpaceEmpty(mount, next)) {
            if (!s.phase || !phaseThrough(world, mount, s.chargeDir, speed)) {
                endCharge(s);
                return;
            }
        } else {
            mount.setVelocity(step.x, step.y, step.z);
            mount.velocityModified = true;
        }
        wind(world, mount.getPos());

        float damage = (float) Math.max(4D, player.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE));
        damage *= s.impact ? SmoothBalance.Rider.chargeImpactDamageMultiplier : SmoothBalance.Rider.chargeDamageMultiplier;
        for (LivingEntity target : world.getEntitiesByClass(LivingEntity.class, mount.getBoundingBox().expand(1.15D),
                e -> e.isAlive() && e != player && e != mount && !ally(player, e))) {
            if (s.chargeHits.add(target.getUuid())) {
                target.damage(player.getDamageSources().playerAttack(player), damage);
                Vec3d push = s.chargeDir.multiply(s.impact ? 1.35D : .75D);
                target.addVelocity(push.x, push.y + (s.impact ? .12D : .06D), push.z);
            }
        }
        if (s.chargeTicks <= 0) endCharge(s);
    }

    private static boolean phaseThrough(ServerWorld world, LivingEntity mount, Vec3d dir, double speed) {
        for (int blocks = 1; blocks <= SmoothBalance.Rider.maxPhaseBlocks; blocks++) {
            Vec3d delta = dir.multiply(blocks + .75D);
            Box exit = mount.getBoundingBox().offset(delta);
            if (world.isSpaceEmpty(mount, exit)) {
                mount.setPosition(
                        mount.getX() + delta.x,
                        mount.getY() + delta.y,
                        mount.getZ() + delta.z
                );
                mount.setVelocity(dir.x * speed, dir.y * speed, dir.z * speed);
                mount.velocityModified = true;
                mount.fallDistance = 0.0F;
                wind(world, mount.getPos());
                return true;
            }
        }
        return false;
    }

    private static void endCharge(MountSession s) {
        s.chargeTicks = 0;
        CHARGE_PROTECTED.remove(s.owner);
        if (valid(s.mount)) {
            CHARGE_PROTECTED.remove(s.mount.getUuid());
            s.mount.setNoGravity(s.chargeOriginalNoGravity);
            s.mount.fallDistance = 0.0F;
        }
    }

    private static void tickAura(ServerPlayerEntity player, MountSession s) {
        s.auraTicks--;
        if (player.age % 10 != 0) return;
        ServerWorld world = player.getServerWorld();
        double radius = SmoothBalance.Rider.auraRadius;
        Box area = player.getBoundingBox().expand(radius);
        for (LivingEntity e : world.getEntitiesByClass(LivingEntity.class, area,
                e -> e.isAlive() && e.squaredDistanceTo(player) <= radius * radius)) {
            if (ally(player, e)) {
                e.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 30, s.auraResistance, false, false, true));
                e.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 30, s.auraStrength, false, false, true));
            } else {
                if (e.isUndead()) e.damage(player.getDamageSources().playerAttack(player), s.auraHarm == 0 ? 6F : 12F);
                else StatusEffects.INSTANT_DAMAGE.applyUpdateEffect(e, s.auraHarm);
            }
        }
        world.spawnParticles(ParticleTypes.ENCHANT, player.getX(), player.getBodyY(.45D), player.getZ(), 22,
                radius * .45D, .5D, radius * .45D, .015D);
    }

    private static void tickFire(ServerPlayerEntity player, MountSession s) {
        s.fireTicks--;
        LivingEntity mount = s.mount;
        ServerWorld world = player.getServerWorld();
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 30, 0, false, false, false));
        mount.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 30, 0, false, false, false));
        player.extinguish();
        mount.extinguish();
        if ((player.age & 1) == 0) {
            world.spawnParticles(ParticleTypes.FLAME, mount.getX(), mount.getY() + .12D, mount.getZ(), 7, .5D, .08D, .5D, .01D);
            world.spawnParticles(ParticleTypes.SMALL_FLAME, mount.getX(), mount.getY() + .08D, mount.getZ(), 4, .4D, .04D, .4D, .005D);
        }
        if (player.age % 4 != 0) return;
        double radius = s.fireRadius ? 1.85D : 1.25D;
        float damage = SpellPowerRuntime.fire(player, s.fireDamage ? 1.25D : .75D);
        for (LivingEntity target : world.getEntitiesByClass(LivingEntity.class, mount.getBoundingBox().expand(radius),
                e -> e.isAlive() && e != player && e != mount && !ally(player, e))) {
            target.damage(player.getDamageSources().playerAttack(player), Math.max(1F, damage));
            target.setOnFireFor(s.fireDamage ? 5 : 3);
        }
    }

    private static boolean evolveMount(ServerPlayerEntity player, MountSession session, ServerWorld world) {
        if (session == null || !valid(session.mount)) return false;

        LivingEntity oldMount = session.mount;
        int desiredTier = desiredMountTier(player);
        if (desiredTier <= mountTier(oldMount)) return false;

        LivingEntity newMount = createBestMount(player, world);
        if (newMount == null || mountTier(newMount) <= mountTier(oldMount)) return false;

        boolean riding = player.getVehicle() == oldMount;
        float healthRatio = oldMount.getMaxHealth() > 0F ? oldMount.getHealth() / oldMount.getMaxHealth() : 1F;
        Vec3d pos = oldMount.getPos();
        float yaw = oldMount.getYaw();
        float pitch = oldMount.getPitch();

        newMount.refreshPositionAndAngles(pos.x, pos.y, pos.z, yaw, pitch);
        if (!world.isSpaceEmpty(newMount)) {
            Vec3d spawn = summonPosition(player);
            newMount.refreshPositionAndAngles(spawn.x, player.getY(), spawn.z, player.getYaw(), 0F);
            if (!world.isSpaceEmpty(newMount)) {
                return false;
            }
        }

        if (riding) player.stopRiding();
        puff(world, oldMount.getPos(), true);
        oldMount.discard();

        if (!world.spawnEntity(newMount)) return false;

        applyMountStats(player, newMount);
        newMount.setHealth(Math.max(1F, Math.min(newMount.getMaxHealth(), newMount.getMaxHealth() * healthRatio)));
        if (riding) player.startRiding(newMount, true);
        puff(world, newMount.getPos(), false);
        world.playSound(null, newMount.getBlockPos(), SoundEvents.ENTITY_HORSE_AMBIENT, SoundCategory.PLAYERS, .75F, 1.1F);

        session.mount = newMount;
        session.chargeTicks = 0;
        session.auraTicks = 0;
        session.fireTicks = 0;
        session.phase = false;
        session.impact = false;
        session.fireDamage = false;
        session.fireRadius = false;
        session.auraResistance = 0;
        session.auraStrength = 0;
        session.auraHarm = 0;
        session.chargeDir = Vec3d.ZERO;
        session.chargeHits.clear();
        CHARGE_PROTECTED.remove(oldMount.getUuid());
        return true;
    }

    private static LivingEntity createBestMount(ServerPlayerEntity player, ServerWorld world) {
        if (has(player, RiderContent.HIPPOGRYPH)) {
            var hippogryph = SmoothEntities.RIDER_HIPPOGRYPH.create(world);
            if (hippogryph != null) {
                configureHorse(hippogryph, player);
                return hippogryph;
            }
        }

        if (has(player, RiderContent.DREAD_STEED)) {
            var dreadSteed = SmoothEntities.RIDER_DREAD_STEED.create(world);
            if (dreadSteed != null) {
                configureHorse(dreadSteed, player);
                return dreadSteed;
            }
        }

        RiderHorseEntity horse = SmoothEntities.RIDER_HORSE.create(world);
        if (horse != null) {
            horse.setVariant(HorseColor.WHITE);
            horse.setWaterStride(has(player, RiderContent.WATER_STRIDE));
            configureHorse(horse, player);
        }
        return horse;
    }

    private static void configureHorse(AbstractHorseEntity horse, ServerPlayerEntity player) {
        horse.setOwnerUuid(player.getUuid());
        horse.setTame(true);
        horse.saddle(null);
    }

    private static void applyMountStats(ServerPlayerEntity player, LivingEntity mount) {
        int health = rank(player, RiderContent.HEALTH_I, RiderContent.HEALTH_II, RiderContent.HEALTH_III);
        int speed = rank(player, RiderContent.SPEED_I, RiderContent.SPEED_II, RiderContent.SPEED_III);
        int armor = rank(player, RiderContent.ARMOR_I, RiderContent.ARMOR_II);
        int jump = rank(player, RiderContent.JUMP_I, RiderContent.JUMP_II);

        setAtLeast(mount, EntityAttributes.GENERIC_MAX_HEALTH, 30D + health * 6D);
        setAtLeast(mount, EntityAttributes.GENERIC_MOVEMENT_SPEED, .225D + speed * .025D);
        setAtLeast(mount, EntityAttributes.GENERIC_ARMOR, armor * 3D);
        setAtLeast(mount, EntityAttributes.HORSE_JUMP_STRENGTH, .7D + jump * .10D);
        if (mount.getHealth() > mount.getMaxHealth()) mount.setHealth(mount.getMaxHealth());
    }

    private static void setAtLeast(LivingEntity entity, EntityAttribute attribute, double value) {
        EntityAttributeInstance instance = entity.getAttributeInstance(attribute);
        if (instance != null && instance.getBaseValue() < value) instance.setBaseValue(value);
    }

    /**
     * Surface walking is handled inside the custom Rider entities themselves.
     * This runtime only grants the Rider/mount lava protection that belongs to
     * the Dread Steed and Hippogryph evolutions.
     */
    private static void fluidTraversal(ServerPlayerEntity player, LivingEntity mount) {
        boolean lavaStride = has(player, RiderContent.DREAD_STEED)
                || has(player, RiderContent.HIPPOGRYPH);

        if (lavaStride) {
            // A 30-tick protection effect only needs a 10-tick refresh.
            if (player.age % 10 == 0) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 30, 0, false, false, false));
                mount.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 30, 0, false, false, false));
            }
            if (player.isOnFire()) player.extinguish();
            if (mount.isOnFire()) mount.extinguish();
        }
    }

    private static void applyMountedDamageBuff(ServerPlayerEntity player) {
        EntityAttributeInstance attack = player.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE);
        if (attack == null || attack.getModifier(MOUNT_ATTACK_MODIFIER) != null) return;
        attack.addTemporaryModifier(new net.minecraft.entity.attribute.EntityAttributeModifier(
                MOUNT_ATTACK_MODIFIER,
                "Smooth Classes Rider Mounted Damage",
                1.0D,
                net.minecraft.entity.attribute.EntityAttributeModifier.Operation.MULTIPLY_TOTAL
        ));
    }

    private static void removeMountedDamageBuff(ServerPlayerEntity player) {
        EntityAttributeInstance attack = player.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE);
        if (attack != null && attack.getModifier(MOUNT_ATTACK_MODIFIER) != null) {
            attack.removeModifier(MOUNT_ATTACK_MODIFIER);
        }
    }

    private static boolean ally(ServerPlayerEntity owner, LivingEntity e) {
        if (e == owner || e == MOUNTS.getOrDefault(owner.getUuid(), MountSession.EMPTY).mount) return true;
        if (owner.isTeammate(e)) return true;
        if (e instanceof TameableEntity tame && owner.getUuid().equals(tame.getOwnerUuid())) return true;
        if (e instanceof AbstractHorseEntity horse && owner.getUuid().equals(horse.getOwnerUuid())) return true;
        if (e instanceof Ownable ownable) {
            Entity master = ownable.getOwner();
            if (master != null && owner.getUuid().equals(master.getUuid())) return true;
            if (master instanceof TameableEntity tame && owner.getUuid().equals(tame.getOwnerUuid())) return true;
        }
        return false;
    }

    private static MountSession mountedSession(ServerPlayerEntity player) {
        if (!AbilityRuntime.isClass(player, RiderClass.ID)) return null;
        MountSession s = MOUNTS.get(player.getUuid());
        return s != null && valid(s.mount) && player.getVehicle() == s.mount ? s : null;
    }

    private static boolean valid(LivingEntity mount) {
        return mount != null && mount.isAlive() && !mount.isRemoved();
    }

    private static boolean has(ServerPlayerEntity p, org.marj4n.smooth_classes.api.talent.Talent t) {
        return AbilityRuntime.hasTalent(p, t.id());
    }

    private static int rank(ServerPlayerEntity p, org.marj4n.smooth_classes.api.talent.Talent... nodes) {
        int r = 0;
        for (var node : nodes) if (has(p, node)) r++;
        return r;
    }

    private static int desiredMountTier(ServerPlayerEntity player) {
        if (has(player, RiderContent.HIPPOGRYPH)) return 3;
        if (has(player, RiderContent.DREAD_STEED)) return 2;
        return 1;
    }

    private static int mountTier(LivingEntity mount) {
        if (!valid(mount)) return 0;
        if (mount.getType() == SmoothEntities.RIDER_HIPPOGRYPH) return 3;
        if (mount.getType() == SmoothEntities.RIDER_DREAD_STEED) return 2;
        return 1;
    }

    private static String describeTier(int tier) {
        return switch (tier) {
            case 3 -> "White Hippogryph";
            case 2 -> "Dread Steed";
            default -> "Rider mount";
        };
    }

    private static Vec3d summonPosition(ServerPlayerEntity player) {
        Vec3d look = horizontal(player.getRotationVec(1F));
        return player.getPos().add(look.multiply(2.25D));
    }

    private static Vec3d horizontal(Vec3d v) {
        Vec3d h = new Vec3d(v.x, 0D, v.z);
        return h.lengthSquared() < 1.0E-6D ? new Vec3d(0D, 0D, 1D) : h.normalize();
    }

    private static void cleanup(UUID owner, boolean particles) {
        MountSession s = MOUNTS.remove(owner);
        if (s == null) return;
        endCharge(s);
        ServerPlayerEntity player = null;
        if (s.mount != null && s.mount.getServer() != null) {
            player = s.mount.getServer().getPlayerManager().getPlayer(owner);
        }
        if (player != null) removeMountedDamageBuff(player);
        if (valid(s.mount)) {
            if (particles && s.mount.getWorld() instanceof ServerWorld world) puff(world, s.mount.getPos(), true);
            s.mount.discard();
        }
    }


    private static void puff(ServerWorld world, Vec3d pos, boolean vanish) {
        world.spawnParticles(ParticleTypes.POOF, pos.x, pos.y + .7D, pos.z,
                vanish ? 24 : 18, .65D, .45D, .65D, .055D);
        world.spawnParticles(ParticleTypes.CLOUD, pos.x, pos.y + .35D, pos.z,
                vanish ? 10 : 7, .5D, .25D, .5D, .025D);
    }

    private static void wind(ServerWorld world, Vec3d pos) {
        world.spawnParticles(ParticleTypes.CLOUD, pos.x, pos.y + .55D, pos.z,
                5, .35D, .3D, .35D, .06D);
        world.spawnParticles(ParticleTypes.SWEEP_ATTACK, pos.x, pos.y + .8D, pos.z,
                1, .15D, .15D, .15D, 0D);
    }

    private static final class MountSession {
        static final MountSession EMPTY = new MountSession(new UUID(0, 0), null);
        final UUID owner;
        LivingEntity mount;
        boolean everMounted;
        int chargeTicks, auraTicks, fireTicks;
        boolean phase, impact, fireDamage, fireRadius;
        boolean chargeOriginalNoGravity;
        int auraResistance, auraStrength, auraHarm;
        Vec3d chargeDir = Vec3d.ZERO;
        final Set<UUID> chargeHits = new HashSet<>();

        MountSession(UUID owner, LivingEntity mount) {
            this.owner = owner;
            this.mount = mount;
        }
    }
}