package org.marj4n.smooth_classes.integration;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.puffish.skillsmod.api.Category;
import net.puffish.skillsmod.api.Skill;
import net.puffish.skillsmod.api.SkillsAPI;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Thin integration boundary. Puffish Skills remains authoritative for tree UI, points and unlock state. */
public final class PuffishSkillsIntegration {
    /** Optional integration: when Smooth Progression is installed it owns stage progression. */
    private static final boolean SMOOTH_PROGRESSION_LOADED =
            FabricLoader.getInstance().isModLoaded("smooth_progression");

    public static final Identifier TREE = id("tree");
    public static final Identifier ASCENDANCY = id("ascendancy");
    public static final Identifier AVENGER = id("avenger");
    public static final Identifier FOREIGNER = id("foreigner");
    public static final Identifier CASTER = id("caster");
    public static final Identifier BERSERKER = id("berserker");
    public static final Identifier ARCHER = id("archer");
    public static final Identifier ASSASSIN = id("assassin");
    public static final Identifier SABER = id("saber");
    public static final Identifier RULER = id("ruler");
    public static final Identifier RIDER = id("rider");
    public static final Identifier LANCER = id("lancer");

    public static final List<Identifier> CLASS_CATEGORIES = List.of(
            AVENGER, FOREIGNER, CASTER, BERSERKER, ARCHER, ASSASSIN, SABER, RULER, RIDER, LANCER
    );

    /** Category objects are registry-stable after datapack/mod bootstrap. Cache only successful lookups. */
    private static final Map<Identifier, Category> CATEGORY_CACHE = new HashMap<>();

    /**
     * Unlock state is queried from many class/effect hooks in the same game tick.
     * Puffish stays authoritative; this cache only deduplicates repeated reads during
     * a short five-tick window and is explicitly invalidated by skill feedback/network cleanup.
     */
    private static final int RUNTIME_CACHE_WINDOW_TICKS = 5;

    private static final class TickStateCache {
        int tickBucket = Integer.MIN_VALUE;
        final Map<Identifier, Boolean> categories = new HashMap<>();
        final Map<Identifier, Map<String, Boolean>> skills = new HashMap<>();

        void reset(int bucket) {
            tickBucket = bucket;
            categories.clear();
            skills.clear();
        }
    }
    private static final Map<UUID, TickStateCache> TICK_STATE_CACHE = new HashMap<>();

    /** Ascendancy tree count is more expensive and does not need per-tick rescanning. */
    private record AscendancyCountCache(long tickBucket, int count) {}
    private static final Map<UUID, AscendancyCountCache> ASCENDANCY_COUNT_CACHE = new HashMap<>();

    private PuffishSkillsIntegration() {}
    private static Identifier id(String path) { return new Identifier("smooth_classes", path); }

    public static Optional<Category> category(Identifier id) {
        Category cached = CATEGORY_CACHE.get(id);
        if (cached != null) return Optional.of(cached);
        Optional<Category> resolved = SkillsAPI.getCategory(id);
        resolved.ifPresent(category -> CATEGORY_CACHE.put(id, category));
        return resolved;
    }

    private static TickStateCache tickState(ServerPlayerEntity player) {
        TickStateCache cache = TICK_STATE_CACHE.computeIfAbsent(player.getUuid(), ignored -> new TickStateCache());
        int bucket = player.age / RUNTIME_CACHE_WINDOW_TICKS;
        if (cache.tickBucket != bucket) cache.reset(bucket);
        return cache;
    }

    public static boolean isCategoryUnlocked(Identifier categoryId, LivingEntity entity) {
        if (!(entity instanceof ServerPlayerEntity player)) return false;
        TickStateCache cache = tickState(player);
        Boolean cached = cache.categories.get(categoryId);
        if (cached != null) return cached;
        boolean unlocked = category(categoryId).map(c -> c.isUnlocked(player)).orElse(false);
        cache.categories.put(categoryId, unlocked);
        return unlocked;
    }

    public static boolean isSkillUnlocked(Identifier categoryId, String skillId, LivingEntity entity) {
        if (!(entity instanceof ServerPlayerEntity player)) return false;
        TickStateCache cache = tickState(player);
        Map<String, Boolean> categorySkills = cache.skills.computeIfAbsent(categoryId, ignored -> new HashMap<>());
        Boolean cached = categorySkills.get(skillId);
        if (cached != null) return cached;
        boolean unlocked = category(categoryId)
                .flatMap(c -> c.getSkill(skillId))
                .map(s -> s.getState(player) == Skill.State.UNLOCKED)
                .orElse(false);
        categorySkills.put(skillId, unlocked);
        return unlocked;
    }

    public static int countUnlockedSkills(Identifier categoryId, ServerPlayerEntity player) {
        if (!ASCENDANCY.equals(categoryId)) return 0;

        long bucket = player.age / 20L;
        AscendancyCountCache cached = ASCENDANCY_COUNT_CACHE.get(player.getUuid());
        if (cached != null && cached.tickBucket() == bucket) return cached.count();

        int count = category(categoryId)
                .map(c -> (int) c.streamUnlockedSkills(player).count())
                .orElse(0);
        ASCENDANCY_COUNT_CACHE.put(player.getUuid(), new AscendancyCountCache(bucket, count));
        return count;
    }

    public static void invalidateRuntimeCache(ServerPlayerEntity player) {
        UUID id = player.getUuid();
        ASCENDANCY_COUNT_CACHE.remove(id);
        TICK_STATE_CACHE.remove(id);
    }

    public static void clearRuntimeCaches() {
        ASCENDANCY_COUNT_CACHE.clear();
        TICK_STATE_CACHE.clear();
        CATEGORY_CACHE.clear();
    }

    /** parity: Ascendancy becomes visible/unlocked after more than 40 unlocked skills in the base tree category. */
    public static void ensureAscendancyUnlocked(ServerPlayerEntity player) {
        // Smooth Progression owns the Tree -> Class -> Ascendancy stage gate when present.
        // Do not auto-unlock Ascendancy here or it could become available before
        // the selected Smooth Classes class has actually been completed.
        if (SMOOTH_PROGRESSION_LOADED) return;

        Optional<Category> tree = category(TREE);
        Optional<Category> asc = category(ASCENDANCY);
        if (tree.isEmpty() || asc.isEmpty() || asc.get().isUnlocked(player)) return;
        if (tree.get().streamUnlockedSkills(player).count() > 40) {
            asc.get().unlock(player);
            invalidateRuntimeCache(player);
        }
    }

    public static Optional<Category> selectedClass(ServerPlayerEntity player) {
        Category selected = null;
        int unlockedCount = 0;
        for (Identifier id : CLASS_CATEGORIES) {
            if (!isCategoryUnlocked(id, player)) continue;
            Optional<Category> resolved = category(id);
            if (resolved.isEmpty()) continue;
            selected = resolved.get();
            if (++unlockedCount > 1) return Optional.empty();
        }
        return unlockedCount == 1 ? Optional.of(selected) : Optional.empty();
    }
}
