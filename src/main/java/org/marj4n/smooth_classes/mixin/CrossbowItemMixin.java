package org.marj4n.smooth_classes.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.marj4n.smooth_classes.runtime.base.BasePathRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CrossbowItem.class)
public abstract class CrossbowItemMixin {
    @Shadow private boolean charged;

    @Inject(method="use", at=@At("HEAD"))
    private void smooth_classes$use(World world, net.minecraft.entity.player.PlayerEntity user, Hand hand,
                                    CallbackInfoReturnable<TypedActionResult<ItemStack>> cir) {
        if(!world.isClient && user instanceof ServerPlayerEntity player) BasePathRuntime.onCrossbowUse(player,charged);
    }

    @Inject(method="onStoppedUsing", at=@At("HEAD"))
    private void smooth_classes$release(ItemStack stack, World world, LivingEntity user, int remaining, CallbackInfo ci) {
        if(!world.isClient && user instanceof ServerPlayerEntity player && remaining<3) BasePathRuntime.onBowRelease(player,false);
    }
}
