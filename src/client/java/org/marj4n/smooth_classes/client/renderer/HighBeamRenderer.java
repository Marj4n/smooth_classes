package org.marj4n.smooth_classes.client.renderer;

import net.minecraft.client.render.*;
import net.minecraft.client.render.block.entity.BeaconBlockEntityRenderer;
import net.minecraft.client.render.entity.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.marj4n.smooth_classes.entity.HighBeamEntity;

public final class HighBeamRenderer extends EntityRenderer<HighBeamEntity> {
    private static final Identifier TEXTURE=new Identifier("minecraft","textures/entity/beacon_beam.png");
    public HighBeamRenderer(EntityRendererFactory.Context context){super(context);}
    @Override public Identifier getTexture(HighBeamEntity e){return TEXTURE;}
    @Override public boolean shouldRender(HighBeamEntity e,Frustum f,double x,double y,double z){return true;}
    @Override public void render(HighBeamEntity e,float yaw,float delta,MatrixStack matrices,VertexConsumerProvider vertices,int light){
        int height=Math.max(128,e.getWorld().getTopY()+64-(int)e.getY());
        int half=e.beamWidth()/2;
        float pulse=1F+.18F*(float)Math.cos((e.age+delta)*Math.PI/2);
        for(int x=-half;x<=half;x++)for(int z=-half;z<=half;z++){
            matrices.push();matrices.translate(x-.5,0,z-.5);
            BeaconBlockEntityRenderer.renderBeam(matrices,vertices,TEXTURE,delta,1F,e.getWorld().getTime(),0,height,new float[]{.65F,.86F,1F},.28F*pulse,.5F*pulse);
            BeaconBlockEntityRenderer.renderBeam(matrices,vertices,TEXTURE,delta,1F,-e.getWorld().getTime(),0,height,new float[]{1F,1F,1F},.21F*pulse,.34F*pulse);
            matrices.pop();
        }
    }
}
