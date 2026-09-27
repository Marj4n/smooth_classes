package org.marj4n.smooth_classes.client.renderer;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.marj4n.smooth_classes.client.renderer.model.RiderHippogryphModel;
import org.marj4n.smooth_classes.entity.RiderHippogryphEntity;

/**
 * Thin boost streaks rendered directly from the animated wing-tip transforms.
 * This is geometry, not a particle effect, so the streak cannot drift away from
 * the feather tip while the wing is banking/flapping.
 */
public final class RiderHippogryphWindTrailFeature
        extends FeatureRenderer<RiderHippogryphEntity, RiderHippogryphModel> {

    public RiderHippogryphWindTrailFeature(
            FeatureRendererContext<RiderHippogryphEntity, RiderHippogryphModel> context
    ) {
        super(context);
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider vertices, int light,
                       RiderHippogryphEntity entity, float limbAngle, float limbDistance,
                       float tickDelta, float animationProgress, float headYaw, float headPitch) {
        float boost = entity.getWingBoostBlend(tickDelta);
        if (!entity.isRiderFlying() || boost < 0.12F) return;

        double speedSq = entity.getVelocity().lengthSquared();
        if (speedSq < 0.10D) return;

        // Fade/length grow with the same smoothed boost state as the wing pose.
        float eased = boost * boost * (3.0F - 2.0F * boost);
        float length = 0.55F + eased * 1.15F;
        int alpha = (int) (55 + eased * 125);
        VertexConsumer out = vertices.getBuffer(RenderLayer.getLightning());

        matrices.push();
        getContextModel().applyLeftWingTipTransform(matrices);
        streak(out, matrices.peek().getPositionMatrix(), length, alpha);
        matrices.pop();

        matrices.push();
        getContextModel().applyRightWingTipTransform(matrices);
        streak(out, matrices.peek().getPositionMatrix(), length, alpha);
        matrices.pop();
    }

    private static void streak(VertexConsumer out, Matrix4f matrix, float length, int alpha) {
        // Two crossed, hair-thin tapered ribbons read as a clean white speed line
        // from every camera angle without the blocky CLOUD/POOF particle look.
        float w0 = 0.016F;
        float w1 = 0.002F;
        quad(out, matrix,
                -w0, 0.0F, 0.0F,
                 w0, 0.0F, 0.0F,
                 w1, 0.0F, length,
                -w1, 0.0F, length,
                alpha);
        quad(out, matrix,
                 0.0F, -w0, 0.0F,
                 0.0F,  w0, 0.0F,
                 0.0F,  w1, length,
                 0.0F, -w1, length,
                alpha);
    }

    private static void quad(VertexConsumer out, Matrix4f matrix,
                             float x1, float y1, float z1,
                             float x2, float y2, float z2,
                             float x3, float y3, float z3,
                             float x4, float y4, float z4,
                             int alpha) {
        out.vertex(matrix, x1, y1, z1).color(245, 251, 255, alpha).next();
        out.vertex(matrix, x2, y2, z2).color(245, 251, 255, alpha).next();
        out.vertex(matrix, x3, y3, z3).color(245, 251, 255, 0).next();
        out.vertex(matrix, x4, y4, z4).color(245, 251, 255, 0).next();
    }
}
