package org.marj4n.smooth_classes.client.renderer;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.*;
import net.minecraft.world.Heightmap;
import org.marj4n.smooth_classes.entity.BloodRainEntity;

public final class BloodRainRenderer extends EntityRenderer<BloodRainEntity> {
    private static final Identifier RAIN=new Identifier("minecraft","textures/environment/rain.png");
    private static final Identifier CLOUD=new Identifier("minecraft","textures/environment/clouds.png");
    private long lastSoundTick=Long.MIN_VALUE;
    public BloodRainRenderer(EntityRendererFactory.Context ctx){super(ctx);}
    @Override public boolean shouldRender(BloodRainEntity e,Frustum f,double x,double y,double z){return true;}
    @Override public Identifier getTexture(BloodRainEntity e){return RAIN;}
    @Override public void render(BloodRainEntity e,float yaw,float delta,MatrixStack matrices,VertexConsumerProvider provider,int light){
        var client=MinecraftClient.getInstance();var camera=client.gameRenderer.getCamera().getPos();var world=e.getWorld();
        var entry=matrices.peek();var rain=provider.getBuffer(RenderLayer.getEntityTranslucent(RAIN));
        float time=e.age+delta;
        if(client.player!=null && e.inside(camera.x-e.getX(),camera.z-e.getZ())
                && (lastSoundTick==Long.MIN_VALUE||world.getTime()-lastSoundTick>=40)){
            lastSoundTick=world.getTime();
            world.playSound(camera.x,camera.y,camera.z,net.minecraft.sound.SoundEvents.WEATHER_RAIN,
                    net.minecraft.sound.SoundCategory.WEATHER,BloodRainEntity.exposed(client.player)?.45F:.12F,1F,false);
        }
        int radius=client.options.getViewDistance().getValue()>8?16:10;
        int cx=MathHelper.floor(camera.x),cz=MathHelper.floor(camera.z);
        for(int x=cx-radius;x<=cx+radius;x++)for(int z=cz-radius;z<=cz+radius;z++){
            if(!e.inside(x+.5-e.getX(),z+.5-e.getZ()))continue;
            int floor=world.getTopY(Heightmap.Type.MOTION_BLOCKING,x,z);
            float bottom=(float)(Math.max(floor,camera.y-12)-e.getY()),top=(float)(Math.max(floor,camera.y+18)-e.getY());
            if(top<=bottom)continue;
            double dx=x+.5-camera.x,dz=z+.5-camera.z,len=Math.sqrt(dx*dx+dz*dz);
            float sx=(float)(len<.01?.5:-dz/len*.5),sz=(float)(len<.01?0:dx/len*.5);
            float px=(float)(x+.5-e.getX()),pz=(float)(z+.5-e.getZ());
            float v=-time*.13F+((x*31+z*17)&31)*.07F;
            int alpha=(int)(180*Math.max(0,1-len/(radius+1)));
            vertex(rain,entry,px-sx,top,pz-sz,0,v,180,8,22,alpha);
            vertex(rain,entry,px+sx,top,pz+sz,1,v,180,8,22,alpha);
            vertex(rain,entry,px+sx,bottom,pz+sz,1,v+(top-bottom)/4,180,8,22,alpha);
            vertex(rain,entry,px-sx,bottom,pz-sz,0,v+(top-bottom)/4,180,8,22,alpha);
        }
        // Local cloud disc uses the storm's synced radius: 64 normally, 96 at 60+ Ascendancy.
        var cloud=provider.getBuffer(RenderLayer.getEntityTranslucent(CLOUD));
        float cloudY=48;
        int cloudRadius=(int)(Math.ceil(e.radius()/8D)*8D);
        float cloudSize=cloudRadius*2F;
        float du=8F/cloudSize;
        for(int x=-cloudRadius;x<cloudRadius;x+=8)for(int z=-cloudRadius;z<cloudRadius;z+=8){
            if(!e.inside(x+4,z+4))continue;
            float u=(x+cloudRadius)/cloudSize,v=(z+cloudRadius)/cloudSize;
            vertex(cloud,entry,x,cloudY,z,u,v,38,4,8,230);
            vertex(cloud,entry,x+8,cloudY,z,u+du,v,38,4,8,230);
            vertex(cloud,entry,x+8,cloudY,z+8,u+du,v+du,38,4,8,230);
            vertex(cloud,entry,x,cloudY,z+8,u,v+du,38,4,8,230);
        }
    }
    private static void vertex(VertexConsumer b,MatrixStack.Entry m,float x,float y,float z,float u,float v,int r,int g,int blue,int a){
        b.vertex(m.getPositionMatrix(),x,y,z).color(r,g,blue,a).texture(u,v).overlay(OverlayTexture.DEFAULT_UV).light(15728880).normal(m.getNormalMatrix(),0,1,0).next();
    }
}
