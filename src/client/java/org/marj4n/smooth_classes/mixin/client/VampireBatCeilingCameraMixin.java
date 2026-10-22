package org.marj4n.smooth_classes.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.BlockView;
import org.joml.Quaternionf;
import org.marj4n.smooth_classes.client.origin.OriginMorphState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Roll only the local player's 3D camera as the bat settles upside-down. */
@Mixin(Camera.class)
public abstract class VampireBatCeilingCameraMixin {
    @Shadow @Final private Quaternionf rotation;

    @Inject(method = "update", at = @At("TAIL"))
    private void smooth_classes$perchCameraRoll(BlockView area, Entity focusedEntity,
            boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (focusedEntity != client.player || !(focusedEntity instanceof AbstractClientPlayerEntity player)) return;
        float perch = OriginMorphState.batPerch(player);
        if (perch > 0.0F) rotation.rotateZ(MathHelper.PI * perch);
    }
}
