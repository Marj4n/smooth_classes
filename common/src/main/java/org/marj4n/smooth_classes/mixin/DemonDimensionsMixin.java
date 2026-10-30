package org.marj4n.smooth_classes.mixin;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;
import org.marj4n.smooth_classes.entity.DemonSeatEntity;
import org.marj4n.smooth_classes.origin.demon.DemonBodyGeometry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(PlayerEntity.class)
public abstract class DemonDimensionsMixin {
    @Inject(method="getDimensions",at=@At("HEAD"),cancellable=true)
    private void demon$dimensions(EntityPose pose,CallbackInfoReturnable<EntityDimensions> cir){PlayerEntity p=(PlayerEntity)(Object)this;if(DemonBodyGeometry.applies(p))cir.setReturnValue(DemonBodyGeometry.dimensions(p,pose));}
    @Inject(method="getActiveEyeHeight",at=@At("HEAD"),cancellable=true)
    private void demon$eye(EntityPose pose,EntityDimensions dimensions,CallbackInfoReturnable<Float> cir){PlayerEntity p=(PlayerEntity)(Object)this;if(DemonBodyGeometry.applies(p))cir.setReturnValue(DemonBodyGeometry.eyeHeight(pose,p.getVehicle() instanceof DemonSeatEntity));}
}
