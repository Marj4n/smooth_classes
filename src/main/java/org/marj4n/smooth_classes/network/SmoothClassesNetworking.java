package org.marj4n.smooth_classes.network;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.marj4n.smooth_classes.SmoothClasses;
import org.marj4n.smooth_classes.gameplay.AscendancyAbilityDispatcher;
import org.marj4n.smooth_classes.gameplay.SignatureAbilityDispatcher;
import org.marj4n.smooth_classes.gameplay.SignatureCooldowns;
import org.marj4n.smooth_classes.runtime.AbilityCooldowns;
import org.marj4n.smooth_classes.integration.PuffishSkillsIntegration;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** two ability channels with server-authoritative cooldown sync. */
public final class SmoothClassesNetworking {
    public static final Identifier CAST_SIGNATURE = SmoothClasses.id("cast_signature");
    public static final Identifier CAST_ASCENDANCY = SmoothClasses.id("cast_ascendancy");
    public static final Identifier ARCANE_SLASH_HOLD = SmoothClasses.id("arcane_slash_hold");
    public static final Identifier RIDER_SUMMON_MOUNT = SmoothClasses.id("rider_summon_mount");
    public static final Identifier RIDER_FLIGHT_INPUT = SmoothClasses.id("rider_flight_input");
    public static final Identifier SYNC_ABILITY_STATE = SmoothClasses.id("sync_ability_state");
    private static final Map<UUID,String> LAST_SELECTION = new HashMap<>();
    private SmoothClassesNetworking() {}

