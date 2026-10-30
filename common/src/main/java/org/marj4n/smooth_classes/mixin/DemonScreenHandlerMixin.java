package org.marj4n.smooth_classes.mixin;
import java.util.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.*;
import net.minecraft.screen.slot.SlotActionType;
import org.marj4n.smooth_classes.origin.demon.DemonTrials;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ScreenHandler.class)
public abstract class DemonScreenHandlerMixin {
    @Unique private List<DemonTrials.Receipt> demon$receipts=List.of();
    @Unique private int demon$playerItems;
    @Inject(method="onSlotClick",at=@At("HEAD"))
    private void demon$before(int index,int button,SlotActionType type,PlayerEntity player,CallbackInfo ci){
        var handler=(ScreenHandler)(Object)this;demon$receipts=DemonTrials.before(handler,player);
        if(!demon$receipts.isEmpty())demon$playerItems=DemonTrials.count(player.getInventory())+handler.getCursorStack().getCount();
    }
    @Inject(method="onSlotClick",at=@At("TAIL"))
    private void demon$after(int index,int button,SlotActionType type,PlayerEntity player,CallbackInfo ci){
        if(!demon$receipts.isEmpty())DemonTrials.after(demon$receipts,(ScreenHandler)(Object)this,player,demon$playerItems);demon$receipts=List.of();
    }
}
