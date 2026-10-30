package org.marj4n.smooth_classes.mixin;
import net.minecraft.block.entity.JukeboxBlockEntity;
import net.minecraft.world.World;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.marj4n.smooth_classes.content.avenger.dance.AvengerDanceRuntime;
@Mixin(JukeboxBlockEntity.class)
public abstract class AvengerJukeboxMixin {
    @Inject(method="tick(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Lnet/minecraft/block/entity/JukeboxBlockEntity;)V",at=@At("TAIL"))
    private static void danceTick(World w,BlockPos p,BlockState s,JukeboxBlockEntity be,CallbackInfo ci){AvengerDanceRuntime.observe(be);}
    @Inject(method="stopPlaying",at=@At("TAIL"))
    private void danceStop(CallbackInfo ci){AvengerDanceRuntime.observe((JukeboxBlockEntity)(Object)this);}
    @Inject(method="startPlaying",at=@At("TAIL"))
    private void danceStart(CallbackInfo ci){AvengerDanceRuntime.observe((JukeboxBlockEntity)(Object)this);}
}
