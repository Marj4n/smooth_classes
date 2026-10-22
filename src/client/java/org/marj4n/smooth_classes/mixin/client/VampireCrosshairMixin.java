package org.marj4n.smooth_classes.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.util.hit.EntityHitResult;
import org.marj4n.smooth_classes.client.origin.OriginClientState;
import org.marj4n.smooth_classes.origin.OriginType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Vampirism-style fang reticle replaces vanilla crosshair on biteable targets. */
@Mixin(InGameHud.class)
public abstract class VampireCrosshairMixin {
    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void smooth_classes$hideCrosshairForBite(DrawContext context, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.player.isCreative() || client.player.isSpectator()) return;
        if (OriginType.byId(OriginClientState.originId).orElse(null) != OriginType.VAMPIRE) return;
        var state = org.marj4n.smooth_classes.origin.OriginRuntime.state(client.player);
        if (state.hasFlag("vampire.form.bat") || state.hasFlag("vampire.form.man_bat")) return;
        if (!client.player.getMainHandStack().isEmpty()) return;
        if (!(client.crosshairTarget instanceof EntityHitResult hit)) return;
        if (!(hit.getEntity() instanceof LivingEntity living) || !living.isAlive() || living == client.player) return;
        if (!(living instanceof VillagerEntity) && !(living instanceof AnimalEntity)) return;
        if (client.player.squaredDistanceTo(living) > 12.25D) return;
        ci.cancel();
    }
}