    public static void registerServer() {
        ServerPlayNetworking.registerGlobalReceiver(ARCANE_SLASH_HOLD,
                (server, player, handler, buf, responseSender) -> {
                    boolean held = buf.readBoolean();
                    server.execute(() -> org.marj4n.smooth_classes.runtime.ArcaneSlashChargeRuntime.hold(player, held));
                });
        ServerPlayNetworking.registerGlobalReceiver(RIDER_SUMMON_MOUNT,
                (server, player, handler, buf, responseSender) -> server.execute(() -> {
                    var result = org.marj4n.smooth_classes.content.rider.runtime.RiderRuntime.summonMount(player);
                    if (!result.success()) player.sendMessage(Text.literal("[Smooth Classes] " + result.detail()), true);
                    sendAbilityState(player);
                }));
        ServerPlayNetworking.registerGlobalReceiver(RIDER_FLIGHT_INPUT,
                (server, player, handler, buf, responseSender) -> {
                    boolean ascend = buf.readBoolean();
                    boolean descend = buf.readBoolean();
                    boolean boost = buf.readBoolean();
                    server.execute(() -> {
                        if (player.getVehicle() instanceof org.marj4n.smooth_classes.entity.RiderHippogryphEntity hippo) {
                            hippo.acceptRiderFlightInput(player, ascend, descend, boost);
                        }
                    });
                });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            org.marj4n.smooth_classes.runtime.ArcaneSlashChargeRuntime.disconnect(handler.player);
            LAST_SELECTION.remove(handler.player.getUuid());
            PuffishSkillsIntegration.invalidateRuntimeCache(handler.player);
        });
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            org.marj4n.smooth_classes.runtime.ArcaneSlashChargeRuntime.clear();
            LAST_SELECTION.clear();
            PuffishSkillsIntegration.clearRuntimeCaches();
        });
        ServerPlayNetworking.registerGlobalReceiver(CAST_SIGNATURE, (server, player, handler, buf, responseSender) ->
                server.execute(() -> {
                    var result = SignatureAbilityDispatcher.cast(player);
                    if (!result.success()) player.sendMessage(Text.literal("[Smooth Classes] " + result.message()), true);
                    sendAbilityState(player);
                }));
        ServerPlayNetworking.registerGlobalReceiver(CAST_ASCENDANCY, (server, player, handler, buf, responseSender) ->
                server.execute(() -> {
                    var ability = AscendancyAbilityDispatcher.selectedAbility(player);
                    var result = ability.isBlank()
                            ? AscendancyAbilityDispatcher.DispatchResult.failPublic("No Ascendancy ability unlocked.")
                            : AscendancyAbilityDispatcher.castNamed(player, ability);
                    if (!result.success()) player.sendMessage(Text.literal("[Smooth Classes] " + result.message()), true);
                    sendAbilityState(player);
                }));
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> server.execute(() -> sendAbilityState(handler.player)));
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if ((server.getTicks() % 20) != 0) return;
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                boolean rider = org.marj4n.smooth_classes.runtime.AbilityRuntime.isClass(
                        player, org.marj4n.smooth_classes.content.rider.RiderClass.ID);
                boolean riderMountActive = org.marj4n.smooth_classes.content.rider.runtime.RiderRuntime.hasActiveMount(player);
                String selection = SignatureAbilityDispatcher.selectedAbility(player) + "|"
                        + AscendancyAbilityDispatcher.selectedAbility(player) + "|rider=" + rider
                        + "|mount=" + riderMountActive;
                if (!selection.equals(LAST_SELECTION.get(player.getUuid()))) sendAbilityState(player);
            }
        });
    }

    public static void sendAbilityState(ServerPlayerEntity player) {
        PuffishSkillsIntegration.ensureAscendancyUnlocked(player);
        String sig = SignatureAbilityDispatcher.selectedAbility(player);
        String asc = AscendancyAbilityDispatcher.selectedAbility(player);

        int sigTotal = "sacred_orb".equals(sig) ? 2400 : sig.isBlank() ? 1 : AbilityCooldowns.adjustedTicks(player, SignatureCooldowns.ticks(sig));
        int ascTotal = "agony".equals(asc) ? 600 : "magic_circle".equals(asc) ? 1200 : "torment".equals(asc) ? 800 : asc.isBlank() ? 1 : AscendancyAbilityDispatcher.effectiveCooldownTicks(player,asc);
        long sigRemain = sig.isBlank() ? 0 : AbilityCooldowns.remainingTicks(player, new Identifier(SmoothClasses.MOD_ID, sig));
        long ascRemain = asc.isBlank() ? 0 : AbilityCooldowns.remainingTicks(player, new Identifier(SmoothClasses.MOD_ID, "ascendancy_" + asc));

        PacketByteBuf out = PacketByteBufs.create();
        out.writeString(sig);
        out.writeInt(sigTotal);
        out.writeLong(sigRemain);
        out.writeString(asc);
        out.writeInt(ascTotal);
        out.writeLong(ascRemain);
        out.writeBoolean(org.marj4n.smooth_classes.content.ruler.runtime.SacredBannerRuntime.isActive(player));
        out.writeBoolean(org.marj4n.smooth_classes.runtime.BloodRainRuntime.active(player));
        out.writeBoolean(org.marj4n.smooth_classes.runtime.WhenOnHighRuntime.active(player));
        boolean riderMountVisible = org.marj4n.smooth_classes.runtime.AbilityRuntime.isClass(
                player, org.marj4n.smooth_classes.content.rider.RiderClass.ID);
        out.writeBoolean(riderMountVisible);
        boolean riderMountActive = org.marj4n.smooth_classes.content.rider.runtime.RiderRuntime.hasActiveMount(player);
        out.writeBoolean(riderMountActive);
        out.writeInt(org.marj4n.smooth_classes.content.rider.runtime.RiderRuntime.summonCooldownTicks(player));
        out.writeLong(org.marj4n.smooth_classes.content.rider.runtime.RiderRuntime.summonRemainingTicks(player));
        ServerPlayNetworking.send(player, SYNC_ABILITY_STATE, out);
        LAST_SELECTION.put(player.getUuid(), sig + "|" + asc + "|rider=" + riderMountVisible
                + "|mount=" + riderMountActive);
    }
}
