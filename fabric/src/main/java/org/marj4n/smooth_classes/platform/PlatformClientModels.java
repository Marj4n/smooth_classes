package org.marj4n.smooth_classes.platform;
public final class PlatformClientModels {
    private PlatformClientModels() {}
    public static void registerLayers() {
        org.marj4n.smooth_classes.client.SmoothClassesClient.registerModelLayers((layer, provider) ->
                net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry.registerModelLayer(layer, provider::get));
    }
}
