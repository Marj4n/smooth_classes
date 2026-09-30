package org.marj4n.smooth_classes.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Vec3d;
import org.marj4n.smooth_classes.client.runtime.BloodRainWeatherState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Makes the local blood-rain sky genuinely night-dark instead of merely rainy daylight. */
@Mixin(ClientWorld.class)
public abstract class BloodRainClientWorldMixin {
    private boolean smooth_classes$insideBloodRain() {
        ClientWorld world = (ClientWorld)(Object)this;
        Vec3d camera = MinecraftClient.getInstance().gameRenderer.getCamera().getPos();
        return BloodRainWeatherState.isInsideStorm(world, camera.x, camera.z);
    }

    @Inject(method="getSkyBrightness", at=@At("RETURN"), cancellable=true)
    private void smooth_classes$darkSkyBrightness(float tickDelta, CallbackInfoReturnable<Float> cir) {
        if (smooth_classes$insideBloodRain()) cir.setReturnValue(0.0F);
    }

    @Inject(method="getSkyColor", at=@At("RETURN"), cancellable=true)
    private void smooth_classes$darkSkyColor(Vec3d cameraPos, float tickDelta, CallbackInfoReturnable<Vec3d> cir) {
        if (!smooth_classes$insideBloodRain()) return;
        cir.setReturnValue(new Vec3d(.018D, .0015D, .0035D));
    }

    @Inject(method="getCloudsColor", at=@At("RETURN"), cancellable=true)
    private void smooth_classes$darkClouds(float tickDelta, CallbackInfoReturnable<Vec3d> cir) {
        if (!smooth_classes$insideBloodRain()) return;
        cir.setReturnValue(new Vec3d(.032D, .0025D, .005D));
    }
}
