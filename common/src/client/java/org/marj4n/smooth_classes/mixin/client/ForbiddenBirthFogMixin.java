package org.marj4n.smooth_classes.mixin.client;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import org.marj4n.smooth_classes.client.origin.demon.ForbiddenBirthClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Owner-only cinematic lens: vanilla reapplies ordinary Nether fog on the next frame after cleanup. */
@Mixin(BackgroundRenderer.class)
public abstract class ForbiddenBirthFogMixin {
    @Inject(method="applyFog",at=@At("TAIL"))
    private static void forbidden$framing(Camera camera,BackgroundRenderer.FogType type,float distance,boolean thick,float partial,CallbackInfo ci){
        if(ForbiddenBirthClient.active()&&type==BackgroundRenderer.FogType.FOG_TERRAIN){RenderSystem.setShaderFogStart(24);RenderSystem.setShaderFogEnd(160);}
    }
}
