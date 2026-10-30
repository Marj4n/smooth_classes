package org.marj4n.smooth_classes.mixin;

import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import org.marj4n.smooth_classes.content.avenger.runtime.AvengerLibrarianSummons;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Save the original ritual book independently of mutable trade offers. */
@Mixin(VillagerEntity.class)
public abstract class AvengerLibrarianMixin implements AvengerLibrarianSummons.BookCarrier {
    @Unique private ItemStack smoothClasses$librarianBook = ItemStack.EMPTY;

    @Override public ItemStack smoothClasses$getLibrarianBook() {
        return smoothClasses$librarianBook;
    }

    @Override public void smoothClasses$setLibrarianBook(ItemStack book) {
        smoothClasses$librarianBook = AvengerLibrarianSummons.copyBook(book);
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    private void smoothClasses$saveLibrarianBook(NbtCompound nbt, CallbackInfo ci) {
        AvengerLibrarianSummons.writeBook(nbt, smoothClasses$librarianBook);
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void smoothClasses$loadLibrarianBook(NbtCompound nbt, CallbackInfo ci) {
        smoothClasses$librarianBook = AvengerLibrarianSummons.readBook(nbt);
    }
}
