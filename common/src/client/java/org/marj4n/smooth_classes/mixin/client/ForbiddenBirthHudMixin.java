package org.marj4n.smooth_classes.mixin.client;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.DrawContext;
import org.marj4n.smooth_classes.client.origin.demon.ForbiddenBirthClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(InGameHud.class)
public abstract class ForbiddenBirthHudMixin {
    @Inject(method="render",at=@At("HEAD"),cancellable=true)
    private void forbidden$hud(DrawContext context,float partial,CallbackInfo ci){if(ForbiddenBirthClient.active())ci.cancel();}
}
