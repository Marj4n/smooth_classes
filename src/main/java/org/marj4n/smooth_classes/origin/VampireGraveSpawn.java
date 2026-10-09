package org.marj4n.smooth_classes.origin;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.LanternBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.enums.BedPart;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.Heightmap;

/**
 * One-time Vampire grave: a compact 5x5 (interior), four-block-high sealed crypt.
 * The soil above it is an actual digging puzzle. The only teleport happens before
 * entering the real bed/coffin sleeping pose; waking uses vanilla bed positioning.
 */
public final class VampireGraveSpawn {
    public static final String AWAKENED_FLAG = "vampire.grave_awakened";
    private static final String INTRO_SLEEPING = "vampire.grave_intro_sleeping";
    private static final int ROOM_RADIUS = 2;  // 5x5 walkable footprint
    private static final int SHELL_RADIUS = 3; // 7x7 including surrounding stone walls
    private static final int INTERIOR_HEIGHT = 4;

    private VampireGraveSpawn() { }

    public static boolean awakenOnce(ServerPlayerEntity player, OriginState state) {
        if (state.origin() != OriginType.VAMPIRE || state.hasFlag(AWAKENED_FLAG)) return false;
        ServerWorld world = player.getServer().getOverworld();
        BlockPos center = locate(world, player.getBlockPos());
        if (center == null) {
            player.sendMessage(Text.literal("No safe grave site was found nearby. Vampire Origin remains selected.")
                    .formatted(Formatting.YELLOW), false);
            return false;
        }

        cancelIntro(player, state);
        build(world, center);
        state.flag(AWAKENED_FLAG);

        // This is the sole intentional position change: from origin selection to coffin.
        // Do not teleport again during or after awakening. Let bed/coffin handle waking.
        BlockPos head = coffinHead(center);
        player.teleport(world, head.getX() + 0.5D, head.getY() + 0.1D,
                head.getZ() + 0.5D, 180.0F, 0.0F);
        player.setVelocity(0.0D, 0.0D, 0.0D);
        player.fallDistance = 0.0F;
        player.sleep(head);
        state.longProgress(INTRO_SLEEPING, 1L);
        state.sunExposure(0);
        player.extinguish();

        player.networkHandler.sendPacket(new TitleFadeS2CPacket(12, 55, 18));
        player.networkHandler.sendPacket(new TitleS2CPacket(
                Text.literal("THE FORGOTTEN CRYPT").formatted(Formatting.DARK_RED)));
        player.networkHandler.sendPacket(new SubtitleS2CPacket(
                Text.literal("Beneath the earth, you awaken.").formatted(Formatting.GRAY)));
        player.playSound(SoundEvents.BLOCK_WOODEN_DOOR_CLOSE, 0.55F, 0.63F);
        return true;
    }

    /**
     * Observe natural sleep/wake only. Never force time-of-day, wake the player on a
     * timer, or teleport them to the room aisle after leaving the coffin.
     */
    public static void tickIntro(ServerPlayerEntity player, OriginState state) {
        if (state.longProgress(INTRO_SLEEPING) == 0L) return;
        if (state.origin() != OriginType.VAMPIRE) {
            cancelIntro(player, state);
            return;
        }
        if (player.isSleeping()) {
            state.sunExposure(0);
            player.extinguish();
            return;
        }
        state.longProgress(INTRO_SLEEPING, 0L);
        state.sunExposure(0);
        player.extinguish();
        player.sendMessage(Text.literal(
                "The earth seals your grave. A shovel and journal rest beside the coffin.")
                .formatted(Formatting.GRAY), false);
    }

    public static void cancelIntro(ServerPlayerEntity player, OriginState state) {
        if (state.longProgress(INTRO_SLEEPING) == 0L) return;
        state.longProgress(INTRO_SLEEPING, 0L);
        if (player.isSleeping()) player.wakeUp(true, true);
    }

