package org.marj4n.smooth_classes.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.BookScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import org.marj4n.smooth_classes.SmoothClasses;
import org.marj4n.smooth_classes.content.avenger.runtime.AvengerSummonRecipes;
import org.marj4n.smooth_classes.network.SmoothClassesNetworking;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Client-side view of the Avenger Death List.
 *
 * <p>The server owns the ledger and sends a fresh snapshot whenever Ctrl+H is
 * pressed. If Patchouli is installed the snapshot is exposed through Patchouli
 * formatting functions and the real {@code smooth_classes:death_list} book is
 * opened. The vanilla written-book screen is kept only as a safe fallback.</p>
 */
public final class DeathListBookClient {
    private static final Identifier BOOK_ID = new Identifier("smooth_classes", "death_list");
    private static final Identifier LEDGER_ENTRY = new Identifier("smooth_classes", "ledger");
    private static final Map<String, Integer> SOULS = new HashMap<>();
    private static final Map<String, Integer> KILLS = new HashMap<>();

    private static int totalSouls;
    private static int discovered;
    private static int recipeCount;
    private static int charges;
    private static int maxCharges;
    private static long rechargeMillis;
    private static boolean patchouliHooksRegistered;

    private DeathListBookClient() {}

    public static void register() {
        registerPatchouliHooks();
        ClientPlayNetworking.registerGlobalReceiver(SmoothClassesNetworking.SYNC_DEATH_LIST,
                (client, handler, buf, responseSender) -> {
                    NbtCompound nbt = buf.readNbt();
                    client.execute(() -> {
                        if (nbt == null) return;
                        syncLedger(nbt);
                        if (!openPatchouliLedger()) openVanillaFallback(nbt);
                    });
                });
    }

    private static void syncLedger(NbtCompound nbt) {
        totalSouls = nbt.getInt("SmoothDeathTotalSouls");
        discovered = nbt.getInt("SmoothDeathDiscovered");
        recipeCount = nbt.getInt("SmoothDeathRecipeCount");
        charges = nbt.getInt("SmoothDeathCharges");
        maxCharges = nbt.getInt("SmoothDeathMaxCharges");
        rechargeMillis = Math.max(0L, nbt.getLong("SmoothDeathRechargeMillis"));

        SOULS.clear();
        KILLS.clear();
        NbtCompound soulData = nbt.getCompound("SmoothDeathSoulCounts");
        for (String id : soulData.getKeys()) SOULS.put(id, soulData.getInt(id));
        NbtCompound killData = nbt.getCompound("SmoothDeathKillCounts");
        for (String id : killData.getKeys()) KILLS.put(id, killData.getInt(id));
    }

    private static void registerPatchouliHooks() {
        if (patchouliHooksRegistered || !FabricLoader.getInstance().isModLoaded("patchouli")) return;
        try {
            Object api = patchouliApi();
            Method registerCommand = findMethod(api, "registerCommand", 2);
            Method registerFunction = findMethod(api, "registerFunction", 2);
            if (registerCommand == null || registerFunction == null) return;

            // Patchouli does not recursively parse formatting commands returned by a
            // custom command/function. Keep all $(...) styling in the book JSON and
            // return raw values only from these callbacks.
            registerCommand.invoke(api, "smooth_death_total_souls", (Function<Object, String>) ignored -> Integer.toString(totalSouls));
            registerCommand.invoke(api, "smooth_death_discovered", (Function<Object, String>) ignored -> Integer.toString(discovered));
            registerCommand.invoke(api, "smooth_death_recipe_count", (Function<Object, String>) ignored -> Integer.toString(recipeCount));
            registerCommand.invoke(api, "smooth_death_charges", (Function<Object, String>) ignored -> Integer.toString(charges));
            registerCommand.invoke(api, "smooth_death_max_charges", (Function<Object, String>) ignored -> Integer.toString(maxCharges));
            registerCommand.invoke(api, "smooth_death_recharge", (Function<Object, String>) ignored -> rechargeText());

            registerFunction.invoke(api, "smooth_death_stock_name", (BiFunction<String, Object, String>) (slot, ignored) -> stockName(slot));
            registerFunction.invoke(api, "smooth_death_stock_mark", (BiFunction<String, Object, String>) (slot, ignored) -> stockMark(slot));
            registerFunction.invoke(api, "smooth_death_stock_count", (BiFunction<String, Object, String>) (slot, ignored) -> stockCount(slot));

            registerFunction.invoke(api, "smooth_death_name", (BiFunction<String, Object, String>) (id, ignored) -> soulName(id));
            registerFunction.invoke(api, "smooth_death_status", (BiFunction<String, Object, String>) (id, ignored) -> soulStatus(id));
            registerFunction.invoke(api, "smooth_death_soul_count", (BiFunction<String, Object, String>) (id, ignored) -> soulCount(id));
            registerFunction.invoke(api, "smooth_death_slain", (BiFunction<String, Object, String>) (id, ignored) -> slainCount(id));
            registerFunction.invoke(api, "smooth_death_main", (BiFunction<String, Object, String>) (id, ignored) -> soulMain(id));
            registerFunction.invoke(api, "smooth_death_off", (BiFunction<String, Object, String>) (id, ignored) -> soulOff(id));
            registerFunction.invoke(api, "smooth_death_note", (BiFunction<String, Object, String>) (id, ignored) -> soulNote(id));

            patchouliHooksRegistered = true;
            SmoothClasses.LOGGER.info("Registered Patchouli Death List value functions.");
        } catch (ReflectiveOperationException | LinkageError ex) {
            SmoothClasses.LOGGER.warn("Patchouli Death List functions could not be registered; vanilla fallback remains available.", ex);
        }
    }

