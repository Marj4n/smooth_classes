package org.marj4n.smooth_classes.origin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import org.marj4n.smooth_classes.network.SmoothClassesNetworking;
import org.marj4n.smooth_classes.registry.SmoothSounds;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Hold-to-feed loop inspired by Vampirism: acquire a biteable target, play a bite,
 * continue feeding with looping audio, drain one of five target blood portions,
 * and refill the Vampire's Blood reservoir.
 */
public final class VampireFeedingRuntime {
    private static final Map<UUID, FeedState> ACTIVE = new HashMap<>();

    private VampireFeedingRuntime() {}

    public static void setTarget(ServerPlayerEntity player, int targetId) {
        if (targetId < 0) {
            ACTIVE.remove(player.getUuid());
            return;
        }
        OriginState state = OriginRuntime.state(player);
        if (state.origin() != OriginType.VAMPIRE
                || state.hasFlag("vampire.form.bat")
                || state.hasFlag("vampire.form.man_bat")) return;
        Entity raw = player.getWorld().getEntityById(targetId);
        if (!(raw instanceof LivingEntity living) || !valid(player, living)) return;
        ACTIVE.put(player.getUuid(), new FeedState(targetId));
        player.getWorld().playSound(null, player.getBlockPos(), SmoothSounds.get("origin_vampire_bite"),
                SoundCategory.PLAYERS, 0.8F, 0.95F + player.getRandom().nextFloat() * 0.1F);
    }

    public static void tick(ServerPlayerEntity player) {
        FeedState feed = ACTIVE.get(player.getUuid());
        if (feed == null) return;
        OriginState state = OriginRuntime.state(player);
        if (state.origin() != OriginType.VAMPIRE
                || state.hasFlag("vampire.form.bat")
                || state.hasFlag("vampire.form.man_bat")) {
            ACTIVE.remove(player.getUuid());
            return;
        }
        Entity raw = player.getWorld().getEntityById(feed.targetId);
        if (!(raw instanceof LivingEntity living) || !valid(player, living)) {
            ACTIVE.remove(player.getUuid());
            return;
        }

        feed.ticks++;

        // Drain one blood pip per second. Keep the loop audio event tied to the
        // actual drain tick so it cannot spam between pips or linger after RMB is released.
        if (feed.ticks % 20 != 0) return;
        player.getWorld().playSound(null, player.getBlockPos(), net.minecraft.sound.SoundEvents.ENTITY_GENERIC_DRINK,
                SoundCategory.PLAYERS, 0.28F, 0.86F + player.getRandom().nextFloat() * 0.06F);
        int capacity = OriginRuntime.vampireBloodCapacity(player);
        int gain = living instanceof VillagerEntity ? 16 : 4;
        if (living instanceof VillagerEntity && living.isSleeping()) gain += 4;

        float amount = Math.max(1.0F, living.getMaxHealth() / 5.0F);
        float next = living.getHealth() - amount;
        if (next <= 0.01F) {
            living.damage(player.getDamageSources().playerAttack(player), Float.MAX_VALUE);
            ACTIVE.remove(player.getUuid());
        } else {
            living.setHealth(next);
            living.timeUntilRegen = 0;
        }

        state.blood(Math.min(capacity, state.blood() + gain));
        state.lastFeedTick(player.age);
        OriginProgressTracker.onVampireFeed(player, living, gain);
        player.heal(1.0F);
        SmoothClassesNetworking.sendOriginState(player);
    }

    private static boolean valid(ServerPlayerEntity player, LivingEntity living) {
        if (!living.isAlive() || living == player || player.squaredDistanceTo(living) > 12.25D) return false;
        return living instanceof VillagerEntity || living instanceof AnimalEntity;
    }

    public static void cleanup(ServerPlayerEntity player) { ACTIVE.remove(player.getUuid()); }
    public static void clear() { ACTIVE.clear(); }

    private static final class FeedState {
        final int targetId;
        int ticks;
        FeedState(int targetId) { this.targetId = targetId; }
    }
}
