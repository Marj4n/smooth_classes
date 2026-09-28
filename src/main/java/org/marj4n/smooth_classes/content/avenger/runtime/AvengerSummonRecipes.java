package org.marj4n.smooth_classes.content.avenger.runtime;

import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Vanilla summon ingredients shown by the Death List. */
public final class AvengerSummonRecipes {
    private static final Map<String, Recipe> RECIPES = new LinkedHashMap<>();
    private static final Map<String, List<Recipe>> BY_MAIN_ITEM = new HashMap<>();
    private static final Map<String, String> FRIENDLY_NAMES = new HashMap<>();

    static {
        // Passive / utility mobs
        r("allay", "amethyst_shard");
        r("axolotl", "tropical_fish", "water_bucket");
        r("bat", "phantom_membrane", "coal");
        r("bee", "honeycomb");
        r("camel", "cactus", "saddle");
        r("cat", "cod");
        r("chicken", "feather");
        r("cod", "cod", "water_bucket");
        r("cow", "leather");
        r("dolphin", "cod", "heart_of_the_sea");
        r("donkey", "leather", "chest");
        r("fox", "sweet_berries");
        r("frog", "slime_ball");
        r("glow_squid", "glow_ink_sac");
        r("goat", "goat_horn");
        r("horse", "leather", "saddle");
        r("iron_golem", "iron_block");
        r("llama", "leather", "hay_block");
        r("mooshroom", "red_mushroom", "leather");
        r("mule", "leather", "golden_carrot");
        r("ocelot", "cod", "jungle_sapling");
        r("panda", "bamboo");
        r("parrot", "feather", "wheat_seeds");
        r("pig", "porkchop");
        r("polar_bear", "cod", "snow_block");
        r("pufferfish", "pufferfish", "water_bucket");
        r("rabbit", "rabbit_hide");
        r("salmon", "salmon", "water_bucket");
        r("sheep", "white_wool");
        r("sniffer", "torchflower_seeds");
        r("snow_golem", "snow_block");
        r("squid", "ink_sac");
        r("strider", "string", "warped_fungus");
        r("tadpole", "slime_ball", "water_bucket");
        r("trader_llama", "leather", "emerald");
        r("tropical_fish", "tropical_fish", "water_bucket");
        r("turtle", "scute");
        r("villager", "emerald");
        r("wandering_trader", "emerald", "lead");
        r("wolf", "bone", "rotten_flesh");

        // Hostile / neutral mobs
        r("blaze", "blaze_rod");
        r("cave_spider", "spider_eye", "cobweb");
        r("creeper", "gunpowder");
        r("drowned", "rotten_flesh", "water_bucket");
        r("elder_guardian", "prismarine_shard", "sponge");
        r("enderman", "ender_pearl");
        r("endermite", "ender_pearl", "chorus_fruit");
        r("evoker", "emerald", "totem_of_undying");
        r("ghast", "ghast_tear");
        r("giant", "rotten_flesh", "carved_pumpkin");
        r("guardian", "prismarine_shard");
        r("hoglin", "porkchop", "crimson_fungus");
        r("husk", "rotten_flesh", "sand");
        r("illusioner", "bow", "emerald");
        r("magma_cube", "magma_cream");
        r("phantom", "phantom_membrane");
        r("piglin", "gold_ingot");
        r("piglin_brute", "gold_block", "golden_axe");
        r("pillager", "crossbow");
        r("ravager", "saddle", "emerald");
        r("shulker", "shulker_shell");
        r("silverfish", "flint", "stone");
        r("skeleton", "bone");
        r("skeleton_horse", "bone", "saddle");
        r("slime", "slime_ball");
        r("spider", "string");
        r("stray", "bone", "packed_ice");
        r("vex", "iron_sword", "emerald");
        r("vindicator", "iron_axe", "emerald");
        r("warden", "sculk_catalyst");
        r("witch", "glass_bottle", "redstone");
        r("wither_skeleton", "bone", "coal");
        r("zoglin", "rotten_flesh", "crimson_fungus");
        r("zombie", "rotten_flesh");
        r("zombie_horse", "rotten_flesh", "saddle");
        r("zombie_villager", "rotten_flesh", "emerald");
        r("zombified_piglin", "rotten_flesh", "gold_nugget");

        // Bosses are intentionally expensive, but still obey the same rule:
        // ingredients alone are useless until the Avenger has actually killed one.
        r("wither", "nether_star", "soul_sand");
        r("ender_dragon", "dragon_breath", "end_crystal");

        for (Recipe recipe : RECIPES.values()) {
            BY_MAIN_ITEM.computeIfAbsent(recipe.mainItemId(), ignored -> new ArrayList<>()).add(recipe);
        }
        BY_MAIN_ITEM.replaceAll((ignored, recipes) -> List.copyOf(recipes));
    }

