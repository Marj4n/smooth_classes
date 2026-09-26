package org.marj4n.smooth_classes.client;

import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.minecraft.util.Identifier;

import java.util.List;

import static org.marj4n.smooth_classes.SmoothClasses.MOD_ID;

/**
 * Explicit client-side model baking for every custom model rendered through
 * Spell Engine's CustomModels API.
 *
 * Spell Engine 1.10.x resolves these identifiers directly from the baked-model
 * map at render time. Registering them explicitly with Fabric's model loader
 * makes that contract deterministic and avoids relying on discovery/order.
 */
public final class ModelRegistry {

    private ModelRegistry() {
    }

    public static final List<Identifier> CUSTOM_MODELS = List.of(
            // Spell projectile models
            id("spell_projectile/swordfall"),
            id("spell_projectile/sword"),
            id("spell_projectile/ice_projectile"),
            id("spell_projectile/fire_projectile"),
            id("spell_projectile/lightning_projectile"),
            id("spell_projectile/arcane_projectile"),
            id("spell_projectile/meteor_projectile"),
            id("spell_projectile/comet_projectile"),
            id("spell_projectile/arcane_slash"),
            id("spell_projectile/arrow"),
            id("spell_projectile/righteous_shield"),
            id("spell_projectile/righteous_hammers"),
            id("spell_projectile/eldritch_hammers"),
            id("spell_projectile/bones"),

            // Custom status/effect models
            id("effect/barrier"),
            id("effect/curse"),
            id("effect/death_mark"),
            id("effect/immobilize"),
            id("effect/magic_circle"),
            id("effect/taunted"),
            id("effect/undying"),
            id("effect/undying_barrier"),
            id("effect/undying_glow"),
            id("effect/vitality_bond")
    );

    public static void registerModels() {
        ModelLoadingPlugin.register(context -> context.addModels(CUSTOM_MODELS));
    }

    private static Identifier id(String path) {
        return new Identifier(MOD_ID, path);
    }
}
