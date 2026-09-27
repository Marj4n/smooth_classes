package org.marj4n.smooth_classes.content.avenger.runtime;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import org.marj4n.smooth_classes.content.avenger.AvengerClass;
import org.marj4n.smooth_classes.runtime.AbilityRuntime;

/** Server lifecycle hooks for passive Avenger gameplay. */
public final class AvengerServerRuntime {
    private AvengerServerRuntime() {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                // tickPlayer only has 20/200-tick work; avoid a Puffish class lookup on the other 19 ticks.
                if (player.age % 20 == 0 && AbilityRuntime.isClass(player, AvengerClass.ID)) {
                    AvengerMinionGameplay.tickPlayer(player);
                }
            }
        });
    }
}
