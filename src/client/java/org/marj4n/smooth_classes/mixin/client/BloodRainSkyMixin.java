package org.marj4n.smooth_classes.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.world.World;
import org.marj4n.smooth_classes.client.runtime.BloodRainWeatherState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Forces full rain/thunder darkness locally while the camera is inside Raining Blood. */
@Mixin(World.class)
public abstract class BloodRainSkyMixin {
    @Inject(method={"getRainGradient", "getThunderGradient"}, at=@At("RETURN"), cancellable=true)
    private void smooth_classes$localStorm(float delta, CallbackInfoReturnable<Float> cir) {
        if (!((Object)this instanceof ClientWorld world)) return;
        var camera = MinecraftClient.getInstance().gameRenderer.getCamera().getPos();
        if (BloodRainWeatherState.isInsideStorm(world, camera.x, camera.z)) cir.setReturnValue(1.0F);
    }


    @Inject(method="getAmbientDarkness", at=@At("RETURN"), cancellable=true)
    private void smooth_classes$bloodRainAmbientDarkness(CallbackInfoReturnable<Integer> cir) {
        if (!((Object)this instanceof ClientWorld world)) return;
        var camera = MinecraftClient.getInstance().gameRenderer.getCamera().getPos();
        if (BloodRainWeatherState.isInsideStorm(world, camera.x, camera.z)) {
            cir.setReturnValue(15);
        }
    }
}
