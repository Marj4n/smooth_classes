package org.marj4n.smooth_classes.client.origin.appearance;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import org.marj4n.smooth_classes.client.SmoothClassesClient;
import org.marj4n.smooth_classes.origin.OriginRuntime;
import org.marj4n.smooth_classes.origin.OriginType;

/**
 * Runs BEFORE vanilla armor features, but does not draw anything.
 *
 * OriginAppearanceFeatureRenderer was appended after the vanilla armor
 * feature in PlayerEntityRenderer.<init>, leaving EQUIPMENT_POSES one frame
 * behind the Man-Bat while walking or swinging. Capture the animated vanilla
 * rig once before armor draws so the VSB skin and equipped armor use exactly
 * the same frame's shoulder/hip transforms.
 */
public final class ManBatPosePreparerFeatureRenderer
        extends FeatureRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {
    private final ManBatFormModel<AbstractClientPlayerEntity> manBat;

    public ManBatPosePreparerFeatureRenderer(
            FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> context,
            EntityRendererFactory.Context ctx) {
        super(context);
        manBat = new ManBatFormModel<>(ctx.getPart(SmoothClassesClient.ORIGIN_MAN_BAT_MODEL));
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
                       AbstractClientPlayerEntity player, float limbAngle, float limbDistance,
                       float tickDelta, float animationProgress, float headYaw, float headPitch) {
        if (player.isInvisible() || player.isSpectator()) return;
        var state = OriginRuntime.state(player);
        if (state.origin() != OriginType.VAMPIRE || !state.hasFlag("vampire.form.man_bat")) return;
        manBat.prepare(player, getContextModel(), limbAngle, limbDistance,
                animationProgress, headYaw, headPitch);
    }
}
