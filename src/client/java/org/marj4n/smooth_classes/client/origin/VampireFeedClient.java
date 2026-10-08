package org.marj4n.smooth_classes.client.origin;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.hit.EntityHitResult;
import org.marj4n.smooth_classes.network.SmoothClassesNetworking;
import org.marj4n.smooth_classes.origin.OriginType;

/** Client controller for Vampirism-style hold-to-feed. */
public final class VampireFeedClient {
    private static int feedingTarget = -1;
    private static float progress;

    private VampireFeedClient() {}

    public static void tick(MinecraftClient client) {
        if (client.player == null || client.world == null || client.currentScreen != null) {
            stop();
            return;
        }
        OriginType origin = OriginType.byId(OriginClientState.originId).orElse(null);
        if (origin != OriginType.VAMPIRE || client.player.isCreative() || client.player.isSpectator()) {
            stop();
            return;
        }

        int targetId = targetId(client);
        boolean holdingUse = client.options.useKey.isPressed();
        var state = org.marj4n.smooth_classes.origin.OriginRuntime.state(client.player);
        boolean transformed = state.hasFlag("vampire.form.bat") || state.hasFlag("vampire.form.man_bat");
        if (holdingUse && !transformed && targetId >= 0) {
            if (feedingTarget != targetId) {
                send(targetId);
                feedingTarget = targetId;
                progress = 0.0F;
            }
            progress = Math.min(1.0F, progress + 1.0F / 20.0F);
        } else {
            stop();
        }
    }

    private static int targetId(MinecraftClient client) {
        if (!client.player.getMainHandStack().isEmpty()) return -1;
        if (!(client.crosshairTarget instanceof EntityHitResult hit)) return -1;
        if (!(hit.getEntity() instanceof LivingEntity living) || !living.isAlive() || living == client.player) return -1;
        if (!org.marj4n.smooth_classes.origin.VampireBloodReserve.isFeedable(living)) return -1;
        if (client.player.squaredDistanceTo(living) > 12.25D) return -1;
        return living.getId();
    }

    private static void stop() {
        if (feedingTarget >= 0) send(-1);
        feedingTarget = -1;
        progress = 0.0F;
    }

    private static void send(int targetId) {
        if (!ClientPlayNetworking.canSend(SmoothClassesNetworking.VAMPIRE_FEED)) return;
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeInt(targetId);
        ClientPlayNetworking.send(SmoothClassesNetworking.VAMPIRE_FEED, buf);
    }

    public static boolean isFeeding() { return feedingTarget >= 0; }
    public static float progress() { return progress; }
}
