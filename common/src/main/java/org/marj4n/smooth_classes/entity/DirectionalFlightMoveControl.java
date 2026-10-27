package org.marj4n.smooth_classes.entity;

import net.minecraft.entity.ai.control.MoveControl;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/** Smooth Classes's summon flight controller. */
public final class DirectionalFlightMoveControl extends MoveControl {
    private final int maxPitchChange;
    private final boolean noGravity;
    private final MobEntity entity;
    private static final double MIN_ALTITUDE = 1.5;
    private long lastAttackTime;
    private static final long ATTACK_COOLDOWN = 5000L;
    private double yOffset;

    public DirectionalFlightMoveControl(MobEntity entity, int maxPitchChange, boolean noGravity) {
        super(entity); this.entity=entity; this.maxPitchChange=maxPitchChange; this.noGravity=noGravity;
    }
    @Override public void tick() {
        if(noGravity) entity.setNoGravity(true);
        yOffset=entity.getTarget()!=null?0:1.5;
        if(state==State.MOVE_TO){
            state=State.WAIT;
            if(entity.getTarget()==null){
                targetX=entity.getX()+entity.getRandom().nextGaussian()*5;
                if (entity instanceof AvengerMinionEntity minion && minion.getOwner()!=null)
                    targetY=minion.getOwner().getY();
                else targetY=entity.getY();
                targetZ=entity.getZ()+entity.getRandom().nextGaussian()*5;
            } else { targetX=entity.getTarget().getX(); targetY=entity.getTarget().getY(); targetZ=entity.getTarget().getZ(); }
            targetY += Math.sin(entity.age*0.3)*0.5;
            double d=targetX-entity.getX(), e=(targetY+yOffset)-entity.getY(), f=targetZ-entity.getZ();
            double g=d*d+e*e+f*f;
            if(g<2.500000277905201E-7){entity.setUpwardSpeed(0);entity.setForwardSpeed(0);return;}
            float h=(float)(MathHelper.atan2(f,d)*(180/Math.PI))-90F;
            entity.setYaw(wrapDegrees(entity.getYaw(),h,90F));
            float speedValue=(float)(speed*entity.getAttributeValue(entity.isOnGround()?EntityAttributes.GENERIC_MOVEMENT_SPEED:EntityAttributes.GENERIC_FLYING_SPEED));
            entity.setMovementSpeed(speedValue);
            double horizontal=Math.sqrt(d*d+f*f);
            boolean recentlyAttacked=entity.getTarget()!=null && (System.currentTimeMillis()-lastAttackTime)>ATTACK_COOLDOWN;
            double ground=groundDistance();
            if(ground<MIN_ALTITUDE&&!recentlyAttacked){targetY=entity.getY()+(MIN_ALTITUDE-ground);e=targetY-entity.getY();}
            if(entity instanceof AvengerMinionEntity minion && minion.getOwner()!=null
                    && entity.getTarget()==null && entity.getY()>minion.getOwner().getY()+2.5) {
                e=minion.getOwner().getY()+1.5-entity.getY();
            }
            if(Math.abs(e)>1.0E-5||Math.abs(horizontal)>1.0E-5){
                float pitch=(float)(-(MathHelper.atan2(e,horizontal)*(180/Math.PI)));
                entity.setPitch(wrapDegrees(entity.getPitch(),pitch,maxPitchChange));
                entity.setUpwardSpeed(e>0?speedValue:-speedValue);
            }
        } else {
            entity.setUpwardSpeed(0F); Vec3d v=entity.getVelocity();
            entity.setPitch((float)(-(MathHelper.atan2(v.y,Math.sqrt(v.x*v.x+v.z*v.z))*(180/Math.PI))));
            entity.setForwardSpeed(.1F); entity.setYaw((float)(MathHelper.atan2(v.z,v.x)*(180/Math.PI))-90F);
        }
    }
    private double groundDistance(){ BlockPos p=entity.getBlockPos(); while(p.getY()>0&&!entity.getWorld().getBlockState(p).isSolidBlock(entity.getWorld(),p))p=p.down(); return entity.getY()-p.getY(); }
    public void onAttack(){lastAttackTime=System.currentTimeMillis();}
}
