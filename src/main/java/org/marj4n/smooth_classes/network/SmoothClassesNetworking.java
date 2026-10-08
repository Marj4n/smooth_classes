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
import org.marj4n.smooth_classes.gameplay.ClassSpecialDispatcher;
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
    public static final Identifier BLADE_WORKS_HOLD = SmoothClasses.id("blade_works_hold");
    public static final Identifier ARCANE_SLASH_HOLD = SmoothClasses.id("arcane_slash_hold");
    public static final Identifier RIDER_FLIGHT_INPUT = SmoothClasses.id("rider_flight_input");
    public static final Identifier CLASS_SPECIAL = SmoothClasses.id("class_special");
    public static final Identifier CLASS_SPECIAL_HOLD = SmoothClasses.id("class_special_hold");
    public static final Identifier CLASS_SPECIAL_SELECT = SmoothClasses.id("class_special_select");
    public static final Identifier OPEN_DEATH_LIST = SmoothClasses.id("open_death_list");
    public static final Identifier SYNC_DEATH_LIST = SmoothClasses.id("sync_death_list");
    public static final Identifier SHADOW_AIM_CAST = SmoothClasses.id("shadow_aim_cast");
    public static final Identifier SYNC_ABILITY_STATE = SmoothClasses.id("sync_ability_state");
    public static final Identifier SYNC_CHARGE_STATE = SmoothClasses.id("sync_charge_state");
    public static final Identifier FORCE_HOTBAR_SLOT = SmoothClasses.id("force_hotbar_slot");
    public static final Identifier OPEN_ORIGIN_SELECTION = SmoothClasses.id("open_origin_selection");
    public static final Identifier SELECT_ORIGIN = SmoothClasses.id("select_origin");
    public static final Identifier SYNC_ORIGIN = SmoothClasses.id("sync_origin");
    public static final Identifier CAST_ORIGIN_ABILITY = SmoothClasses.id("cast_origin_ability");
    public static final Identifier VAMPIRE_FEED = SmoothClasses.id("vampire_feed");
    public static final Identifier VAMPIRE_MAN_BAT_JUMP = SmoothClasses.id("vampire_man_bat_jump");
    private static final Map<UUID,String> LAST_SELECTION = new HashMap<>();
    private SmoothClassesNetworking() {}

    public static void registerServer() {
        ServerPlayNetworking.registerGlobalReceiver(SELECT_ORIGIN,
                (server, player, handler, buf, responseSender) -> {
                    String originId = buf.readString(64);
                    server.execute(() -> {
                        var state = org.marj4n.smooth_classes.origin.OriginRuntime.state(player);
                        if (state.hasOrigin()) return;
                        org.marj4n.smooth_classes.origin.OriginType.byId(originId)
                                .filter(org.marj4n.smooth_classes.origin.OriginType::isV1Playable)
                                .ifPresent(origin -> org.marj4n.smooth_classes.origin.OriginRuntime.setOrigin(player, origin));
                    });
                });
        ServerPlayNetworking.registerGlobalReceiver(CAST_ORIGIN_ABILITY,
                (server, player, handler, buf, responseSender) -> {
                    int slot = buf.readVarInt();
                    server.execute(() -> {
                        var result = org.marj4n.smooth_classes.origin.OriginAbilityDispatcher.activate(player, slot);
                        if (!result.success()) player.sendMessage(Text.literal("[Origin] " + result.message()), true);
                        sendOriginState(player);
                    });
                });
        ServerPlayNetworking.registerGlobalReceiver(BLADE_WORKS_HOLD,
                (server, player, handler, buf, responseSender) -> {
                    boolean held = buf.readBoolean();
                    server.execute(() -> org.marj4n.smooth_classes.content.archer.runtime.PortalOfSovereigntyRuntime.hold(player, held));
                });
        ServerPlayNetworking.registerGlobalReceiver(ARCANE_SLASH_HOLD,
                (server, player, handler, buf, responseSender) -> {
                    boolean held = buf.readBoolean();
                    server.execute(() -> org.marj4n.smooth_classes.runtime.ArcaneSlashChargeRuntime.hold(player, held));
                });
        ServerPlayNetworking.registerGlobalReceiver(CLASS_SPECIAL,
                (server, player, handler, buf, responseSender) -> {
                    boolean alternate = buf.isReadable() && buf.readBoolean();
                    server.execute(() -> {
                        var result = ClassSpecialDispatcher.activate(player, alternate);
                        if (!result.success()) player.sendMessage(Text.literal("[Smooth Classes] " + result.detail()), true);
                        sendAbilityState(player);
                    });
                });
        ServerPlayNetworking.registerGlobalReceiver(CLASS_SPECIAL_SELECT,
                (server, player, handler, buf, responseSender) -> {
                    int selection = buf.readVarInt();
                    server.execute(() -> {
                        var result = ClassSpecialDispatcher.select(player, selection);
                        if (!result.success()) player.sendMessage(Text.literal("[Smooth Classes] " + result.detail()), true);
                        sendAbilityState(player);
                    });
                });
        ServerPlayNetworking.registerGlobalReceiver(CLASS_SPECIAL_HOLD,
                (server, player, handler, buf, responseSender) -> {
                    boolean held = buf.readBoolean();
                    server.execute(() -> ClassSpecialDispatcher.hold(player, held));
                });
        ServerPlayNetworking.registerGlobalReceiver(OPEN_DEATH_LIST,
                (server, player, handler, buf, responseSender) -> server.execute(() -> {
                    if (!org.marj4n.smooth_classes.runtime.AbilityRuntime.isClass(
                            player, org.marj4n.smooth_classes.content.avenger.AvengerClass.ID)) return;
                    PacketByteBuf out = PacketByteBufs.create();
                    out.writeNbt(org.marj4n.smooth_classes.content.avenger.runtime.AvengerReworkRuntime.deathListBookNbt(player));
                    ServerPlayNetworking.send(player, SYNC_DEATH_LIST, out);
                }));
        ServerPlayNetworking.registerGlobalReceiver(SHADOW_AIM_CAST,
                (server, player, handler, buf, responseSender) -> {
                    int mode = buf.readByte();
                    int entityId = -1;
                    net.minecraft.util.math.Vec3d blockHit = null;
                    if (mode == 1) entityId = buf.readInt();
                    else if (mode == 2) blockHit = new net.minecraft.util.math.Vec3d(
                            buf.readDouble(), buf.readDouble(), buf.readDouble());
                    final int targetEntityId = entityId;
                    final net.minecraft.util.math.Vec3d targetBlockHit = blockHit;
                    server.execute(() -> {
                        var result = org.marj4n.smooth_classes.content.assassin.runtime.ShadowTechniqueRuntime
                                .castAimed(player, targetEntityId, targetBlockHit);
                        if (!result.success()) player.sendMessage(Text.literal("[Smooth Classes] " + result.detail()), true);
                        sendAbilityState(player);
                    });
                });
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
        ServerPlayNetworking.registerGlobalReceiver(VAMPIRE_FEED,
                (server, player, handler, buf, responseSender) -> {
                    int targetId = buf.readInt();
                    server.execute(() -> org.marj4n.smooth_classes.origin.VampireFeedingRuntime.setTarget(player, targetId));
                });
        ServerPlayNetworking.registerGlobalReceiver(VAMPIRE_MAN_BAT_JUMP,
                (server, player, handler, buf, responseSender) ->
                        server.execute(() -> org.marj4n.smooth_classes.origin.VampireManBatRuntime.tryAirJump(player)));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            org.marj4n.smooth_classes.runtime.ArcaneSlashChargeRuntime.disconnect(handler.player);
            ClassSpecialDispatcher.cleanup(handler.player);
            org.marj4n.smooth_classes.origin.VampireFeedingRuntime.cleanup(handler.player);
            LAST_SELECTION.remove(handler.player.getUuid());
            PuffishSkillsIntegration.invalidateRuntimeCache(handler.player);
            org.marj4n.smooth_classes.runtime.AbilityRuntime.invalidateRuntimeCache(handler.player);
        });
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            org.marj4n.smooth_classes.runtime.ArcaneSlashChargeRuntime.clear();
            ClassSpecialDispatcher.clear();
            org.marj4n.smooth_classes.origin.VampireFeedingRuntime.clear();
            LAST_SELECTION.clear();
            PuffishSkillsIntegration.clearRuntimeCaches();
            org.marj4n.smooth_classes.runtime.AbilityRuntime.clearRuntimeCaches();
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
                boolean avenger = org.marj4n.smooth_classes.runtime.AbilityRuntime.isClass(
                        player, org.marj4n.smooth_classes.content.avenger.AvengerClass.ID);
                int avengerCharges = avenger
                        ? org.marj4n.smooth_classes.content.avenger.runtime.AvengerReworkRuntime.summonCharges(player) : 0;
                long avengerRemaining = avenger
                        ? org.marj4n.smooth_classes.content.avenger.runtime.AvengerReworkRuntime.summonRechargeRemainingTicks(player) : 0L;
                boolean shadowActive = org.marj4n.smooth_classes.content.assassin.runtime.ShadowTechniqueRuntime.active(player);
                long shadowRemaining = org.marj4n.smooth_classes.content.assassin.runtime.ShadowTechniqueRuntime.remainingTicks(player);
                var classSpecial = ClassSpecialDispatcher.hudState(player);
                String selection = SignatureAbilityDispatcher.selectedAbility(player) + "|"
                        + AscendancyAbilityDispatcher.selectedAbility(player)
                        + "|avenger=" + avenger + "|avc=" + avengerCharges + "|avr=" + (avengerRemaining / 20L)
                        + "|shadow=" + shadowActive + "|shr=" + (shadowRemaining / 20L)
                        + "|h=" + classSpecial.id() + "|hcd=" + (classSpecial.remainingTicks() / 20L)
                        + "|ha=" + classSpecial.blockedActive() + "|hv=" + classSpecial.variant()
                        + "|hm=" + (classSpecial.modeRemainingTicks() / 20L)
                        + "|hs=" + (classSpecial.secondaryRemainingTicks() / 20L);
                if (!selection.equals(LAST_SELECTION.get(player.getUuid()))) sendAbilityState(player);
            }
        });
    }

    /** Sends the render-only hold-charge state. Gameplay remains server-owned by each runtime. */
    public static void sendChargeState(ServerPlayerEntity player, boolean active, boolean completed,
                                       String ability, int elapsedTicks, int totalTicks) {
        PacketByteBuf out = PacketByteBufs.create();
        out.writeBoolean(active);
        out.writeBoolean(completed);
        out.writeString(ability == null ? "" : ability);
        out.writeInt(Math.max(0, elapsedTicks));
        out.writeInt(Math.max(1, totalTicks));
        ServerPlayNetworking.send(player, SYNC_CHARGE_STATE, out);
    }

    public static void sendAbilityState(ServerPlayerEntity player) {
        PuffishSkillsIntegration.ensureAscendancyUnlocked(player);
        String sig = SignatureAbilityDispatcher.selectedAbility(player);
        String asc = AscendancyAbilityDispatcher.selectedAbility(player);

        int sigTotal = "preparation".equals(sig)
                ? org.marj4n.smooth_classes.content.assassin.runtime.ShadowTechniqueRuntime.cooldownHudTicks(player)
                : "sacred_orb".equals(sig) ? SignatureCooldowns.ticks("sacred_orb") : sig.isBlank() ? 1
                : AbilityCooldowns.adjustedTicks(player, SignatureCooldowns.ticks(sig));
        int ascTotal = asc.isBlank() ? 1 : AscendancyAbilityDispatcher.effectiveCooldownTicks(player, asc);
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
        boolean avengerSummonVisible = org.marj4n.smooth_classes.runtime.AbilityRuntime.isClass(
                player, org.marj4n.smooth_classes.content.avenger.AvengerClass.ID);
        int avengerCharges = avengerSummonVisible
                ? org.marj4n.smooth_classes.content.avenger.runtime.AvengerReworkRuntime.summonCharges(player) : 0;
        long avengerRemaining = avengerSummonVisible
                ? org.marj4n.smooth_classes.content.avenger.runtime.AvengerReworkRuntime.summonRechargeRemainingTicks(player) : 0L;
        out.writeBoolean(avengerSummonVisible);
        out.writeInt(avengerCharges);
        out.writeInt(org.marj4n.smooth_classes.content.avenger.runtime.AvengerReworkRuntime.maxSummonCharges());
        out.writeInt(org.marj4n.smooth_classes.content.avenger.runtime.AvengerReworkRuntime.summonRechargeTotalTicks());
        out.writeLong(avengerRemaining);
        boolean shadowActive = org.marj4n.smooth_classes.content.assassin.runtime.ShadowTechniqueRuntime.active(player);
        long shadowRemaining = org.marj4n.smooth_classes.content.assassin.runtime.ShadowTechniqueRuntime.remainingTicks(player);
        out.writeBoolean(shadowActive);
        out.writeLong(shadowRemaining);
        out.writeInt(org.marj4n.smooth_classes.content.assassin.runtime.ShadowTechniqueRuntime.range(player));
        var classSpecial = ClassSpecialDispatcher.hudState(player);
        out.writeString(classSpecial.id());
        out.writeInt(classSpecial.totalTicks());
        out.writeLong(classSpecial.remainingTicks());
        out.writeBoolean(classSpecial.blockedActive());
        out.writeInt(classSpecial.variant());
        out.writeInt(classSpecial.modeTotalTicks());
        out.writeLong(classSpecial.modeRemainingTicks());
        out.writeInt(classSpecial.secondaryTotalTicks());
        out.writeLong(classSpecial.secondaryRemainingTicks());
        int treasuryCapacity = org.marj4n.smooth_classes.content.archer.runtime.ArcherSpecialRuntime.treasuryCapacity(player);
        out.writeVarInt(treasuryCapacity);
        var treasurySlots = org.marj4n.smooth_classes.content.archer.runtime.ArcherSpecialRuntime.treasurySlots(player);
        for (int i = 0; i < org.marj4n.smooth_classes.content.archer.runtime.ArcherTreasuryState.MAX_SLOT_COUNT; i++) {
            out.writeItemStack(treasurySlots[i]);
        }
        ServerPlayNetworking.send(player, SYNC_ABILITY_STATE, out);
        LAST_SELECTION.put(player.getUuid(), sig + "|" + asc
                + "|avenger=" + avengerSummonVisible + "|avc=" + avengerCharges + "|avr=" + (avengerRemaining / 20L)
                + "|shadow=" + shadowActive + "|shr=" + (shadowRemaining / 20L)
                + "|h=" + classSpecial.id() + "|hcd=" + (classSpecial.remainingTicks() / 20L)
                + "|ha=" + classSpecial.blockedActive() + "|hv=" + classSpecial.variant()
                + "|hm=" + (classSpecial.modeRemainingTicks() / 20L)
                + "|hs=" + (classSpecial.secondaryRemainingTicks() / 20L));
    }

    public static void openOriginSelection(ServerPlayerEntity player) {
        ServerPlayNetworking.send(player, OPEN_ORIGIN_SELECTION, PacketByteBufs.empty());
    }

    public static void sendOriginState(ServerPlayerEntity player) {
        var server = player.getServer();
        if (server == null) return;
        for (ServerPlayerEntity receiver : server.getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(receiver, SYNC_ORIGIN, buildOriginStatePacket(player));
        }
    }

    private static PacketByteBuf buildOriginStatePacket(ServerPlayerEntity player) {
        var state = org.marj4n.smooth_classes.origin.OriginRuntime.state(player);
        PacketByteBuf out = PacketByteBufs.create();
        out.writeUuid(player.getUuid());
        out.writeString(state.originId());
        out.writeVarInt(state.blood());
        out.writeVarInt(org.marj4n.smooth_classes.origin.OriginRuntime.vampireBloodCapacity(player));
        out.writeVarInt(state.sunExposure());
        out.writeVarInt(state.wetnessTicks());
        out.writeVarInt(state.wingStaminaTicks());
        out.writeVarInt(state.instability());
        out.writeVarInt(state.soul());
        out.writeVarInt(state.boneMass());
        out.writeVarInt(state.carbonLayer());
        out.writeVarInt(state.livingMass());
        out.writeLong(state.lastFeedTick());

        java.util.Map<String, Integer> progress = new java.util.HashMap<>(state.progressSnapshot());
        if (state.origin() == org.marj4n.smooth_classes.origin.OriginType.VAMPIRE) {
            long readyAt = state.longProgress("vampire.bat_form_ready_at");
            long remaining = Math.max(0L, readyAt - player.getWorld().getTime());
            progress.put("ui.vampire.bat_cooldown_ticks", (int)Math.min(Integer.MAX_VALUE, remaining));
        }
        out.writeVarInt(progress.size());
        for (var entry : progress.entrySet()) {
            out.writeString(entry.getKey());
            out.writeVarInt(entry.getValue());
        }
        var flags = state.flagsSnapshot();
        out.writeVarInt(flags.size());
        for (String flag : flags) out.writeString(flag);
        return out;
    }

    /** Server-authoritative short hotbar lock used by the Archer Treasury draw animation. */
    public static void forceHotbarSlot(ServerPlayerEntity player, int slot) {
        PacketByteBuf out = PacketByteBufs.create();
        out.writeVarInt(Math.max(0, Math.min(8, slot)));
        ServerPlayNetworking.send(player, FORCE_HOTBAR_SLOT, out);
    }
}
