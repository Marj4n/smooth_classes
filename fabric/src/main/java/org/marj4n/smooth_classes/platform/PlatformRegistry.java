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
    public static <V,T extends V> T register(net.minecraft.registry.Registry<V> registry, net.minecraft.util.Identifier id, T value) {
        return net.minecraft.registry.Registry.register(registry, id, value);
    }
}
