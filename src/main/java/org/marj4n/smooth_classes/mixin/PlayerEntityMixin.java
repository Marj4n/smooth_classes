package org.marj4n.smooth_classes.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.marj4n.smooth_classes.runtime.CombatEventRuntime;
import org.marj4n.smooth_classes.runtime.base.BasePathRuntime;
import org.marj4n.smooth_classes.runtime.classpass.ClassPassiveRuntime;
import org.marj4n.smooth_classes.runtime.AscendancyRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {
    @Inject(method="onKilledOther", at=@At("HEAD"))
    private void smooth_classes$killedOther(ServerWorld world, LivingEntity other, CallbackInfoReturnable<Boolean> cir) {
        if ((Object)this instanceof ServerPlayerEntity player) {
            CombatEventRuntime.onKilledOther(player, world, other);
        }
    }

    @Inject(method="takeShieldHit", at=@At("HEAD"))
    private void smooth_classes$takeShieldHit(LivingEntity attacker, CallbackInfo ci) {
        if ((Object)this instanceof ServerPlayerEntity player) {
            BasePathRuntime.onShieldHit(player, attacker);
            ClassPassiveRuntime.onShieldHit(player, attacker);
            AscendancyRuntime.shieldHit(player);
        }
    }
    @Inject(method="dropItem(Lnet/minecraft/item/ItemStack;ZZ)Lnet/minecraft/entity/ItemEntity;", at=@At("HEAD"), cancellable=true)
    private void smooth_classes$keepDeathList(ItemStack stack, boolean throwRandomly, boolean retainOwnership,
                                               CallbackInfoReturnable<ItemEntity> cir) {
        if (org.marj4n.smooth_classes.content.avenger.runtime.AvengerReworkRuntime.isDeathList(stack)) {
            cir.setReturnValue(null);
        }
    }

    @Inject(method="dropInventory", at=@At("HEAD"))
    private void smooth_classes$keepDeathListOnDeath(CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        for (int slot = 0; slot < player.getInventory().size(); slot++) {
            ItemStack stack = player.getInventory().getStack(slot);
            if (org.marj4n.smooth_classes.content.avenger.runtime.AvengerReworkRuntime.isDeathList(stack)) {
                // The permanent ledger lives in PersistentState. Removing only the physical
                // book here prevents a duplicate item entity; it is recreated after respawn.
                player.getInventory().setStack(slot, ItemStack.EMPTY);
            }
        }
    }

}
