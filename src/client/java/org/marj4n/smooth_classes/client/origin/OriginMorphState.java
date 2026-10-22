package org.marj4n.smooth_classes.client.origin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import org.marj4n.smooth_classes.origin.OriginRuntime;
import org.marj4n.smooth_classes.origin.OriginType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Client-only interpolation for body transformations so forms do not pop instantly. */
public final class OriginMorphState {
    private static final Map<UUID, Float> MERMAID = new HashMap<>();
    private static final Map<UUID, Float> BAT = new HashMap<>();
    private static final Map<UUID, Float> BAT_FLIGHT = new HashMap<>();
    private static final Map<UUID, Float> BAT_PERCH = new HashMap<>();

    private OriginMorphState() {}

    public static void tick(MinecraftClient client) {
        if (client.world == null) {
            MERMAID.clear();
            BAT.clear();
            BAT_FLIGHT.clear();
            BAT_PERCH.clear();
            return;
        }
        for (AbstractClientPlayerEntity player : client.world.getPlayers()) {
            var state = OriginRuntime.state(player);
            UUID id = player.getUuid();

            boolean mermaidTarget = state.origin() == OriginType.MERMAID
                    && (player.isTouchingWaterOrRain() || player.isSubmergedInWater() || player.isSwimming());
            MERMAID.put(id, approach(MERMAID.getOrDefault(id, mermaidTarget ? 0.0F : 0.0F), mermaidTarget ? 1.0F : 0.0F, 1.0F / 14.0F));

            boolean batTarget = state.origin() == OriginType.VAMPIRE && state.hasFlag("vampire.form.bat");
            BAT.put(id, approach(BAT.getOrDefault(id, 0.0F), batTarget ? 1.0F : 0.0F, 1.0F / 10.0F));

            boolean batFlightTarget = batTarget && (
                    player.getAbilities().flying
                            || (!player.isOnGround() && Math.abs(player.getVelocity().y) > 0.08D)
            );
            BAT_FLIGHT.put(id, approach(
                    BAT_FLIGHT.getOrDefault(id, batFlightTarget ? 1.0F : 0.0F),
                    batFlightTarget ? 1.0F : 0.0F,
                    1.0F / 6.0F
            ));

            // Blend the upside-down perch model and camera across ~12 ticks.
            // The local player's flags are mirrored separately by the origin packet.
            boolean perched = batTarget && state.hasFlag("vampire.bat.perched");
            if (player == client.player) {
                perched = "vampire".equals(OriginClientState.originId)
                        && OriginClientState.hasFlag("vampire.form.bat")
                        && OriginClientState.hasFlag("vampire.bat.perched");
            }
            BAT_PERCH.put(id, approach(BAT_PERCH.getOrDefault(id, 0.0F),
                    perched ? 1.0F : 0.0F, 1.0F / 12.0F));
        }
    }

    private static float approach(float value, float target, float step) {
        if (value < target) return Math.min(target, value + step);
        if (value > target) return Math.max(target, value - step);
        return value;
    }

    public static float mermaid(AbstractClientPlayerEntity player) {
        return player == null ? 0.0F : MERMAID.getOrDefault(player.getUuid(), 0.0F);
    }

    public static float bat(AbstractClientPlayerEntity player) {
        return player == null ? 0.0F : BAT.getOrDefault(player.getUuid(), 0.0F);
    }

    public static float batFlight(AbstractClientPlayerEntity player) {
        return player == null ? 0.0F : BAT_FLIGHT.getOrDefault(player.getUuid(), 0.0F);
    }

    public static float batPerch(AbstractClientPlayerEntity player) {
        return player == null ? 0.0F : BAT_PERCH.getOrDefault(player.getUuid(), 0.0F);
    }

    public static boolean hideMermaidLegs(AbstractClientPlayerEntity player) {
        return mermaid(player) >= 0.48F;
    }
}
