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
    private static KeyBinding riderMount;
    private static boolean arcaneHoldSent;
    private static boolean arcaneWasDown;
    private static int arcaneHoldTicks;
    private static boolean riderFlightWasMounted;
    private static boolean riderFlightLastAscend;
    private static boolean riderFlightLastDescend;
    private static boolean riderFlightLastBoost;
    private static int riderFlightHeartbeat;
    private static final AbilityHud HUD = new AbilityHud();
    private static final RighteousHammersRenderer HAMMER_RENDERER = new RighteousHammersRenderer();

    public static KeyBinding signatureKey() { return signature; }
    public static KeyBinding ascendancyKey() { return ascendancy; }
    public static KeyBinding riderMountKey() { return riderMount; }

    @Override
    public void onInitializeClient() {
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
        // The local player entity is not rendered in first person, so render its
        // orbit in world space after entities using camera-relative coordinates.
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            var client=net.minecraft.client.MinecraftClient.getInstance();
            var player=client.player;
            if (player==null || !client.options.getPerspective().isFirstPerson()
                    || context.matrixStack()==null || context.consumers()==null) return;
            var status=player.getStatusEffect(SmoothEffects.RIGHTEOUS_HAMMERS);
            if (status==null) return;
            float delta=context.tickDelta();
            var camera=context.camera().getPos();
            var matrices=context.matrixStack();
            matrices.push();
            matrices.translate(MathHelper.lerp(delta,player.prevX,player.getX())-camera.x,
                    MathHelper.lerp(delta,player.prevY,player.getY())-camera.y,
                    MathHelper.lerp(delta,player.prevZ,player.getZ())-camera.z);
            if (status!=null) HAMMER_RENDERER.renderEffect(0L,status.getAmplifier(),player,delta,
                    matrices,context.consumers(),0xF000F0);
            matrices.pop();
        });

        signature = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.smooth_classes.signature", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_V, "key.category.smooth_classes"));
        ascendancy = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.smooth_classes.ascendancy", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_R, "key.category.smooth_classes"));
        riderMount = KeyBindingHelper.registerKeyBinding(new KeyBinding(
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
                    boolean riderMountVisible=buf.readBoolean();
                    boolean riderMountActive=buf.readBoolean();
                    int riderMountTotal=buf.readInt();
                    long riderMountRemaining=buf.readLong();
                    boolean avengerSummonVisible=buf.readBoolean();
                    int avengerSummonCharges=buf.readInt();
                    int avengerSummonMaxCharges=buf.readInt();
                    int avengerSummonTotal=buf.readInt();
                    long avengerSummonRemaining=buf.readLong();
                    client.execute(() -> {
                        AbilityHudState.sync(sig,sigTotal,sigRemaining,asc,ascTotal,ascRemaining);
                        AbilityHudState.bannerActive=bannerActive;
                        AbilityHudState.bloodRainActive=bloodRainActive;
                        AbilityHudState.whenOnHighActive=whenOnHighActive;
                        AbilityHudState.syncRiderMount(riderMountVisible, riderMountActive, riderMountTotal, riderMountRemaining);
                        AbilityHudState.syncAvengerSummon(avengerSummonVisible, avengerSummonCharges,
                                avengerSummonMaxCharges, avengerSummonTotal, avengerSummonRemaining);
                    });
                });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
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
            syncRiderFlightInput(client);
            while (signature.wasPressed()) cast(client, false);
            while (riderMount.wasPressed()) {
                if (client.player == null) continue;
                if (AbilityHudState.avengerSummonVisible) {
                    if (ClientPlayNetworking.canSend(SmoothClassesNetworking.AVENGER_SUMMON)) {
                        ClientPlayNetworking.send(SmoothClassesNetworking.AVENGER_SUMMON, PacketByteBufs.empty());
                    }
                    continue;
                }
                if (AbilityHudState.riderMountActive || isLocalRiderMount(client)) {
                    client.player.sendMessage(Text.literal("Rider mount is already active."), true);
                    client.player.getWorld().playSound(client.player, client.player.getBlockPos(),
                            SmoothSounds.ABILITY_BLOCKED, SoundCategory.PLAYERS, 0.1F, 1.5F);
                    continue;
                }
                if (ClientPlayNetworking.canSend(SmoothClassesNetworking.RIDER_SUMMON_MOUNT)) {
                    ClientPlayNetworking.send(SmoothClassesNetworking.RIDER_SUMMON_MOUNT, PacketByteBufs.empty());
                }
            }
            while (ascendancy.wasPressed()) {
                if (!"arcane_slash".equals(AbilityHudState.ascendancyAbility)) cast(client, true);
                else if (freshArcanePress) {
                    cast(client, true);
                    freshArcanePress = false;
                }
            }
            if (client.player != null && client.player.hasStatusEffect(SmoothEffects.ARCANE_SLASH))
                client.player.setSprinting(false);
        });
        HudRenderCallback.EVENT.register((context, tickDelta) -> HUD.render(context, tickDelta));
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

    private static void sendArcaneHold(boolean held) {
        if (net.minecraft.client.MinecraftClient.getInstance().getNetworkHandler() == null) return;
        if (!ClientPlayNetworking.canSend(SmoothClassesNetworking.ARCANE_SLASH_HOLD)) return;
        var packet = PacketByteBufs.create();
        packet.writeBoolean(held);
        ClientPlayNetworking.send(SmoothClassesNetworking.ARCANE_SLASH_HOLD, packet);
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
        EntityRendererRegistry.register(SmoothEntities.BLOOD_RAIN, org.marj4n.smooth_classes.client.renderer.BloodRainRenderer::new);
        EntityRendererRegistry.register(SmoothEntities.HIGH_BEAM, org.marj4n.smooth_classes.client.renderer.HighBeamRenderer::new);
        EntityRendererRegistry.register(SmoothEntities.SACRED_BANNER, SacredBannerRenderer::new);
        EntityRendererRegistry.register(SmoothEntities.SPELL_TARGET, SpellTargetRenderer::new);
        EntityRendererRegistry.register(SmoothEntities.DREADGLARE, DreadglareRenderer::new);
        EntityRendererRegistry.register(SmoothEntities.GREATER_DREADGLARE, GreaterDreadglareRenderer::new);
        EntityRendererRegistry.register(SmoothEntities.WRAITH, WraithRenderer::new);
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

    private static boolean isLocalRiderMount(net.minecraft.client.MinecraftClient client) {
        if (client.player == null) return false;
        var vehicle = client.player.getVehicle();
        return vehicle instanceof org.marj4n.smooth_classes.entity.RiderHorseEntity
                || vehicle instanceof org.marj4n.smooth_classes.entity.RiderDreadSteedEntity
                || vehicle instanceof org.marj4n.smooth_classes.entity.RiderHippogryphEntity;
    }

}