    /** OP-only replay for existing test worlds; never resets the progression tree. */
    public static boolean replayForTest(ServerPlayerEntity player) {
        OriginState state = OriginRuntime.state(player);
        if (state.origin() != OriginType.VAMPIRE) return false;
        boolean alreadyAwakened = state.hasFlag(AWAKENED_FLAG);
        state.unflag(AWAKENED_FLAG);
        boolean spawned = awakenOnce(player, state);
        if (!spawned && alreadyAwakened) state.flag(AWAKENED_FLAG);
        return spawned;
    }

    private static BlockPos locate(ServerWorld world, BlockPos around) {
        // Natural, mostly flat ground only: do not carve into buildings or fluids.
        for (int radius = 16; radius <= 160; radius += 8) {
            for (int point = 0; point < 12; point++) {
                double angle = (point / 12.0D) * Math.PI * 2D;
                int x = around.getX() + (int) Math.round(Math.cos(angle) * radius);
                int z = around.getZ() + (int) Math.round(Math.sin(angle) * radius);
                int top = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
                BlockPos center = new BlockPos(x, top - 8, z);
                if (safeSite(world, center, top)) return center;
            }
        }
        return null;
    }

    private static boolean safeSite(ServerWorld world, BlockPos center, int top) {
        if (top < 65 || top > 180 || !world.getWorldBorder().contains(center)) return false;
        for (int dx = -SHELL_RADIUS; dx <= SHELL_RADIUS; dx++) {
            for (int dz = -SHELL_RADIUS; dz <= SHELL_RADIUS; dz++) {
                int x = center.getX() + dx, z = center.getZ() + dz;
                int localTop = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
                if (Math.abs(localTop - top) > 1) return false;
                BlockState surface = world.getBlockState(new BlockPos(x, localTop - 1, z));
                if (!isNaturalSurface(surface)) return false;
                for (int y = center.getY(); y < top; y++) {
                    BlockPos p = new BlockPos(x, y, z);
                    BlockState state = world.getBlockState(p);
                    if (world.getBlockEntity(p) != null || !state.getFluidState().isEmpty()) return false;
                    if (y < localTop - 1 && (state.isAir() || !state.isOpaqueFullCube(world, p))) return false;
                }
                for (int above = top; above <= top + 3; above++) {
                    BlockPos p = new BlockPos(x, above, z);
                    BlockState state = world.getBlockState(p);
                    if (world.getBlockEntity(p) != null || !state.getFluidState().isEmpty()) return false;
                    if (!state.isAir() && !state.isReplaceable()) return false;
                }
            }
        }
        return true;
    }

    private static boolean isNaturalSurface(BlockState state) {
        return state.isIn(BlockTags.DIRT) || state.isOf(Blocks.GRASS_BLOCK)
                || state.isOf(Blocks.SAND) || state.isOf(Blocks.RED_SAND)
                || state.isOf(Blocks.GRAVEL) || state.isOf(Blocks.STONE)
                || state.isOf(Blocks.SNOW_BLOCK) || state.isOf(Blocks.MUD);
    }

    private static BlockPos coffinHead(BlockPos center) {
        return center.add(-1, 1, 1);
    }

