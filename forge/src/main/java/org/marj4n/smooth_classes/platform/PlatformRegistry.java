package org.marj4n.smooth_classes.platform;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.RegisterEvent;

/** Construct intrusive registry entries only inside their own RegisterEvent. */
public final class PlatformRegistry {
    private PlatformRegistry() {}
    private static RegisterEvent activeEvent;
    private static final java.util.Set<Identifier> completed = new java.util.HashSet<>();

    public static void initialize(IEventBus modBus) {
        modBus.addListener(PlatformRegistry::registerContent);
    }

    private static void registerContent(RegisterEvent event) {
        var key = event.getRegistryKey();
        activeEvent = event;
        try {
            // Keep each class uninitialized until Forge unlocks that registry.
            if (key.equals(Registries.BLOCK.getKey())) {
                org.marj4n.smooth_classes.registry.SmoothBlocks.register();
            } else if (key.equals(Registries.ITEM.getKey())) {
                org.marj4n.smooth_classes.registry.SmoothItems.register();
            } else if (key.equals(Registries.STATUS_EFFECT.getKey())) {
                org.marj4n.smooth_classes.effects.SmoothEffects.register();
            } else if (key.equals(Registries.ENTITY_TYPE.getKey())) {
                org.marj4n.smooth_classes.registry.SmoothEntities.register();
            } else if (key.equals(Registries.PARTICLE_TYPE.getKey())) {
                org.marj4n.smooth_classes.registry.SmoothParticles.register();
            } else if (key.equals(Registries.SOUND_EVENT.getKey())) {
                org.marj4n.smooth_classes.registry.SmoothSounds.register();
            } else {
                return;
            }
            completed.add(key.getValue());
        } finally {
            activeEvent = null;
        }
    }

    public static void initializeContent() {
        if (completed.size() != 6) {
            throw new IllegalStateException("Smooth Classes common setup ran before all six registry events: " + completed);
        }
    }

    public static <V,T extends V> T register(Registry<V> registry, Identifier id, T value) {
        if (activeEvent == null || !activeEvent.getRegistryKey().equals(registry.getKey())) {
            throw new IllegalStateException("Registration outside its Forge RegisterEvent: " + id);
        }
        if (!id.getNamespace().equals("smooth_classes")) {
            throw new IllegalArgumentException("Foreign registry namespace: " + id);
        }
        activeEvent.register(registry.getKey(), id, () -> value);
        return value;
    }
}
