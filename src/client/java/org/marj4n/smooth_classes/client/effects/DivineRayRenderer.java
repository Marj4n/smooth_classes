package org.marj4n.smooth_classes.client.effects;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.spell_engine.api.effect.CustomModelStatusEffect;
import org.joml.Matrix4f;
import org.marj4n.smooth_classes.effects.SmoothEffects;

/** Full-bright celestial column centered on the marked ally. */
public final class DivineRayRenderer implements CustomModelStatusEffect.Renderer {
    @Override
    public void renderEffect(long startTime,int amplifier,LivingEntity entity,float delta,
                             MatrixStack matrices,VertexConsumerProvider consumers,int light) {
        var status=entity.getStatusEffect(SmoothEffects.DIVINE_RAY);
        if(status==null)return;
        float fade=Math.min(1F,Math.max(0F,(status.getDuration()-delta)/16F));
        int alpha=(int)(86*fade);
        if(alpha<=0)return;
        float x=Math.max(.65F,entity.getWidth()*.85F);
        float y=entity.getHeight()+4F;
        Matrix4f matrix=matrices.peek().getPositionMatrix();
        VertexConsumer out=consumers.getBuffer(RenderLayer.getLightning());
        face(out,matrix,-x,0,0,x,0,0,x,y,0,-x,y,0,alpha);
        face(out,matrix,0,0,-x,0,0,x,0,y,x,0,y,-x,alpha);
        float core=x*.3F;
        face(out,matrix,-core,0,0,core,0,0,core,y,0,-core,y,0,alpha);
        face(out,matrix,0,0,-core,0,0,core,0,y,core,0,y,-core,alpha);
    }

    private static void face(VertexConsumer out,Matrix4f matrix,
                             float x1,float y1,float z1,float x2,float y2,float z2,
                             float x3,float y3,float z3,float x4,float y4,float z4,int alpha) {
        out.vertex(matrix,x1,y1,z1).color(255,247,206,alpha).next();
        out.vertex(matrix,x2,y2,z2).color(255,247,206,alpha).next();
        out.vertex(matrix,x3,y3,z3).color(255,247,206,alpha).next();
        out.vertex(matrix,x4,y4,z4).color(255,247,206,alpha).next();
    }
}
