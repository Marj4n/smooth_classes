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
                if (AbilityRuntime.isClass(player, AvengerClass.ID)) {
                    AvengerMinionGameplay.tickPlayer(player);
                }
            }
        });
    }
}
