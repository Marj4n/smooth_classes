package org.marj4n.smooth_classes.mixin;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.util.*;
import net.minecraft.world.World;
import org.marj4n.smooth_classes.origin.demon.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(Item.class)
public abstract class DemonItemMixin {
    @Inject(method="use",at=@At("HEAD"),cancellable=true)
    private void demon$use(World world,PlayerEntity p,Hand hand,CallbackInfoReturnable<TypedActionResult<ItemStack>> cir){
        ItemStack stack=p.getStackInHand(hand);
        if(DemonDiet.reject(p,stack)){cir.setReturnValue(TypedActionResult.fail(stack));return;}
        if(DemonDiet.applies(p)&&DemonDiet.plant(stack.getItem())>0){
            if(p.canConsume(false)){p.setCurrentHand(hand);cir.setReturnValue(TypedActionResult.consume(stack));}
            else cir.setReturnValue(TypedActionResult.fail(stack));
        }
    }
    @Inject(method="getMaxUseTime",at=@At("HEAD"),cancellable=true)
    private void demon$duration(ItemStack stack,CallbackInfoReturnable<Integer> cir){if(DemonDiet.plant(stack.getItem())>0)cir.setReturnValue(32);}
    @Inject(method="getUseAction",at=@At("HEAD"),cancellable=true)
    private void demon$animation(ItemStack stack,CallbackInfoReturnable<UseAction> cir){if(DemonDiet.plant(stack.getItem())>0)cir.setReturnValue(UseAction.EAT);}
    @Inject(method="finishUsing",at=@At("HEAD"),cancellable=true)
    private void demon$finish(ItemStack stack,World world,LivingEntity user,CallbackInfoReturnable<ItemStack> cir){if(user instanceof PlayerEntity p&&DemonDiet.applies(p)&&DemonDiet.plant(stack.getItem())>0)cir.setReturnValue(DemonDiet.consume(world,p,stack));}
}
