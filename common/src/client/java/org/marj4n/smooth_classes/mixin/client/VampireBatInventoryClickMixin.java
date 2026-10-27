package org.marj4n.smooth_classes.mixin.client;

import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;
import org.marj4n.smooth_classes.client.origin.OriginClientState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Do not predict slot changes that the Bat packet gate will reject. */
@Mixin(ClientPlayerInteractionManager.class)
public abstract class VampireBatInventoryClickMixin {
    private static boolean locked() {
        return "vampire".equals(OriginClientState.originId)
                && OriginClientState.hasFlag("vampire.form.bat");
    }

    @Inject(method = "clickSlot", at = @At("HEAD"), cancellable = true)
    private void smooth_classes$noBatSlotClick(int syncId, int slotId, int button, SlotActionType actionType,
                                                PlayerEntity player, CallbackInfo ci) {
        if (locked()) ci.cancel();
    }

    @Inject(method = "clickCreativeStack", at = @At("HEAD"), cancellable = true)
    private void smooth_classes$noBatCreativeItem(ItemStack stack, int slotId, CallbackInfo ci) {
        if (locked()) ci.cancel();
    }
}
