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
import org.marj4n.smooth_classes.client.renderer.model.GreaterDreadglareModel;
import org.marj4n.smooth_classes.entity.GreaterDreadglareEntity;


@Environment(value= EnvType.CLIENT)
public class GreaterDreadglareRenderer extends MobEntityRenderer<GreaterDreadglareEntity, GreaterDreadglareModel> {


     private static final Identifier TEXTURE = new Identifier("smooth_classes","textures/entity/dreadglare.png");

    public GreaterDreadglareRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, new GreaterDreadglareModel(ctx.getPart(SmoothClassesClient.GREATER_DREADGLARE_MODEL)), 1.0f);
    }

    @Override
    public Identifier getTexture(GreaterDreadglareEntity entity) {
        return TEXTURE;
    }

    @Override
    protected void scale(GreaterDreadglareEntity entity, MatrixStack matrixStack, float partialTickTime) {
        matrixStack.scale(1.5f, 1.5f, 1.5f);
    }

    @Override
    public void render(GreaterDreadglareEntity greaterDreadglareEntity, float entityYaw, float partialTicks, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int packedLightIn) {
        matrixStack.push();
        matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(greaterDreadglareEntity.getPitch(partialTicks)));
        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(greaterDreadglareEntity.getYaw(partialTicks)));

        super.render(greaterDreadglareEntity, entityYaw, partialTicks, matrixStack, vertexConsumerProvider, packedLightIn);

        matrixStack.pop();
    }
}
