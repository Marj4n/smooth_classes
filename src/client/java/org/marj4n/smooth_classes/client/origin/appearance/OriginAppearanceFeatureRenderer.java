package org.marj4n.smooth_classes.client.origin.appearance;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.marj4n.smooth_classes.SmoothClasses;
import org.marj4n.smooth_classes.client.SmoothClassesClient;
import org.marj4n.smooth_classes.client.origin.OriginMorphState;
import org.marj4n.smooth_classes.origin.OriginRuntime;
import org.marj4n.smooth_classes.origin.OriginState;
import org.marj4n.smooth_classes.origin.OriginType;

/**
 * Origin appearance renderer. Full-body origins replace the vanilla body.
 */
public final class OriginAppearanceFeatureRenderer
        extends FeatureRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {
    private static final Identifier WHITE = SmoothClasses.id("textures/entity/origin/white.png");
    private static final Identifier FACILESS_FACE = SmoothClasses.id("textures/entity/origin/doppelganger_overlay.png");
    private static final Identifier DEMON_TINT = SmoothClasses.id("textures/entity/origin/demon_overlay.png");
    private static final Identifier MERMAID_TAIL = SmoothClasses.id("textures/entity/origin/mermaid_tail_mermod.png");
    private static final Identifier VOID = SmoothClasses.id("textures/entity/origin/void_overlay.png");
    private static final Identifier VOID_EYES = SmoothClasses.id("textures/entity/origin/void_eyes_defile.png");
    private static final Identifier HOMUNCULUS = SmoothClasses.id("textures/entity/origin/homunculus_player.png");
    private static final Identifier SPRIGGAN = SmoothClasses.id("textures/entity/origin/spriggan_player.png");
    private static final Identifier SLIME = new Identifier("minecraft", "textures/entity/slime/slime.png");
    private static final Identifier VAMPIRE = SmoothClasses.id("textures/entity/origin/vampire_nycto.png");
    private static final Identifier MAN_BAT = SmoothClasses.id("textures/entity/origin/vsb_vampire.png");
    private static final Identifier MAN_BAT_EMISSIVE = SmoothClasses.id("textures/entity/origin/vsb_vampire_e.png");
    private static final Identifier LORD_MAN_BAT = SmoothClasses.id("textures/entity/origin/vsb_vampire_lord.png");
    private static final Identifier LORD_MAN_BAT_EMISSIVE = SmoothClasses.id("textures/entity/origin/vsb_vampire_lord_e.png");
    private static final Identifier BAT = new Identifier("minecraft", "textures/entity/bat.png");
    private static final Identifier VAMPIRE_EYES = SmoothClasses.id("textures/entity/origin/vampire_eyes_nycto.png");
    private static final Identifier[] UNDEAD = new Identifier[]{
            SmoothClasses.id("textures/entity/origin/undead/amethyst.png"),
            SmoothClasses.id("textures/entity/origin/undead/diamond.png"),
            SmoothClasses.id("textures/entity/origin/undead/emerald.png"),
            SmoothClasses.id("textures/entity/origin/undead/gold.png"),
            SmoothClasses.id("textures/entity/origin/undead/jade.png"),
            SmoothClasses.id("textures/entity/origin/undead/turquoise.png")
    };

    private final OriginCosmeticModel<AbstractClientPlayerEntity> cosmetics;
    private final OriginReplacementModel<AbstractClientPlayerEntity> replacement;
    private final OriginReplacementModel<AbstractClientPlayerEntity> undead;
    private final MermodTailModel<AbstractClientPlayerEntity> mermaidTail;
    private final SlimeReplacementModel<AbstractClientPlayerEntity> slime;
    private final HomunculusReplacementModel<AbstractClientPlayerEntity> homunculus;
    private final BatFormModel<AbstractClientPlayerEntity> bat;
    private final ManBatFormModel<AbstractClientPlayerEntity> manBat;

    public OriginAppearanceFeatureRenderer(
            FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> context,
            EntityRendererFactory.Context ctx
    ) {
        super(context);
        this.cosmetics = new OriginCosmeticModel<>(ctx.getPart(SmoothClassesClient.ORIGIN_COSMETICS_MODEL));
        this.replacement = new OriginReplacementModel<>(ctx.getPart(SmoothClassesClient.ORIGIN_REPLACEMENT_MODEL));
        this.undead = new OriginReplacementModel<>(ctx.getPart(SmoothClassesClient.ORIGIN_UNDEAD_MODEL));
        this.mermaidTail = new MermodTailModel<>(ctx.getPart(SmoothClassesClient.ORIGIN_MERMOD_TAIL_MODEL));
        this.slime = new SlimeReplacementModel<>(ctx.getPart(SmoothClassesClient.ORIGIN_SLIME_MODEL));
        this.homunculus = new HomunculusReplacementModel<>(ctx.getPart(SmoothClassesClient.ORIGIN_HOMUNCULUS_MODEL));
        this.bat = new BatFormModel<>(ctx.getPart(SmoothClassesClient.ORIGIN_BAT_MODEL));
        this.manBat = new ManBatFormModel<>(ctx.getPart(SmoothClassesClient.ORIGIN_MAN_BAT_MODEL));
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
                       AbstractClientPlayerEntity player, float limbAngle, float limbDistance,
                       float tickDelta, float animationProgress, float headYaw, float headPitch) {
        if (player.isSpectator() || player.isInvisible()) return;

        OriginState state = OriginRuntime.state(player);
        OriginType origin = state.origin();
        if (origin == null) return;

        PlayerEntityModel<AbstractClientPlayerEntity> parent = this.getContextModel();
        cosmetics.prepare(parent, player, limbAngle, limbDistance, animationProgress);

        switch (origin) {
            case HUMAN -> { }
            case VAMPIRE -> renderVampireState(state, parent, matrices, vertexConsumers, light,
                    player, limbAngle, limbDistance, animationProgress, headYaw, headPitch);
            case WEREWOLF -> {
                // Werewolf stays visually normal here. The dedicated wolf-form
                // implementation is intentionally left to the Werewolves-style runtime.
            }
            case MERMAID -> {
                float morph = OriginMorphState.mermaid(player);
                if (morph > 0.01F) {
                    mermaidTail.prepare(parent, player, limbAngle, limbDistance, animationProgress);
                    VertexConsumer tail = vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(MERMAID_TAIL));
                    matrices.push();
                    float ease = morph * morph * (3.0F - 2.0F * morph);
                    matrices.translate(0.0D, (1.0F - ease) * 0.18D, 0.0D);
                    matrices.scale(0.72F + ease * 0.28F, 0.42F + ease * 0.58F, 0.72F + ease * 0.28F);
                    mermaidTail.render(matrices, tail, light, OverlayTexture.DEFAULT_UV, 1, 1, 1, ease);
                    matrices.pop();
                }
            }
            case DEMON -> {
                renderOverlay(parent, DEMON_TINT, matrices, vertexConsumers, light, 1, 1, 1, 0.10F);
                renderCosmetic(cosmetics::renderDemon, WHITE, matrices, vertexConsumers, light,
                        0.58F, 0.08F, 0.08F, 1.0F);
            }
            case ANGEL -> renderCosmetic(cosmetics::renderAngel, WHITE, matrices, vertexConsumers, light,
                    1.0F, 0.96F, 0.80F, 0.95F);
            case SLIME -> {
                if (!state.hasFlag("slime.form.humanoid")) {
                    slime.prepare(parent, player, limbAngle, limbDistance, animationProgress);
                    renderSlime(matrices, vertexConsumers, light);
                }
            }
            case HOMUNCULUS -> {
                homunculus.copyFrom(parent, player, limbAngle, limbDistance, animationProgress, headYaw, headPitch);
                matrices.push();
                float scale = HomunculusReplacementModel.RENDER_SCALE;
                matrices.translate(0.0D, 1.8D * (1.0D - scale), 0.0D);
                matrices.scale(scale, scale, scale);
                VertexConsumer consumer = vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(HOMUNCULUS));
                homunculus.render(matrices, consumer, light, OverlayTexture.DEFAULT_UV, 1, 1, 1, 1);
                homunculus.renderEyes(matrices, vertexConsumers.getBuffer(RenderLayer.getEyes(HOMUNCULUS)),
                        OverlayTexture.DEFAULT_UV);
                homunculus.renderWeaponArm(player, matrices, vertexConsumers, light);
                matrices.pop();
            }
            case VOID -> {
                replacement.copyFrom(parent);
                renderReplacement(replacement, VOID, matrices, vertexConsumers, light, 1, 1, 1, 1);
                VertexConsumer voidEyes = vertexConsumers.getBuffer(RenderLayer.getEyes(VOID_EYES));
                replacement.render(matrices, voidEyes, 0xF000F0, OverlayTexture.DEFAULT_UV, 1, 1, 1, 1);
                renderCosmetic(cosmetics::renderVoid, WHITE, matrices, vertexConsumers, 0xF000F0,
                        0.48F, 0.05F, 0.80F, 0.95F);
            }
            case UNDEAD -> {
                undead.copyFrom(parent);
                Identifier undeadTexture = undeadTexture(player);
                renderReplacement(undead, undeadTexture, matrices, vertexConsumers, light, 1, 1, 1, 1);
                if (state.carbonLayer() > 0) {
                    float dark = Math.min(0.55F, state.carbonLayer() / 150.0F);
                    renderReplacement(undead, undeadTexture, matrices, vertexConsumers, light,
                            1.0F - dark, 1.0F - dark, 1.0F - dark, 1.0F);
                }
            }
            case SPRIGGAN -> {
                replacement.copyFrom(parent);
                renderReplacement(replacement, SPRIGGAN, matrices, vertexConsumers, light, 1, 1, 1, 1);
                renderCosmetic(cosmetics::renderSpriggan, WHITE, matrices, vertexConsumers, light,
                        0.35F, 0.25F, 0.12F, 1.0F);
            }
            case DOPPELGANGER -> renderCosmetic(cosmetics::renderDoppelganger, WHITE, matrices, vertexConsumers, light,
                    0.72F, 0.64F, 0.58F, 1.0F);
        }
    }

    private void renderVampireState(OriginState state, PlayerEntityModel<AbstractClientPlayerEntity> parent,
                                    MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
                                    AbstractClientPlayerEntity player, float limbAngle, float limbDistance,
                                    float animationProgress, float headYaw, float headPitch) {
        if (state.hasFlag("vampire.form.man_bat")) {
            // During first-person Player Animator weapon attacks, the vanilla
            // humanoid rig becomes the animation carrier. Do not draw VSB on
            // top of that camera-space pass; TPV remains the exact VSB mesh.
            if (ManBatFormModel.useVanillaFirstPersonCombat(player)) return;
            // Man-Bat currently uses the user-supplied Vampires Strike Back Figura vampire
            // avatar as a native Smooth Classes reference port. Gameplay remains Smooth Classes.
            // Armor is intentionally left enabled by ArmorStealthRendererMixin.
            manBat.prepare(player, parent, limbAngle, limbDistance, animationProgress, headYaw, headPitch);
            matrices.push();
            // Exact source coordinates are mapped from Figura Y-up to vanilla
            // model-space Y-down by VsbFiguraAvatarRenderer itself.
            boolean lord = state.hasFlag("vampire.evolution.lord");
            VertexConsumer beast = vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(lord ? LORD_MAN_BAT : MAN_BAT));
            manBat.render(matrices, beast, light, OverlayTexture.DEFAULT_UV, 1, 1, 1, 1);
            // Lord uses the same UV layout, with wine-red emissive eyes.
            VertexConsumer emissive = vertexConsumers.getBuffer(RenderLayer.getEyes(
                    lord ? LORD_MAN_BAT_EMISSIVE : MAN_BAT_EMISSIVE));
            manBat.render(matrices, emissive, 0xF000F0, OverlayTexture.DEFAULT_UV, 1, 1, 1, 1);
            // Items are intentionally rendered by Minecraft's regular
            // HeldItemFeatureRenderer after the body layer, using the same
            // PlayerEntityModel pose as Better Combat. Never re-render items
            // at Figura's separate wrists: that rotates swords backwards.
            matrices.pop();
        } else if (state.hasFlag("vampire.form.bat")) {
            float flightBlend = org.marj4n.smooth_classes.client.origin.OriginMorphState.batFlight(player);

            // V1.6.2: always use Minecraft's actual BatEntityModel.  Grounded state is
            // only a different pose/anchor; there is no custom crawl mesh anymore.
            bat.prepare(player, player.age + 0.5F, limbAngle, limbDistance, flightBlend);
            matrices.push();

            // The player feature renderer is centred on a humanoid body, while the
            // vanilla bat model is authored around a much higher pivot.  Keep the
            // grounded mesh low enough that its rear feet and folded wing-hands meet
            // the floor, but do not bury it.  Airborne state rises smoothly into the
            // normal vanilla-flight anchor.
            // Keep the V1.5.7 ground anchor feel, but lift it by 0.10 blocks so the
            // crawl no longer sinks into the floor. Airborne alignment now stays inside the same compact collision body instead of lifting the mesh toward the old humanoid anchor.
            double groundedY = org.marj4n.smooth_classes.origin.OriginBodyGeometry.BAT_GROUNDED_RENDER_Y;
            double airborneY = org.marj4n.smooth_classes.origin.OriginBodyGeometry.BAT_AIRBORNE_RENDER_Y;
            double y = MathHelper.lerp(flightBlend, groundedY, airborneY);
            double z = 0.04D * (1.0F - flightBlend);
            matrices.translate(0.0D, y, z);
            float batScale = org.marj4n.smooth_classes.origin.OriginBodyGeometry.BAT_RENDER_SCALE;
            matrices.scale(batScale, batScale, batScale);

            VertexConsumer batConsumer = vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(BAT));
            if (state.hasFlag("vampire.evolution.lord")) {
                // Preserve vanilla Bat geometry and animation; only tint the Lord palette.
                bat.render(matrices, batConsumer, light, OverlayTexture.DEFAULT_UV,
                        0.76F, 0.46F, 0.67F, 1.0F);
            } else {
                // Base Bat Form now uses an albino palette: pale fur, soft pink membrane,
                // and subtle cool-lavender shadowing while preserving vanilla geometry.
                bat.render(matrices, batConsumer, light, OverlayTexture.DEFAULT_UV,
                        1.0F, 0.90F, 0.94F, 1.0F);
            }
            matrices.pop();
        }
    }

    public static boolean requiresFullReplacement(AbstractClientPlayerEntity player) {
        if (player == null) return false;
        OriginState state = OriginRuntime.state(player);
        OriginType origin = state.origin();
        if (origin == null) return false;
        return switch (origin) {
            case VOID, UNDEAD, HOMUNCULUS, SPRIGGAN -> true;
            case SLIME -> !state.hasFlag("slime.form.humanoid");
            case VAMPIRE -> state.hasFlag("vampire.form.bat") ||
                    (state.hasFlag("vampire.form.man_bat")
                            && !ManBatFormModel.useVanillaFirstPersonCombat(player));
            default -> false;
        };
    }

    public static boolean isMermaidTailActive(AbstractClientPlayerEntity player) {
        return player != null && (player.isTouchingWaterOrRain() || player.isSubmergedInWater() || player.isSwimming());
    }

    private static Identifier undeadTexture(AbstractClientPlayerEntity player) {
        int index = Math.floorMod(player.getUuid().hashCode(), UNDEAD.length);
        return UNDEAD[index];
    }

    private void renderSlime(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        VertexConsumer inner = vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(SLIME));
        slime.renderInner(matrices, inner, light, OverlayTexture.DEFAULT_UV);
        VertexConsumer outer = vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(SLIME));
        slime.renderOuter(matrices, outer, light, OverlayTexture.DEFAULT_UV);
    }

    private void renderReplacement(OriginReplacementModel<AbstractClientPlayerEntity> model, Identifier texture,
                                   MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
                                   float red, float green, float blue, float alpha) {
        VertexConsumer consumer = vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(texture));
        model.render(matrices, consumer, light, OverlayTexture.DEFAULT_UV, red, green, blue, alpha);
    }

    private void renderOverlay(PlayerEntityModel<AbstractClientPlayerEntity> model, Identifier texture,
                               MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
                               float red, float green, float blue, float alpha) {
        VertexConsumer consumer = vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(texture));
        model.render(matrices, consumer, light, OverlayTexture.DEFAULT_UV, red, green, blue, alpha);
    }

    private void renderCosmetic(CosmeticRenderer renderer, Identifier texture,
                                MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
                                float red, float green, float blue, float alpha) {
        VertexConsumer consumer = vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(texture));
        renderer.render(matrices, consumer, light, OverlayTexture.DEFAULT_UV, red, green, blue, alpha);
    }

    @FunctionalInterface
    private interface CosmeticRenderer {
        void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay,
                    float red, float green, float blue, float alpha);
    }
}
