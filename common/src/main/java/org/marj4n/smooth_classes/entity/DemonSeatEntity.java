package org.marj4n.smooth_classes.entity;

import java.util.UUID;
import net.minecraft.entity.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import org.marj4n.smooth_classes.origin.demon.*;

/** Transient mount only; it never stores inventory or progresses an Origin. */
public final class DemonSeatEntity extends Entity {
    private UUID owner;
    private BlockPos origin;
    private int rotation;
    private Vec3d standing;
    public DemonSeatEntity(EntityType<? extends DemonSeatEntity> type,World world){super(type,world);setNoGravity(true);noClip=true;}
    public void configure(UUID owner,BlockPos origin,int rotation,Vec3d standing){
        this.owner=owner;this.origin=origin;this.rotation=rotation;this.standing=standing;
        // PlayerEntity adds -0.35 to the mount's +0.35. Keep logical feet above
        // the foundation, including a save/reconnect before a mount tick.
        setPosition(standing.x,standing.y,standing.z);setYaw(180+rotation*90);
    }
    @Override protected void initDataTracker(){}
    @Override protected void readCustomDataFromNbt(NbtCompound n){/* Transient seat is discarded on load. */}
    @Override protected void writeCustomDataToNbt(NbtCompound n){}
    @Override public boolean shouldSave(){return false;}
    @Override public double getMountedHeightOffset(){return .35;}
    @Override public boolean isAttackable(){return false;}
    @Override protected boolean canAddPassenger(Entity passenger){return owner!=null&&owner.equals(passenger.getUuid())&&getPassengerList().isEmpty();}
    @Override public Vec3d updatePassengerForDismount(LivingEntity passenger){return standing==null?getPos():standing;}
    @Override public void tick(){
        super.tick();
        if(!getWorld().isClient&&(owner==null||!hasPassengers()||age%20==0&&(!(getWorld() instanceof ServerWorld w)||origin==null||!DemonThrone.validate(w,origin,rotation))))discard();
    }
}
