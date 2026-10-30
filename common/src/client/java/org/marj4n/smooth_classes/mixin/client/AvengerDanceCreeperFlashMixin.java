package org.marj4n.smooth_classes.mixin.client;
import net.minecraft.client.render.entity.CreeperEntityRenderer;
import net.minecraft.entity.mob.CreeperEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.marj4n.smooth_classes.client.avenger.dance.AvengerDanceClient;
@Mixin(CreeperEntityRenderer.class)
public abstract class AvengerDanceCreeperFlashMixin {
    @Inject(method="getAnimationCounter(Lnet/minecraft/entity/mob/CreeperEntity;F)F",at=@At("HEAD"),cancellable=true)
    private void danceFlash(CreeperEntity e,float delta,CallbackInfoReturnable<Float> cir){var p=AvengerDanceClient.playback(e);if(p!=null&&AvengerDanceClient.phase(e.getWorld().getTime(),delta,p.start())%1<.06f)cir.setReturnValue(.6f);}
}