    private static void build(ServerWorld world, BlockPos center) {
        // dy=0 floor, dy=1..4 open room, dy=5/6 dirt ceiling, dy=7 natural grave level.
        // Only the 5x5 central surface is dressed; surrounding terrain stays natural.
        for (int dx = -SHELL_RADIUS; dx <= SHELL_RADIUS; dx++) {
            for (int dz = -SHELL_RADIUS; dz <= SHELL_RADIUS; dz++) {
                for (int dy = 0; dy <= 7; dy++) {
                    if (dy == 7 && (Math.abs(dx) > ROOM_RADIUS || Math.abs(dz) > ROOM_RADIUS)) continue;
                    BlockPos p = center.add(dx, dy, dz);
                    boolean interior = Math.abs(dx) <= ROOM_RADIUS && Math.abs(dz) <= ROOM_RADIUS;
                    BlockState replacement;
                    if (dy == 0) {
                        replacement = floorMaterial(dx, dz);
                    } else if (dy <= INTERIOR_HEIGHT) {
                        replacement = interior ? Blocks.AIR.getDefaultState() : wallMaterial(dx, dy, dz);
                    } else if (dy == 5) {
                        // The diggable cap starts directly above the four-high room.
                        replacement = interior && (dx + dz) % 4 != 0
                                ? Blocks.ROOTED_DIRT.getDefaultState() : Blocks.DIRT.getDefaultState();
                    } else if (dy == 6) {
                        replacement = Blocks.DIRT.getDefaultState();
                    } else {
                        replacement = surfaceMaterial(dx, dz);
                    }
                    world.setBlockState(p, replacement, Block.NOTIFY_ALL);
                }
            }
        }

        // Compact ruined archwork: corner pedestals and old iron reinforcement.
        for (int x : new int[]{-2, 2}) {
            for (int z : new int[]{-2, 2}) {
                world.setBlockState(center.add(x, 1, z),
                        Blocks.CHISELED_POLISHED_BLACKSTONE.getDefaultState(), Block.NOTIFY_ALL);
                world.setBlockState(center.add(x, 2, z),
                        Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState(), Block.NOTIFY_ALL);
            }
        }
        for (int z : new int[]{-1, 1}) {
            world.setBlockState(center.add(-3, 2, z),
                    Blocks.IRON_BARS.getDefaultState(), Block.NOTIFY_ALL);
        }
        // Two low-light wall-side lamps (kept away from the coffin and excavation path).
        for (int[] pos : new int[][]{{-2, -1}, {2, 1}}) {
            BlockPos chain = center.add(pos[0], 4, pos[1]);
            world.setBlockState(chain, Blocks.CHAIN.getDefaultState(), Block.NOTIFY_ALL);
            world.setBlockState(chain.down(), Blocks.SOUL_LANTERN.getDefaultState()
                    .with(LanternBlock.HANGING, true), Block.NOTIFY_ALL);
        }
        world.setBlockState(center.add(2, 3, -2), Blocks.COBWEB.getDefaultState(), Block.NOTIFY_ALL);
        world.setBlockState(center.add(-2, 3, 2), Blocks.COBWEB.getDefaultState(), Block.NOTIFY_ALL);

        // Subtle pedestal in the floor, not a raised obstruction to waking.
        for (int x = -2; x <= 0; x++) {
            for (int z = 0; z <= 1; z++) {
                world.setBlockState(center.add(x, 0, z),
                        ((x + z) & 1) == 0 ? Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState()
                                : Blocks.CHISELED_POLISHED_BLACKSTONE.getDefaultState(), Block.NOTIFY_ALL);
            }
        }

        // The real, sleep-capable coffin. The long axis points south.
        Block coffin = coffinOrBed();
        BlockPos foot = center.add(-1, 1, 0);
        BlockPos head = coffinHead(center);
        BlockState bed = coffin.getDefaultState().with(BedBlock.FACING, Direction.SOUTH);
        int bedFlags = Block.NOTIFY_LISTENERS | Block.FORCE_STATE;
        world.setBlockState(head, bed.with(BedBlock.PART, BedPart.HEAD), bedFlags);
        world.setBlockState(foot, bed.with(BedBlock.PART, BedPart.FOOT), bedFlags);

        // Directly next to the coffin's foot, not on the opposite crypt wall.
        BlockPos chestPos = center.add(0, 1, 0);
        world.setBlockState(chestPos, Blocks.CHEST.getDefaultState()
                .with(ChestBlock.FACING, Direction.SOUTH), Block.NOTIFY_ALL);
        BlockEntity tile = world.getBlockEntity(chestPos);
        if (tile instanceof ChestBlockEntity chest) {
            chest.setStack(0, loreBook());
            chest.setStack(1, new ItemStack(Items.IRON_SHOVEL));
            chest.markDirty();
        }

        // No ladder, trapdoor, staircase or prepared escape tunnel. The player
        // digs through rooted dirt and topsoil above the central aisle.

        // Small 5x5 surface grave: uneven earth mound, cracked headstone, worn
        // stone border and two quiet blue lamps. No surrounding 9x9 build.
        for (int x = -1; x <= 0; x++) {
            for (int z = -1; z <= 1; z++) {
                world.setBlockState(center.add(x, 7, z), Blocks.PODZOL.getDefaultState(), Block.NOTIFY_ALL);
                world.setBlockState(center.add(x, 8, z),
                        (x + z) % 3 == 0 ? Blocks.ROOTED_DIRT.getDefaultState()
                                : Blocks.COARSE_DIRT.getDefaultState(), Block.NOTIFY_ALL);
            }
        }
        for (int z = -1; z <= 1; z++) {
            for (int x : new int[]{-2, 1}) {
                if (z != 0 || x == -2) {
                    world.setBlockState(center.add(x, 8, z),
                            Blocks.MOSSY_COBBLESTONE_SLAB.getDefaultState(), Block.NOTIFY_ALL);
                }
            }
        }
        // Weathered north headstone fits entirely within the 5x5 surface.
        world.setBlockState(center.add(-1, 8, -2), Blocks.MOSSY_STONE_BRICKS.getDefaultState(), Block.NOTIFY_ALL);
        world.setBlockState(center.add(-1, 9, -2), Blocks.CHISELED_STONE_BRICKS.getDefaultState(), Block.NOTIFY_ALL);
        world.setBlockState(center.add(-1, 10, -2), Blocks.STONE_BRICK_SLAB.getDefaultState(), Block.NOTIFY_ALL);
        world.setBlockState(center.add(1, 8, -2), Blocks.SOUL_LANTERN.getDefaultState(), Block.NOTIFY_ALL);
        world.setBlockState(center.add(1, 8, 2), Blocks.COBBLESTONE_SLAB.getDefaultState(), Block.NOTIFY_ALL);
    }

