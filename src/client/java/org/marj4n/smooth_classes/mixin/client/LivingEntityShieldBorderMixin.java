package org.marj4n.smooth_classes.mixin.client;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import org.joml.Matrix4f;
import org.marj4n.smooth_classes.effects.SmoothEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Translucent, entity-following shield without a hard rectangular outline. */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityShieldBorderMixin {
    @Inject(method = "render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("TAIL"), require = 0)
    private void smooth_classes$shieldBorder(LivingEntity entity, float yaw, float tickDelta,
                                              MatrixStack matrices, VertexConsumerProvider consumers,
                                              int light, CallbackInfo ci) {
        boolean undying=entity.hasStatusEffect(SmoothEffects.UNDYING);
        boolean barrier=entity.hasStatusEffect(SmoothEffects.BARRIER);
        boolean aegis=entity.hasStatusEffect(SmoothEffects.GOLDEN_AEGIS);
        if (!undying && !barrier && !aegis) return;
        int r=undying?255:barrier?159:255, g=undying?255:barrier?71:218,
                b=undying?255:barrier?255:70;
        float pulse=(float)Math.sin((entity.age+tickDelta)*0.14)*0.035F;
        matrices.push();
        float width=Math.max(.55F,entity.getWidth()*.72F)+pulse;
        float top=entity.getHeight()+.16F;
        Matrix4f matrix=matrices.peek().getPositionMatrix();
        // Lightning's position/color layer blends transparent quads without a texture.
        // Each face is filled, so the player remains visible through the shield.
        VertexConsumer fill=consumers.getBuffer(RenderLayer.getLightning());
        // Status effects are synced per living entity, including owned pets and summons.
        // Keep Undying as visible as Barrier so their individual shields read clearly.
        int opacity=undying?52:barrier?52:40;
        float x=width, y0=.02F, y1=top;
        quad(fill,matrix,-x,y0,-x, x,y0,-x, x,y1,-x, -x,y1,-x,r,g,b,opacity);
        quad(fill,matrix, x,y0,x, -x,y0,x, -x,y1,x, x,y1,x,r,g,b,opacity);
        quad(fill,matrix,-x,y0,x, -x,y0,-x, -x,y1,-x, -x,y1,x,r,g,b,opacity);
        quad(fill,matrix,x,y0,-x, x,y0,x, x,y1,x, x,y1,-x,r,g,b,opacity);
        quad(fill,matrix,-x,y1,-x, x,y1,-x, x,y1,x, -x,y1,x,r,g,b,opacity);
        quad(fill,matrix,-x,y0,x, x,y0,x, x,y0,-x, -x,y0,-x,r,g,b,opacity);
        matrices.pop();
    }

    private static void quad(VertexConsumer out, Matrix4f matrix,
                             float x1,float y1,float z1,float x2,float y2,float z2,
                             float x3,float y3,float z3,float x4,float y4,float z4,
                             int r,int g,int b,int alpha) {
        out.vertex(matrix,x1,y1,z1).color(r,g,b,alpha).next();
        out.vertex(matrix,x2,y2,z2).color(r,g,b,alpha).next();
        out.vertex(matrix,x3,y3,z3).color(r,g,b,alpha).next();
        out.vertex(matrix,x4,y4,z4).color(r,g,b,alpha).next();
    }

}