    private static String rechargeText() {
        if (rechargeMillis <= 0L) return "READY";
        long seconds = Math.max(1L, (rechargeMillis + 999L) / 1000L);
        return seconds + "s";
    }

    private static AvengerSummonRecipes.Recipe stockRecipe(String rawSlot) {
        int slot;
        try {
            slot = Math.max(0, Integer.parseInt(rawSlot == null ? "0" : rawSlot.trim()));
        } catch (RuntimeException ignored) {
            return null;
        }
        List<AvengerSummonRecipes.Recipe> known = new ArrayList<>();
        for (AvengerSummonRecipes.Recipe recipe : AvengerSummonRecipes.allRecipes()) {
            if (KILLS.getOrDefault(recipe.entityId(), 0) > 0) known.add(recipe);
        }
        known.sort(Comparator.comparing(recipe -> AvengerSummonRecipes.friendlyEntityName(recipe.entityId())));
        return slot < known.size() ? known.get(slot) : null;
    }

    private static String stockName(String slot) {
        AvengerSummonRecipes.Recipe recipe = stockRecipe(slot);
        return recipe == null ? "" : AvengerSummonRecipes.friendlyEntityName(recipe.entityId());
    }

    private static String stockMark(String slot) {
        return stockRecipe(slot) == null ? "" : "x";
    }

    private static String stockCount(String slot) {
        AvengerSummonRecipes.Recipe recipe = stockRecipe(slot);
        return recipe == null ? "" : Integer.toString(SOULS.getOrDefault(recipe.entityId(), 0));
    }

    private static String normalizedSoulId(String rawId) {
        return rawId == null ? "" : rawId.trim();
    }

    private static AvengerSummonRecipes.Recipe soulRecipe(String rawId) {
        return AvengerSummonRecipes.recipe(normalizedSoulId(rawId));
    }

    private static boolean discoveredSoul(String rawId) {
        return KILLS.getOrDefault(normalizedSoulId(rawId), 0) > 0;
    }

    private static String soulName(String rawId) {
        String id = normalizedSoulId(rawId);
        return AvengerSummonRecipes.recipe(id) == null ? "Unknown Soul" : AvengerSummonRecipes.friendlyEntityName(id);
    }

    private static String soulStatus(String rawId) {
        return discoveredSoul(rawId) ? "DISCOVERED" : "UNDISCOVERED";
    }

    private static String soulCount(String rawId) {
        String id = normalizedSoulId(rawId);
        return discoveredSoul(id) ? Integer.toString(SOULS.getOrDefault(id, 0)) : "?";
    }

    private static String slainCount(String rawId) {
        String id = normalizedSoulId(rawId);
        return discoveredSoul(id) ? Integer.toString(KILLS.getOrDefault(id, 0)) : "0";
    }

    private static String soulMain(String rawId) {
        AvengerSummonRecipes.Recipe recipe = soulRecipe(rawId);
        if (recipe == null) return "Unknown";
        if (!discoveredSoul(rawId)) return "UNDISCOVERED";
        return AvengerSummonRecipes.friendlyItemName(recipe.mainItemId());
    }

    private static String soulOff(String rawId) {
        AvengerSummonRecipes.Recipe recipe = soulRecipe(rawId);
        if (recipe == null) return "Unknown";
        if (!discoveredSoul(rawId)) return "UNDISCOVERED";
        return recipe.offhandItemId() == null ? "Not required" : AvengerSummonRecipes.friendlyItemName(recipe.offhandItemId());
    }

    private static String soulNote(String rawId) {
        return discoveredSoul(rawId)
                ? "Use H with the listed ingredients to summon this soul."
                : "Kill this creature once to reveal its recipe.";
    }

    private static boolean openPatchouliLedger() {
        if (!FabricLoader.getInstance().isModLoaded("patchouli")) return false;
        try {
            if (!patchouliHooksRegistered) registerPatchouliHooks();
            Object api = patchouliApi();
            Method openEntry = findMethod(api, "openBookEntry", 3);
            if (openEntry != null) {
                openEntry.invoke(api, BOOK_ID, LEDGER_ENTRY, 0);
                return true;
            }
            Method openBook = findMethod(api, "openBookGUI", 1);
            if (openBook != null) {
                openBook.invoke(api, BOOK_ID);
                return true;
            }
        } catch (ReflectiveOperationException | LinkageError | IllegalArgumentException ex) {
            SmoothClasses.LOGGER.warn("Could not open the Patchouli Death List; using vanilla fallback.", ex);
        }
        return false;
    }

    private static Object patchouliApi() throws ReflectiveOperationException {
        Class<?> apiClass = Class.forName("vazkii.patchouli.api.PatchouliAPI");
        return apiClass.getMethod("get").invoke(null);
    }

    private static Method findMethod(Object target, String name, int parameterCount) {
        for (Method method : target.getClass().getMethods()) {
            if (method.getName().equals(name) && method.getParameterCount() == parameterCount) return method;
        }
        return null;
    }

    private static void openVanillaFallback(NbtCompound nbt) {
        ItemStack virtualBook = new ItemStack(Items.WRITTEN_BOOK);
        virtualBook.setNbt(nbt.copy());
        MinecraftClient.getInstance().setScreen(new BookScreen(BookScreen.Contents.create(virtualBook)));
    }
}
