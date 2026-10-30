package org.marj4n.smooth_classes.platform;
public final class PlatformRegistry {
    private PlatformRegistry() {}
    public static void initializeContent() {
        org.marj4n.smooth_classes.registry.SmoothSounds.register();
        org.marj4n.smooth_classes.registry.SmoothItems.register();
        org.marj4n.smooth_classes.registry.SmoothParticles.register();
        org.marj4n.smooth_classes.effects.SmoothEffects.register();
        org.marj4n.smooth_classes.registry.SmoothBlocks.register();
        org.marj4n.smooth_classes.registry.SmoothEntities.register();
    }
    public static <V,T extends V> T register(RegistryAccess<V> registry, net.minecraft.util.Identifier id, T value) {
        registry.register(id, value);
        return value;
    }
}
