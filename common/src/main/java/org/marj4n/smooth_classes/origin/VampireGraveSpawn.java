package org.marj4n.smooth_classes.origin;

import org.marj4n.smooth_classes.platform.PlatformLoader;
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
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.world.Heightmap;

/**
 * One persistent church per Overworld, with UUID-owned crypts around it.
 * New Vampires awaken beneath their assigned grave; subsequent joins never
 * rebuild the church, duplicate a crypt, or refill the starting chest.
 */
public final class VampireGraveSpawn {
    public static final String AWAKENED_FLAG = "vampire.grave_awakened";
    private static final String INTRO_SLEEPING = "vampire.grave_intro_sleeping";
    private static final int ROOM_RADIUS = 2;   // 5x5 interior footprint
    private static final int SHELL_RADIUS = 3;  // 7x7 including stone walls
    private static final int INTERIOR_HEIGHT = 4;

    private static final Identifier CHURCH_TEMPLATE = new Identifier("smooth_classes", "hauntedchurch_original");
    // One of the ORIGINAL grave plots (next to an existing ordinary R.I.P. sign).
    // Anchor is in schematic coordinates, no special surface gravestone is spawned.
    private static final int GRAVE_X = 22;
    private static final int GRAVE_Z = 44;
    private static final int GRAVE_SURFACE_Y = 6;
    private static final int TERRAIN_BLEND_RADIUS = 10;

    private VampireGraveSpawn() { }

    private static final java.util.Map<net.minecraft.server.MinecraftServer, java.util.LinkedHashSet<java.util.UUID>> PENDING = new java.util.WeakHashMap<>();

