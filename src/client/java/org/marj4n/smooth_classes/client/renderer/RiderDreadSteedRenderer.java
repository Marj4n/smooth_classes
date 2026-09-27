package org.marj4n.smooth_classes.client.renderer;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.feature.EyesFeatureRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.HorseEntityModel;
import net.minecraft.util.Identifier;
import org.marj4n.smooth_classes.SmoothClasses;
import org.marj4n.smooth_classes.entity.RiderDreadSteedEntity;

/**
 * Standalone Rider Dread Steed renderer.
 * The Dread Knight Horse textures are redistributed under the upstream LGPL-3.0-or-later
 * license; see THIRD_PARTY_NOTICES.md.
 */
@Environment(EnvType.CLIENT)
public final class RiderDreadSteedRenderer
        extends MobEntityRenderer<RiderDreadSteedEntity, HorseEntityModel<RiderDreadSteedEntity>> {

    private static final Identifier TEXTURE =
            SmoothClasses.id("textures/entity/rider/dread_steed.png");
    private static final Identifier EYES_TEXTURE =
            SmoothClasses.id("textures/entity/rider/dread_steed_eyes.png");

    public RiderDreadSteedRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, new HorseEntityModel<>(ctx.getPart(EntityModelLayers.SKELETON_HORSE)), 0.75F);
        addFeature(new EyesFeatureRenderer<RiderDreadSteedEntity, HorseEntityModel<RiderDreadSteedEntity>>(this) {
            @Override
            public RenderLayer getEyesTexture() {
                return RenderLayer.getEyes(EYES_TEXTURE);
            }
        });
    }

    @Override
    public Identifier getTexture(RiderDreadSteedEntity entity) {
        return TEXTURE;
    }
}
