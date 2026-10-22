package org.marj4n.smooth_classes.mixin.client;

import org.marj4n.smooth_classes.client.StealthVisualState;
import org.marj4n.smooth_classes.origin.OriginRuntime;
import org.marj4n.smooth_classes.origin.OriginType;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.WorldView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Prevent the shadow from flashing on remote players while Stealth is refreshed. */
@Mixin(EntityRenderDispatcher.class)
public abstract class StealthShadowRendererMixin {
    @Inject(method = "renderShadow", at = @At("HEAD"), cancellable = true)
    private static void smooth_classes$renderShadow(MatrixStack matrices, VertexConsumerProvider consumers,
            Entity entity, float opacity, float tickDelta, WorldView world, float radius, CallbackInfo ci) {
        if (entity instanceof LivingEntity living && StealthVisualState.active(living)) {
            ci.cancel();
            return;
        }
        // The normal player shadow is nearly a full block wide and makes tiny Bat Form
        // look like it is still using a humanoid body. Hide that oversized shadow until
        // a dedicated tiny bat shadow is rendered.
        if (entity instanceof AbstractClientPlayerEntity player) {
            var state = OriginRuntime.state(player);
            if (state.origin() == OriginType.VAMPIRE && state.hasFlag("vampire.form.bat")) {
                ci.cancel();
            }
        }
    }
}
