package org.marj4n.smooth_classes.content.assassin.runtime;

import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.marj4n.smooth_classes.SmoothClasses;
import org.marj4n.smooth_classes.content.assassin.AssassinClass;
import org.marj4n.smooth_classes.content.assassin.AssassinContent;
import org.marj4n.smooth_classes.entity.ShadowAnchorEntity;
import org.marj4n.smooth_classes.gameplay.SignatureAbilityDispatcher;
import org.marj4n.smooth_classes.network.SmoothClassesNetworking;
import org.marj4n.smooth_classes.registry.SmoothEntities;
import org.marj4n.smooth_classes.runtime.*;

/**
 * Server-authoritative Shadow Technique.
 *
 * First cast is aim-driven by the client: hold the signature key, aim at a living
 * target or block, then release. The actual destination is verified here. A second
 * cast animates the player back to the original shadow.
 */
public final class ShadowTechniqueRuntime {
    private static final int SHADOW_LIFETIME_TICKS = 200; // 10s
    private static final int DASH_TICKS = 6;

    private record Shadow(ServerWorld world, ShadowAnchorEntity anchor, long expires, float yaw, float pitch) {}
    private record Dash(ServerWorld world, Vec3d start, Vec3d end, float yaw, float pitch,
                        int tick, boolean returning, UUID blindTarget) {}

    private static final Map<UUID, Shadow> ACTIVE = new HashMap<>();
    private static final Map<UUID, Dash> DASHES = new HashMap<>();
    private static final Map<UUID, Long> BLINDED = new HashMap<>();
    private static final Set<UUID> EMPOWERED = new HashSet<>();

    private ShadowTechniqueRuntime() {}

