package org.marj4n.smooth_classes.mixin.client;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.world.BlockView;
import org.marj4n.smooth_classes.client.origin.demon.ForbiddenBirthClient;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Camera.class)
public abstract class ForbiddenBirthCameraMixin {
    @Shadow private boolean thirdPerson;
    @Shadow protected abstract void setPos(double x,double y,double z);
    @Shadow protected abstract void setRotation(float yaw,float pitch);
    @Inject(method="update",at=@At("TAIL"))
    private void forbidden$camera(BlockView area,Entity focus,boolean third,boolean inverse,float partial,CallbackInfo ci){var pose=ForbiddenBirthClient.camera(partial);if(pose==null)return;var direction=pose.target().subtract(pose.position());setPos(pose.position().x,pose.position().y,pose.position().z);setRotation((float)(Math.toDegrees(Math.atan2(direction.z,direction.x))-90),(float)-Math.toDegrees(Math.atan2(direction.y,Math.hypot(direction.x,direction.z))));thirdPerson=true;}
}
