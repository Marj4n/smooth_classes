package org.marj4n.smooth_classes.mixin.client;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.MinecraftClient;
import org.marj4n.smooth_classes.client.origin.demon.DemonVisualModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(MinecraftClient.class)
public abstract class DemonResourceReloadMixin {
    @Inject(method="reloadResources()Ljava/util/concurrent/CompletableFuture;",at=@At("RETURN"))
    private void demon$reload(CallbackInfoReturnable<CompletableFuture<Void>> cir){cir.getReturnValue().thenRun(()->MinecraftClient.getInstance().execute(DemonVisualModel::invalidate));}
}