    public static boolean blinded(MobEntity mob) {
        Long until = BLINDED.get(mob.getUuid());
        return until != null && mob.getServer() != null && mob.getServer().getTicks() < until;
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            BLINDED.values().removeIf(until -> server.getTicks() >= until);

            for (UUID id : new ArrayList<>(DASHES.keySet())) {
                ServerPlayerEntity player = server.getPlayerManager().getPlayer(id);
                tickDash(id, player);
            }

            for (UUID id : new ArrayList<>(ACTIVE.keySet())) {
                Shadow shadow = ACTIVE.get(id);
                ServerPlayerEntity player = server.getPlayerManager().getPlayer(id);
                if (shadow == null) continue;
                if (player == null || !player.isAlive() || player.getServerWorld() != shadow.world || shadow.anchor.isRemoved()) {
                    finish(id, player, true);
                    continue;
                }
                if (shadow.world.getTime() >= shadow.expires && !DASHES.containsKey(id)) {
                    finish(id, player, true);
                }
            }
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> finish(handler.player.getUuid(), handler.player, true));
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            for (UUID id : new ArrayList<>(ACTIVE.keySet())) finish(id, server.getPlayerManager().getPlayer(id), true);
            DASHES.clear();
            EMPOWERED.clear();
            BLINDED.clear();
        });
    }

    public static boolean owns(ShadowAnchorEntity entity) {
        for (Shadow shadow : ACTIVE.values()) if (shadow.anchor == entity) return true;
        return false;
    }

    public static boolean active(ServerPlayerEntity player) {
        return player != null && ACTIVE.containsKey(player.getUuid());
    }

    public static long remainingTicks(ServerPlayerEntity player) {
        if (player == null) return 0L;
        Shadow shadow = ACTIVE.get(player.getUuid());
        if (shadow == null || shadow.world != player.getServerWorld()) return 0L;
        return Math.max(0L, shadow.expires - shadow.world.getTime());
    }

    public static int range(ServerPlayerEntity player) {
        if (player == null) return 10;
        int range = 10;
        if (AbilityRuntime.hasTalent(player, AssassinContent.PREPARATION_SHADOWSTRIKE.id())) range += 4;
        if (AbilityRuntime.hasTalent(player, AssassinContent.PREPARATION_SHADOWSTRIKE_VAMPIRE.id())) range += 4;
        if (AbilityRuntime.hasTalent(player, AssassinContent.PREPARATION_SHADOWSTRIKE_SHIELD.id())) range += 4;
        return range;
    }

    /** Base cooldown shown by the tree: 30s -> 25s -> 20s on the two pursuit upgrades. */
    public static int cooldownBaseTicks(ServerPlayerEntity player) {
        int ticks = 600;
        if (player != null && AbilityRuntime.hasTalent(player, AssassinContent.PREPARATION_SHADOWSTRIKE_VAMPIRE.id())) ticks -= 100;
        if (player != null && AbilityRuntime.hasTalent(player, AssassinContent.PREPARATION_SHADOWSTRIKE_SHIELD.id())) ticks -= 100;
        return Math.max(20, ticks);
    }

    /** HUD total includes generic haste, but does not roll random optional-mod cooldown procs. */
    public static int cooldownHudTicks(ServerPlayerEntity player) {
        return player == null ? 600 : AbilityCooldowns.adjustedTicks(player, cooldownBaseTicks(player));
    }

    private static void finish(UUID id, ServerPlayerEntity player, boolean cooldown) {
        Shadow shadow = ACTIVE.remove(id);
        DASHES.remove(id);
        EMPOWERED.remove(id);
        if (shadow != null && !shadow.anchor.isRemoved()) {
            vanishFx(shadow.world, shadow.anchor.getPos());
            shadow.anchor.discard();
        }
        if (player != null) {
            int cooldownTicks = 0;
            if (cooldown) {
                int base = cooldownBaseTicks(player);
                base = org.marj4n.smooth_classes.integration.OptionalCompatRuntime.signatureCooldown(player, base);
                cooldownTicks = AbilityCooldowns.adjustedTicks(player, base);
            }
            AbilityCooldowns.start(player, SmoothClasses.id("preparation"), cooldownTicks);
            SmoothClassesNetworking.sendAbilityState(player);
        }
    }

    private static void vanishFx(ServerWorld world, Vec3d pos) {
        world.spawnParticles(ParticleTypes.LARGE_SMOKE, pos.x, pos.y + .9D, pos.z, 18, .35D, .7D, .35D, .025D);
        world.spawnParticles(ParticleTypes.REVERSE_PORTAL, pos.x, pos.y + .9D, pos.z, 24, .4D, .8D, .4D, .08D);
    }

    public static void onKill(ServerPlayerEntity player) {
        if (ACTIVE.containsKey(player.getUuid()) || AbilityCooldowns.remainingTicks(player, SmoothClasses.id("preparation")) > 0)
            finish(player.getUuid(), player, false);
    }

    public static float empower(DamageSource source, float amount) {
        return source.isOf(DamageTypes.PLAYER_ATTACK) && source.getAttacker() instanceof ServerPlayerEntity player
                && EMPOWERED.contains(player.getUuid()) ? amount * 1.3F : amount;
    }

    public static void confirmedHit(DamageSource source) {
        if (source.isOf(DamageTypes.PLAYER_ATTACK) && source.getAttacker() instanceof ServerPlayerEntity player)
            EMPOWERED.remove(player.getUuid());
    }

    private static boolean free(ServerPlayerEntity player, Vec3d pos) {
        Box box = player.getBoundingBox().offset(pos.subtract(player.getPos()));
        return player.getServerWorld().getWorldBorder().contains(box)
                && pos.y >= player.getWorld().getBottomY()
                && pos.y + player.getHeight() < player.getWorld().getTopY()
                && player.getWorld().isSpaceEmpty(player, box);
    }

    private static Vec3d safeDestination(ServerPlayerEntity player, Vec3d requested) {
        if (free(player, requested)) return requested;
        Vec3d up = requested.add(0, 1, 0);
        return free(player, up) ? up : null;
    }

    /** New aim-driven entry point used by the Shadow input packet. */
    public static ExecutionResult castAimed(ServerPlayerEntity player, int entityId, Vec3d blockHit) {
        if (!AbilityRuntime.isClass(player, AssassinClass.ID)) return ExecutionResult.failure("You are not an Assassin.");
        if (!"preparation".equals(SignatureAbilityDispatcher.selectedAbility(player)))
            return ExecutionResult.failure("Shadow Technique is not selected.");
        if (DASHES.containsKey(player.getUuid())) return ExecutionResult.failure("Shadow movement is already in progress.");

        Shadow old = ACTIVE.get(player.getUuid());
        if (old != null) return beginReturn(player, old);

        long cooldown = AbilityCooldowns.remainingTicks(player, SmoothClasses.id("preparation"));
        if (cooldown > 0) return ExecutionResult.failure("Shadow Technique is on cooldown.");

        int range = range(player);
        LivingEntity target = null;
        Vec3d destination;

        if (entityId >= 0) {
            Entity entity = player.getServerWorld().getEntityById(entityId);
            if (!(entity instanceof LivingEntity living) || living == player || !living.isAlive())
                return ExecutionResult.failure("That shadow target is no longer valid.");
            if (living.isTeammate(player)
                    || (living instanceof net.minecraft.entity.player.PlayerEntity other && !player.shouldDamagePlayer(other)))
                return ExecutionResult.failure("You cannot shadow-step to that target.");
            if (player.getEyePos().squaredDistanceTo(living.getBoundingBox().getCenter()) > (range + 2D) * (range + 2D))
                return ExecutionResult.failure("That target is outside shadow range.");
            if (!player.canSee(living)) return ExecutionResult.failure("That target is not visible.");

            target = living;
            double behind = Math.max(1.4D, (target.getWidth() + player.getWidth()) * .5D + .45D);
            Vec3d back = Vec3d.fromPolar(0F, target.getYaw()).multiply(-behind);
            destination = safeDestination(player, target.getPos().add(back));
        } else {
            if (blockHit == null) return ExecutionResult.failure("Aim at a creature or block, then release Shadow Technique.");
            Vec3d eye = player.getEyePos();
            Vec3d offset = blockHit.subtract(eye);
            if (offset.lengthSquared() > (range + .75D) * (range + .75D))
                return ExecutionResult.failure("That block is outside shadow range.");
            if (offset.lengthSquared() < .001D) return ExecutionResult.failure("No safe shadow destination.");

            // Never trust the client-selected point by itself. Re-raycast from the
            // server player's current view so block Shadow steps cannot pass through
            // walls or lock a point the player is no longer actually aiming at.
            HitResult serverHit = player.raycast(range, 1F, false);
            if (serverHit.getType() == HitResult.Type.MISS
                    || serverHit.getPos().squaredDistanceTo(blockHit) > 2.25D)
                return ExecutionResult.failure("That shadow destination is no longer in sight.");

            Vec3d requested = blockHit.subtract(offset.normalize().multiply(.65D));
            destination = safeDestination(player, requested);
        }

        if (destination == null) return ExecutionResult.failure("No safe space at the destination.");

        ShadowAnchorEntity anchor = SmoothEntities.SHADOW_ANCHOR.create(player.getWorld());
        if (anchor == null) return ExecutionResult.failure("Cannot create shadow.");
        anchor.refreshPositionAndAngles(player.getX(), player.getY(), player.getZ(), player.getYaw(), 0);
        if (!player.getWorld().spawnEntity(anchor)) return ExecutionResult.failure("Cannot create shadow.");

        ACTIVE.put(player.getUuid(), new Shadow(player.getServerWorld(), anchor,
                player.getWorld().getTime() + SHADOW_LIFETIME_TICKS, player.getYaw(), player.getPitch()));
        EMPOWERED.add(player.getUuid());

        UUID blindTarget = target == null ? null : target.getUuid();
        DASHES.put(player.getUuid(), new Dash(player.getServerWorld(), player.getPos(), destination,
                player.getYaw(), player.getPitch(), 0, false, blindTarget));
        SmoothClassesNetworking.sendAbilityState(player);
        SkillFx.sound(player, "soundeffect_39", .6F, 1.2F);
        return ExecutionResult.success(1, "Shadow Technique");
    }

    private static ExecutionResult beginReturn(ServerPlayerEntity player, Shadow shadow) {
        if (player.getServerWorld() != shadow.world || !free(player, shadow.anchor.getPos()))
            return ExecutionResult.failure("The return point is obstructed.");
        DASHES.put(player.getUuid(), new Dash(shadow.world, player.getPos(), shadow.anchor.getPos(),
                shadow.yaw, shadow.pitch, 0, true, null));
        return ExecutionResult.success(1, "Returning to shadow");
    }

    private static void tickDash(UUID id, ServerPlayerEntity player) {
        Dash dash = DASHES.get(id);
        if (dash == null) return;
        if (player == null || !player.isAlive() || player.getServerWorld() != dash.world) {
            finish(id, player, true);
            return;
        }

        int nextTick = dash.tick + 1;
        float raw = MathHelper.clamp(nextTick / (float) DASH_TICKS, 0F, 1F);
        float eased = raw * raw * (3F - 2F * raw); // smoothstep
        Vec3d pos = dash.start.lerp(dash.end, eased);
        player.teleport(dash.world, pos.x, pos.y, pos.z, dash.yaw, dash.pitch);
        player.fallDistance = 0F;
        dash.world.spawnParticles(ParticleTypes.REVERSE_PORTAL, pos.x, pos.y + .85D, pos.z,
                4, .18D, .4D, .18D, .02D);

        if (nextTick < DASH_TICKS) {
            DASHES.put(id, new Dash(dash.world, dash.start, dash.end, dash.yaw, dash.pitch,
                    nextTick, dash.returning, dash.blindTarget));
            return;
        }

        DASHES.remove(id);
        player.teleport(dash.world, dash.end.x, dash.end.y, dash.end.z, dash.yaw, dash.pitch);
        player.fallDistance = 0F;

        if (dash.returning) {
            finish(id, player, true);
            return;
        }

        if (dash.blindTarget != null) {
            Entity found = dash.world.getEntity(dash.blindTarget);
            if (found instanceof LivingEntity target && target.isAlive()) {
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 60, 0));
                if (target instanceof MobEntity mob) {
                    BLINDED.put(mob.getUuid(), (long) player.getServer().getTicks() + 60L);
                    mob.setTarget(null);
                    mob.getNavigation().stop();
                }

                // Shadow Strike upgrade: arrival performs a real vanilla main-hand attack.
                // This intentionally routes through PlayerEntity#attack so enchantments,
                // weapon damage, attack events and the existing +30% Shadow ambush bonus
                // all behave exactly like a normal melee hit.
                if (AbilityRuntime.hasTalent(player, AssassinContent.PREPARATION_SHADOWSTRIKE.id())
                        && player.squaredDistanceTo(target) <= 16D
                        && player.canSee(target)) {
                    player.attack(target);
                    SkillFx.sound(player, "soundeffect_39", .7F, .82F);
                    dash.world.spawnParticles(ParticleTypes.SWEEP_ATTACK,
                            target.getX(), target.getBodyY(.55D), target.getZ(), 1, 0D, 0D, 0D, 0D);
                }
            }
        }
        SmoothClassesNetworking.sendAbilityState(player);
    }

    /** Legacy dispatcher path kept for compatibility; it now only returns to an existing shadow. */
    public static ExecutionResult cast(ServerPlayerEntity player) {
        Shadow old = ACTIVE.get(player.getUuid());
        if (old != null) return beginReturn(player, old);
        return ExecutionResult.failure("Hold the Shadow Technique key to aim, then release on a target or block.");
    }
}
