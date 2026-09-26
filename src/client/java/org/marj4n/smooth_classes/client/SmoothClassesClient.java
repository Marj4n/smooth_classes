package org.marj4n.smooth_classes.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
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
    private static KeyBinding signature;
    private static KeyBinding ascendancy;
    private static final AbilityHud HUD = new AbilityHud();

    public static KeyBinding signatureKey() { return signature; }
    public static KeyBinding ascendancyKey() { return ascendancy; }

    @Override
    public void onInitializeClient() {
        ModelRegistry.registerModels();
        registerVisualEffects();
        registerEntities();

        signature = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.smooth_classes.signature", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_V, "key.category.smooth_classes"));
        ascendancy = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.smooth_classes.ascendancy", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_R, "key.category.smooth_classes"));

        ClientPlayNetworking.registerGlobalReceiver(SmoothClassesNetworking.SYNC_ABILITY_STATE,
                (client, handler, buf, responseSender) -> {
                    String sig = buf.readString();
                    int sigTotal = buf.readInt();
                    long sigRemaining = buf.readLong();
                    String asc = buf.readString();
                    int ascTotal = buf.readInt();
                    long ascRemaining = buf.readLong();
                    client.execute(() -> AbilityHudState.sync(sig,sigTotal,sigRemaining,asc,ascTotal,ascRemaining));
                });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (signature.wasPressed()) cast(client, false);
            while (ascendancy.wasPressed()) cast(client, true);
        });
        HudRenderCallback.EVENT.register((context, tickDelta) -> HUD.render(context, tickDelta));
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
        EntityRendererRegistry.register(SmoothEntities.SPELL_TARGET, SpellTargetRenderer::new);
        EntityRendererRegistry.register(SmoothEntities.DREADGLARE, DreadglareRenderer::new);
        EntityRendererRegistry.register(SmoothEntities.GREATER_DREADGLARE, GreaterDreadglareRenderer::new);
        EntityRendererRegistry.register(SmoothEntities.WRAITH, WraithRenderer::new);
        EntityModelLayerRegistry.registerModelLayer(DREADGLARE_MODEL, DreadglareModel::getTexturedModelData);
        EntityModelLayerRegistry.registerModelLayer(GREATER_DREADGLARE_MODEL, GreaterDreadglareModel::getTexturedModelData);
        EntityModelLayerRegistry.registerModelLayer(WRAITH_MODEL, WraithModel::getTexturedModelData);
    }

    private static void registerVisualEffects() {
        CustomModelStatusEffect.register(SmoothEffects.BLADESTORM, new BladestormRenderer());
        CustomModelStatusEffect.register(SmoothEffects.ARCANE_VOLLEY, new ArcaneVolleyRenderer());
        CustomModelStatusEffect.register(SmoothEffects.FROST_VOLLEY, new FrostVolleyRenderer());
        CustomModelStatusEffect.register(SmoothEffects.VITALITY_BOND, new VitalityBondRenderer());
        CustomModelStatusEffect.register(SmoothEffects.UNDYING, new UndyingRenderer());
        CustomParticleStatusEffect.register(SmoothEffects.UNDYING, new UndyingParticles(2));
        CustomModelStatusEffect.register(SmoothEffects.BARRIER, new BarrierRenderer());
        CustomParticleStatusEffect.register(SmoothEffects.BARRIER, new BarrierParticles(1));
        CustomParticleStatusEffect.register(SmoothEffects.RAGE, new RageParticles(1));
        CustomParticleStatusEffect.register(SmoothEffects.EVASION, new EvasionParticles(1));
        CustomModelStatusEffect.register(SmoothEffects.IMMOBILIZE, new ImmobilizeRenderer());
        CustomModelStatusEffect.register(SmoothEffects.DEATH_MARK, new DeathMarkRenderer());
        CustomModelStatusEffect.register(SmoothEffects.TAUNTED, new TauntedRenderer());
        CustomModelStatusEffect.register(SmoothEffects.MARKSMANSHIP, new MarksmanshipRenderer());
        CustomModelStatusEffect.register(SmoothEffects.RIGHTEOUS_HAMMERS, new RighteousHammersRenderer());
        CustomModelStatusEffect.register(SmoothEffects.BONE_ARMOR, new BoneArmorRenderer());
        CustomModelStatusEffect.register(SmoothEffects.MAGIC_CIRCLE, new MagicCircleRenderer());
        CustomModelStatusEffect.register(SmoothEffects.AGONY, new CurseRenderer());
        CustomModelStatusEffect.register(SmoothEffects.TORMENT, new CurseRenderer());
    }
}
