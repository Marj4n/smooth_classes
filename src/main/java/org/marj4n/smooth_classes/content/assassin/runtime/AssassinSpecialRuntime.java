package org.marj4n.smooth_classes.content.assassin.runtime;

import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.marj4n.smooth_classes.SmoothClasses;
import org.marj4n.smooth_classes.content.assassin.AssassinClass;
import org.marj4n.smooth_classes.effects.SmoothEffects;
import org.marj4n.smooth_classes.runtime.AbilityCooldowns;
import org.marj4n.smooth_classes.runtime.AbilityRuntime;
import org.marj4n.smooth_classes.runtime.ExecutionResult;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Assassin H special: intentional combat stealth instead of waiting for passive RNG. */
public final class AssassinSpecialRuntime {
    public static final Identifier ID = SmoothClasses.id("class_special_vanish");
    public static final int ACTIVE_TICKS = 6 * 20;
    public static final int COOLDOWN_TICKS = 22 * 20;
    private static final Map<UUID, Long> ACTIVE_UNTIL = new HashMap<>();

    private AssassinSpecialRuntime() {}

    public static ExecutionResult activate(ServerPlayerEntity player) {
        if (!AbilityRuntime.isClass(player, AssassinClass.ID))
            return ExecutionResult.failure("Only Assassin can use Vanish.");
        if (player.hasStatusEffect(SmoothEffects.REVEALED))
            return ExecutionResult.failure("Revealed prevents Vanish.");
        if (active(player)) return ExecutionResult.failure("Vanish is already active.");
        long remaining = remainingTicks(player);
        if (remaining > 0L)
            return ExecutionResult.failure("Vanish can be used again in " + seconds(remaining) + "s.");

        long now = player.getServer().getTicks();
        ACTIVE_UNTIL.put(player.getUuid(), now + ACTIVE_TICKS);
        AbilityCooldowns.start(player, ID, COOLDOWN_TICKS);
        player.addStatusEffect(new StatusEffectInstance(SmoothEffects.STEALTH, ACTIVE_TICKS, 0, false, false, true));

        for (MobEntity mob : player.getServerWorld().getEntitiesByClass(MobEntity.class,
                player.getBoundingBox().expand(18D), mob -> mob.getTarget() == player)) {
            mob.setTarget(null);
        }
        player.getServerWorld().spawnParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                player.getX(), player.getBodyY(0.45D), player.getZ(), 22, 0.7D, 0.6D, 0.7D, 0.04D);
        player.getServerWorld().spawnParticles(ParticleTypes.POOF,
                player.getX(), player.getBodyY(0.45D), player.getZ(), 30, 0.65D, 0.7D, 0.65D, 0.08D);
        player.getServerWorld().playSound(null, player.getBlockPos(), SoundEvents.ENTITY_ENDERMAN_TELEPORT,
                SoundCategory.PLAYERS, 0.35F, 1.65F);
        player.sendMessage(Text.literal("Vanish").formatted(Formatting.DARK_PURPLE), true);
        return ExecutionResult.success(1, "Vanish broke nearby aggro and granted Stealth for 6s.");
    }

    public static void tick(ServerPlayerEntity player) {
        Long until = ACTIVE_UNTIL.get(player.getUuid());
        if (until == null) return;
        if (!AbilityRuntime.isClass(player, AssassinClass.ID) || !player.isAlive()
                || player.getServer().getTicks() >= until || !player.hasStatusEffect(SmoothEffects.STEALTH)) {
            ACTIVE_UNTIL.remove(player.getUuid());
        }
    }

    public static boolean active(ServerPlayerEntity player) {
        return ACTIVE_UNTIL.getOrDefault(player.getUuid(), 0L) > player.getServer().getTicks()
                && player.hasStatusEffect(SmoothEffects.STEALTH);
    }
    public static long remainingTicks(ServerPlayerEntity player) {
        return AbilityCooldowns.remainingTicks(player, ID);
    }
    public static int cooldownTicks() { return COOLDOWN_TICKS; }
    public static void cleanup(ServerPlayerEntity player) { ACTIVE_UNTIL.remove(player.getUuid()); }
    public static void clear() { ACTIVE_UNTIL.clear(); }
    private static int seconds(long ticks) { return (int) Math.ceil(Math.max(0L, ticks) / 20.0D); }
}
