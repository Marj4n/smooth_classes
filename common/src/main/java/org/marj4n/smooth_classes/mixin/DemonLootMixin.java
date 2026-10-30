package org.marj4n.smooth_classes.mixin;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import org.marj4n.smooth_classes.origin.demon.DemonTrials;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(LootableContainerBlockEntity.class)
public abstract class DemonLootMixin {
    @Shadow protected Identifier lootTableId;
    @Inject(method="checkLootInteraction",at=@At("HEAD"))
    private void demon$loot(PlayerEntity player,CallbackInfo ci){if(lootTableId!=null&&player!=null)DemonTrials.provenance((LootableContainerBlockEntity)(Object)this,lootTableId);}
}
