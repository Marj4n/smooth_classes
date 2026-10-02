package org.marj4n.smooth_classes.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.entity.HorseEntityRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.util.math.MathHelper;
import org.marj4n.smooth_classes.client.effects.RighteousHammersRenderer;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.sound.SoundCategory;
import net.spell_engine.api.effect.CustomModelStatusEffect;
import net.spell_engine.api.effect.CustomParticleStatusEffect;
import org.lwjgl.glfw.GLFW;
import org.marj4n.smooth_classes.SmoothClasses;
import org.marj4n.smooth_classes.client.effects.*;
import org.marj4n.smooth_classes.client.renderer.*;
import org.marj4n.smooth_classes.client.renderer.model.*;
import org.marj4n.smooth_classes.effects.SmoothEffects;
import org.marj4n.smooth_classes.network.SmoothClassesNetworking;
import org.marj4n.smooth_classes.registry.SmoothEntities;
import org.marj4n.smooth_classes.registry.SmoothSounds;

public final class SmoothClassesClient implements ClientModInitializer {
    public static final EntityModelLayer DREADGLARE_MODEL = new EntityModelLayer(SmoothClasses.id("dreadglare"), "main");
    public static final EntityModelLayer GREATER_DREADGLARE_MODEL = new EntityModelLayer(SmoothClasses.id("greater_dreadglare"), "main");
    public static final EntityModelLayer WRAITH_MODEL = new EntityModelLayer(SmoothClasses.id("wraith"), "main");
    public static final EntityModelLayer RIDER_HIPPOGRYPH_MODEL = new EntityModelLayer(SmoothClasses.id("rider_hippogryph"), "main");
    private static KeyBinding signature;
    private static KeyBinding ascendancy;
    private static KeyBinding classSpecial;
    private static boolean bladeHoldSent, bladeWasDown;
    private static int bladeHoldTicks;
    private static boolean arcaneHoldSent;
    private static boolean arcaneWasDown;
    private static int arcaneHoldTicks;
    private static boolean riderFlightWasMounted;
    private static boolean shadowWasDown;
    private static boolean deathListComboWasDown;
    private static boolean classSpecialHoldSent;
    private static boolean classSpecialWasDown;
    private static int classSpecialHoldTicks;
    private static boolean riderFlightLastAscend;
    private static boolean riderFlightLastDescend;
    private static boolean riderFlightLastBoost;
    private static int riderFlightHeartbeat;
    private static final AbilityHud HUD = new AbilityHud();
    private static final RighteousHammersRenderer HAMMER_RENDERER = new RighteousHammersRenderer();

    public static KeyBinding signatureKey() { return signature; }
    public static KeyBinding ascendancyKey() { return ascendancy; }
    public static KeyBinding classSpecialKey() { return classSpecial; }

