package org.marj4n.smooth_classes.client.renderer;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.marj4n.smooth_classes.SmoothClasses;
import org.marj4n.smooth_classes.client.SmoothClassesClient;
import org.marj4n.smooth_classes.client.renderer.model.RiderHippogryphModel;
import org.marj4n.smooth_classes.entity.RiderHippogryphEntity;

@Environment(EnvType.CLIENT)
public final class RiderHippogryphRenderer
        extends MobEntityRenderer<RiderHippogryphEntity, RiderHippogryphModel> {

    private static final Identifier TEXTURE =
            SmoothClasses.id("textures/entity/rider/hippogryph_white.png");

    public RiderHippogryphRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, new RiderHippogryphModel(ctx.getPart(SmoothClassesClient.RIDER_HIPPOGRYPH_MODEL)), 0.8F);
        this.addFeature(new RiderHippogryphWindTrailFeature(this));
    }

    @Override
    protected void scale(RiderHippogryphEntity entity, MatrixStack matrices, float amount) {
        matrices.scale(1.2F, 1.2F, 1.2F);
    }

    @Override
    public Identifier getTexture(RiderHippogryphEntity entity) {
        return TEXTURE;
    }
}
