package org.marj4n.smooth_classes.origin;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
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

/** One-time Overworld origin introduction. NOT a respawn mechanic. */
public final class VampireGraveSpawn {
    public static final String AWAKENED_FLAG = "vampire.grave_awakened";
    private static final int ROOM_RADIUS = 2; // 5x5 clear interior, 3 blocks tall
    private static final int SHELL_RADIUS = ROOM_RADIUS + 1;
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
        // Only mark complete after we have a valid site and before the teleport.
        build(world, center);
        state.flag(AWAKENED_FLAG);
        player.teleport(world, center.getX() + 0.5D, center.getY() + 1.0D,
                center.getZ() + 0.5D, 180.0F, 0.0F);
        player.setVelocity(0, 0, 0);
        player.fallDistance = 0;
        player.networkHandler.sendPacket(new TitleFadeS2CPacket(15, 75, 20));
        player.networkHandler.sendPacket(new TitleS2CPacket(Text.literal("THE FORGOTTEN GRAVE").formatted(Formatting.DARK_RED)));
        player.networkHandler.sendPacket(new SubtitleS2CPacket(Text.literal("You awaken beneath the earth...").formatted(Formatting.GRAY)));
        player.playSound(SoundEvents.BLOCK_WOODEN_DOOR_OPEN, 0.7F, 0.7F);
        player.sendMessage(Text.literal("Find the chest. Read the book. Use the iron shovel to dig your way out."), false);
        return true;
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
        // Prefer an undisturbed patch away from the exact world-spawn location.
        // Bounded search: avoid loading a huge area or scanning every chunk.
        for (int radius = 16; radius <= 112; radius += 8) {
            for (int point = 0; point < 12; point++) {
                double angle = (point / 12.0D) * Math.PI * 2D;
                int x = around.getX() + (int)Math.round(Math.cos(angle) * radius);
                int z = around.getZ() + (int)Math.round(Math.sin(angle) * radius);
                int top = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
                BlockPos center = new BlockPos(x, top - 7, z);
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
                // Don't bulldoze player buildings or structures. The surface must look natural.
                if (!isNaturalSurface(surface)) return false;
                for (int y = center.getY(); y < top; y++) {
                    BlockPos p = new BlockPos(x, y, z);
                    BlockState state = world.getBlockState(p);
                    if (world.getBlockEntity(p) != null || !state.getFluidState().isEmpty()) return false;
                    if (y < localTop - 1 && (state.isAir() || !state.isOpaqueFullCube(world, p))) return false;
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

    private static void build(ServerWorld world, BlockPos center) {
        // center.y is the floor level. Air cavity y+1 through y+3.
        // Three layers of diggable dirt cap the ceiling; grass at the surface.
        for (int dx = -SHELL_RADIUS; dx <= SHELL_RADIUS; dx++) {
            for (int dz = -SHELL_RADIUS; dz <= SHELL_RADIUS; dz++) {
                for (int dy = 0; dy <= 6; dy++) {
                    BlockPos p = center.add(dx, dy, dz);
                    boolean air = Math.abs(dx) <= ROOM_RADIUS && Math.abs(dz) <= ROOM_RADIUS
                            && dy >= 1 && dy <= 3;
                    BlockState replacement = air ? Blocks.AIR.getDefaultState()
                            : dy == 6 ? Blocks.GRASS_BLOCK.getDefaultState()
                            : dy == 0 ? Blocks.COARSE_DIRT.getDefaultState()
                            : Blocks.DIRT.getDefaultState();
                    world.setBlockState(p, replacement, Block.NOTIFY_ALL);
                }
            }
        }

        // Vanilla beds and Bewitchment coffins both extend BedBlock and use two halves.
        Block coffin = coffinOrBed();
        BlockPos foot = center.add(-1, 1, 0);
        BlockPos head = foot.south();
        BlockState bed = coffin.getDefaultState().with(BedBlock.FACING, Direction.SOUTH);
        // Skip neighbor shape checks during initial two-block placement.
        int flags = Block.NOTIFY_LISTENERS | Block.FORCE_STATE;
        world.setBlockState(head, bed.with(BedBlock.PART, BedPart.HEAD), flags);
        world.setBlockState(foot, bed.with(BedBlock.PART, BedPart.FOOT), flags);

        BlockPos chestPos = center.add(2, 1, -1);
        world.setBlockState(chestPos, Blocks.CHEST.getDefaultState().with(ChestBlock.FACING, Direction.WEST), Block.NOTIFY_ALL);
        BlockEntity tile = world.getBlockEntity(chestPos);
        if (tile instanceof ChestBlockEntity chest) {
            chest.setStack(0, loreBook());
            chest.setStack(1, new ItemStack(Items.IRON_SHOVEL));
            chest.markDirty();
        }
        world.setBlockState(center.add(-2, 1, -2), Blocks.TORCH.getDefaultState(), Block.NOTIFY_ALL);
    }

    private static Block coffinOrBed() {
        if (FabricLoader.getInstance().isModLoaded("bewitchment")) {
            Identifier id = new Identifier("bewitchment", "black_coffin");
            // Registry lookup keeps Bewitchment an OPTIONAL dependency.
            if (Registries.BLOCK.containsId(id)) return Registries.BLOCK.get(id);
        }
        return Blocks.RED_BED;
    }

    private static ItemStack loreBook() {
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        NbtCompound nbt = book.getOrCreateNbt();
        nbt.putString("title", "The Forgotten Grave");
        nbt.putString("author", "Unknown");
        nbt.putInt("generation", 0);
        NbtList pages = new NbtList();
        pages.add(net.minecraft.nbt.NbtString.of(Text.Serializer.toJson(Text.literal(
                "To whoever awakens within this coffin...\n\nYou were not buried because you died.\n\nYou were buried because they feared what you would become."))));
        pages.add(net.minecraft.nbt.NbtString.of(Text.Serializer.toJson(Text.literal(
                "Your name has been erased, and the world has forgotten you.\n\nBut death has refused to claim you. The blood has awakened once more."))));
        pages.add(net.minecraft.nbt.NbtString.of(Text.Serializer.toJson(Text.literal(
                "The earth above you is not a prison forever. Take the shovel. Dig your way out.\n\nFind those who buried you.\n\nThe dead do not always stay dead."))));
        nbt.put("pages", pages);
        return book;
    }
}
