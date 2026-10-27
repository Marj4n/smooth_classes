package org.marj4n.smooth_classes.platform.forge;
@net.minecraftforge.fml.common.Mod("smooth_classes")
public final class ForgeEntrypoint {
    public ForgeEntrypoint() {
        var bus = net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus();
        org.marj4n.smooth_classes.platform.PlatformRegistry.initialize(bus);
        bus.addListener((net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event) ->
                event.enqueueWork(() -> new org.marj4n.smooth_classes.SmoothClasses().onInitialize()));
        net.minecraftforge.fml.DistExecutor.safeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT, () -> ForgeClient::register);
    }
    private static final class ForgeClient {
        private static void register() {
            // Model loading plugins must be present before the first resource reload.
            org.marj4n.smooth_classes.client.ModelRegistry.registerModels();
            var bus = net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus();
            bus.addListener((net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers event) ->
                    org.marj4n.smooth_classes.client.SmoothClassesClient.registerEntities());
            bus.addListener((net.minecraftforge.client.event.RegisterParticleProvidersEvent event) ->
                    org.marj4n.smooth_classes.client.SmoothClassesClient.registerParticleFactories());
            net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus().addListener(
                    (net.minecraftforge.client.event.EntityRenderersEvent.RegisterLayerDefinitions event) ->
                            org.marj4n.smooth_classes.client.SmoothClassesClient.registerModelLayers(event::registerLayerDefinition));
            net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus().addListener(ForgeClient::setup);
        }
        private static void setup(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent event) {
            event.enqueueWork(() -> new org.marj4n.smooth_classes.client.SmoothClassesClient().onInitializeClient());
        }
    }
}
