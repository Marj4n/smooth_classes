package org.marj4n.smooth_classes.client.renderer;

import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.marj4n.smooth_classes.entity.SacredBannerEntity;

/** Battle Standard geometry and texture from the user-supplied Simply Swords 1.70.2 JAR. */
public final class SacredBannerRenderer extends EntityRenderer<SacredBannerEntity> {
    private static final Identifier TEXTURE = new Identifier("smooth_classes",
            "textures/entity/sacred_banner/battlestandard_texture.png");
    private static final float SCALE = 64F / 30F; // 30 model pixels -> 4 blocks
    private final ModelPart model;

    public SacredBannerRenderer(EntityRendererFactory.Context context) {
        super(context);
        ModelData data = new ModelData();
        data.getRoot().addChild("supports", ModelPartBuilder.create()
                .uv(0,17).cuboid(0,-30,0,1,21,1)
                .uv(0,17).cuboid(0,-9,0,1,9,1)
                .uv(14,17).cuboid(-4,-28,0,4,1,1)
                .uv(18,2).cuboid(-2,-22,0,2,1,1)
                .uv(18,0).cuboid(1,-22,0,2,1,1)
                .uv(4,17).cuboid(1,-28,0,4,1,1),
                ModelTransform.pivot(0,24,0));
        model = TexturedModelData.of(data,64,64).createModel();
    }

    @Override public void render(SacredBannerEntity entity,float yaw,float delta,
                                 MatrixStack matrices,VertexConsumerProvider vertices,int light) {
        matrices.push();
        try {
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180F-entity.getYaw()));
            matrices.scale(-SCALE,-SCALE,SCALE);
            matrices.translate(0,-1.5,0);
            model.render(matrices,vertices.getBuffer(RenderLayer.getEntityCutoutNoCull(TEXTURE)),
                    light,OverlayTexture.DEFAULT_UV,1F,1F,1F,1F);
            renderGoldenCloth(matrices, vertices, light, entity.age + delta);
        } finally {
            matrices.pop();
        }
        super.render(entity,yaw,delta,matrices,vertices,light);
    }

    private static void renderGoldenCloth(MatrixStack matrices, VertexConsumerProvider vertices, int light, float time) {
        var atlas = net.minecraft.client.texture.SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE;
        var sprite = net.minecraft.client.MinecraftClient.getInstance().getSpriteAtlas(atlas)
                .apply(new Identifier("smooth_classes", "item/sacred_banner_white"));
        // Full supplied White Faction cloth, with its black border preserved.
        float u0 = sprite.getFrameU(0D);
        float u1 = sprite.getFrameU(16D);
        float v0 = sprite.getFrameV(0D);
        float v1 = sprite.getFrameV(16D);
        var buffer = vertices.getBuffer(RenderLayer.getEntityCutoutNoCull(atlas));
        var entry = matrices.peek();
        float left=-4F/16F, right=5F/16F;
        float top=(24F-29.75F)/16F, bottom=(24F-12.75F)/16F;
        // A subdivided sheet flexes from the fixed top seam toward the free hem.
        // Movement is entirely visual; the pole and aura stay fixed on the ground.
        final int rows=16, columns=8;
        for (int row=0;row<rows;row++) {
            float t0=row/(float)rows, t1=(row+1)/(float)rows;
            for (int col=0;col<columns;col++) {
                float s0=col/(float)columns, s1=(col+1)/(float)columns;
                windVertex(buffer,entry,left,right,top,bottom,u0,u1,v0,v1,s0,t0,time,light);
                windVertex(buffer,entry,left,right,top,bottom,u0,u1,v0,v1,s1,t0,time,light);
                windVertex(buffer,entry,left,right,top,bottom,u0,u1,v0,v1,s1,t1,time,light);
                windVertex(buffer,entry,left,right,top,bottom,u0,u1,v0,v1,s0,t1,time,light);
            }
        }
    }

    private static void windVertex(net.minecraft.client.render.VertexConsumer buffer,
            MatrixStack.Entry entry,float left,float right,float top,float bottom,
            float u0,float u1,float v0,float v1,float s,float t,float time,int light) {
        float phase=time*0.13F-t*5.5F+s*3F;
        float freedom=t*t;
        float x=left+(right-left)*s;
        float y=top+(bottom-top)*t+(float)Math.sin(phase)*0.035F*freedom;
        float z=-0.25F/16F-(0.07F+(float)Math.sin(phase)*0.055F)*freedom;
        clothVertex(buffer,entry,x,y,z,u0+(u1-u0)*s,v0+(v1-v0)*t,light);
    }

    private static void clothVertex(net.minecraft.client.render.VertexConsumer buffer,
            MatrixStack.Entry entry,float x,float y,float z,float u,float v,int light) {
        buffer.vertex(entry.getPositionMatrix(),x,y,z).color(255,255,255,255)
                .texture(u,v).overlay(OverlayTexture.DEFAULT_UV).light(light)
                .normal(entry.getNormalMatrix(),0,0,-1).next();
    }

    @Override public Identifier getTexture(SacredBannerEntity entity) { return TEXTURE; }
}
