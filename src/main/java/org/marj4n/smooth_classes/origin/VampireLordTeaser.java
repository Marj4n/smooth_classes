package org.marj4n.smooth_classes.origin;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * One-off lore cliffhanger awarded upon unlocking Vampire Lord.
 * Seven COSMETIC lightning strikes, spaced around the player, culminate in a
 * written book. The existing Lord abilities/progression remain unchanged.
 * The progress counter, start time and reward flag are persisted in OriginState
 * to avoid accidental duplicate books across relogs or server restarts.
 */
public final class VampireLordTeaser {
    private static final String START_TICK = "vampire.lord.teaser.start_tick";
    private static final String BOOK_READY_TICK = "vampire.lord.teaser.book_ready_tick";
    private static final String STRIKES = "vampire.lord.teaser.strikes";
    private static final String REWARDED = "vampire.lord.teaser.book_awarded";

    private static final int BOLT_COUNT = 7;
    private static final int TICKS_BETWEEN_BOLTS = 14;
    private static final int TICKS_AFTER_LAST_BOLT = 48;
    private static final double RADIUS = 3.8D;

    private VampireLordTeaser() { }

    /** Called only on the first successful unlocking of the final Vampire node. */
    public static void begin(ServerPlayerEntity player, OriginState state) {
        if (state.origin() != OriginType.VAMPIRE || state.hasFlag(REWARDED)) return;
        if (state.longProgress(START_TICK) != 0L || state.longProgress(BOOK_READY_TICK) != 0L) return;
        state.progress(STRIKES, 0);
        state.longProgress(START_TICK, Math.max(1L, player.getServerWorld().getTime()));
        player.sendMessage(Text.literal("Seven thunders answer the broken Grail...")
                .formatted(Formatting.DARK_PURPLE), true);
    }

    /** OP-only preview on a disposable test world; does NOT unlock Vampire Lord. */
    public static boolean replayForTest(ServerPlayerEntity player) {
        OriginState state = OriginRuntime.state(player);
        if (state.origin() != OriginType.VAMPIRE) return false;
        state.unflag(REWARDED);
        state.longProgress(START_TICK, 0L);
        state.longProgress(BOOK_READY_TICK, 0L);
        state.progress(STRIKES, 0);
        VampireTransformationEffects.playLordEvolution(player);
        begin(player, state);
        return true;
    }

    /** Runs during normal server ticks; no external scheduler or forced time changes. */
    public static void tick(ServerPlayerEntity player, OriginState state) {
        if (state.origin() != OriginType.VAMPIRE || state.hasFlag(REWARDED)) return;
        long now = player.getServerWorld().getTime();
        long start = state.longProgress(START_TICK);
        if (start == 0L) return;

        int fired = state.progress(STRIKES);
        if (fired < BOLT_COUNT) {
            if (now >= start + (long) fired * TICKS_BETWEEN_BOLTS) {
                strike(player, fired);
                state.progress(STRIKES, fired + 1);
                if (fired + 1 == BOLT_COUNT) {
                    state.longProgress(BOOK_READY_TICK, now + TICKS_AFTER_LAST_BOLT);
                }
            }
            return;
        }

        long bookAt = state.longProgress(BOOK_READY_TICK);
        if (bookAt > 0L && now >= bookAt) {
            // Set first: this is the only point that grants the book. Never duplicate
            // it on relog even if the inventory is full (we drop it at the player).
            if (!state.flag(REWARDED)) return;
            state.longProgress(START_TICK, 0L);
            state.longProgress(BOOK_READY_TICK, 0L);
            deliverBook(player);
        }
    }

    private static void strike(ServerPlayerEntity player, int index) {
        ServerWorld world = player.getServerWorld();
        double angle = Math.toRadians(player.getYaw()) + (Math.PI * 2.0D * index / BOLT_COUNT);
        double x = player.getX() + Math.cos(angle) * RADIUS;
        double z = player.getZ() + Math.sin(angle) * RADIUS;
        double y = player.getY();

        // In 1.20.1 cosmetic bolts still draw the complete lightning effect, but
        // cannot deal lightning damage, ignite the ground or summon lightning mobs.
        LightningEntity bolt = EntityType.LIGHTNING_BOLT.create(world);
        if (bolt != null) {
            bolt.setCosmetic(true);
            bolt.refreshPositionAfterTeleport(x, y, z);
            world.spawnEntity(bolt);
        }
        world.spawnParticles(ParticleTypes.ELECTRIC_SPARK,
                x, y + 0.50D, z, 18, 0.22D, 0.65D, 0.22D, 0.09D);
        world.spawnParticles(ParticleTypes.CRIMSON_SPORE,
                x, y + 0.15D, z, 13, 0.28D, 0.35D, 0.28D, 0.03D);
    }

    private static void deliverBook(ServerPlayerEntity player) {
        ItemStack book = makeBook();
        if (!player.getInventory().insertStack(book)) {
            player.dropItem(book, false);
        }
        player.getInventory().markDirty();
        player.playerScreenHandler.sendContentUpdates();
        player.getServerWorld().playSound(null, player.getBlockPos(),
                SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 0.9F, 0.72F);
        player.networkHandler.sendPacket(new TitleFadeS2CPacket(10, 55, 25));
        player.networkHandler.sendPacket(new TitleS2CPacket(
                Text.literal("THE UNWRITTEN TESTAMENT").formatted(Formatting.DARK_RED)));
        player.networkHandler.sendPacket(new SubtitleS2CPacket(
                Text.literal("The Grail has not told its last secret.").formatted(Formatting.GRAY)));
        player.sendMessage(Text.literal("A strange book has appeared. Its final page remains unwritten...")
                .formatted(Formatting.LIGHT_PURPLE), false);
    }

    private static ItemStack makeBook() {
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        NbtCompound nbt = book.getOrCreateNbt();
        nbt.putString("title", "The Unwritten Testament");
        nbt.putString("author", "Unknown");
        nbt.putInt("generation", 0);

        NbtCompound display = new NbtCompound();
        display.putString("Name", Text.Serializer.toJson(
                Text.literal("The Unwritten Testament").formatted(Formatting.DARK_PURPLE, Formatting.ITALIC)));
        nbt.put("display", display);

        NbtList pages = new NbtList();
        pages.add(page("SEVEN THUNDERS\n\nThe seven veils are broken.\nSeven thunders answered your awakening.\n\nBut thunder does not explain why a forgotten heart began to beat."));
        pages.add(page("ONE GRAVE\n\nHundreds slept beneath the chapel.\nOnly one coffin stirred when the Holy Grail shattered.\n\nThe priests named it a curse. The gravedigger called it mercy."));
        pages.add(page("THE UNANSWERED CALL\n\nDid the Grail truly choose you?\nOr did something hidden within its fragments recognize what the living had forgotten?\n\nEven the dead may carry secrets."));
        pages.add(NbtString.of(Text.Serializer.toJson(
                Text.literal("THE NEXT CHAPTER\n\nThe answer lies beyond the Seventh Veil.\n\n")
                        .append(Text.literal("COMING SOON").formatted(Formatting.DARK_RED, Formatting.BOLD)))));
        nbt.put("pages", pages);
        return book;
    }

    private static NbtString page(String content) {
        return NbtString.of(Text.Serializer.toJson(Text.literal(content)));
    }
}