    private AvengerSummonRecipes() {}

    private static void r(String mob, String main) { r(mob, main, null); }
    private static void r(String mob, String main, String offhand) {
        RECIPES.put("minecraft:" + mob, new Recipe("minecraft:" + mob, "minecraft:" + main,
                offhand == null ? null : "minecraft:" + offhand));
    }

    public static Recipe recipe(String entityId) { return RECIPES.get(entityId); }
    public static boolean supported(String entityId) { return RECIPES.containsKey(entityId); }

    /**
     * Exact off-hand fusion recipes take precedence. If the off-hand does not form
     * a known fusion, a main-hand-only recipe may still be used while holding any
     * unrelated off-hand item (shield, torch, etc.).
     */
    public static List<Recipe> matching(ItemStack main, ItemStack offhand) {
        String mainId = itemId(main);
        List<Recipe> candidates = BY_MAIN_ITEM.get(mainId);
        if (candidates == null || candidates.isEmpty()) return List.of();

        String offId = itemId(offhand);
        List<Recipe> fusion = null;
        List<Recipe> mainOnly = null;
        for (Recipe recipe : candidates) {
            if (recipe.offhandItemId() == null) {
                if (mainOnly == null) mainOnly = new ArrayList<>(2);
                mainOnly.add(recipe);
            } else if (recipe.offhandItemId().equals(offId)) {
                if (fusion == null) fusion = new ArrayList<>(2);
                fusion.add(recipe);
            }
        }
        if (fusion != null && !fusion.isEmpty()) return fusion;
        return mainOnly == null ? List.of() : mainOnly;
    }

    public static String itemId(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "";
        return Registries.ITEM.getId(stack.getItem()).toString();
    }

    public static String friendlyEntityName(String entityId) {
        if (entityId == null || entityId.isBlank()) return "Unknown";
        return FRIENDLY_NAMES.computeIfAbsent("entity|" + entityId, ignored -> friendlyName(entityId));
    }

    public static String friendlyItemName(String itemId) {
        if (itemId == null || itemId.isBlank()) return "Not required";
        return FRIENDLY_NAMES.computeIfAbsent("item|" + itemId, ignored -> friendlyName(itemId));
    }

    private static String friendlyName(String id) {
        int colon = id.indexOf(':');
        return titleCase(colon >= 0 ? id.substring(colon + 1) : id);
    }

    private static String titleCase(String path) {
        String[] words = path.replace('-', '_').split("_");
        StringBuilder out = new StringBuilder();
        for (String word : words) {
            if (word.isBlank()) continue;
            if (!out.isEmpty()) out.append(' ');
            out.append(word.substring(0, 1).toUpperCase(Locale.ROOT));
            if (word.length() > 1) out.append(word.substring(1));
        }
        return out.toString();
    }

    public record Recipe(String entityId, String mainItemId, String offhandItemId) {
        public Identifier entityIdentifier() { return new Identifier(entityId); }
    }
}
