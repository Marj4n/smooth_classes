package org.marj4n.smooth_classes.mixin;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.marj4n.smooth_classes.origin.demon.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(PlayerEntity.class)
public abstract class DemonPlayerMixin {
    @ModifyVariable(method="addExhaustion",at=@At("HEAD"),argsOnly=true)
    private float demon$exhaustion(float amount){return DemonDiet.applies((PlayerEntity)(Object)this)?(float)(amount*DemonConfig.INSTANCE.exhaustionMultiplier):amount;}
    @Inject(method="eatFood",at=@At("HEAD"),cancellable=true)
    private void demon$food(World world,ItemStack stack,CallbackInfoReturnable<ItemStack> cir){if(DemonDiet.reject((PlayerEntity)(Object)this,stack))cir.setReturnValue(stack);}
}
