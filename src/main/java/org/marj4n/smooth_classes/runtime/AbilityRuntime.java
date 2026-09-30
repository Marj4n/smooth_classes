package org.marj4n.smooth_classes.runtime;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.marj4n.smooth_classes.integration.PuffishSkillsIntegration;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Shared server-side bridge. Puffish Skills owns class/category and talent unlock state. */
public final class AbilityRuntime {
    private AbilityRuntime() {}

    /** Talent state is stable between unlock events; keep a tiny five-tick cache. */
    private static final int TALENT_CACHE_WINDOW_TICKS = 5;
    private static final class TalentTickCache {
        int tickBucket = Integer.MIN_VALUE;
        final Map<Identifier, Integer> ranks = new HashMap<>();
        void reset(int bucket) { tickBucket = bucket; ranks.clear(); }
    }
    private static final Map<UUID, TalentTickCache> TALENT_CACHE = new HashMap<>();

    public static boolean isClass(ServerPlayerEntity player, Identifier classId) {
        return PuffishSkillsIntegration.isCategoryUnlocked(classId, player);
    }

    public static int talentRank(ServerPlayerEntity player, Identifier talentId) {
        TalentTickCache cache = TALENT_CACHE.computeIfAbsent(player.getUuid(), ignored -> new TalentTickCache());
        int bucket = player.age / TALENT_CACHE_WINDOW_TICKS;
        if (cache.tickBucket != bucket) cache.reset(bucket);
        Integer cached = cache.ranks.get(talentId);
        if (cached != null) return cached;

        String path = talentId.getPath();
        int slash = path.indexOf('/');
        if (slash <= 0) {
            cache.ranks.put(talentId, 0);
            return 0;
        }

        Identifier categoryId = new Identifier(talentId.getNamespace(), path.substring(0, slash));
        int rank = 0;
        for (String skillId : PuffishTalentSkillMap.skillIds(path)) {
            if (PuffishSkillsIntegration.isSkillUnlocked(categoryId, skillId, player)) rank++;
        }
        cache.ranks.put(talentId, rank);
        return rank;
    }

    public static boolean hasTalent(ServerPlayerEntity player, Identifier talentId) {
        return talentRank(player, talentId) > 0;
    }

    public static void invalidateRuntimeCache(ServerPlayerEntity player) {
        TALENT_CACHE.remove(player.getUuid());
    }

    public static void clearRuntimeCaches() {
        TALENT_CACHE.clear();
    }

    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
