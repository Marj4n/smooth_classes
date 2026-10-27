package org.marj4n.smooth_classes.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.hit.EntityHitResult;
import org.marj4n.smooth_classes.network.SmoothClassesNetworking;

/**
 * Highlight ONLY the Avenger summon under the local player's crosshair.
 * The ownership verdict is obtained from the server since scoreboard tags are
 * not guaranteed to be synchronized to the vanilla client entity.
 */
public final class AvengerSummonHoverGlow {
    private static ClientWorld trackedWorld;
    private static int hoveredId = -1;
    private static int confirmedOwnedId = -1;

    private AvengerSummonHoverGlow() { }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(SmoothClassesNetworking.SYNC_SUMMON_HOVER,
                (client, handler, buf, responseSender) -> {
                    int id = buf.readVarInt();
                    boolean owned = buf.readBoolean();
                    client.execute(() -> {
                        if (id == hoveredId) confirmedOwnedId = owned ? id : -1;
                    });
                });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.world == null || client.getNetworkHandler() == null) {
                hoveredId = -1;
                confirmedOwnedId = -1;
                trackedWorld = null;
                return;
            }
            if (trackedWorld != client.world) {
                trackedWorld = client.world;
                hoveredId = -1;
                confirmedOwnedId = -1;
            }
            Entity target = client.crosshairTarget instanceof EntityHitResult hit ? hit.getEntity() : null;
            int next = target == null ? -1 : target.getId();
            if (next == hoveredId) return;
            hoveredId = next;
            confirmedOwnedId = -1;
            if (next >= 0 && ClientPlayNetworking.canSend(SmoothClassesNetworking.CHECK_SUMMON_HOVER)) {
                PacketByteBuf buf = PacketByteBufs.create();
                buf.writeVarInt(next);
                ClientPlayNetworking.send(SmoothClassesNetworking.CHECK_SUMMON_HOVER, buf);
            }
        });
    }

    public static boolean isHighlighted(Entity entity) {
        MinecraftClient client = MinecraftClient.getInstance();
        return entity != null && client.player != null && client.world == entity.getWorld()
                && entity.getId() == confirmedOwnedId && entity.getId() == hoveredId
                && client.crosshairTarget instanceof EntityHitResult hit && hit.getEntity() == entity;
    }
}
