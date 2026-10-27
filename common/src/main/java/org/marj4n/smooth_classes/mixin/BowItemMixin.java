package org.marj4n.smooth_classes.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import org.marj4n.smooth_classes.runtime.base.BasePathRuntime;
import org.marj4n.smooth_classes.content.archer.runtime.ArcherRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BowItem.class)
public abstract class BowItemMixin {
    @Shadow public abstract int getMaxUseTime(ItemStack stack);
    @Invoker("getPullProgress") public static float smooth_classes$getPullProgress(int ticks){throw new AssertionError();}

    @Inject(method="onStoppedUsing", at=@At("HEAD"), cancellable=true)
    private void smooth_classes$release(ItemStack stack, World world, LivingEntity user, int remaining, CallbackInfo ci) {
        if (!world.isClient && user instanceof ServerPlayerEntity player) {
            int useTicks=getMaxUseTime(stack)-remaining;
            String bowName=stack.getName().getString().toLowerCase(java.util.Locale.ROOT);
            float required=(bowName.contains("shortbow")||bowName.contains("love"))?0.5F:1.0F;
            boolean full=smooth_classes$getPullProgress(useTicks)>=required;
            BasePathRuntime.onBowRelease(player,full);
            if (full && (ArcherRuntime.fireElementalArrows(player)
                    || ArcherRuntime.fireArrowRain(player)
                    || ArcherRuntime.fireMarksman(player))) ci.cancel();
        }
    }
}
