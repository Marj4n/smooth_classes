package org.marj4n.smooth_classes.mixin.client;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.client.gui.DrawContext;
import org.marj4n.smooth_classes.client.origin.demon.ForbiddenBirthClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ToastManager.class)
public abstract class ForbiddenBirthToastMixin {
    @Inject(method="draw",at=@At("HEAD"),cancellable=true)
    private void forbidden$toast(DrawContext context,CallbackInfo ci){if(ForbiddenBirthClient.active())ci.cancel();}
}
