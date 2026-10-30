package org.marj4n.smooth_classes.mixin;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.marj4n.smooth_classes.origin.demon.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(World.class)
public abstract class DemonWorldMixin {
    @ModifyVariable(method="setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;II)Z",at=@At("HEAD"),argsOnly=true)
    private BlockState demon$evaporate(BlockState next,BlockPos pos,BlockState original,int flags,int depth){
        if(!DemonRuntime.running||!((Object)this instanceof ServerWorld w))return next;
        DemonTrials.blockChanged(w,pos,next);
        return DemonDomain.vaporizeNewWater(w,pos,next)?DemonDomain.dry(next):next;
    }
}
