package org.marj4n.smooth_classes.client.renderer;

import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.marj4n.smooth_classes.entity.BladePortalEntity;


/**
 * Portal of Sovereignty: bright anime-style golden gates with a much more
 * textured center vortex and a mirrored front/back appearance.
 */
public final class BladePortalRenderer extends EntityRenderer<BladePortalEntity> {
    private static final Identifier CORE_TEXTURE = new Identifier("minecraft", "textures/block/white_concrete.png");
    private static final int FULL_BRIGHT = 0xF000F0;

    public BladePortalRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public Identifier getTexture(BladePortalEntity entity) {
        return CORE_TEXTURE;
    }

    @Override
    public void render(BladePortalEntity entity, float yaw, float delta, MatrixStack matrices,
                       VertexConsumerProvider vertices, int light) {
        LivingEntity caster = entity.caster();
        if (caster == null) return;

        float direction = MathHelper.lerpAngleDegrees(delta, caster.prevYaw, caster.getYaw());
        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-direction));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(caster.getPitch(delta)));

        float progress = entity.opening(delta);
        float opening = progress * progress * (3F - 2F * progress);
        float summonPulse = 1F + MathHelper.sin(progress * (float) Math.PI) * .075F;
        float scale = entity.scale();
        int detail = scale >= 0.95F ? 72 : scale >= 0.75F ? 56 : 40;
        int detailLow = Math.max(20, detail / 2);
        matrices.scale(opening * summonPulse * scale, opening * summonPulse * scale, opening * scale);

        Matrix4f position = matrices.peek().getPositionMatrix();
        Matrix3f normal = matrices.peek().getNormalMatrix();

        VertexConsumer body = vertices.getBuffer(RenderLayer.getEntityTranslucent(CORE_TEXTURE));
        // golden tunnel core with more layers so the middle does not look flat.
        mirroredDisc(body, position, normal, 1.10D, 0.118F, 255, 240, 166, 88, detail);
        mirroredDisc(body, position, normal, 1.02D, 0.090F, 255, 230, 116, 108, detail);
        mirroredDisc(body, position, normal, 0.92D, 0.060F, 255, 214, 82, 128, detail);
        mirroredDisc(body, position, normal, 0.78D, 0.038F, 255, 236, 148, 118, detailLow);
        mirroredDisc(body, position, normal, 0.62D, 0.020F, 255, 205, 62, 96, detailLow);
        mirroredDisc(body, position, normal, 0.44D, 0.008F, 255, 244, 190, 80, detailLow);

        VertexConsumer glow = vertices.getBuffer(RenderLayer.getLightning());
        double spin = (entity.age + delta) * .012D;
        float pulse = .93F + MathHelper.sin((entity.age + delta) * .18F) * .07F;

        // vivid center texture / vortex.
        centerDisc(glow, position, 0.38D, 1F, .97F, .80F, .18F * pulse, detailLow, .004F);
        ring(glow, position, .05, .16, spin * 0.30D, 1F, .99F, .84F, .44F * pulse, detailLow, 1D, .010F);
        ring(glow, position, .16, .26, -spin * 0.48D, 1F, .86F, .22F, .36F * pulse, detailLow, 1D, .012F);
        ring(glow, position, .26, .38, spin * 0.66D, 1F, .97F, .70F, .30F * pulse, detailLow, 1D, .014F);
        rippleBands(glow, position, spin, .30F * pulse, 5);
        spiral(glow, position, spin * -2.4D, .70F * pulse, scale >= .82F ? 28 : 20);
        spokes(glow, position, spin * 0.82D, 10, .12D, .92D, 1F, .89F, .32F, .22F * pulse, .014F);

        // Concentric circular portal ornaments.
        ring(glow, position, .18, .23, spin * 0.28D, 1F, .98F, .76F, .54F * pulse, detail, 1D, .018F);
        ring(glow, position, .31, .35, -spin * 0.34D, 1F, .88F, .34F, .50F * pulse, detail, 1D, .021F);
        ring(glow, position, .44, .48, spin * 0.22D, 1F, .95F, .62F, .47F * pulse, detail, 1D, .023F);
        ring(glow, position, .57, .61, -spin * 0.18D, 1F, .84F, .24F, .44F * pulse, detail, 1D, .025F);
        ring(glow, position, .70, .74, spin * 0.14D, 1F, .96F, .70F, .42F * pulse, detail, 1D, .028F);
        ring(glow, position, .82, .86, -spin * 0.12D, 1F, .88F, .30F, .40F * pulse, detail, 1D, .031F);

        // Outer anime gate crown.
        ring(glow, position, .94, 1.02, -spin * .18D, 1F, .99F, .84F, .74F * pulse, detail, 1D, .038F);
        ring(glow, position, 1.03, 1.12, spin * .44D, 1F, .91F, .48F, .98F, detail, 1D, .046F);
        ring(glow, position, 1.16, 1.25, -spin * .60D, 1F, .82F, .18F, .92F, detail, .82D, .055F);
        ring(glow, position, 1.30, 1.40, spin * .30D, 1F, .90F, .36F, .78F * pulse, detailLow, .24D, .064F);
        ring(glow, position, 1.44, 1.48, -spin * .40D, 1F, .99F, .72F, .46F * pulse, detailLow, .32D, .070F);

        matrices.pop();
    }

    private static void mirroredDisc(VertexConsumer out, Matrix4f position, Matrix3f normal,
                                     double radius, float z, int red, int green, int blue,
                                     int alpha, int segments) {
        // Incremental circle rotation: two trig calls per disc instead of four per segment.
        double step = Math.PI * 2D / segments;
        double cosStep = Math.cos(step), sinStep = Math.sin(step);
        double ax = 1D, ay = 0D;
        for (int i = 0; i < segments; i++) {
            double bx = ax * cosStep - ay * sinStep;
            double by = ay * cosStep + ax * sinStep;
            float fax=(float)(ax*radius), fay=(float)(ay*radius);
            float fbx=(float)(bx*radius), fby=(float)(by*radius);
            discTriangle(out, position, normal, z, fax, fay, fbx, fby, red, green, blue, alpha, 1F);
            discTriangle(out, position, normal, -z, fbx, fby, fax, fay, red, green, blue, alpha, -1F);
            ax=bx; ay=by;
        }
    }

    private static void discTriangle(VertexConsumer out, Matrix4f position, Matrix3f normal,
                                     float z, float ax, float ay, float bx, float by,
                                     int red, int green, int blue, int alpha, float normalZ) {
        solidVertex(out, position, normal, 0F, 0F, z, .5F, .5F, red, green, blue, alpha, normalZ);
        solidVertex(out, position, normal, ax, ay, z, .5F + ax / 2.4F, .5F + ay / 2.4F, red, green, blue, alpha, normalZ);
        solidVertex(out, position, normal, bx, by, z, .5F + bx / 2.4F, .5F + by / 2.4F, red, green, blue, alpha, normalZ);
        solidVertex(out, position, normal, 0F, 0F, z, .5F, .5F, red, green, blue, alpha, normalZ);
    }

    private static void centerDisc(VertexConsumer out, Matrix4f matrix, double radius,
                                   float red, float green, float blue, float alpha,
                                   int segments, float depth) {
        double step=Math.PI*2D/segments;
        double cosStep=Math.cos(step), sinStep=Math.sin(step);
        double ax=1D, ay=0D;
        for(int i=0;i<segments;i++){
            double bx=ax*cosStep-ay*sinStep;
            double by=ay*cosStep+ax*sinStep;
            glowQuadXY(out,matrix,
                    0D,0D, ax*radius,ay*radius, bx*radius,by*radius, 0D,0D,
                    red,green,blue,alpha*.92F,depth);
            ax=bx; ay=by;
        }
    }

    private static void rippleBands(VertexConsumer out, Matrix4f matrix, double spin, float alpha, int bands) {
        for (int i = 0; i < bands; i++) {
            double inner = 0.09D + i * 0.11D;
            double outer = inner + 0.045D;
            double offset = (i % 2 == 0 ? 1D : -1D) * spin * (0.35D + i * 0.09D);
            float bandAlpha = alpha * (1F - i / (float) (bands + 1));
            float g = i % 2 == 0 ? .92F : .82F;
            float b = i % 2 == 0 ? .44F : .20F;
            ring(out, matrix, inner, outer, offset, 1F, g, b, bandAlpha, 36 + i * 4, 1D, .012F + i * .002F);
        }
    }

    private static void spokes(VertexConsumer out, Matrix4f matrix, double spin, int count,
                               double startRadius, double endRadius,
                               float red, float green, float blue, float alpha, float depth) {
        double width = Math.PI / count * 0.18D;
        for (int i = 0; i < count; i++) {
            double base = spin + i * (Math.PI * 2D / count);
            glowQuad(out, matrix,
                    base - width, startRadius,
                    base - width * 0.55D, endRadius,
                    base + width * 0.55D, endRadius,
                    base + width, startRadius,
                    red, green, blue, alpha, depth);
        }
    }

    private static void solidVertex(VertexConsumer out, Matrix4f position, Matrix3f normal,
                                    float x, float y, float z, float u, float v,
                                    int red, int green, int blue, int alpha, float normalZ) {
        out.vertex(position, x, y, z)
                .color(red, green, blue, alpha)
                .texture(u, v)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(FULL_BRIGHT)
                .normal(normal, 0F, 0F, normalZ)
                .next();
    }

    private static void ring(VertexConsumer out, Matrix4f matrix,
                             double inner, double outer, double spin,
                             float red, float green, float blue, float alpha,
                             int segments, double fill, float depth) {
        // The ring is the hottest portal-render path. Rotate unit vectors incrementally
        // instead of evaluating sin/cos for every segment/vertex.
        double step=Math.PI*2D/segments;
        double fillStep=step*fill;
        double cosStep=Math.cos(step), sinStep=Math.sin(step);
        double ax=Math.cos(spin), ay=Math.sin(spin);
        double zx=Math.cos(spin+fillStep), zy=Math.sin(spin+fillStep);
        for(int i=0;i<segments;i++){
            glowQuadXY(out,matrix,
                    ax*inner,ay*inner,
                    ax*outer,ay*outer,
                    zx*outer,zy*outer,
                    zx*inner,zy*inner,
                    red,green,blue,alpha,depth);
            double nextAx=ax*cosStep-ay*sinStep;
            double nextAy=ay*cosStep+ax*sinStep;
            double nextZx=zx*cosStep-zy*sinStep;
            double nextZy=zy*cosStep+zx*sinStep;
            ax=nextAx; ay=nextAy; zx=nextZx; zy=nextZy;
        }
    }

    private static void spiral(VertexConsumer out, Matrix4f matrix, double spin, float alpha, int steps) {
        double angleStep=Math.PI*2.5D/steps;
        double cosStep=Math.cos(angleStep), sinStep=Math.sin(angleStep);
        for(int arm=0;arm<4;arm++){
            double angle=spin+arm*Math.PI*.5D;
            double ax=Math.cos(angle), ay=Math.sin(angle);
            for(int i=0;i<steps;i++){
                double bx=ax*cosStep-ay*sinStep;
                double by=ay*cosStep+ax*sinStep;
                double t=i/(double)steps;
                double next=(i+1)/(double)steps;
                double inner=.08D+.82D*t;
                double outer=.08D+.82D*next;
                glowQuadXY(out,matrix,
                        ax*inner,ay*inner,
                        ax*(inner+.052D),ay*(inner+.052D),
                        bx*(outer+.052D),by*(outer+.052D),
                        bx*outer,by*outer,
                        1F,.92F,.42F,alpha*(float)(1D-t*.34D),.050F);
                ax=bx; ay=by;
            }
        }
    }

    private static void glowQuad(VertexConsumer out, Matrix4f matrix,
                                 double a, double ra, double b, double rb,
                                 double c, double rc, double d, double rd,
                                 float red, float green, float blue, float alpha, float depth) {
        glowQuadXY(out,matrix,
                Math.cos(a)*ra,Math.sin(a)*ra,
                Math.cos(b)*rb,Math.sin(b)*rb,
                Math.cos(c)*rc,Math.sin(c)*rc,
                Math.cos(d)*rd,Math.sin(d)*rd,
                red,green,blue,alpha,depth);
    }

    private static void glowQuadXY(VertexConsumer out, Matrix4f matrix,
                                   double ax,double ay,double bx,double by,
                                   double cx,double cy,double dx,double dy,
                                   float red,float green,float blue,float alpha,float depth) {
        glowVertexXY(out,matrix,ax,ay, depth,red,green,blue,alpha);
        glowVertexXY(out,matrix,bx,by, depth,red,green,blue,alpha);
        glowVertexXY(out,matrix,cx,cy, depth,red,green,blue,alpha);
        glowVertexXY(out,matrix,dx,dy, depth,red,green,blue,alpha);
        glowVertexXY(out,matrix,dx,dy,-depth,red,green,blue,alpha);
        glowVertexXY(out,matrix,cx,cy,-depth,red,green,blue,alpha);
        glowVertexXY(out,matrix,bx,by,-depth,red,green,blue,alpha);
        glowVertexXY(out,matrix,ax,ay,-depth,red,green,blue,alpha);
    }

    private static void glowVertexXY(VertexConsumer out, Matrix4f matrix,
                                     double x, double y, float z,
                                     float red, float green, float blue, float alpha) {
        out.vertex(matrix,(float)x,(float)y,z).color(red,green,blue,alpha).next();
    }
}
