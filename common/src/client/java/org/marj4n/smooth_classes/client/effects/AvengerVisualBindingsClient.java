package org.marj4n.smooth_classes.client.effects;

import net.fabricmc.fabric.api.client.networking.v1.*;
import org.marj4n.smooth_classes.content.avenger.runtime.AvengerVisualBindings;

/** Reconnect/chunk tracking resends binding; no per-frame packets or replacement models. */
public final class AvengerVisualBindingsClient {
    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(
                AvengerVisualBindings.CHANNEL,
                (client, handler, buffer, sender) -> {
                    String dimension = buffer.readString(128);
                    var uuid = buffer.readUuid();
                    boolean bound = buffer.readBoolean();
                    client.execute(
                            () -> {
                                if (client.world != null
                                        && client.world
                                                .getRegistryKey()
                                                .getValue()
                                                .toString()
                                                .equals(dimension))
                                    AvengerVisualBindings.updateClient(client.world, uuid, bound);
                            });
                });
        ClientPlayConnectionEvents.DISCONNECT.register(
                (handler, client) -> client.execute(AvengerVisualBindings::clearClient));
    }

    private AvengerVisualBindingsClient() {}
}