    public static void register() {
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % 40 != 0) return;
            var queue = PENDING.get(server);
            if (queue == null || queue.isEmpty()) return;
            java.util.UUID id = queue.iterator().next();
            queue.remove(id);
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(id);
            if (player != null && stateEligible(player)) awakenOnce(player, OriginRuntime.state(player));
        });
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(PENDING::remove);
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (stateEligible(handler.player)) server.execute(() -> awakenOnce(handler.player, OriginRuntime.state(handler.player)));
        });
        net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            if (stateEligible(newPlayer)) PENDING.computeIfAbsent(newPlayer.getServer(), ignored -> new java.util.LinkedHashSet<>()).add(newPlayer.getUuid());
        });
    }

    private static boolean stateEligible(ServerPlayerEntity player) {
        OriginState state = OriginRuntime.state(player);
        return player.isAlive() && state.origin() == OriginType.VAMPIRE && !state.hasFlag(AWAKENED_FLAG);
    }

    public static boolean awakenOnce(ServerPlayerEntity player, OriginState state) {
        if (state.origin() != OriginType.VAMPIRE || state.hasFlag(AWAKENED_FLAG)) return false;
        ServerWorld world = player.getServer().getOverworld();
        VampireGraveyardState cemetery = VampireGraveyardState.get(player.getServer());
        // A reset Origin/player flag must never duplicate a grave or refill its loot.
        if (cemetery.ownedBy(player.getUuid()) != null) {
            state.flag(AWAKENED_FLAG);
            return false;
        }
        if (!ensureChurch(world, cemetery)) {
            player.sendMessage(Text.literal("No untouched site was found for the shared Vampire church. No terrain was replaced. An administrator can adopt an existing crypt with /smoothclasses origin grave adopt.")
                    .formatted(Formatting.YELLOW), false);
            return false;
        }

        VampireGraveyardState.Plot selected = null;
        for (int batch = 0; batch < 2 && selected == null; batch++) {
            for (VampireGraveyardState.Plot plot : cemetery.plots()) {
                if (!plot.available()) continue;
                if (VampireGraveyardSites.fingerprint(world, plot.center()) != plot.fingerprint()) {
                    cemetery.block(plot); // Preserve player edits and naturally changed plots.
                    continue;
                }
                if (VampireGraveyardSites.emptyOfEntities(world, plot.center())) { selected = plot; break; }
            }
            if (selected == null && batch == 0) VampireGraveyardSites.reserveMore(world, cemetery);
        }
        if (selected == null) {
            if (cemetery.nextCandidate() < VampireGraveyardLayout.MAX_CANDIDATES || cemetery.availableCount() > 0) {
                var queue = PENDING.computeIfAbsent(player.getServer(), ignored -> new java.util.LinkedHashSet<>());
                queue.add(player.getUuid());
                if (state.longProgress("vampire.grave_wait_notice") == 0L) {
                    state.longProgress("vampire.grave_wait_notice", 1L);
                    player.sendMessage(Text.literal("Preparing a safe grave around the shared church. Your awakening will begin when a plot is ready.").formatted(Formatting.GRAY), false);
                }
            } else {
                player.sendMessage(Text.literal("No untouched grave plots remain around the shared church. No existing graves or buildings were replaced.").formatted(Formatting.YELLOW), false);
            }
            return false;
        }

        // Reserve UUID ownership before placing anything. All calls run on the
        // server thread, including queued joins, so two players cannot claim a plot.
        cemetery.claim(player.getUuid(), selected);
        buildCrypt(world, selected.center());
        if (!selected.original()) buildHeadstone(world, selected.center(), player.getGameProfile().getName());
        cemetery.finish(selected);
        state.flag(AWAKENED_FLAG);
        return beginIntro(player, state, selected.center());
    }

    private static boolean ensureChurch(ServerWorld world, VampireGraveyardState cemetery) {
        if (cemetery.hasChurch()) return true;
        // Adopt a verifiable old crypt near spawn instead of placing a second church.
        BlockPos oldCenter = VampireGraveyardSites.findLegacyCrypt(world, world.getSpawnPos(), 12);
        if (oldCenter != null) {
            adopt(world, cemetery, oldCenter);
            return true;
        }
        StructureTemplate template = world.getStructureTemplateManager().getTemplateOrBlank(CHURCH_TEMPLATE);
        if (template.getSize().getX() != 54 || template.getSize().getZ() != 54) return false;
        BlockPos origin = VampireGraveyardSites.findChurch(world, template.getSize());
        if (origin == null) return false;
        if (!placeChurch(world, origin, template)) return false;
        cemetery.establishChurch(origin);
        BlockPos original = origin.add(GRAVE_X, 0, GRAVE_Z);
        cemetery.reserve(original, VampireGraveyardSites.fingerprint(world, original), true);
        VampireGraveyardSites.reserveMore(world, cemetery);
        return true;
    }

    private static void adopt(ServerWorld world, VampireGraveyardState cemetery, BlockPos oldCenter) {
        cemetery.establishChurch(oldCenter.add(-GRAVE_X, 0, -GRAVE_Z));
        // The old crypt may already belong to a legacy player. Never refill or
        // give its coffin/chest to a newcomer when UUID ownership is unknown.
        var old = cemetery.reserve(oldCenter, VampireGraveyardSites.fingerprint(world, oldCenter), true);
        cemetery.block(old);
    }

    public static boolean adoptExisting(ServerPlayerEntity player) {
        VampireGraveyardState cemetery = VampireGraveyardState.get(player.getServer());
        if (cemetery.hasChurch()) return false;
        ServerWorld world = player.getServer().getOverworld();
        if (player.getServerWorld() != world) return false;
        BlockPos center = VampireGraveyardSites.findLegacyCrypt(world, player.getBlockPos(), 2, false);
        if (center == null) return false;
        adopt(world, cemetery, center);
        return true;
    }

    public static String status(ServerPlayerEntity player) {
        VampireGraveyardState cemetery = VampireGraveyardState.get(player.getServer());
        if (!cemetery.hasChurch()) return "No shared Vampire church has been registered in this world.";
        return "Shared church (Overworld): " + cemetery.church().toShortString()
                + ". Assigned graves: " + cemetery.claimedCount() + "; reserved free plots: " + cemetery.availableCount() + ".";
    }

    private static boolean beginIntro(ServerPlayerEntity player, OriginState state, BlockPos center) {
        ServerWorld world = player.getServer().getOverworld();
        cancelIntro(player, state);
        state.longProgress("vampire.grave_wait_notice", 0L);
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
        player.networkHandler.sendPacket(new TitleS2CPacket(Text.literal("THE FORGOTTEN CRYPT").formatted(Formatting.DARK_RED)));
        player.networkHandler.sendPacket(new SubtitleS2CPacket(Text.literal("Beneath the earth, you awaken.").formatted(Formatting.GRAY)));
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

    /** OP replay reuses the existing coffin; it never rebuilds a church or refills loot. */
    public static boolean replayForTest(ServerPlayerEntity player) {
        OriginState state = OriginRuntime.state(player);
        if (state.origin() != OriginType.VAMPIRE) return false;
        var plot = VampireGraveyardState.get(player.getServer()).ownedBy(player.getUuid());
        if (plot == null) return awakenOnce(player, state);
        if (!plot.built()) return false;
        BlockState bed = player.getServer().getOverworld().getBlockState(coffinHead(plot.center()));
        if (!(bed.getBlock() instanceof BedBlock) || bed.get(BedBlock.PART) != BedPart.HEAD) return false;
        return beginIntro(player, state, plot.center());
    }

    private static BlockPos coffinHead(BlockPos center) {
        return center.add(-1, 1, 1);
    }

    /** Called once for the shared church, never for individual player joins. */
    private static boolean placeChurch(ServerWorld world, BlockPos templateOrigin, StructureTemplate template) {
        StructurePlacementData placement = new StructurePlacementData()
                .setIgnoreEntities(true).setPlaceFluids(false).setUpdateNeighbors(false);
        if (!template.place(world, templateOrigin, templateOrigin, placement, world.getRandom(), Block.NOTIFY_LISTENERS)) return false;
        blendTerrain(world, templateOrigin, template.getSize().getX(), template.getSize().getZ());
        return true;
    }

    /** Excavate only the assigned 7x7 crypt. The church is never re-placed here. */
    private static void buildCrypt(ServerWorld world, BlockPos center) {
        // y=0 is the floor, y=1..4 is the interior, y=5..6 is DIGGABLE
        // soil, y=7+ is the UNMODIFIED original graveyard from hauntedchurch.schem.
        // Actual interior: exactly 5x5 and four blocks high.
        for (int dx = -SHELL_RADIUS; dx <= SHELL_RADIUS; dx++) {
            for (int dz = -SHELL_RADIUS; dz <= SHELL_RADIUS; dz++) {
                for (int dy = 0; dy <= 6; dy++) {
                    BlockPos pos = center.add(dx, dy, dz);
                    boolean interior = Math.abs(dx) <= ROOM_RADIUS && Math.abs(dz) <= ROOM_RADIUS;
                    BlockState replacement;
                    if (dy == 0) {
                        replacement = floorMaterial(dx, dz);
                    } else if (dy <= INTERIOR_HEIGHT) {
                        replacement = interior ? Blocks.AIR.getDefaultState() : wallMaterial(dx, dy, dz);
                    } else {
                        // Two layers of natural, shovel-diggable dirt, including
                        // the crypt cap. Never replace the nisan above at y >= 7.
                        replacement = ((dx + dz + dy) & 3) == 0
                                ? Blocks.ROOTED_DIRT.getDefaultState() : Blocks.DIRT.getDefaultState();
                    }
                    world.setBlockState(pos, replacement, Block.NOTIFY_ALL);
                }
            }
        }

        // Compact floor pedestals, wall bars and eerie low light unchanged.
        for (int x : new int[]{-2, 2}) {
            for (int z : new int[]{-2, 2}) {
                world.setBlockState(center.add(x, 1, z),
                        Blocks.CHISELED_POLISHED_BLACKSTONE.getDefaultState(), Block.NOTIFY_ALL);
                world.setBlockState(center.add(x, 2, z),
                        Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState(), Block.NOTIFY_ALL);
            }
        }
        for (int z : new int[]{-1, 1}) {
            world.setBlockState(center.add(-3, 2, z), Blocks.IRON_BARS.getDefaultState(), Block.NOTIFY_ALL);
        }
        for (int[] lamp : new int[][]{{-2, -1}, {2, 1}}) {
            BlockPos chain = center.add(lamp[0], 4, lamp[1]);
            world.setBlockState(chain, Blocks.CHAIN.getDefaultState(), Block.NOTIFY_ALL);
            world.setBlockState(chain.down(), Blocks.SOUL_LANTERN.getDefaultState()
                    .with(LanternBlock.HANGING, true), Block.NOTIFY_ALL);
        }
        world.setBlockState(center.add(2, 3, -2), Blocks.COBWEB.getDefaultState(), Block.NOTIFY_ALL);
        world.setBlockState(center.add(-2, 3, 2), Blocks.COBWEB.getDefaultState(), Block.NOTIFY_ALL);

        for (int x = -2; x <= 0; x++) {
            for (int z = 0; z <= 1; z++) {
                world.setBlockState(center.add(x, 0, z),
                        ((x + z) & 1) == 0 ? Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState()
                                : Blocks.CHISELED_POLISHED_BLACKSTONE.getDefaultState(), Block.NOTIFY_ALL);
            }
        }

        // Existing sleep-capable Bewitchment coffin or fallback black bed.
        // One placement, one natural sleep / wake: no post-wake teleport.
        Block coffin = coffinOrBed();
        BlockPos foot = center.add(-1, 1, 0);
        BlockPos head = coffinHead(center);
        BlockState bed = coffin.getDefaultState().with(BedBlock.FACING, Direction.SOUTH);
        int bedFlags = Block.NOTIFY_LISTENERS | Block.FORCE_STATE;
        world.setBlockState(head, bed.with(BedBlock.PART, BedPart.HEAD), bedFlags);
        world.setBlockState(foot, bed.with(BedBlock.PART, BedPart.FOOT), bedFlags);

        // The starting journal and iron shovel are beside the coffin, as before.
        BlockPos chestPos = center.add(0, 1, 0);
        world.setBlockState(chestPos, Blocks.CHEST.getDefaultState()
                .with(ChestBlock.FACING, Direction.SOUTH), Block.NOTIFY_ALL);
        BlockEntity tile = world.getBlockEntity(chestPos);
        if (tile instanceof ChestBlockEntity chest) {
            chest.setStack(0, loreBook());
            chest.setStack(1, new ItemStack(Items.IRON_SHOVEL));
            chest.markDirty();
        }
    }

    private static void buildHeadstone(ServerWorld world, BlockPos center, String playerName) {
        world.setBlockState(center.add(0, 7, -1), Blocks.STONE_BRICKS.getDefaultState(), Block.NOTIFY_ALL);
        for (int x = -1; x <= 1; x++)
            world.setBlockState(center.add(x, 8, -1), Blocks.STONE_BRICKS.getDefaultState(), Block.NOTIFY_ALL);
        BlockPos signPos = center.add(0, 7, 0);
        world.setBlockState(signPos, Blocks.OAK_SIGN.getDefaultState().with(net.minecraft.block.SignBlock.ROTATION, 0), Block.NOTIFY_ALL);
        if (world.getBlockEntity(signPos) instanceof net.minecraft.block.entity.SignBlockEntity sign) {
            sign.setText(sign.getFrontText().withMessage(0, Text.literal("R.I.P."))
                    .withMessage(1, Text.literal(playerName)).withMessage(2, Text.literal("The Forgotten")), true);
            sign.markDirty();
        }
    }

    private static void blendTerrain(ServerWorld world, BlockPos templateOrigin, int sizeX, int sizeZ) {
        int minX = templateOrigin.getX();
        int maxX = minX + sizeX - 1;
        int minZ = templateOrigin.getZ();
        int maxZ = minZ + sizeZ - 1;
        int plateauY = templateOrigin.getY() + GRAVE_SURFACE_Y;
        int seaLevel = world.getSeaLevel();

        for (int x = minX - TERRAIN_BLEND_RADIUS - 1; x <= maxX + TERRAIN_BLEND_RADIUS + 1; x++) {
            for (int z = minZ - TERRAIN_BLEND_RADIUS - 1; z <= maxZ + TERRAIN_BLEND_RADIUS + 1; z++) {
                int dx = x < minX ? (minX - x) : (x > maxX ? (x - maxX) : 0);
                int dz = z < minZ ? (minZ - z) : (z > maxZ ? (z - maxZ) : 0);
                if (dx == 0 && dz == 0) continue;

                double noise = terrainNoise(x, z);
                int localRadius = Math.max(5, TERRAIN_BLEND_RADIUS + ((columnHash(x, z) >>> 4) & 3) - 1);
                double distance = Math.sqrt(dx * dx + dz * dz) + noise * 0.85D;
                if (distance > localRadius + 0.75D) continue;

                int oceanFloorY = world.getTopY(Heightmap.Type.OCEAN_FLOOR, x, z) - 1;
                int motionTopY = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
                int naturalTopY = Math.max(oceanFloorY, motionTopY);
                if (naturalTopY < world.getBottomY() + 1) naturalTopY = world.getBottomY() + 1;

                double t = Math.max(0.0D, Math.min(1.0D, distance / (double) localRadius));
                int rimY = plateauY - (noise > 0.4D ? 0 : 1);
                int targetSurfaceY = (int) Math.round(lerp(rimY, naturalTopY, t));
                targetSurfaceY = Math.max(Math.min(targetSurfaceY, plateauY), naturalTopY);

                int foundationStartY = Math.max(world.getBottomY() + 1, Math.min(oceanFloorY + 1, targetSurfaceY));
                if (foundationStartY > targetSurfaceY) continue;

                BlockState topState = chooseBlendTop(world, x, z, targetSurfaceY, seaLevel);
                BlockState fillState = chooseBlendFill(world, x, z, targetSurfaceY, seaLevel);

                for (int y = foundationStartY; y <= targetSurfaceY; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    world.setBlockState(pos, y == targetSurfaceY ? topState : fillState, Block.NOTIFY_LISTENERS);
                }

                // Prevent a visibly straight man-made face by adding a little toe of
                // gravel/coarse dirt at the waterline / coastline on near-edge columns.
                if (distance < 2.35D) {
                    BlockPos toe = new BlockPos(x, Math.max(world.getBottomY() + 1, targetSurfaceY - 1), z);
                    if (world.getBlockState(toe.down()).isAir()) {
                        world.setBlockState(toe.down(), fillState, Block.NOTIFY_LISTENERS);
                    }
                }
            }
        }
    }

    private static BlockState chooseBlendTop(ServerWorld world, int x, int z, int y, int seaLevel) {
        BlockState sample = sampleSurface(world, x, z);
        if (sample.isOf(Blocks.RED_SAND) || sample.isOf(Blocks.RED_SANDSTONE)) {
            return Blocks.RED_SAND.getDefaultState();
        }
        if (sample.isOf(Blocks.SAND) || sample.isOf(Blocks.SANDSTONE) || sample.isOf(Blocks.SMOOTH_SANDSTONE)) {
            return Blocks.SAND.getDefaultState();
        }
        if (sample.isOf(Blocks.GRAVEL)) {
            return Blocks.GRAVEL.getDefaultState();
        }
        if (sample.isOf(Blocks.MUD) || sample.isOf(Blocks.MUDDY_MANGROVE_ROOTS)) {
            return Blocks.MUD.getDefaultState();
        }
        if (y <= seaLevel - 2) {
            return ((x + z) & 1) == 0 ? Blocks.GRAVEL.getDefaultState() : Blocks.SAND.getDefaultState();
        }
        if (y <= seaLevel) {
            int hash = Math.floorMod(x * 17 + z * 31, 5);
            if (hash == 0) return Blocks.GRAVEL.getDefaultState();
            if (hash < 3) return Blocks.COARSE_DIRT.getDefaultState();
            return Blocks.ROOTED_DIRT.getDefaultState();
        }
        return surfaceMaterial(x, z);
    }

    private static BlockState chooseBlendFill(ServerWorld world, int x, int z, int y, int seaLevel) {
        BlockState sample = sampleSurface(world, x, z);
        if (sample.isOf(Blocks.RED_SAND) || sample.isOf(Blocks.RED_SANDSTONE)) {
            return Blocks.RED_SANDSTONE.getDefaultState();
        }
        if (sample.isOf(Blocks.SAND) || sample.isOf(Blocks.SANDSTONE) || sample.isOf(Blocks.SMOOTH_SANDSTONE)) {
            return Blocks.SANDSTONE.getDefaultState();
        }
        if (sample.isOf(Blocks.GRAVEL)) {
            return Blocks.STONE.getDefaultState();
        }
        if (y <= seaLevel - 3) {
            return ((x ^ z) & 1) == 0 ? Blocks.STONE.getDefaultState() : Blocks.GRAVEL.getDefaultState();
        }
        int hash = Math.floorMod(x * 29 + z * 11 + y * 7, 9);
        if (hash < 2) return Blocks.STONE.getDefaultState();
        if (hash < 4) return Blocks.GRAVEL.getDefaultState();
        if (hash == 4) return Blocks.ROOTED_DIRT.getDefaultState();
        return Blocks.DIRT.getDefaultState();
    }

    private static BlockState sampleSurface(ServerWorld world, int x, int z) {
        int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
        if (y < world.getBottomY()) y = world.getBottomY();
        return world.getBlockState(new BlockPos(x, y, z));
    }

    private static int columnHash(int x, int z) {
        int h = x * 73428767 ^ z * 91293199;
        h ^= (h >>> 13);
        h *= 1274126177;
        return h;
    }

    private static double terrainNoise(int x, int z) {
        int h = columnHash(x, z);
        return ((h >>> 8) & 1023) / 511.5D - 1.0D;
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
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
        if (PlatformLoader.getInstance().isModLoaded("bewitchment")) {
            Identifier id = new Identifier("bewitchment", "black_coffin");
            if (Registries.BLOCK.containsId(id)) {
                Block block = Registries.BLOCK.get(id);
                if (block instanceof BedBlock) return block;
            }
        }
        return Blocks.BLACK_BED;
    }

    /** The last gravedigger's testimony; its secrets are discovered after waking. */
    private static ItemStack loreBook() {
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        NbtCompound nbt = book.getOrCreateNbt();
        nbt.putString("title", "The Forgotten Crypt");
        nbt.putString("author", "The Last Gravedigger");
        nbt.putInt("generation", 0);
        NbtList pages = new NbtList();
        addLorePage(pages, "If you are reading this, death has failed to keep you. "
                + "You were human once. Neither royalty nor legend, only another soul laid to rest beneath this church.");
        addLorePage(pages, "Your name has faded from our records. Decades passed, perhaps centuries. "
                + "The living forgot you, as they eventually forget us all. Your grave became one among hundreds.");
        addLorePage(pages, "Then the Holy Grail shattered. Something ancient seeped into the earth, "
                + "carrying a hunger older than mankind. Most graves remained silent. Yours did not.");
        addLorePage(pages, "The priests heard a heartbeat beneath your grave. They called it a curse, "
                + "built a stone chamber around your coffin, and bound your restless heart to silence.");
        addLorePage(pages, "I was ordered to leave you buried. Instead, I placed this journal "
                + "and an iron shovel beside your coffin. If a human soul survives within you, it deserves a chance.");
        addLorePage(pages, "The priests believed their bell and prayers could keep you asleep forever. "
                + "I do not know why the broken Grail chose your grave. I only know that you were once one of us.");
        addLorePage(pages, "Take the shovel. Break through the earth above you. "
                + "The sun may reject you; blood may sustain you. But what you become now is yours to decide.\n\n"
                + "- The Last Gravedigger");
        nbt.put("pages", pages);
        return book;
    }

    private static void addLorePage(NbtList pages, String text) {
        pages.add(net.minecraft.nbt.NbtString.of(
                Text.Serializer.toJson(Text.literal(text))));
    }

}
