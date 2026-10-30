package org.marj4n.smooth_classes.mixin.client;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(GameRenderer.class)
public abstract class AvengerDanceFrameMixin {
    @Inject(method="render",at=@At("TAIL"))
    private void danceFrame(float delta,long time,boolean tick,CallbackInfo ci){org.marj4n.smooth_classes.client.avenger.dance.DanceDiagnostics.frame(delta);}
}
