package org.marj4n.smooth_classes.content.avenger.runtime;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import org.marj4n.smooth_classes.network.SmoothClassesNetworking;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Server-authoritative short-lived ritual choreography. Never physically buries an
 * entity inside terrain: the renderer provides the emergence transform while
 * the server keeps the mob collision-safe on the surface.
 *
 * Recall returns the soul/ingredients ONLY when the dissolve completes. On
 * logout it completes synchronously; on server shutdown it is cancelled, leaving
 * the original summoned mob intact. This avoids duping soul ledger entries.
 */
public final class AvengerSoulAnimationRuntime {
    public static final int EMERGE_TICKS = 18;
    public static final int ABSORB_TICKS = 14;
    public static final byte EMERGE = 0;
    public static final byte ABSORB = 1;

    private static final Map<UUID, Phase> ACTIVE = new HashMap<>();
    private static boolean registered;

    private AvengerSoulAnimationRuntime() {}

    public static void register() {
        if (registered) return;
        registered = true;
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (Phase phase : new ArrayList<>(ACTIVE.values())) {
                if (phase.entity.isRemoved() || phase.entity.getWorld() != phase.world) {
                    finish(phase, server);
                } else if (--phase.ticks <= 0) {
                    finish(phase, server);
                } else {
                    // Stop wandering/knockback until the body has fully materialized.
                    phase.entity.setVelocity(Vec3d.ZERO);
                    phase.entity.velocityModified = true;
                    if (phase.entity instanceof MobEntity mob) mob.getNavigation().stop();
                }
            }
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            // Refunds must reach the real inventory before the player data is saved.
            for (Phase phase : new ArrayList<>(ACTIVE.values())) {
                if (phase.owner.equals(handler.player.getUuid())) finish(phase, server);
            }
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            // Never persist a disabled-AI mob or half-finished recall across restart.
            for (Phase phase : new ArrayList<>(ACTIVE.values())) {
                if (phase.action == ABSORB) cancel(phase);
                else release(phase);
            }
            ACTIVE.clear();
        });
    }

    public static boolean busy(LivingEntity entity) {
        return entity != null && ACTIVE.containsKey(entity.getUuid());
    }

    public static void emerge(ServerPlayerEntity caster, LivingEntity entity) {
        if (entity == null || entity.isRemoved()) return;
        boolean ground = groundCompatible(entity);
        Phase phase = new Phase(caster.getUuid(), entity, caster.getServerWorld(), EMERGE, EMERGE_TICKS, null);
        ACTIVE.put(entity.getUuid(), phase);
        freeze(phase);
        caster.swingHand(net.minecraft.util.Hand.MAIN_HAND, true);
        broadcast(caster, entity, EMERGE, ground, EMERGE_TICKS);
    }

    public static boolean recall(ServerPlayerEntity caster, LivingEntity entity,
                                 AvengerSummonRecipes.Recipe recipe) {
        if (entity == null || entity.isRemoved() || recipe == null || busy(entity)) return false;
        Phase phase = new Phase(caster.getUuid(), entity, caster.getServerWorld(), ABSORB, ABSORB_TICKS, recipe);
        ACTIVE.put(entity.getUuid(), phase);
        entity.removeAllPassengers();
        entity.stopRiding();
        freeze(phase);
        caster.swingHand(net.minecraft.util.Hand.MAIN_HAND, true);
        broadcast(caster, entity, ABSORB, false, ABSORB_TICKS);
        return true;
    }

    private static void freeze(Phase phase) {
        phase.oldInvulnerable = phase.entity.isInvulnerable();
        phase.entity.setInvulnerable(true);
        if (phase.entity instanceof MobEntity mob) {
            phase.oldNoAi = mob.isAiDisabled();
            mob.setAiDisabled(true);
            mob.getNavigation().stop();
        }
        phase.entity.setVelocity(Vec3d.ZERO);
    }

    private static void release(Phase phase) {
        if (phase.entity.isRemoved()) return;
        phase.entity.setInvulnerable(phase.oldInvulnerable);
        if (phase.entity instanceof MobEntity mob) mob.setAiDisabled(phase.oldNoAi);
    }

    private static void finish(Phase phase, MinecraftServer server) {
        if (!ACTIVE.remove(phase.entity.getUuid(), phase)) return;
        if (phase.action == EMERGE) {
            release(phase);
            return;
        }
        // The state transition happens exactly once, even if an external mod
        // removed the minion while the absorption effect was playing.
        AvengerDeathListState state = AvengerDeathListState.get(server);
        state.restoreSoul(phase.owner, phase.recipe.entityId());
        ServerPlayerEntity caster = server.getPlayerManager().getPlayer(phase.owner);
        if (caster != null) {
            AvengerReworkRuntime.refundRecipeItem(caster, phase.recipe.mainItemId());
            if (phase.recipe.offhandItemId() != null) {
                AvengerReworkRuntime.refundRecipeItem(caster, phase.recipe.offhandItemId());
            }
            SmoothClassesNetworking.sendAbilityState(caster);
        } else {
            // Normally impossible because disconnect completes the phase.
            // Keep the summon if we cannot safely refund the ingredients.
            state.consumeSoul(phase.owner, phase.recipe.entityId());
            release(phase);
            return;
        }
        phase.entity.discard(); // Recall never calls a damage/kill hook.
    }

    private static void cancel(Phase phase) {
        release(phase);
    }

    private static boolean groundCompatible(LivingEntity entity) {
        if (entity.getHeight() > 3.5F || entity.getWorld().getBlockState(entity.getBlockPos().down()).isAir()) return false;
        EntityType<?> type = entity.getType();
        // Flying and swimming bodies materialize from a soul vortex instead.
        return type != EntityType.WITHER && type != EntityType.ENDER_DRAGON
                && type != EntityType.PHANTOM && type != EntityType.GHAST
                && type != EntityType.VEX && type != EntityType.BLAZE
                && type != EntityType.BAT && type != EntityType.COD
                && type != EntityType.SALMON && type != EntityType.TROPICAL_FISH
                && type != EntityType.PUFFERFISH && type != EntityType.SQUID
                && type != EntityType.GLOW_SQUID && type != EntityType.DOLPHIN
                && type != EntityType.GUARDIAN && type != EntityType.ELDER_GUARDIAN
                && type != EntityType.ALLAY && type != EntityType.BEE
                && type != EntityType.PARROT && type != EntityType.AXOLOTL
                && type != EntityType.TADPOLE;
    }

    private static void broadcast(ServerPlayerEntity caster, LivingEntity entity,
                                  byte action, boolean ground, int ticks) {
        // Immutable per-receiver packets: Fabric consumes the buffer when sending.
        var viewers = PlayerLookup.around(caster.getServerWorld(), entity.getPos(), 64D);
        for (ServerPlayerEntity viewer : viewers) send(viewer, caster, entity, action, ground, ticks);
        if (!viewers.contains(caster)) send(caster, caster, entity, action, ground, ticks);
    }

    private static void send(ServerPlayerEntity viewer, ServerPlayerEntity caster, LivingEntity entity,
                             byte action, boolean ground, int ticks) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(entity.getId());
        buf.writeUuid(caster.getUuid());
        buf.writeByte(action);
        buf.writeBoolean(ground);
        buf.writeVarInt(ticks);
        buf.writeDouble(entity.getX());
        buf.writeDouble(entity.getY());
        buf.writeDouble(entity.getZ());
        ServerPlayNetworking.send(viewer, SmoothClassesNetworking.AVENGER_SOUL_ANIMATION, buf);
    }

    private static final class Phase {
        final UUID owner;
        final LivingEntity entity;
        final ServerWorld world;
        final byte action;
        final AvengerSummonRecipes.Recipe recipe;
        int ticks;
        boolean oldNoAi;
        boolean oldInvulnerable;

        Phase(UUID owner, LivingEntity entity, ServerWorld world, byte action,
              int ticks, AvengerSummonRecipes.Recipe recipe) {
            this.owner = owner;
            this.entity = entity;
            this.world = world;
            this.action = action;
            this.ticks = ticks;
            this.recipe = recipe;
        }
    }
}
