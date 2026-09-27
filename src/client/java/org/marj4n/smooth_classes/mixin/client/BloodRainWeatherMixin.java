package org.marj4n.smooth_classes.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.WorldRenderer;
import org.marj4n.smooth_classes.client.runtime.BloodRainWeatherState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Replaces vanilla precipitation with Raining Blood only for clients whose camera is
 * inside a local blood-rain storm. No world weather value is changed, so normal rain
 * immediately becomes visible again after leaving the 64-block radius or when the
 * storm entity expires.
 */
@Mixin(WorldRenderer.class)
public abstract class BloodRainWeatherMixin {

    @Inject(method = "renderWeather", at = @At("HEAD"), cancellable = true)
    private void smoothClasses$hideVanillaWeatherInsideBloodRain(
            LightmapTextureManager manager,
            float tickDelta,
            double cameraX,
            double cameraY,
            double cameraZ,
            CallbackInfo ci) {
        var client = MinecraftClient.getInstance();
        if (client.world != null && BloodRainWeatherState.isInsideStorm(client.world, cameraX, cameraZ)) {
            ci.cancel();
        }
    }

    @Inject(method = "tickRainSplashing", at = @At("HEAD"), cancellable = true)
    private void smoothClasses$hideVanillaRainEffectsInsideBloodRain(Camera camera, CallbackInfo ci) {
        var client = MinecraftClient.getInstance();
        if (client.world == null) {
            return;
        }

        var pos = camera.getPos();
        if (BloodRainWeatherState.isInsideStorm(client.world, pos.x, pos.z)) {
            ci.cancel();
        }
    }
}
