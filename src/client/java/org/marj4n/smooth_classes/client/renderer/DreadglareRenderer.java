package org.marj4n.smooth_classes.client.renderer;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.marj4n.smooth_classes.client.SmoothClassesClient;
import org.marj4n.smooth_classes.client.renderer.model.DreadglareModel;
import org.marj4n.smooth_classes.entity.DreadglareEntity;


@Environment(value= EnvType.CLIENT)
public class DreadglareRenderer extends MobEntityRenderer<DreadglareEntity, DreadglareModel> {


     private static final Identifier TEXTURE = new Identifier("smooth_classes","textures/entity/dreadglare.png");

    public DreadglareRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, new DreadglareModel(ctx.getPart(SmoothClassesClient.DREADGLARE_MODEL)), 1.0f);
    }

    @Override
    public Identifier getTexture(DreadglareEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(DreadglareEntity dreadglareEntity, float entityYaw, float partialTicks, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int packedLightIn) {
        matrixStack.push();
        matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(dreadglareEntity.getPitch(partialTicks)));
        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(dreadglareEntity.getYaw(partialTicks)));

        super.render(dreadglareEntity, entityYaw, partialTicks, matrixStack, vertexConsumerProvider, packedLightIn);

        matrixStack.pop();
    }
}
