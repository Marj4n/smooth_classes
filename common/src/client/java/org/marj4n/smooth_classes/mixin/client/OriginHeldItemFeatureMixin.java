package org.marj4n.smooth_classes.mixin.client;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.HeldItemFeatureRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.marj4n.smooth_classes.origin.OriginRuntime;
import org.marj4n.smooth_classes.origin.OriginType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Only suppress held items for handless Slime and tiny Bat Form.
 * Man-Bat needs vanilla HeldItemFeatureRenderer for Better Combat parity. */
@Mixin(HeldItemFeatureRenderer.class)
public abstract class OriginHeldItemFeatureMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void smooth_classes$hideOriginHeldItem(MatrixStack matrices, VertexConsumerProvider vertices,
                                                    int light, LivingEntity entity, float limbAngle,
                                                    float limbDistance, float tickDelta, float animationProgress,
                                                    float headYaw, float headPitch, CallbackInfo ci) {
        if (!(entity instanceof PlayerEntity player)) return;
        var state = OriginRuntime.state(player);
        if ((state.origin() == OriginType.SLIME && !state.hasFlag("slime.form.humanoid"))
                || (state.origin() == OriginType.VAMPIRE && state.hasFlag("vampire.form.bat"))) {
            ci.cancel();
        }
    }
}
