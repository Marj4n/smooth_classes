package org.marj4n.smooth_classes.mixin.client;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import org.marj4n.smooth_classes.client.origin.demon.ForbiddenBirthClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(GameRenderer.class)
public abstract class ForbiddenBirthHandMixin {
    @Inject(method="renderHand",at=@At("HEAD"),cancellable=true)
    private void forbidden$hands(MatrixStack m,Camera camera,float partial,CallbackInfo ci){if(ForbiddenBirthClient.active())ci.cancel();}
    @Inject(method="getFov",at=@At("HEAD"),cancellable=true)
    private void forbidden$fov(Camera camera,float partial,boolean changing,CallbackInfoReturnable<Double> cir){var pose=ForbiddenBirthClient.camera(partial);if(pose!=null)cir.setReturnValue(pose.fov());}
}
