package org.marj4n.smooth_classes.runtime;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.marj4n.smooth_classes.SmoothClasses;
import org.marj4n.smooth_classes.effects.SmoothEffects;
import org.marj4n.smooth_classes.network.SmoothClassesNetworking;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Server-owned hold state. A release cannot shorten a completed cast's cooldown. */
public final class ArcaneSlashChargeRuntime {
    private static final Identifier ID = SmoothClasses.id("ascendancy_arcane_slash");
    private static final Map<UUID, Integer> HELD_AT = new HashMap<>();
    private static final Set<UUID> CHARGING = new HashSet<>();
    private ArcaneSlashChargeRuntime() {}

    public static void hold(ServerPlayerEntity player, boolean held) {
        if (held) HELD_AT.put(player.getUuid(), player.getServer().getTicks());
        else {
            HELD_AT.remove(player.getUuid());
            if (CHARGING.contains(player.getUuid()))
                player.removeStatusEffect(SmoothEffects.ARCANE_SLASH);
        }
    }

    public static boolean isHeld(ServerPlayerEntity player) {
        Integer tick = HELD_AT.get(player.getUuid());
        return tick != null && player.getServer().getTicks() - tick <= 12;
    }

    public static void begin(ServerPlayerEntity player) {
        CHARGING.add(player.getUuid());
        player.setSprinting(false);
    }

    public static boolean canContinue(ServerPlayerEntity player) {
        return CHARGING.contains(player.getUuid()) && isHeld(player)
                && player.isAlive() && !player.isSpectator()
                && ArcaneSlashVisuals.hasSword(player);
    }

    public static void finish(ServerPlayerEntity player) {
        if (!CHARGING.remove(player.getUuid())) return;
        AbilityCooldowns.start(player, ID, org.marj4n.smooth_classes.gameplay.AscendancyAbilityDispatcher.effectiveCooldownTicks(player,"arcane_slash"));
        SmoothClassesNetworking.sendAbilityState(player);
    }

    public static void removed(ServerPlayerEntity player) {
        if (!CHARGING.remove(player.getUuid())) return;
        // Cancellation is exactly four seconds, without haste reduction.
        AbilityCooldowns.start(player, ID, 80);
        SmoothClassesNetworking.sendAbilityState(player);
    }

    public static void disconnect(ServerPlayerEntity player) {
        if (CHARGING.remove(player.getUuid())) AbilityCooldowns.start(player, ID, 80);
        HELD_AT.remove(player.getUuid());
        player.removeStatusEffect(SmoothEffects.ARCANE_SLASH);
    }

    public static void clear() {
        CHARGING.clear();
        HELD_AT.clear();
    }
}
