package org.marj4n.smooth_classes.client.renderer;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.util.Identifier;
import org.marj4n.smooth_classes.client.SmoothClassesClient;
import org.marj4n.smooth_classes.client.renderer.model.WraithModel;
import org.marj4n.smooth_classes.entity.WraithEntity;


@Environment(value= EnvType.CLIENT)
public class WraithRenderer extends MobEntityRenderer<WraithEntity, WraithModel> {


     private static final Identifier TEXTURE = new Identifier("smooth_classes","textures/entity/wraith.png");

    public WraithRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, new WraithModel(ctx.getPart(SmoothClassesClient.WRAITH_MODEL)), 1.0f);
    }

    @Override
    public Identifier getTexture(WraithEntity entity) {
        return TEXTURE;
    }

}
