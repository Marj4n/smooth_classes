package org.marj4n.smooth_classes.origin;

import net.minecraft.block.BlockState;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.marj4n.smooth_classes.network.SmoothClassesNetworking;

/**
 * Server-authoritative ceiling roost for the tiny Vampire Bat Form.
 * A stationary, airborne bat close to a solid ceiling attaches after 40 ticks.
 * The player stays fixed until sneaking, losing the supporting block, or leaving Bat Form.
 * No new key binding and no custom client packet are required.
 */
public final class VampireBatPerchRuntime {
    public static final String PERCHED = "vampire.bat.perched";
    private static final String STILL_TICKS = "vampire.bat.perch.still_ticks";
    private static final String ANCHOR_X = "vampire.bat.perch.anchor_x";
    private static final String ANCHOR_Y = "vampire.bat.perch.anchor_y";
    private static final String ANCHOR_Z = "vampire.bat.perch.anchor_z";
    private static final int REQUIRED_STILL_TICKS = 40;
    private static final double CEILING_MAX_GAP = 0.36D;

    private VampireBatPerchRuntime() {}

    public static void tick(ServerPlayerEntity player, OriginState state) {
        if (state.origin() != OriginType.VAMPIRE || !state.hasFlag("vampire.form.bat")
                || player.isSpectator()) {
            stop(player, state, false);
            return;
        }

        if (state.hasFlag(PERCHED)) {
            BlockPos support = anchor(state);
            // A portal, /tp, or dimension change must not drag the player back
            // to an old ceiling anchor.
            double anchorX = support.getX() + 0.5D;
            double anchorY = support.getY() - OriginBodyGeometry.BAT_HEIGHT - 0.025D;
            double anchorZ = support.getZ() + 0.5D;
            if (player.isSneaking() || !validCeiling(player, support)
                    || player.squaredDistanceTo(anchorX, anchorY, anchorZ) > 100.0D) {
                stop(player, state, true);
                return;
            }

            // Re-apply no gravity after reconnect and hold the stored attachment point.
            // Horizontal input is also suppressed on the client while perched.
            player.setNoGravity(true);
            player.setVelocity(Vec3d.ZERO);
            player.fallDistance = 0.0F;
            double requiredY = anchorY;
            double requiredX = anchorX;
            double requiredZ = anchorZ;
            if (player.squaredDistanceTo(requiredX, requiredY, requiredZ) > 0.025D) {
                player.teleport(player.getServerWorld(), requiredX, requiredY, requiredZ,
                        player.getYaw(), player.getPitch());
            }
            return;
        }

        // Pressing sneak is also how the player detaches, so never perch during a descent.
        if (!player.getAbilities().flying || player.isOnGround() || player.isSneaking()
                || player.isTouchingWater() || player.hasVehicle()
                || player.getVelocity().lengthSquared() > 0.0025D) {
            state.longProgress(STILL_TICKS, 0L);
            return;
        }

        BlockPos support = ceilingAbove(player);
        if (support == null) {
            state.longProgress(STILL_TICKS, 0L);
            return;
        }
        int still = (int) state.longProgress(STILL_TICKS) + 1;
        if (still < REQUIRED_STILL_TICKS) {
            state.longProgress(STILL_TICKS, still);
            return;
        }
        state.longProgress(STILL_TICKS, 0L);
        state.longProgress(ANCHOR_X, support.getX() + 30_000_000L);
        state.longProgress(ANCHOR_Y, support.getY() + 4096L);
        state.longProgress(ANCHOR_Z, support.getZ() + 30_000_000L);
        state.flag(PERCHED);

        // Flight is disabled while physically attached; on release it is restored.
        player.getAbilities().flying = false;
        player.sendAbilitiesUpdate();
        player.setNoGravity(true);
        player.setVelocity(Vec3d.ZERO);
        player.fallDistance = 0.0F;
        player.teleport(player.getServerWorld(), support.getX() + 0.5D,
                support.getY() - OriginBodyGeometry.BAT_HEIGHT - 0.025D,
                support.getZ() + 0.5D, player.getYaw(), player.getPitch());
        SmoothClassesNetworking.sendOriginState(player);
    }

    /** Call on form exit as well as during the server tick to avoid leftover gravity flags. */
    public static void stop(ServerPlayerEntity player, OriginState state, boolean returnToFlight) {
        state.longProgress(STILL_TICKS, 0L);
        if (!state.unflag(PERCHED)) return;
        player.setNoGravity(false);
        if (returnToFlight && state.hasFlag("vampire.form.bat") && !player.isSpectator()) {
            player.getAbilities().allowFlying = true;
            player.getAbilities().flying = true;
            player.sendAbilitiesUpdate();
        }
        player.setVelocity(Vec3d.ZERO);
        SmoothClassesNetworking.sendOriginState(player);
    }

    private static BlockPos anchor(OriginState state) {
        return new BlockPos((int) (state.longProgress(ANCHOR_X) - 30_000_000L),
                (int) (state.longProgress(ANCHOR_Y) - 4096L), (int) (state.longProgress(ANCHOR_Z) - 30_000_000L));
    }

    private static BlockPos ceilingAbove(ServerPlayerEntity player) {
        double top = player.getBoundingBox().maxY;
        BlockPos ceiling = BlockPos.ofFloored(player.getX(), top + CEILING_MAX_GAP, player.getZ());
        if (ceiling.getY() < top - 0.06D || ceiling.getY() - top > CEILING_MAX_GAP) return null;
        return validCeiling(player, ceiling) ? ceiling : null;
    }

    private static boolean validCeiling(ServerPlayerEntity player, BlockPos pos) {
        if (!player.getWorld().isChunkLoaded(pos.getX() >> 4, pos.getZ() >> 4)) return false;
        BlockState block = player.getWorld().getBlockState(pos);
        // Full downward face: stone, logs, and bottom-facing full-support slabs;
        // no leaves, fences, or other flimsy/partial attachment points.
        return block.isSideSolidFullSquare(player.getWorld(), pos, Direction.DOWN);
    }
}