    private static BlockState floorMaterial(int x, int z) {
        int hash = Math.floorMod(x * 19 + z * 31 + x * z * 7, 13);
        if (hash < 2) return Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.getDefaultState();
        if (hash == 2) return Blocks.MOSSY_STONE_BRICKS.getDefaultState();
        return Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState();
    }

    private static BlockState wallMaterial(int x, int y, int z) {
        int hash = Math.floorMod(x * 13 + y * 23 + z * 11, 11);
        if (hash == 0) return Blocks.CHISELED_STONE_BRICKS.getDefaultState();
        if (hash < 3) return Blocks.CRACKED_STONE_BRICKS.getDefaultState();
        if (hash < 5) return Blocks.MOSSY_STONE_BRICKS.getDefaultState();
        return Blocks.STONE_BRICKS.getDefaultState();
    }

    private static BlockState surfaceMaterial(int x, int z) {
        int hash = Math.floorMod(x * 13 + z * 7 + x * z, 13);
        if (hash < 2) return Blocks.PODZOL.getDefaultState();
        if (hash < 4) return Blocks.COARSE_DIRT.getDefaultState();
        return Blocks.GRASS_BLOCK.getDefaultState();
    }

    private static Block coffinOrBed() {
        if (FabricLoader.getInstance().isModLoaded("bewitchment")) {
            Identifier id = new Identifier("bewitchment", "black_coffin");
            if (Registries.BLOCK.containsId(id)) {
                Block block = Registries.BLOCK.get(id);
                if (block instanceof BedBlock) return block;
            }
        }
        return Blocks.BLACK_BED;
    }

    private static ItemStack loreBook() {
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        NbtCompound nbt = book.getOrCreateNbt();
        nbt.putString("title", "The Forgotten Crypt");
        nbt.putString("author", "The Last Gravedigger");
        nbt.putInt("generation", 0);
        NbtList pages = new NbtList();
        pages.add(net.minecraft.nbt.NbtString.of(Text.Serializer.toJson(Text.literal(
                "To the one who rises from this coffin...\n\nYou were not buried because you died.\n\nYou were buried because they feared what you would become."))));
        pages.add(net.minecraft.nbt.NbtString.of(Text.Serializer.toJson(Text.literal(
                "The stones remember your name, though the living have forgotten it.\n\nThe last light of day cannot reach you here. Wait for the night to answer."))));
        pages.add(net.minecraft.nbt.NbtString.of(Text.Serializer.toJson(Text.literal(
                "A shovel rests in the chest beside you. Above the roof lies only soil and an old grave. Dig your way to freedom.\n\nThe dead do not always stay dead."))));
        nbt.put("pages", pages);
        return book;
    }
}
