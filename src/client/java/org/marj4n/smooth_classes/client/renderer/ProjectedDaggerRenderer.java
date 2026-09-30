package org.marj4n.smooth_classes.client.renderer;

import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.marj4n.smooth_classes.entity.ProjectedDaggerEntity;

public final class ProjectedDaggerRenderer extends EntityRenderer<ProjectedDaggerEntity> {
    private final ItemRenderer items;
    public ProjectedDaggerRenderer(EntityRendererFactory.Context context) { super(context); items = context.getItemRenderer(); }
    private static void renderTrail(ProjectedDaggerEntity entity, float delta, MatrixStack matrices,
                                    VertexConsumerProvider vertices) {
        var velocity=entity.getVelocity();
        double speedSq=velocity.lengthSquared();
        if(entity.age<=ProjectedDaggerEntity.PORTAL_EMERGENCE_TICKS || speedSq<1.0E-6D)return;

        double inv=1D/Math.sqrt(speedSq);
        double dx=velocity.x*inv, dy=velocity.y*inv, dz=velocity.z*inv;

        // direction x world-up = (-z, 0, x). Use primitive math to avoid the
        // temporary Vec3d/array churn caused by every dagger trail every frame.
        double sx=-dz, sy=0D, sz=dx;
        double sideSq=sx*sx+sz*sz;
        if(sideSq<1.0E-6D){sx=1D;sz=0D;}
        else {double sideInv=1D/Math.sqrt(sideSq);sx*=sideInv;sz*=sideInv;}

        // direction x side
        double ox=dy*sz-dz*sy;
        double oy=dz*sx-dx*sz;
        double oz=dx*sy-dy*sx;
        double otherSq=ox*ox+oy*oy+oz*oz;
        if(otherSq>1.0E-8D){double otherInv=1D/Math.sqrt(otherSq);ox*=otherInv;oy*=otherInv;oz*=otherInv;}

        double length=entity.visualTrailLength(delta);
        double hx=dx*-.15D, hy=dy*-.15D, hz=dz*-.15D;
        double tailMul=-.15D-length;
        double tx=dx*tailMul, ty=dy*tailMul, tz=dz*tailMul;
        var buffer=vertices.getBuffer(net.minecraft.client.render.RenderLayer.getLightning());
        var matrix=matrices.peek().getPositionMatrix();
        trailRibbon(buffer,matrix,hx,hy,hz,tx,ty,tz,sx,sy,sz);
        trailRibbon(buffer,matrix,hx,hy,hz,tx,ty,tz,ox,oy,oz);
    }

    private static void trailRibbon(net.minecraft.client.render.VertexConsumer buffer,org.joml.Matrix4f matrix,
                                    double hx,double hy,double hz,double tx,double ty,double tz,
                                    double wx,double wy,double wz){
        double ax=hx+wx*.009D, ay=hy+wy*.009D, az=hz+wz*.009D;
        double bx=hx-wx*.009D, by=hy-wy*.009D, bz=hz-wz*.009D;
        double dx=tx+wx*.001D, dy=ty+wy*.001D, dz=tz+wz*.001D;
        double ex=tx-wx*.001D, ey=ty-wy*.001D, ez=tz-wz*.001D;
        trailVertex(buffer,matrix,ax,ay,az,.55F);trailVertex(buffer,matrix,bx,by,bz,.55F);
        trailVertex(buffer,matrix,ex,ey,ez,0F);trailVertex(buffer,matrix,dx,dy,dz,0F);
        trailVertex(buffer,matrix,dx,dy,dz,0F);trailVertex(buffer,matrix,ex,ey,ez,0F);
        trailVertex(buffer,matrix,bx,by,bz,.55F);trailVertex(buffer,matrix,ax,ay,az,.55F);
    }

    private static void trailVertex(net.minecraft.client.render.VertexConsumer buffer,org.joml.Matrix4f matrix,
                                    double x,double y,double z,float alpha){
        buffer.vertex(matrix,(float)x,(float)y,(float)z).color(1F,1F,1F,alpha).next();
    }
    @Override public Identifier getTexture(ProjectedDaggerEntity entity) { return SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE; }
    @Override public void render(ProjectedDaggerEntity entity, float yaw, float delta, MatrixStack matrices,
                                 VertexConsumerProvider vertices, int light) {
        if (entity.weapon().isEmpty()) return;
        renderTrail(entity, delta, matrices, vertices);
        matrices.push();
        // Generated dagger models point along local +X/+Y. Rotate that diagonal to
        // local +Z, then align +Z with the projectile's yaw and pitch.
        // FIXED adds the item-frame Y rotation and turns the blade sideways.
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(entity.getYaw()));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90 - entity.getPitch()));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(45));
        if (entity.age <= ProjectedDaggerEntity.PORTAL_EMERGENCE_TICKS) {
            float progress = Math.min(1F, (entity.age + delta) / (float) ProjectedDaggerEntity.PORTAL_EMERGENCE_TICKS);
            matrices.translate(0F, 0F, 0.24F + progress * 0.14F);
            float scale = 0.82F + progress * 0.28F;
            matrices.scale(scale, scale, scale);
        } else {
            matrices.scale(1.1F, 1.1F, 1.1F);
        }
        items.renderItem(entity.weapon(), ModelTransformationMode.NONE, 15728880,
                OverlayTexture.DEFAULT_UV, matrices, vertices, entity.getWorld(), entity.getId());
        matrices.pop();
    }
}
