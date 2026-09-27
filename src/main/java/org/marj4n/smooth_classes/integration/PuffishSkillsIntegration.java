package org.marj4n.smooth_classes.integration;

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

    public static final List<Identifier> CLASS_CATEGORIES = List.of(
            AVENGER, FOREIGNER, CASTER, BERSERKER, ARCHER, ASSASSIN, SABER, RULER
    );

    private PuffishSkillsIntegration() {}
    private static Identifier id(String path) { return new Identifier("smooth_classes", path); }

    public static Optional<Category> category(Identifier id) { return SkillsAPI.getCategory(id); }

    public static boolean isCategoryUnlocked(Identifier categoryId, LivingEntity entity) {
        if (!(entity instanceof ServerPlayerEntity player)) return false;
        return category(categoryId).map(c -> c.isUnlocked(player)).orElse(false);
    }

    public static boolean isSkillUnlocked(Identifier categoryId, String skillId, LivingEntity entity) {
        if (!(entity instanceof ServerPlayerEntity player)) return false;
        return category(categoryId)
                .flatMap(c -> c.getSkill(skillId))
                .map(s -> s.getState(player) == Skill.State.UNLOCKED)
                .orElse(false);
    }


    /**
     * Ascendancy point count is read frequently by channelled effects. Keep a
     * one-second server-side cache so those effects do not rescan the Puffish
     * category every game tick. Skill unlock feedback invalidates it immediately.
     */
    private record AscendancyCountCache(long tickBucket, int count) {}
    private static final Map<UUID, AscendancyCountCache> ASCENDANCY_COUNT_CACHE = new HashMap<>();

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
        ASCENDANCY_COUNT_CACHE.remove(player.getUuid());
    }

    public static void clearRuntimeCaches() {
        ASCENDANCY_COUNT_CACHE.clear();
    }

    /** parity: Ascendancy becomes visible/unlocked after more than 40
     * unlocked skills in the base tree category. */
    public static void ensureAscendancyUnlocked(ServerPlayerEntity player) {
        Optional<Category> tree=category(TREE);
        Optional<Category> asc=category(ASCENDANCY);
        if(tree.isEmpty() || asc.isEmpty() || asc.get().isUnlocked(player)) return;
        if(tree.get().streamUnlockedSkills(player).count()>40) asc.get().unlock(player);
    }

    public static Optional<Category> selectedClass(ServerPlayerEntity player) {
        List<Category> unlocked = CLASS_CATEGORIES.stream()
                .map(SkillsAPI::getCategory).flatMap(Optional::stream)
                .filter(c -> c.isUnlocked(player)).toList();
        return unlocked.size() == 1 ? Optional.of(unlocked.get(0)) : Optional.empty();
    }
}
