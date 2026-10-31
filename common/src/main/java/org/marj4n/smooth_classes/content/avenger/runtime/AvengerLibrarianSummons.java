package org.marj4n.smooth_classes.content.avenger.runtime;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.EnchantedBookItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;
import net.minecraft.village.VillagerProfession;

/** Book-bound Death List villagers; never changes natural villager trade tables. */
public final class AvengerLibrarianSummons {
    private static final String BOOK_KEY = "SmoothClassesAvengerLibrarianBook";
    public static final int EMERALD_PRICE = 16;

    private AvengerLibrarianSummons() {}

    public interface BookCarrier {
        ItemStack smoothClasses$getLibrarianBook();
        void smoothClasses$setLibrarianBook(ItemStack book);
    }

    public static boolean isRecipe(ItemStack main, ItemStack offhand) {
        return main.isOf(Items.EMERALD) && !offhand.isEmpty()
                && offhand.getItem() instanceof EnchantedBookItem;
    }

    public static boolean hasStoredEnchantments(ItemStack book) {
        // Copy raw stored enchantments rather than resolving a vanilla-only list.
        // Mod IDs, multiple enchantments, levels and other book NBT are retained.
        return !book.isEmpty() && book.getItem() instanceof EnchantedBookItem
                && !EnchantedBookItem.getEnchantmentNbt(book).isEmpty();
    }

    public static ItemStack copyBook(ItemStack source) {
        if (!hasStoredEnchantments(source)) return ItemStack.EMPTY;
        ItemStack copy = source.copy();
        copy.setCount(1);
        return copy;
    }

    public static TradeOfferList offersFor(ItemStack book) {
        ItemStack result = copyBook(book);
        if (result.isEmpty()) throw new IllegalArgumentException("An enchanted book with stored enchantments is required");
        TradeOfferList offers = new TradeOfferList();
        offers.add(new TradeOffer(new ItemStack(Items.EMERALD, EMERALD_PRICE),
                new ItemStack(Items.BOOK), result, 12, 1, 0.2F));
        return offers;
    }

    public static void bind(VillagerEntity villager, ItemStack source) {
        ItemStack book = copyBook(source);
        if (book.isEmpty()) throw new IllegalArgumentException("Cannot bind an empty enchanted book");
        villager.setVillagerData(villager.getVillagerData()
                .withProfession(VillagerProfession.LIBRARIAN).withLevel(1));
        // Villagers with trade experience retain their profession without a POI.
        // Normal lectern/work/restock behavior remains vanilla.
        villager.setExperience(1);
        ((BookCarrier) villager).smoothClasses$setLibrarianBook(book);
        restoreBoundOffer(villager);
    }

    /** Repair pre-fix summons while retaining uses/prices of an already correct offer. */
    public static void restoreBoundOffer(VillagerEntity villager) {
        if (!(villager instanceof BookCarrier carrier)) return;
        ItemStack book = copyBook(carrier.smoothClasses$getLibrarianBook());
        if (book.isEmpty()) return; // Never touch ordinary or emerald-only villagers.
        villager.setVillagerData(villager.getVillagerData()
                .withProfession(VillagerProfession.LIBRARIAN));
        if (villager.getExperience() < 1) villager.setExperience(1);
        replaceIncorrectOffer(villager.getOffers(), book);
    }

    public static void replaceIncorrectOffer(TradeOfferList offers, ItemStack book) {
        ItemStack original = copyBook(book);
        if (original.isEmpty()) return;
        if (!offers.isEmpty()) {
            ItemStack sold = offers.get(0).getSellItem();
            if (sold.isOf(original.getItem()) && java.util.Objects.equals(sold.getNbt(), original.getNbt())) return;
        }
        // MerchantEntity.setOffersFromServer is a no-op on Java 1.20.1 server
        // entities. Mutate the actual authoritative list returned by getOffers.
        offers.clear();
        offers.addAll(offersFor(original));
    }

    public static ItemStack boundBook(LivingEntity entity) {
        if (!(entity instanceof VillagerEntity) || !(entity instanceof BookCarrier carrier)
                || !AvengerReworkRuntime.isAvengerSummon(entity)) return ItemStack.EMPTY;
        return copyBook(carrier.smoothClasses$getLibrarianBook());
    }

    public static void writeBook(NbtCompound nbt, ItemStack book) {
        ItemStack copy = copyBook(book);
        if (copy.isEmpty()) nbt.remove(BOOK_KEY);
        else nbt.put(BOOK_KEY, copy.writeNbt(new NbtCompound()));
    }

    public static ItemStack readBook(NbtCompound nbt) {
        if (!nbt.contains(BOOK_KEY, NbtElement.COMPOUND_TYPE)) return ItemStack.EMPTY;
        return copyBook(ItemStack.fromNbt(nbt.getCompound(BOOK_KEY)));
    }
}
