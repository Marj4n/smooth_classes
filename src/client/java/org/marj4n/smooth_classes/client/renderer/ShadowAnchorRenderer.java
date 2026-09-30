package org.marj4n.smooth_classes.client.renderer;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.*;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.marj4n.smooth_classes.entity.ShadowAnchorEntity;
public final class ShadowAnchorRenderer extends EntityRenderer<ShadowAnchorEntity> {
    private static final Identifier TEXTURE = new Identifier("minecraft", "textures/block/white_concrete.png");
    private final ModelPart body;
    public ShadowAnchorRenderer(EntityRendererFactory.Context context) {
        super(context); body = context.getPart(EntityModelLayers.PLAYER);
        for (String name : new String[]{"hat","jacket","left_sleeve","right_sleeve","left_pants","right_pants"})
            body.getChild(name).visible = false;
    }
    public Identifier getTexture(ShadowAnchorEntity entity) { return TEXTURE; }
    public void render(ShadowAnchorEntity entity, float yaw, float delta, MatrixStack matrices, VertexConsumerProvider consumers, int light) {
        matrices.push(); matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180-yaw));
        matrices.scale(-1,-1,1); matrices.translate(0,-1.501,0);
        float alpha = entity.getCommandTags().contains("smooth_classes_shadow_preview") ? .34F : .72F;
        // Shadow Technique lasts 200 ticks. Fade during the final two seconds instead of popping out.
        if (entity.age > 160) alpha *= Math.max(.04F, (200F - entity.age) / 40F);
        body.render(matrices, consumers.getBuffer(RenderLayer.getEntityTranslucent(TEXTURE)), light,
                OverlayTexture.DEFAULT_UV, .03F,.015F,.055F,alpha);
        matrices.pop(); super.render(entity,yaw,delta,matrices,consumers,light);
    }
}
