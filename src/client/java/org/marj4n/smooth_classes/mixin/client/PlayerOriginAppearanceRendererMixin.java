package org.marj4n.smooth_classes.mixin.client;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import org.marj4n.smooth_classes.client.origin.appearance.OriginAppearanceFeatureRenderer;
import org.marj4n.smooth_classes.client.origin.appearance.ManBatPosePreparerFeatureRenderer;
import org.marj4n.smooth_classes.origin.OriginRuntime;
import org.marj4n.smooth_classes.origin.OriginType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hides vanilla body pieces whenever an Origin replaces them with a real model. */
@SuppressWarnings("unchecked")
@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerOriginAppearanceRendererMixin
        extends LivingEntityRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {
    @Unique private boolean smooth_classes$headVisible;
    @Unique private boolean smooth_classes$hatVisible;
    @Unique private boolean smooth_classes$bodyVisible;
    @Unique private boolean smooth_classes$jacketVisible;
    @Unique private boolean smooth_classes$rightArmVisible;
    @Unique private boolean smooth_classes$leftArmVisible;
    @Unique private boolean smooth_classes$rightSleeveVisible;
    @Unique private boolean smooth_classes$leftSleeveVisible;
    @Unique private boolean smooth_classes$rightLegVisible;
    @Unique private boolean smooth_classes$leftLegVisible;
    @Unique private boolean smooth_classes$rightPantsVisible;
    @Unique private boolean smooth_classes$leftPantsVisible;
    @Unique private boolean smooth_classes$visibilityChanged;

    protected PlayerOriginAppearanceRendererMixin(EntityRendererFactory.Context ctx,
                                                   PlayerEntityModel<AbstractClientPlayerEntity> model,
                                                   float shadowRadius) {
        super(ctx, model, shadowRadius);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void smooth_classes$addOriginAppearance(EntityRendererFactory.Context ctx, boolean slim, CallbackInfo ci) {
        // The vanilla armor feature runs before our custom appearance feature.
        // Give Man-Bat a renderless first feature to capture the CURRENT
        // Better Combat rig before armor reads the equipment pose.
        this.features.add(0, new ManBatPosePreparerFeatureRenderer(
                (FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>>) this,
                ctx
        ));
        this.addFeature(new OriginAppearanceFeatureRenderer(
                (FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>>) this,
                ctx
        ));
    }

    @Inject(method = "render(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/render/entity/PlayerEntityRenderer;setModelPose(Lnet/minecraft/client/network/AbstractClientPlayerEntity;)V",
                    shift = At.Shift.AFTER))
    private void smooth_classes$hideReplacedBody(AbstractClientPlayerEntity player, float yaw, float tickDelta,
                                                  MatrixStack matrices, VertexConsumerProvider consumers, int light,
                                                  CallbackInfo ci) {
        smooth_classes$visibilityChanged = false;
        if (player == null) return;
        OriginType origin = OriginRuntime.state(player).origin();
        if (origin == null) return;

        PlayerEntityModel<AbstractClientPlayerEntity> model = this.getModel();
        smooth_classes$saveVisibility(model);

        if (OriginAppearanceFeatureRenderer.requiresFullReplacement(player)) {
            model.head.visible = false;
            model.hat.visible = false;
            model.body.visible = false;
            model.jacket.visible = false;
            model.rightArm.visible = false;
            model.leftArm.visible = false;
            model.rightSleeve.visible = false;
            model.leftSleeve.visible = false;
            model.rightLeg.visible = false;
            model.leftLeg.visible = false;
            model.rightPants.visible = false;
            model.leftPants.visible = false;
            smooth_classes$visibilityChanged = true;
        } else if (origin == OriginType.MERMAID && org.marj4n.smooth_classes.client.origin.OriginMorphState.hideMermaidLegs(player)) {
            // Human legs on dry land, complete siren tail while wet.
            model.rightLeg.visible = false;
            model.leftLeg.visible = false;
            model.rightPants.visible = false;
            model.leftPants.visible = false;
            smooth_classes$visibilityChanged = true;
        } else if (origin == OriginType.DOPPELGANGER) {
            // Keep the player's normal skin/body, but remove the hat layer so the
            // flush faceless plate can fully cover the face without clipping.
            model.hat.visible = false;
            smooth_classes$visibilityChanged = true;
        }
    }

    @Inject(method = "render(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("RETURN"))
    private void smooth_classes$restoreBody(AbstractClientPlayerEntity player, float yaw, float tickDelta,
                                             MatrixStack matrices, VertexConsumerProvider consumers, int light,
                                             CallbackInfo ci) {
        if (!smooth_classes$visibilityChanged) return;
        smooth_classes$restoreVisibility(this.getModel());
        smooth_classes$visibilityChanged = false;
    }

    @Unique
    private void smooth_classes$saveVisibility(PlayerEntityModel<AbstractClientPlayerEntity> model) {
        smooth_classes$headVisible = model.head.visible;
        smooth_classes$hatVisible = model.hat.visible;
        smooth_classes$bodyVisible = model.body.visible;
        smooth_classes$jacketVisible = model.jacket.visible;
        smooth_classes$rightArmVisible = model.rightArm.visible;
        smooth_classes$leftArmVisible = model.leftArm.visible;
        smooth_classes$rightSleeveVisible = model.rightSleeve.visible;
        smooth_classes$leftSleeveVisible = model.leftSleeve.visible;
        smooth_classes$rightLegVisible = model.rightLeg.visible;
        smooth_classes$leftLegVisible = model.leftLeg.visible;
        smooth_classes$rightPantsVisible = model.rightPants.visible;
        smooth_classes$leftPantsVisible = model.leftPants.visible;
    }

    @Unique
    private void smooth_classes$restoreVisibility(PlayerEntityModel<AbstractClientPlayerEntity> model) {
        model.head.visible = smooth_classes$headVisible;
        model.hat.visible = smooth_classes$hatVisible;
        model.body.visible = smooth_classes$bodyVisible;
        model.jacket.visible = smooth_classes$jacketVisible;
        model.rightArm.visible = smooth_classes$rightArmVisible;
        model.leftArm.visible = smooth_classes$leftArmVisible;
        model.rightSleeve.visible = smooth_classes$rightSleeveVisible;
        model.leftSleeve.visible = smooth_classes$leftSleeveVisible;
        model.rightLeg.visible = smooth_classes$rightLegVisible;
        model.leftLeg.visible = smooth_classes$leftLegVisible;
        model.rightPants.visible = smooth_classes$rightPantsVisible;
        model.leftPants.visible = smooth_classes$leftPantsVisible;
    }
}
