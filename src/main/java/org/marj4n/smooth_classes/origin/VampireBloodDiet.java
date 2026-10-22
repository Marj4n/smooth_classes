package org.marj4n.smooth_classes.origin;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.passive.WanderingTraderEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

/**
 * Feeding quality and blood-type discovery for Vampire. Non-hostile modded mobs
 * receive a deterministic fallback type; datapacks can override the grouping
 * through the smooth_classes:vampire_blood_type_* entity-type tags.
 */
public final class VampireBloodDiet {
    public enum Type {
        HUMANOID("humanoid", 4, 16),
        LIVESTOCK("livestock", 3, 10),
        WILD("wild", 2, 6),
        AQUATIC("aquatic", 1, 4);

        private final String key;
        private final int tier;
        private final int bloodPerPortion;

        Type(String key, int tier, int bloodPerPortion) {
            this.key = key;
            this.tier = tier;
            this.bloodPerPortion = bloodPerPortion;
        }

        public String key() { return key; }
        public int tier() { return tier; }
        public int bloodPerPortion() { return bloodPerPortion; }
    }

    public static final String DISCOVERY_PREFIX = "vampire.blood_type.";
    public static final int DISTINCT_TYPES = 4;

    private static final TagKey<EntityType<?>> HUMANOID = typeTag("vampire_blood_type_humanoid");
    private static final TagKey<EntityType<?>> LIVESTOCK = typeTag("vampire_blood_type_livestock");
    private static final TagKey<EntityType<?>> WILD = typeTag("vampire_blood_type_wild");
    private static final TagKey<EntityType<?>> AQUATIC = typeTag("vampire_blood_type_aquatic");

    private VampireBloodDiet() {}

    private static TagKey<EntityType<?>> typeTag(String path) {
        return TagKey.of(RegistryKeys.ENTITY_TYPE, new Identifier("smooth_classes", path));
    }

    public static Type type(LivingEntity entity) {
        EntityType<?> entityType = entity.getType();
        if (entityType.isIn(HUMANOID)) return Type.HUMANOID;
        if (entityType.isIn(LIVESTOCK)) return Type.LIVESTOCK;
        if (entityType.isIn(WILD)) return Type.WILD;
        if (entityType.isIn(AQUATIC)) return Type.AQUATIC;

        if (entity instanceof VillagerEntity || entity instanceof WanderingTraderEntity) return Type.HUMANOID;
        String path = Registries.ENTITY_TYPE.getId(entityType).getPath();
        if (containsAny(path, "villager", "trader", "merchant", "humanoid", "npc")) return Type.HUMANOID;
        if (containsAny(path, "fish", "squid", "dolphin", "axolotl", "tadpole", "turtle", "jellyfish",
                "whale", "seahorse", "octopus", "crab", "shrimp", "salmon", "cod", "puffer",
                "nautilus", "manatee", "manta", "eel", "shark")) return Type.AQUATIC;
        if (containsAny(path, "cow", "mooshroom", "pig", "sheep", "chicken", "horse", "donkey",
                "mule", "llama", "goat", "alpaca", "bison", "buffalo", "yak", "turkey")) return Type.LIVESTOCK;
        return Type.WILD;
    }

    private static boolean containsAny(String name, String... tokens) {
        for (String token : tokens) if (name.contains(token)) return true;
        return false;
    }

    /** One blood reserve pip is drained per feeding event. */
    public static int gain(LivingEntity entity) {
        int gain = type(entity).bloodPerPortion();
        if (entity instanceof VillagerEntity villager && villager.isSleeping()) gain += 4;
        return gain;
    }

    /**
     * Preserve old progress when possible. The old 'animal' pool combined all
     * land animals; credit it as Livestock for existing worlds. Old Hostile/Elite
     * discoveries are excluded because hostile mobs cannot be fed on normally.
     */
    public static void migrateLegacy(OriginState state) {
        if (state.hasFlag("vampire.blood_types_migrated_v16")) return;
        if (state.hasFlag("vampire.blood_category.humanoid")) state.flag(DISCOVERY_PREFIX + "humanoid");
        if (state.hasFlag("vampire.blood_category.animal")) state.flag(DISCOVERY_PREFIX + "livestock");
        if (state.hasFlag("vampire.blood_category.aquatic")) state.flag(DISCOVERY_PREFIX + "aquatic");
        state.flag("vampire.blood_types_migrated_v16");
    }

    public static int discovered(OriginState state) {
        int count = 0;
        for (Type type : Type.values()) if (state.hasFlag(DISCOVERY_PREFIX + type.key())) count++;
        return count;
    }
}
