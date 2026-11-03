package org.marj4n.smooth_classes.content.avenger.runtime;

import com.google.gson.*;
import net.minecraft.entity.*;
import net.minecraft.entity.mob.*;
import net.minecraft.item.*;
import net.minecraft.server.MinecraftServer;
import org.marj4n.smooth_classes.platform.PlatformRegistries;
import org.marj4n.smooth_classes.util.ResourceIds;
import org.marj4n.smooth_classes.SmoothClasses;
import java.util.*;
import java.nio.file.*;

/** Server-authoritative catalog built once after datapacks and all bridged registries are ready. */
public final class AvengerSummonCatalog {
    private static final Set<String> INTERNAL =
            Set.of(
                    "smooth_classes:wraith",
                    "smooth_classes:dreadglare",
                    "smooth_classes:greater_dreadglare");
    private static final Set<String> ATTACHMENTS =
            Set.of(
                    "aether:cloud_minion",
                    "alexscaves:boundroid_winch",
                    "alexsmobs:centipede_body",
                    "alexsmobs:murmur_head",
                    "aquamirae:pillagers_patrol",
                    "archers_expansion:alter_ego",
                    "betternether:naga_projectile",
                    "eldritch_end:tentacle",
                    "eldritch_end:undead_tentacle",
                    "friendsandfoes:player_illusion",
                    "mutantmonsters:endersoul_clone",
                    "simplyswords:creeper_head",
                    "simplyswords:falling_sniffer",
                    "simplyswords:goat_stampede",
                    "simplyswords:riftmane_charger",
                    "simplyswords:skeleton_minion",
                    "simplyswords:watcher_bat",
                    "simplyswords:wolf_minion",
                    "skarrier_mobs:endorah_body",
                    "skarrier_mobs:endorah_head_melee",
                    "skarrier_mobs:endorah_head_melee_flying",
                    "skarrier_mobs:endorah_head_ranged",
                    "skarrier_mobs:endorah_head_ranged_flying",
                    "skarrier_mobs:flore_head",
                    "skarrier_mobs:grave_skull",
                    "skarrier_mobs:grave_skull_big",
                    "skarrier_mobs:endorah_energy_jaws",
                    "skarrier_mobs:endorah_energy_jaws_big",
                    "skarrier_mobs:gad_cloud",
                    "skarrier_mobs:grave_spike",
                    "skarrier_mobs:healing_flesh",
                    "skarrier_mobs:lit_tremor_bomb",
                    "skarrier_mobs:semi_living_helmet_bite");
    private static final Set<String> UNOBTAINABLE =
            Set.of(
                    "air",
                    "barrier",
                    "bedrock",
                    "structure_block",
                    "structure_void",
                    "jigsaw",
                    "command_block",
                    "chain_command_block",
                    "repeating_command_block",
                    "command_block_minecart",
                    "debug_stick",
                    "light",
                    "knowledge_book",
                    "spawner",
                    "end_portal_frame",
                    "budding_amethyst",
                    "petrified_oak_slab",
                    "reinforced_deepslate");

    private AvengerSummonCatalog() {}