    @Override
    public void onInitializeClient() {
        DeathListBookClient.register();
        ModelRegistry.registerModels();
        net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap.INSTANCE.putBlock(
                org.marj4n.smooth_classes.registry.SmoothBlocks.ARCANE_FIRE,
                net.minecraft.client.render.RenderLayer.getCutout());
        net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry.getInstance().register(
                org.marj4n.smooth_classes.registry.SmoothParticles.ARCANE_FLAME,
                org.marj4n.smooth_classes.client.effects.ArcaneFlameFactory::new);
        net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry.getInstance().register(
                org.marj4n.smooth_classes.registry.SmoothParticles.BLACK_FLAME,
                org.marj4n.smooth_classes.client.effects.ArcaneFlameFactory::new);
        registerVisualEffects();
        registerEntities();
        // Render large class visuals directly in world space. This avoids relying on
        // player feature-renderer hooks, which some animation/culling mods can replace.
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            ShadowAimClient.render(context);
            var client = net.minecraft.client.MinecraftClient.getInstance();
            if (client.world == null || context.matrixStack() == null || context.consumers() == null) return;

            float delta = context.tickDelta();
            var camera = context.camera().getPos();
            var matrices = context.matrixStack();

            // Crimson Revenant now uses particles + first-person vision overlay instead
            // of a rigid model, so there is no special world-space model rendering here.

            // The local player entity itself is not rendered in first person, so keep
            // the existing world-space hammer fallback for that one effect.
            var player = client.player;
            if (player == null || !client.options.getPerspective().isFirstPerson()) return;
            var status = player.getStatusEffect(SmoothEffects.RIGHTEOUS_HAMMERS);
            if (status == null) return;
            matrices.push();
            matrices.translate(
                    MathHelper.lerp(delta, player.prevX, player.getX()) - camera.x,
                    MathHelper.lerp(delta, player.prevY, player.getY()) - camera.y,
                    MathHelper.lerp(delta, player.prevZ, player.getZ()) - camera.z);
            HAMMER_RENDERER.renderEffect(0L, status.getAmplifier(), player, delta,
                    matrices, context.consumers(), 0xF000F0);
            matrices.pop();
        });

        signature = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.smooth_classes.signature", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_V, "key.category.smooth_classes"));
        ascendancy = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.smooth_classes.ascendancy", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_R, "key.category.smooth_classes"));
        classSpecial = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.smooth_classes.class_special", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_H, "key.category.smooth_classes"));

        ClientPlayNetworking.registerGlobalReceiver(SmoothClassesNetworking.SYNC_ABILITY_STATE,
                (client, handler, buf, responseSender) -> {
                    String sig = buf.readString();
                    int sigTotal = buf.readInt();
                    long sigRemaining = buf.readLong();
                    String asc = buf.readString();
                    int ascTotal = buf.readInt();
                    long ascRemaining = buf.readLong();
                    boolean bannerActive=buf.readBoolean();
                    boolean bloodRainActive=buf.readBoolean();
                    boolean whenOnHighActive=buf.readBoolean();
                    boolean avengerSummonVisible=buf.readBoolean();
                    int avengerSummonCharges=buf.readInt();
                    int avengerSummonMaxCharges=buf.readInt();
                    int avengerSummonTotal=buf.readInt();
                    long avengerSummonRemaining=buf.readLong();
                    boolean shadowActive=buf.readBoolean();
                    long shadowRemaining=buf.readLong();
                    int shadowRange=buf.readInt();
                    String classSpecialId=buf.readString();
                    int classSpecialTotal=buf.readInt();
                    long classSpecialRemaining=buf.readLong();
                    boolean classSpecialActive=buf.readBoolean();
                    int classSpecialVariant=buf.readInt();
                    int classSpecialModeTotal=buf.readInt();
                    long classSpecialModeRemaining=buf.readLong();
                    int classSpecialSecondaryTotal=buf.readInt();
                    long classSpecialSecondaryRemaining=buf.readLong();
                    int treasuryCapacity = buf.readVarInt();
                    net.minecraft.item.ItemStack[] treasurySlots = new net.minecraft.item.ItemStack[8];
                    for (int i = 0; i < treasurySlots.length; i++) treasurySlots[i] = buf.readItemStack();
                    client.execute(() -> {
                        AbilityHudState.sync(sig,sigTotal,sigRemaining,asc,ascTotal,ascRemaining);
                        AbilityHudState.bannerActive=bannerActive;
                        AbilityHudState.bloodRainActive=bloodRainActive;
                        AbilityHudState.whenOnHighActive=whenOnHighActive;
                        AbilityHudState.syncAvengerSummon(avengerSummonVisible, avengerSummonCharges,
                                avengerSummonMaxCharges, avengerSummonTotal, avengerSummonRemaining);
                        AbilityHudState.syncShadow(shadowActive, shadowRemaining, shadowRange);
                        AbilityHudState.syncClassSpecial(classSpecialId, classSpecialTotal, classSpecialRemaining,
                                classSpecialActive, classSpecialVariant, classSpecialModeTotal, classSpecialModeRemaining,
                                classSpecialSecondaryTotal, classSpecialSecondaryRemaining);
                        AbilityHudState.syncTreasury(treasuryCapacity, treasurySlots);
                    });
                });

        ClientPlayNetworking.registerGlobalReceiver(SmoothClassesNetworking.FORCE_HOTBAR_SLOT,
                (client, handler, buf, responseSender) -> {
                    int slot = buf.readVarInt();
                    client.execute(() -> {
                        if (client.player != null) client.player.getInventory().selectedSlot = Math.max(0, Math.min(8, slot));
                    });
                });

        ClientPlayNetworking.registerGlobalReceiver(SmoothClassesNetworking.SYNC_CHARGE_STATE,
                (client, handler, buf, responseSender) -> {
                    boolean active = buf.readBoolean();
                    boolean completed = buf.readBoolean();
                    String ability = buf.readString();
                    int elapsed = buf.readInt();
                    int total = buf.readInt();
                    client.execute(() -> org.marj4n.smooth_classes.client.charge.ChargeHudState.sync(
                            active, completed, ability, elapsed, total));
                });
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register(
                (handler, client) -> {
                    org.marj4n.smooth_classes.client.charge.ChargeHudState.reset();
                    AbilityHudState.reset();
                    ShadowAimClient.reset();
                    deathListComboWasDown = false;
                    classSpecialHoldSent = false;
                    classSpecialWasDown = false;
                    classSpecialHoldTicks = 0;
                });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null && client.world != null
                    && org.marj4n.smooth_classes.client.runtime.BloodRainWeatherState.isInsideStorm(
                    client.world, client.player.getX(), client.player.getZ())) {
                // Remove the local fire overlay immediately as well as the server fire state.
                client.player.extinguish();
                client.player.setFireTicks(0);
            }

            // Ctrl+H is a fixed Death List shortcut, independent of the shared H keybind
            // and independent of HUD sync. The server remains authoritative and ignores
            // the request when the player is not an Avenger. This avoids losing Ctrl+H
            // when another mod/keybind owns H or the Avenger HUD state has not synced yet.
            boolean deathListComboDown = false;
            if (client.player != null && client.getNetworkHandler() != null
                    && client.currentScreen == null && client.isWindowFocused()) {
                long window = client.getWindow().getHandle();
                boolean control = InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_LEFT_CONTROL)
                        || InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_RIGHT_CONTROL);
                deathListComboDown = control && InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_H);
                if (deathListComboDown && !deathListComboWasDown
                        && ClientPlayNetworking.canSend(SmoothClassesNetworking.OPEN_DEATH_LIST)) {
                    ClientPlayNetworking.send(SmoothClassesNetworking.OPEN_DEATH_LIST, PacketByteBufs.empty());
                }
            }
            deathListComboWasDown = deathListComboDown;
            boolean preparationSelected = "preparation".equals(AbilityHudState.signatureAbility);
            boolean shadowDown = preparationSelected && client.player != null && client.getNetworkHandler() != null
                    && client.currentScreen == null && client.isWindowFocused() && signature.isPressed();
            boolean freshShadowPress = shadowDown && !shadowWasDown;
            boolean releasedShadow = !shadowDown && shadowWasDown;
            shadowWasDown = shadowDown;
            if (freshShadowPress && (AbilityHudState.shadowActive || AbilityHudState.signatureRemainingMs() <= 0L)) {
                ShadowAimClient.begin(client);
            }
            if (shadowDown && ShadowAimClient.isAiming()) ShadowAimClient.update(client);
            if (releasedShadow && ShadowAimClient.isAiming()) ShadowAimClient.release(client);
            if (!preparationSelected && ShadowAimClient.isAiming()) ShadowAimClient.reset();

            boolean holdingArcane = client.player != null && client.currentScreen == null
                    && client.isWindowFocused() && ascendancy.isPressed()
                    && "arcane_slash".equals(AbilityHudState.ascendancyAbility);
            boolean freshArcanePress = holdingArcane && !arcaneWasDown;
            arcaneWasDown = holdingArcane;
            // Send press/heartbeat before cast on the same ordered connection.
            if (holdingArcane && (!arcaneHoldSent || ++arcaneHoldTicks >= 2)) {
                sendArcaneHold(true);
                arcaneHoldSent = true;
                arcaneHoldTicks = 0;
            } else if (!holdingArcane && arcaneHoldSent) {
                sendArcaneHold(false);
                arcaneHoldSent = false;
                arcaneHoldTicks = 0;
            }
            boolean bladeSelected = "unlimited_blade_works".equals(AbilityHudState.signatureAbility);
            boolean bladeDown = client.player != null && client.getNetworkHandler() != null
                    && client.currentScreen == null && client.isWindowFocused() && signature.isPressed() && bladeSelected;
            boolean freshBladePress = bladeDown && !bladeWasDown;
            bladeWasDown = bladeDown;
            if (bladeDown && (!bladeHoldSent || ++bladeHoldTicks >= 2)) {
                sendBladeHold(true); bladeHoldSent = true; bladeHoldTicks = 0;
            } else if (!bladeDown && bladeHoldSent) {
                sendBladeHold(false); bladeHoldSent = false; bladeHoldTicks = 0;
            }

            // Berserker H is a real hold-to-charge class special, matching the
            // Arcane Slash / Portal of Sovereignty charge UX. Heartbeats keep
            // the server authoritative and releasing early triggers the 4s fail CD.
            boolean classSpecialDown = "crimson_revenant".equals(AbilityHudState.classSpecialId)
                    && client.player != null && client.getNetworkHandler() != null
                    && client.currentScreen == null && client.isWindowFocused()
                    && classSpecial.isPressed();
            boolean freshClassSpecialPress = classSpecialDown && !classSpecialWasDown;
            classSpecialWasDown = classSpecialDown;
            if (classSpecialDown && (!classSpecialHoldSent || ++classSpecialHoldTicks >= 2)) {
                sendClassSpecialHold(true);
                classSpecialHoldSent = true;
                classSpecialHoldTicks = 0;
            } else if (!classSpecialDown && classSpecialHoldSent) {
                sendClassSpecialHold(false);
                classSpecialHoldSent = false;
                classSpecialHoldTicks = 0;
            }
            if (freshClassSpecialPress) {
                // Hold packet is queued first on the same connection, so the server
                // sees H as held before it validates the Berserker charge start.
                sendClassSpecial(false);
            }

            syncRiderFlightInput(client);
            while (signature.wasPressed()) {
                if (preparationSelected) {
                    // Shadow Technique casts on key release using the live mob/block aim preview.
                    continue;
                }
                if (!bladeSelected) cast(client, false);
                else if (freshBladePress) { cast(client, false); freshBladePress = false; }
            }
            while (classSpecial.wasPressed()) {
                if (client.player == null) continue;
                if ("crimson_revenant".equals(AbilityHudState.classSpecialId)) continue;
                if (!AbilityHudState.classSpecialVisible) continue;

                // Treasury H has two context-sensitive actions:
                // held item -> store it; empty selected hotbar slot -> hold-H selector to withdraw.
                if ("treasury_key".equals(AbilityHudState.classSpecialId)) {
                    if (AbilityHudState.classSpecialActive || AbilityHudState.classSpecialRemainingMs() > 0L) continue;
                    if (!client.player.getMainHandStack().isEmpty()) {
                        sendClassSpecial(false);
                    } else if (AbilityHudState.treasuryHasAny() && client.currentScreen == null) {
                        client.setScreen(new ClassSpecialRadialScreen("treasury_key", 0));
                    } else {
                        client.player.sendMessage(Text.literal("Treasury is empty."), true);
                    }
                    continue;
                }

                // Caster and Foreigner use the same hold-H radial interaction.
                if (ClassSpecialRadialScreen.supports(AbilityHudState.classSpecialId)) {
                    if (AbilityHudState.classSpecialActive || AbilityHudState.classSpecialRemainingMs() > 0L) continue;
                    if (client.currentScreen == null) {
                        client.setScreen(new ClassSpecialRadialScreen(
                                AbilityHudState.classSpecialId, AbilityHudState.classSpecialVariant));
                    }
                    continue;
                }

                sendClassSpecial(false);
            }
            while (ascendancy.wasPressed()) {
                if (!"arcane_slash".equals(AbilityHudState.ascendancyAbility)) cast(client, true);
                else if (freshArcanePress) {
                    cast(client, true);
                    freshArcanePress = false;
                }
            }
            if (client.player != null && (client.player.hasStatusEffect(SmoothEffects.ARCANE_SLASH)
                    || org.marj4n.smooth_classes.client.charge.ChargeHudState.active(org.marj4n.smooth_classes.client.charge.ChargeHudState.PORTAL_OF_SOVEREIGNTY)))
                client.player.setSprinting(false);
        });
        HudRenderCallback.EVENT.register((context, tickDelta) -> {
            HUD.render(context, tickDelta);
            CrimsonRevenantVisionOverlay.render(context, tickDelta);
        });
    }

    private static void syncRiderFlightInput(net.minecraft.client.MinecraftClient client) {
        // The server already grants fire resistance/extinguishes Rider lava mounts.
        // Clear the local flags too so the rider/mount never spends even one render
        // frame with Minecraft's orange burning overlay while lava-walking.
        if (client.player != null) {
            var vehicle = client.player.getVehicle();
            boolean lavaRiderMount = vehicle instanceof org.marj4n.smooth_classes.entity.RiderDreadSteedEntity
                    || vehicle instanceof org.marj4n.smooth_classes.entity.RiderHippogryphEntity;
            if (lavaRiderMount && (vehicle.isInLava() || client.player.isInLava()
                    || vehicle.isOnFire() || client.player.isOnFire())) {
                vehicle.extinguish();
                vehicle.setFireTicks(0);
                client.player.extinguish();
                client.player.setFireTicks(0);
            }
        }

        boolean mounted = client.player != null
                && client.player.getVehicle() instanceof org.marj4n.smooth_classes.entity.RiderHippogryphEntity;
        boolean ascend = mounted && client.currentScreen == null && client.options.jumpKey.isPressed();
        boolean descend = mounted && client.currentScreen == null && client.options.sneakKey.isPressed();
        boolean boost = mounted && client.currentScreen == null && client.options.sprintKey.isPressed();

        boolean changed = mounted != riderFlightWasMounted
                || ascend != riderFlightLastAscend
                || descend != riderFlightLastDescend
                || boost != riderFlightLastBoost;
        boolean heartbeat = mounted && ++riderFlightHeartbeat >= 5;

        if ((changed || heartbeat) && ClientPlayNetworking.canSend(SmoothClassesNetworking.RIDER_FLIGHT_INPUT)) {
            var packet = PacketByteBufs.create();
            packet.writeBoolean(ascend);
            packet.writeBoolean(descend);
            packet.writeBoolean(boost);
            ClientPlayNetworking.send(SmoothClassesNetworking.RIDER_FLIGHT_INPUT, packet);
            riderFlightHeartbeat = 0;
        }

        if (!mounted) riderFlightHeartbeat = 0;
        riderFlightWasMounted = mounted;
        riderFlightLastAscend = ascend;
        riderFlightLastDescend = descend;
        riderFlightLastBoost = boost;
    }

    private static void sendBladeHold(boolean held) {
        if (net.minecraft.client.MinecraftClient.getInstance().getNetworkHandler() == null) return;
        if (!ClientPlayNetworking.canSend(SmoothClassesNetworking.BLADE_WORKS_HOLD)) return;
        var packet = PacketByteBufs.create(); packet.writeBoolean(held);
        ClientPlayNetworking.send(SmoothClassesNetworking.BLADE_WORKS_HOLD, packet);
    }

    private static void sendArcaneHold(boolean held) {
        if (net.minecraft.client.MinecraftClient.getInstance().getNetworkHandler() == null) return;
        if (!ClientPlayNetworking.canSend(SmoothClassesNetworking.ARCANE_SLASH_HOLD)) return;
        var packet = PacketByteBufs.create();
        packet.writeBoolean(held);
        ClientPlayNetworking.send(SmoothClassesNetworking.ARCANE_SLASH_HOLD, packet);
    }

    private static void sendClassSpecial(boolean alternate) {
        if (net.minecraft.client.MinecraftClient.getInstance().getNetworkHandler() == null) return;
        if (!ClientPlayNetworking.canSend(SmoothClassesNetworking.CLASS_SPECIAL)) return;
        var packet = PacketByteBufs.create();
        packet.writeBoolean(alternate);
        ClientPlayNetworking.send(SmoothClassesNetworking.CLASS_SPECIAL, packet);
    }

    public static void sendClassSpecialSelection(int selection) {
        if (net.minecraft.client.MinecraftClient.getInstance().getNetworkHandler() == null) return;
        if (!ClientPlayNetworking.canSend(SmoothClassesNetworking.CLASS_SPECIAL_SELECT)) return;
        var packet = PacketByteBufs.create();
        packet.writeVarInt(Math.max(0, selection));
        ClientPlayNetworking.send(SmoothClassesNetworking.CLASS_SPECIAL_SELECT, packet);
    }

    public static void sendClassSpecialHold(boolean held) {
        if (net.minecraft.client.MinecraftClient.getInstance().getNetworkHandler() == null) return;
        if (!ClientPlayNetworking.canSend(SmoothClassesNetworking.CLASS_SPECIAL_HOLD)) return;
        var packet = PacketByteBufs.create();
        packet.writeBoolean(held);
        ClientPlayNetworking.send(SmoothClassesNetworking.CLASS_SPECIAL_HOLD, packet);
    }

    private static void cast(net.minecraft.client.MinecraftClient client, boolean asc) {
        if (client.player == null) return;
        long remaining = asc ? AbilityHudState.ascendancyRemainingMs() : AbilityHudState.signatureRemainingMs();
        String ability = asc ? AbilityHudState.ascendancyAbility : AbilityHudState.signatureAbility;
        if (ability == null || ability.isBlank()) {
            client.player.sendMessage(Text.literal(asc ? "No Ascendancy ability unlocked." : "No Signature ability unlocked."), true);
            client.player.getWorld().playSound(client.player, client.player.getBlockPos(), SmoothSounds.ABILITY_BLOCKED, SoundCategory.PLAYERS, 0.1F, 1.5F);
            return;
        }
        if (!asc && "sacred_orb".equals(ability) && AbilityHudState.bannerActive) {
            client.player.sendMessage(Text.literal("Sacred Banner is still active."), true);
            return;
        }
        if (asc && "magic_circle".equals(ability) && AbilityHudState.bloodRainActive) {
            client.player.sendMessage(Text.literal("Raining Blood is still active."),true);return;
        }
        if (AbilityHudState.whenOnHighActive) {
            client.player.sendMessage(Text.literal("When On High is still channeling."),true);return;
        }
        if (remaining > 0) {
            client.player.sendMessage(Text.literal("Ability can be used again in " + (int)Math.ceil(remaining/1000D) + "s"), true);
            client.player.getWorld().playSound(client.player, client.player.getBlockPos(), SmoothSounds.ABILITY_BLOCKED, SoundCategory.PLAYERS, 0.1F, 1.5F);
            return;
        }
        var id = asc ? SmoothClassesNetworking.CAST_ASCENDANCY : SmoothClassesNetworking.CAST_SIGNATURE;
        if (ClientPlayNetworking.canSend(id)) {
            ClientPlayNetworking.send(id, PacketByteBufs.empty());
            client.player.getWorld().playSound(client.player, client.player.getBlockPos(), SmoothSounds.ABILITY_USE, SoundCategory.PLAYERS, 0.4F, 1.5F);
        }
    }

    private static void registerEntities() {
        EntityRendererRegistry.register(SmoothEntities.TORMENT_FIELD, org.marj4n.smooth_classes.client.renderer.TormentFieldRenderer::new);
        EntityRendererRegistry.register(SmoothEntities.SHADOW_ANCHOR, org.marj4n.smooth_classes.client.renderer.ShadowAnchorRenderer::new);
        EntityRendererRegistry.register(SmoothEntities.BLOOD_RAIN, org.marj4n.smooth_classes.client.renderer.BloodRainRenderer::new);
        EntityRendererRegistry.register(SmoothEntities.HIGH_BEAM, org.marj4n.smooth_classes.client.renderer.HighBeamRenderer::new);
        EntityRendererRegistry.register(SmoothEntities.SACRED_BANNER, SacredBannerRenderer::new);
        EntityRendererRegistry.register(SmoothEntities.SPELL_TARGET, SpellTargetRenderer::new);
        EntityRendererRegistry.register(SmoothEntities.DREADGLARE, DreadglareRenderer::new);
        EntityRendererRegistry.register(SmoothEntities.GREATER_DREADGLARE, GreaterDreadglareRenderer::new);
        EntityRendererRegistry.register(SmoothEntities.WRAITH, WraithRenderer::new);
        EntityRendererRegistry.register(SmoothEntities.BLADE_PORTAL, org.marj4n.smooth_classes.client.renderer.BladePortalRenderer::new);
        EntityRendererRegistry.register(SmoothEntities.PROJECTED_DAGGER, org.marj4n.smooth_classes.client.renderer.ProjectedDaggerRenderer::new);
        EntityRendererRegistry.register(SmoothEntities.LANCER_IMPALE, org.marj4n.smooth_classes.client.renderer.LancerImpaleRenderer::new);
        EntityRendererRegistry.register(SmoothEntities.RIDER_HORSE, HorseEntityRenderer::new);
        EntityRendererRegistry.register(SmoothEntities.RIDER_DREAD_STEED, RiderDreadSteedRenderer::new);
        EntityRendererRegistry.register(SmoothEntities.RIDER_HIPPOGRYPH, RiderHippogryphRenderer::new);
        EntityModelLayerRegistry.registerModelLayer(DREADGLARE_MODEL, DreadglareModel::getTexturedModelData);
        EntityModelLayerRegistry.registerModelLayer(GREATER_DREADGLARE_MODEL, GreaterDreadglareModel::getTexturedModelData);
        EntityModelLayerRegistry.registerModelLayer(WRAITH_MODEL, WraithModel::getTexturedModelData);
        EntityModelLayerRegistry.registerModelLayer(RIDER_HIPPOGRYPH_MODEL, RiderHippogryphModel::getTexturedModelData);
    }

    private static void registerVisualEffects() {
        CustomModelStatusEffect.register(SmoothEffects.BLADESTORM, new BladestormRenderer());
        CustomModelStatusEffect.register(SmoothEffects.ARCANE_VOLLEY, new ArcaneVolleyRenderer());
        CustomModelStatusEffect.register(SmoothEffects.FROST_VOLLEY, new FrostVolleyRenderer());
        CustomModelStatusEffect.register(SmoothEffects.VITALITY_BOND, new VitalityBondRenderer());
        CustomModelStatusEffect.register(SmoothEffects.UNDYING, new UndyingRenderer());
        CustomModelStatusEffect.register(SmoothEffects.DIVINE_RAY, new DivineRayRenderer());
        CustomParticleStatusEffect.register(SmoothEffects.UNDYING, new UndyingParticles(2));
        CustomParticleStatusEffect.register(SmoothEffects.BARRIER, new BarrierParticles(1));
        CustomParticleStatusEffect.register(SmoothEffects.RAGE, new RageParticles(1));
        CustomParticleStatusEffect.register(SmoothEffects.EVASION, new EvasionParticles(1));
        CustomModelStatusEffect.register(SmoothEffects.IMMOBILIZE, new ImmobilizeRenderer());
        CustomModelStatusEffect.register(SmoothEffects.DEATH_MARK, new DeathMarkRenderer());
        CustomModelStatusEffect.register(SmoothEffects.TAUNTED, new TauntedRenderer());
        CustomModelStatusEffect.register(SmoothEffects.MARKSMANSHIP, new MarksmanshipRenderer());
        CustomModelStatusEffect.register(SmoothEffects.MAGIC_CIRCLE, new MagicCircleRenderer());
        CustomModelStatusEffect.register(SmoothEffects.AGONY, new CurseRenderer());
        CustomModelStatusEffect.register(SmoothEffects.TORMENT, new CurseRenderer());
    }

}
