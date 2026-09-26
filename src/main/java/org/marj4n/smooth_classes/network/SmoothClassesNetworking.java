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

/** Continued-style two ability channels with server-authoritative cooldown sync. */
public final class SmoothClassesNetworking {
    public static final Identifier CAST_SIGNATURE = SmoothClasses.id("cast_signature");
    public static final Identifier CAST_ASCENDANCY = SmoothClasses.id("cast_ascendancy");
    public static final Identifier SYNC_ABILITY_STATE = SmoothClasses.id("sync_ability_state");
    private static final Map<UUID,String> LAST_SELECTION = new HashMap<>();
    private SmoothClassesNetworking() {}

    public static void registerServer() {
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
                String selection = SignatureAbilityDispatcher.selectedAbility(player) + "|" + AscendancyAbilityDispatcher.selectedAbility(player);
                if (!selection.equals(LAST_SELECTION.get(player.getUuid()))) sendAbilityState(player);
            }
        });
    }

    public static void sendAbilityState(ServerPlayerEntity player) {
        PuffishSkillsIntegration.ensureAscendancyUnlocked(player);
        String sig = SignatureAbilityDispatcher.selectedAbility(player);
        String asc = AscendancyAbilityDispatcher.selectedAbility(player);

        int sigTotal = sig.isBlank() ? 1 : AbilityCooldowns.adjustedTicks(player, SignatureCooldowns.ticks(sig));
        int ascTotal = asc.isBlank() ? 1 : AbilityCooldowns.adjustedTicks(player, AscendancyAbilityDispatcher.cooldownTicks(asc));
        long sigRemain = sig.isBlank() ? 0 : AbilityCooldowns.remainingTicks(player, new Identifier(SmoothClasses.MOD_ID, sig));
        long ascRemain = asc.isBlank() ? 0 : AbilityCooldowns.remainingTicks(player, new Identifier(SmoothClasses.MOD_ID, "ascendancy_" + asc));

        PacketByteBuf out = PacketByteBufs.create();
        out.writeString(sig);
        out.writeInt(sigTotal);
        out.writeLong(sigRemain);
        out.writeString(asc);
        out.writeInt(ascTotal);
        out.writeLong(ascRemain);
        ServerPlayNetworking.send(player, SYNC_ABILITY_STATE, out);
        LAST_SELECTION.put(player.getUuid(), sig + "|" + asc);
    }
}