    public static void rebuild(MinecraftServer server) {
        var world = server.getOverworld();
        var base = AvengerSummonRecipes.builtinRecipes();
        List<Map<String, Object>> report = new ArrayList<>();
        Map<String, MobEntity> eligible = new TreeMap<>();
        for (var id :
                PlatformRegistries.ENTITY_TYPE.getIds().stream()
                        .sorted(Comparator.comparing(Object::toString))
                        .toList()) {
            String key = id.toString();
            if (INTERNAL.contains(key)) {
                report.add(row(key, "internal_class_minion"));
                continue;
            }
            if (ATTACHMENTS.contains(key)
                    || key.contains("_dummy")
                    || key.endsWith(":target_dummy")) {
                report.add(row(key, "attachment_effect_or_generator"));
                continue;
            }
            Entity e = null;
            try {
                var type = PlatformRegistries.ENTITY_TYPE.get(id);
                e = type.create(world);
                if (!(e instanceof MobEntity mob)) {
                    report.add(row(key, "not_a_mob"));
                    continue;
                }
                if (mob.getType() != type) {
                    // Some registry factories wrap a vanilla mob without creating a new soul type.
                    report.add(
                            row(
                                    key,
                                    "factory_alias_of: "
                                            + PlatformRegistries.ENTITY_TYPE.getId(mob.getType())));
                    continue;
                }
                if (!AvengerSummonSafety.allowed(mob)) {
                    report.add(row(key, "boss_bar_or_banned"));
                    continue;
                }
                eligible.put(key, mob);
            } catch (RuntimeException | LinkageError failure) {
                report.add(
                        row(
                                key,
                                "constructor_error: "
                                        + failure.getClass().getSimpleName()
                                        + ": "
                                        + String.valueOf(failure.getMessage())));
            }
            // Probes are never added to the world, initialized, or ticked.
        }
        List<AvengerRecipeAllocator.Request> requests = new ArrayList<>();
        for (var recipe : base) {
            if (!eligible.containsKey(recipe.entityId())
                    || !itemExists(recipe.mainItemId())
                    || recipe.offhandItemId() != null && !itemExists(recipe.offhandItemId()))
                continue;
            requests.add(
                    new AvengerRecipeAllocator.Request(
                            recipe.entityId(), recipe.mainItemId(), recipe.offhandItemId(), true));
        }
        Set<String> already = new HashSet<>();
        for (var request : requests) already.add(request.entity());
        for (var entry : eligible.entrySet())
            if (!already.contains(entry.getKey())) {
                List<String> drops = new ArrayList<>();
                try {
                    readLoot(
                            server,
                            entry.getValue().getLootTable().toString(),
                            drops,
                            new HashSet<>(),
                            0);
                } catch (RuntimeException ignored) {
                }
                String main =
                        drops.stream()
                                .filter(AvengerSummonCatalog::ingredient)
                                .sorted(
                                        Comparator.comparingInt(
                                                (String id) ->
                                                        id.startsWith(
                                                                        entry.getKey().split(":")[0]
                                                                                + ":")
                                                                ? 0
                                                                : 1))
                                .findFirst()
                                .orElse(fallback(entry.getKey(), entry.getValue()));
                String off =
                        drops.stream()
                                .filter(AvengerSummonCatalog::ingredient)
                                .filter(id -> !id.equals(main))
                                .sorted()
                                .findFirst()
                                .orElse(null);
                requests.add(new AvengerRecipeAllocator.Request(entry.getKey(), main, off, false));
            }
        List<String> pool =
                PlatformRegistries.ITEM.getIds().stream()
                        .map(Object::toString)
                        .filter(AvengerSummonCatalog::commonDiscriminator)
                        .filter(AvengerSummonCatalog::ingredient)
                        .sorted()
                        .toList();
        var allocated = AvengerRecipeAllocator.allocate(requests, pool);
        var recipes =
                allocated.stream()
                        .map(e -> new AvengerSummonRecipes.Recipe(e.entity(), e.main(), e.off()))
                        .toList();
        AvengerSummonRecipes.installCatalog(recipes);
        for (var e : allocated) {
            var row = row(e.entity(), "eligible");
            row.put("main", e.main());
            row.put("off", e.off() == null ? "" : e.off());
            report.add(row);
        }
        try {
            Path output =
                    server.getRunDirectory()
                            .toPath()
                            .resolve("logs/smooth-classes-avenger-catalog.json");
            Files.createDirectories(output.getParent());
            Files.writeString(
                    output, new GsonBuilder().setPrettyPrinting().create().toJson(report));
        } catch (java.io.IOException failure) {
            SmoothClasses.LOGGER.warn("Could not write Avenger catalog audit", failure);
        }
        SmoothClasses.LOGGER.info(
                "Avenger catalog ready: {} eligible mobs; unique hand recipes; {} registry entries inspected",
                recipes.size(),
                report.size());
    }

    private static Map<String, Object> row(String id, String status) {
        var map = new LinkedHashMap<String, Object>();
        map.put("entity", id);
        map.put("status", status);
        return map;
    }

    private static boolean itemExists(String id) {
        return PlatformRegistries.ITEM.containsId(ResourceIds.parse(id))
                && !id.equals("minecraft:air");
    }

    private static boolean ingredient(String id) {
        if (!itemExists(id)) return false;
        var item = PlatformRegistries.ITEM.get(ResourceIds.parse(id));
        if (item instanceof SpawnEggItem || item instanceof EntityBucketItem) return false;
        String path = id.substring(id.indexOf(':') + 1);
        return !UNOBTAINABLE.contains(path)
                && !path.endsWith("_spawn_egg")
                && !path.equals("enchanted_book");
    }

