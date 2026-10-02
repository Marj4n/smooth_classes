package org.marj4n.smooth_classes.runtime;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.spell_power.api.SpellPower;
import net.spell_power.api.SpellSchools;
import org.marj4n.smooth_classes.config.SmoothBalance;

/** Server-authoritative persistent ability cooldowns. */
public final class AbilityCooldowns {
    private AbilityCooldowns() {}

    public static boolean ready(ServerPlayerEntity player, Identifier ability) {
        return remainingTicks(player, ability) <= 0L;
    }

    public static long remainingTicks(ServerPlayerEntity player, Identifier ability) {
        MinecraftServer server = player.getServer();
        if (server == null) return 0L;
        long millis = AbilityCooldownState.get(server)
                .remainingMillis(player.getUuid(), ability, System.currentTimeMillis());
        return millis <= 0L ? 0L : Math.max(1L, (long) Math.ceil(millis / 50.0D));
    }

    /** 1.10.x cooldown formula: only haste above the neutral 1.0 multiplier reduces CD. */
    public static int adjustedTicks(ServerPlayerEntity player, int baseTicks) {
        double baseMs = Math.max(0, baseTicks) * 50.0D;
        double hasteBonus = Math.max(0.0D, SpellPower.getHaste(player, SpellSchools.GENERIC) - 1.0D);
        double reducedMs = baseMs - (hasteBonus * (2000.0D * SmoothBalance.General.spellHasteCooldownReductionModifier));
        double minimumMs = SmoothBalance.General.minimumAchievableCooldown * 1000.0D;
        reducedMs = Math.max(minimumMs, reducedMs);
        return Math.max(1, (int) Math.ceil(reducedMs / 50.0D));
    }

    public static void start(ServerPlayerEntity player, Identifier ability, int ticks) {
        MinecraftServer server = player.getServer();
        if (server == null) return;
        long durationMillis = Math.max(0L, (long) ticks) * 50L;
        AbilityCooldownState.get(server).start(
                player.getUuid(), ability, durationMillis, System.currentTimeMillis());
    }

    /** Explicit admin/reset hook. Normal logout must never call this. */
    public static void clear(ServerPlayerEntity player) {
        MinecraftServer server = player.getServer();
        if (server != null) AbilityCooldownState.get(server).clearPlayer(player.getUuid());
    }

}
