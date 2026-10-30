package org.marj4n.smooth_classes.origin;

import net.minecraft.block.*;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.enums.BedPart;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.Heightmap;
import java.util.*;

/** Conservative placement: no destructive fallback, no occupied chunks, exact reserved-plot snapshots. */
final class VampireGraveyardSites {
    private static final int MARGIN = 14; // Covers blendTerrain's complete write footprint.
    private VampireGraveyardSites() {}

    static BlockPos findChurch(ServerWorld world, net.minecraft.util.math.Vec3i templateSize) {
        BlockPos spawn = world.getSpawnPos();
        int rotation = (int) Math.floorMod(world.getSeed(), 8L);
        for (int distance = 512; distance <= 1536; distance += 512) {
            for (int i = 0; i < 8; i++) {
                double angle = (i + rotation) * Math.PI / 4.0D;
                int x = spawn.getX() + (int) Math.round(Math.cos(angle) * distance);
                int z = spawn.getZ() + (int) Math.round(Math.sin(angle) * distance);
                int top = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
                if (top < 58 || top > 205) continue;
                BlockPos origin = new BlockPos(x - 27, top - 6, z - 27);
                BlockPos min = origin.add(-MARGIN, -1, -MARGIN);
                BlockPos max = origin.add(templateSize.getX() - 1 + MARGIN, templateSize.getY() + 1, templateSize.getZ() - 1 + MARGIN);
                if (!withinWorld(world, min, max)) continue;
                // Reject steep/wet sites cheaply, before loading all footprint chunks.
                int lowest = Integer.MAX_VALUE, highest = Integer.MIN_VALUE;
                boolean dry = true;
                for (int dx = -MARGIN; dx < templateSize.getX() + MARGIN; dx += 16) {
                    for (int dz = -MARGIN; dz < templateSize.getZ() + MARGIN; dz += 16) {
                        int sx = origin.getX() + dx, sz = origin.getZ() + dz;
                        int sy = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, sx, sz) - 1;
                        lowest = Math.min(lowest, sy); highest = Math.max(highest, sy);
                        dry &= world.getFluidState(new BlockPos(sx, sy, sz)).isEmpty();
                    }
                }
                if (!dry || highest - lowest > 8) continue;
                if (!freshChunks(world, min, max)) continue;
                // Check every column, including the ocean-floor foundation used by
                // terrain blending. Coarse samples alone can miss a narrow valley.
                for (int sx = min.getX(); sx <= max.getX() && dry; sx++) {
                    for (int sz = min.getZ(); sz <= max.getZ(); sz++) {
                        int motion = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, sx, sz) - 1;
                        int floor = world.getTopY(Heightmap.Type.OCEAN_FLOOR, sx, sz) - 1;
                        lowest = Math.min(lowest, Math.min(motion, floor));
                        highest = Math.max(highest, Math.max(motion, floor));
                        if (highest - lowest > 8 || !world.getFluidState(new BlockPos(sx, motion, sz)).isEmpty()) {
                            dry = false;
                            break;
                        }
                    }
                }
                if (!dry) continue;
                // Blend writes are below the local surface; scan down far enough to
                // cover the supported terrain delta as well as the template itself.
                BlockPos scanMin = new BlockPos(min.getX(), Math.max(world.getBottomY() + 1, Math.min(min.getY(), lowest - 2)), min.getZ());
                BlockPos scanMax = new BlockPos(max.getX(), Math.max(max.getY(), highest + 2), max.getZ());
                if (!naturalVolume(world, scanMin, scanMax)) continue;
                if (world.getOtherEntities(null, new Box(scanMin, scanMax.add(1, 1, 1)), e -> e instanceof PlayerEntity || e.hasCustomName()).isEmpty()) return origin;
            }
        }
        return null;
    }

    static void reserveMore(ServerWorld world, VampireGraveyardState state) {
        int free = state.availableCount();
        // New batches expand the surrounding rings into untouched land. Existing
        // reservations remain usable even after players visit the church chunks.
        int checks = 0;
        while (free < 8 && checks++ < 64) {
            int index = state.takeCandidate();
            if (index < 0) return;
            var offset = VampireGraveyardLayout.offset(index);
            int x = state.church().getX() + 27 + offset.x();
            int z = state.church().getZ() + 27 + offset.z();
            BlockPos horizontalMin = new BlockPos(x - 4, world.getBottomY() + 1, z - 4);
            BlockPos horizontalMax = new BlockPos(x + 4, world.getTopY() - 1, z + 4);
            if (!world.getWorldBorder().contains(horizontalMin) || !world.getWorldBorder().contains(horizontalMax)) continue;
            if (!freshChunks(world, horizontalMin, horizontalMax)) continue;
            int surface = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
            BlockPos center = new BlockPos(x, surface - 6, z);
            BlockPos min = center.add(-4, -1, -4), max = center.add(4, 10, 4);
            if (!withinWorld(world, min, max)) continue;
            boolean flat = true;
            for (int dx = -4; dx <= 4 && flat; dx++) {
                for (int dz = -4; dz <= 4; dz++) {
                    int height = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x + dx, z + dz) - 1;
                    if (Math.abs(height - surface) > 2) { flat = false; break; }
                }
            }
            if (!flat || !naturalVolume(world, min, max)) continue;
            state.reserve(center, fingerprint(world, center), false);
            free++;
        }
    }

    static boolean emptyOfEntities(ServerWorld world, BlockPos center) {
        return world.getOtherEntities(null, new Box(center.add(-4, 0, -4), center.add(5, 11, 5))).isEmpty();
    }

    static long fingerprint(ServerWorld world, BlockPos center) {
        long hash = 0xcbf29ce484222325L;
        for (BlockPos pos : BlockPos.iterate(center.add(-4, -1, -4), center.add(4, 10, 4))) {
            // Registry names + property values are stable across reloads and loader
            // registry remapping; numeric raw block-state IDs are deliberately avoided.
            hash = hashText(hash, world.getBlockState(pos).toString());
            var tile = world.getBlockEntity(pos);
            if (tile != null) hash = hashText(hash, tile.createNbt().toString());
        }
        return hash;
    }

    private static long hashText(long hash, String text) {
        for (int i = 0; i < text.length(); i++) hash = (hash ^ text.charAt(i)) * 0x100000001b3L;
        return hash;
    }

    static boolean freshChunks(ServerWorld world, BlockPos min, BlockPos max) {
        for (int x = min.getX() >> 4; x <= max.getX() >> 4; x++) {
            for (int z = min.getZ() >> 4; z <= max.getZ() >> 4; z++) {
                if (world.getChunk(x, z).getInhabitedTime() > 0L) return false;
            }
        }
        return true;
    }

    private static boolean withinWorld(ServerWorld world, BlockPos min, BlockPos max) {
        return min.getY() > world.getBottomY() && max.getY() < world.getTopY()
                && world.getWorldBorder().contains(min) && world.getWorldBorder().contains(max);
    }

    private static boolean naturalVolume(ServerWorld world, BlockPos min, BlockPos max) {
        for (BlockPos pos : BlockPos.iterate(min, max)) {
            BlockState block = world.getBlockState(pos);
            if (world.getBlockEntity(pos) != null || !block.getFluidState().isEmpty() || !natural(block)) return false;
        }
        return true;
    }

    private static boolean natural(BlockState state) {
        return state.isAir() || state.isIn(BlockTags.DIRT) || state.isIn(BlockTags.BASE_STONE_OVERWORLD)
                || state.isIn(BlockTags.SAND) || state.isIn(BlockTags.TERRACOTTA)
                || state.isIn(BlockTags.LOGS) || state.isIn(BlockTags.LEAVES)
                || state.getBlock() instanceof PlantBlock
                || state.isOf(Blocks.GRAVEL) || state.isOf(Blocks.CLAY)
                || state.isOf(Blocks.SNOW) || state.isOf(Blocks.SNOW_BLOCK)
                || state.isOf(Blocks.ICE) || state.isOf(Blocks.PACKED_ICE) || state.isOf(Blocks.BLUE_ICE);
    }

    static BlockPos findLegacyCrypt(ServerWorld world, BlockPos around, int radiusChunks) {
        return findLegacyCrypt(world, around, radiusChunks, true);
    }

    static BlockPos findLegacyCrypt(ServerWorld world, BlockPos around, int radiusChunks, boolean requireJournal) {
        List<BlockPos> matches = new ArrayList<>();
        int cx = around.getX() >> 4, cz = around.getZ() >> 4;
        for (int x = cx - radiusChunks; x <= cx + radiusChunks; x++) {
            for (int z = cz - radiusChunks; z <= cz + radiusChunks; z++) {
                // Discover old churches only in loaded chunks. Never generate an
                // enormous search area or inspect offline player save files.
                if (!world.isChunkLoaded(x, z)) continue;
                for (var tile : world.getChunk(x, z).getBlockEntities().values()) {
                    if (!(tile instanceof ChestBlockEntity chest)) continue;
                    BlockPos center = tile.getPos().down();
                    if (!legacyGeometry(world, center)) continue;
                    boolean journal = false;
                    for (int slot = 0; slot < chest.size(); slot++) {
                        NbtCompound tag = chest.getStack(slot).getNbt();
                        if (tag != null && "The Forgotten Crypt".equals(tag.getString("title"))
                                && "The Last Gravedigger".equals(tag.getString("author"))) { journal = true; break; }
                    }
                    if (journal || !requireJournal) matches.add(center);
                }
            }
        }
        return matches.stream().min(Comparator.comparingDouble(p -> p.getSquaredDistance(around))).orElse(null);
    }

    static boolean legacyGeometry(ServerWorld world, BlockPos center) {
        if (!(world.getBlockEntity(center.up()) instanceof ChestBlockEntity)) return false;
        BlockState bed = world.getBlockState(center.add(-1, 1, 1));
        if (!(bed.getBlock() instanceof BedBlock) || bed.get(BedBlock.PART) != BedPart.HEAD) return false;
        for (int x : new int[]{-2, 2}) for (int z : new int[]{-2, 2}) {
            if (!world.getBlockState(center.add(x, 1, z)).isOf(Blocks.CHISELED_POLISHED_BLACKSTONE)
                    || !world.getBlockState(center.add(x, 2, z)).isOf(Blocks.POLISHED_BLACKSTONE_BRICKS)) return false;
        }
        return true;
    }
}