    private static void readLoot(
            MinecraftServer server,
            String table,
            List<String> items,
            Set<String> visited,
            int depth) {
        if (depth > 6 || !visited.add(table)) return;
        var id = ResourceIds.parse(table);
        var path = ResourceIds.parse(id.getNamespace() + ":loot_tables/" + id.getPath() + ".json");
        var resource = server.getResourceManager().getResource(path);
        if (resource.isEmpty()) return;
        try (var reader = resource.get().getReader()) {
            walk(server, JsonParser.parseReader(reader), items, visited, depth);
        } catch (java.io.IOException | JsonParseException ignored) {
        }
    }

    private static void walk(
            MinecraftServer server,
            JsonElement json,
            List<String> items,
            Set<String> visited,
            int depth) {
        if (json.isJsonArray()) {
            for (var child : json.getAsJsonArray()) walk(server, child, items, visited, depth);
            return;
        }
        if (!json.isJsonObject()) return;
        var object = json.getAsJsonObject();
        if (object.has("type")
                && object.has("name")
                && object.get("type").isJsonPrimitive()
                && object.get("name").isJsonPrimitive()) {
            String type = object.get("type").getAsString(), name = object.get("name").getAsString();
            if (type.equals("minecraft:item") || type.equals("item"))
                items.add(ResourceIds.parse(name).toString());
            if (type.equals("minecraft:loot_table") || type.equals("loot_table"))
                readLoot(server, name, items, visited, depth + 1);
        }
        for (var entry : object.entrySet())
            if (entry.getValue().isJsonObject() || entry.getValue().isJsonArray())
                walk(server, entry.getValue(), items, visited, depth);
    }

    private static String fallback(String id, MobEntity mob) {
        if (id.contains("fish")
                || id.contains("shark")
                || id.contains("whale")
                || id.contains("eel")) return "minecraft:cod";
        if (id.contains("bird")
                || id.contains("eagle")
                || id.contains("owl")
                || id.contains("duck")) return "minecraft:feather";
        if (id.contains("skeleton") || id.contains("bone")) return "minecraft:bone";
        if (id.contains("slime")) return "minecraft:slime_ball";
        return mob instanceof HostileEntity ? "minecraft:rotten_flesh" : "minecraft:leather";
    }

    private static boolean commonDiscriminator(String id) {
        if (!id.startsWith("minecraft:")) return false;
        String path = id.substring(10);
        if (Set.of(
                        "coal",
                        "charcoal",
                        "flint",
                        "clay_ball",
                        "brick",
                        "wheat",
                        "wheat_seeds",
                        "pumpkin_seeds",
                        "melon_seeds",
                        "beetroot_seeds",
                        "stick",
                        "string",
                        "bone",
                        "feather",
                        "leather",
                        "rotten_flesh",
                        "slime_ball",
                        "gunpowder",
                        "redstone",
                        "lapis_lazuli",
                        "amethyst_shard",
                        "copper_ingot",
                        "iron_nugget",
                        "gold_nugget",
                        "quartz",
                        "sugar",
                        "paper",
                        "book",
                        "ink_sac",
                        "glow_ink_sac",
                        "kelp",
                        "dried_kelp",
                        "apple",
                        "carrot",
                        "potato",
                        "beetroot",
                        "melon_slice",
                        "bowl",
                        "glass_bottle",
                        "spider_eye",
                        "fermented_spider_eye",
                        "dirt",
                        "cobblestone",
                        "stone",
                        "sand",
                        "red_sand",
                        "gravel",
                        "clay",
                        "moss_block",
                        "mud",
                        "netherrack",
                        "soul_sand",
                        "soul_soil",
                        "blackstone",
                        "sandstone",
                        "basalt",
                        "calcite",
                        "tuff")
                .contains(path)) return true;
        for (String suffix :
                new String[] {
                    "_wool",
                    "_carpet",
                    "_terracotta",
                    "_stained_glass",
                    "_concrete",
                    "_concrete_powder",
                    "_planks",
                    "_log",
                    "_wood",
                    "_stem",
                    "_hyphae",
                    "_leaves",
                    "_sapling",
                    "_fence",
                    "_button",
                    "_pressure_plate",
                    "_slab",
                    "_stairs",
                    "_trapdoor",
                    "_dye"
                }) if (path.endsWith(suffix)) return true;
        return false;
    }
}
