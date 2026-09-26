package org.marj4n.smooth_classes.runtime;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.spell_power.api.SpellPower;
import net.spell_power.api.SpellSchools;
import org.marj4n.smooth_classes.config.SmoothBalance;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Server-authoritative signature cooldowns. Puffish owns unlocks; this owns only cast timing. */
public final class AbilityCooldowns {
    private static final Map<UUID, Map<Identifier, Long>> READY_AT = new HashMap<>();

    private AbilityCooldowns() {}

    public static boolean ready(ServerPlayerEntity player, Identifier ability) {
        return remainingTicks(player, ability) <= 0;
    }

    public static long remainingTicks(ServerPlayerEntity player, Identifier ability) {
        long now = player.getServerWorld().getTime();
        long readyAt = READY_AT.getOrDefault(player.getUuid(), Map.of()).getOrDefault(ability, 0L);
        return Math.max(0L, readyAt - now);
    }


    /** Continued 1.10.x cooldown formula: only haste above the neutral 1.0 multiplier reduces CD. */
    public static int adjustedTicks(ServerPlayerEntity player, int baseTicks) {
        double baseMs = Math.max(0, baseTicks) * 50.0D;
        double hasteBonus = Math.max(0.0D, SpellPower.getHaste(player, SpellSchools.GENERIC) - 1.0D);
        double reducedMs = baseMs - (hasteBonus * (2000.0D * SmoothBalance.General.spellHasteCooldownReductionModifier));
        double minimumMs = SmoothBalance.General.minimumAchievableCooldown * 1000.0D;
        reducedMs = Math.max(minimumMs, reducedMs);
        return Math.max(1, (int)Math.ceil(reducedMs / 50.0D));
    }

    public static void start(ServerPlayerEntity player, Identifier ability, int ticks) {
        READY_AT.computeIfAbsent(player.getUuid(), ignored -> new HashMap<>())
                .put(ability, player.getServerWorld().getTime() + Math.max(0, ticks));
    }

    public static void clear(ServerPlayerEntity player) {
        READY_AT.remove(player.getUuid());
    }
}
