package org.marj4n.smooth_classes.content.avenger.runtime;

import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.text.Text;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AvengerLibrarianSummonsTest {
    @BeforeAll static void bootstrap() {
        SharedConstants.createGameVersion();
        Bootstrap.initialize();
    }

    private static ItemStack book() {
        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
        NbtList enchantments = new NbtList();
        for (String id : new String[]{"minecraft:mending", "example_mod:arcane_edge"}) {
            NbtCompound entry = new NbtCompound();
            entry.putString("id", id);
            entry.putShort("lvl", (short) (id.startsWith("minecraft:") ? 1 : 7));
            enchantments.add(entry);
        }
        book.getOrCreateNbt().put("StoredEnchantments", enchantments);
        book.getOrCreateNbt().putString("ExampleModData", "preserve me");
        book.setCustomName(Text.literal("Bound Archive"));
        return book;
    }

    @Test void fusionRequiresEmeraldMainHandAndEnchantedBookOffhand() {
        assertTrue(AvengerLibrarianSummons.isRecipe(new ItemStack(Items.EMERALD), book()));
        assertFalse(AvengerLibrarianSummons.isRecipe(book(), new ItemStack(Items.EMERALD)));
        assertFalse(AvengerLibrarianSummons.isRecipe(new ItemStack(Items.DIAMOND), book()));
        assertFalse(AvengerLibrarianSummons.isRecipe(new ItemStack(Items.EMERALD), new ItemStack(Items.BOOK)));
    }

    @Test void bookFusionUsesTheExistingVillagerSoulAndConsumesOffhand() {
        var recipes = AvengerSummonRecipes.matching(new ItemStack(Items.EMERALD), book());
        assertEquals(1, recipes.size());
        assertEquals("minecraft:villager", recipes.get(0).entityId());
        assertEquals("minecraft:enchanted_book", recipes.get(0).offhandItemId());
        assertNull(AvengerSummonRecipes.recipe("minecraft:villager").offhandItemId());
    }

    @Test void unrelatedOffhandAndExistingEmeraldFusionRecipesRemainUnchanged() {
        assertNull(AvengerSummonRecipes.matching(new ItemStack(Items.EMERALD), new ItemStack(Items.SHIELD))
                .get(0).offhandItemId());
        assertEquals("minecraft:wandering_trader", AvengerSummonRecipes.matching(
                new ItemStack(Items.EMERALD), new ItemStack(Items.LEAD)).get(0).entityId());
    }

    @Test void copyRetainsAllStoredEnchantmentsAndCustomNbtWithoutAliasing() {
        ItemStack source = book();
        source.setCount(3);
        ItemStack copy = AvengerLibrarianSummons.copyBook(source);
        assertEquals(1, copy.getCount());
        assertEquals(3, source.getCount());
        assertEquals(source.getNbt(), copy.getNbt());
        assertNotSame(source.getNbt(), copy.getNbt());
        source.getOrCreateNbt().putString("ExampleModData", "changed");
        assertEquals("preserve me", copy.getNbt().getString("ExampleModData"));
    }

    @Test void emptyOrUnenchantedItemsCannotCreateOffers() {
        assertFalse(AvengerLibrarianSummons.hasStoredEnchantments(ItemStack.EMPTY));
        assertFalse(AvengerLibrarianSummons.hasStoredEnchantments(new ItemStack(Items.ENCHANTED_BOOK)));
        assertFalse(AvengerLibrarianSummons.hasStoredEnchantments(new ItemStack(Items.BOOK)));
        assertThrows(IllegalArgumentException.class,
                () -> AvengerLibrarianSummons.offersFor(new ItemStack(Items.ENCHANTED_BOOK)));
    }

    @Test void guaranteedTradeSellsTheExactBookForEmeraldsAndAnOrdinaryBook() {
        ItemStack source = book();
        TradeOfferList offers = AvengerLibrarianSummons.offersFor(source);
        assertEquals(1, offers.size());
        TradeOffer offer = offers.get(0);
        assertTrue(offer.getOriginalFirstBuyItem().isOf(Items.EMERALD));
        assertEquals(16, offer.getOriginalFirstBuyItem().getCount());
        assertTrue(offer.getSecondBuyItem().isOf(Items.BOOK));
        assertEquals(1, offer.getSecondBuyItem().getCount());
        assertEquals(source.getNbt(), offer.getSellItem().getNbt());
        assertEquals(12, offer.getMaxUses());
        offer.getSellItem().getOrCreateNbt().putString("ExampleModData", "offer mutation");
        assertEquals("preserve me", source.getNbt().getString("ExampleModData"));
    }

    @Test void vanillaOfferSaveReloadRetainsModdedIdsAndLevels() {
        TradeOffer offer = AvengerLibrarianSummons.offersFor(book()).get(0);
        offer.use();
        TradeOffer loaded = new TradeOffer(offer.toNbt());
        assertEquals(1, loaded.getUses());
        assertEquals(book().getNbt(), loaded.getSellItem().getNbt());
    }

    @Test void originalRecallIngredientSurvivesNbtRoundTripIndependentOfTradeMutation() {
        ItemStack source = book();
        NbtCompound data = new NbtCompound();
        AvengerLibrarianSummons.writeBook(data, source);
        ItemStack loaded = AvengerLibrarianSummons.readBook(data);
        assertEquals(1, loaded.getCount());
        assertEquals(source.getNbt(), loaded.getNbt());
        loaded.getOrCreateNbt().putString("ExampleModData", "mutated refund");
        assertEquals(source.getNbt(), AvengerLibrarianSummons.readBook(data).getNbt());
    }

    @Test void ordinaryVillagerSaveDataHasNoBoundBook() {
        NbtCompound data = new NbtCompound();
        assertTrue(AvengerLibrarianSummons.readBook(data).isEmpty());
        AvengerLibrarianSummons.writeBook(data, book());
        AvengerLibrarianSummons.writeBook(data, ItemStack.EMPTY);
        assertTrue(data.isEmpty());
    }
}
