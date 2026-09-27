package org.marj4n.smooth_classes.entity;

import java.util.UUID;
import net.minecraft.entity.*;
import net.minecraft.entity.data.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.world.World;
import org.marj4n.smooth_classes.runtime.WhenOnHighRuntime;

/** Visual beam anchor; only the server channel runtime applies damage. */
public final class HighBeamEntity extends Entity {
    private static final TrackedData<Integer> WIDTH=DataTracker.registerData(HighBeamEntity.class,TrackedDataHandlerRegistry.INTEGER);
    public UUID owner;
    public HighBeamEntity(EntityType<?> type,World world){super(type,world);setNoGravity(true);}
    public int beamWidth(){return dataTracker.get(WIDTH);}
    public void setBeamWidth(int value){dataTracker.set(WIDTH,value);}
    @Override protected void initDataTracker(){dataTracker.startTracking(WIDTH,1);}
    @Override public void tick(){
        super.tick();var world=getWorld();
        if(!world.isClient){if(!WhenOnHighRuntime.ownsBeam(this))discard();return;}
        double half=beamWidth()/2D;
        // Dense downward shower and a fresh impact ring every damage pulse.
        if(age%4==0)for(int i=0;i<36;i++){
            double angle=i*Math.PI*2/36;
            world.addParticle(ParticleTypes.END_ROD,getX()+Math.cos(angle)*half,getY()+.2,getZ()+Math.sin(angle)*half,Math.cos(angle)*.35,.18,Math.sin(angle)*.35);
        }
        for(int i=0;i<36;i++){
            double x=getX()+(random.nextDouble()-.5)*beamWidth(),z=getZ()+(random.nextDouble()-.5)*beamWidth();
            world.addParticle(ParticleTypes.END_ROD,x,getY()+random.nextDouble()*20,z,0,-.6-random.nextDouble(),0);
        }
        for(int i=0;i<12;i++){
            double angle=random.nextDouble()*Math.PI*2,r=half+random.nextDouble()*.8;
            world.addParticle(ParticleTypes.ELECTRIC_SPARK,getX()+Math.cos(angle)*r,getY()+.15,getZ()+Math.sin(angle)*r,Math.cos(angle)*.15,.15,Math.sin(angle)*.15);
        }
        if(age%4==0)world.addParticle(ParticleTypes.FLASH,getX(),getY()+.3,getZ(),0,0,0);
    }
    @Override protected void writeCustomDataToNbt(NbtCompound n){if(owner!=null)n.putUuid("Owner",owner);n.putInt("Width",beamWidth());}
    @Override protected void readCustomDataFromNbt(NbtCompound n){owner=n.containsUuid("Owner")?n.getUuid("Owner"):null;setBeamWidth(n.getInt("Width")==3?3:1);}
}
