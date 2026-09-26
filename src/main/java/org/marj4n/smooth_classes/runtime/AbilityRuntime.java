package org.marj4n.smooth_classes.runtime;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.marj4n.smooth_classes.integration.PuffishSkillsIntegration;

/** Shared server-side bridge. Puffish Skills owns class/category and talent unlock state. */
public final class AbilityRuntime {
    private AbilityRuntime() {}

    public static boolean isClass(ServerPlayerEntity player, Identifier classId) {
        return PuffishSkillsIntegration.isCategoryUnlocked(classId, player);
    }

    public static int talentRank(ServerPlayerEntity player, Identifier talentId) {
        String path = talentId.getPath();
        int slash = path.indexOf('/');
        if (slash <= 0) return 0;

        Identifier categoryId = new Identifier(talentId.getNamespace(), path.substring(0, slash));
        int rank = 0;
        for (String skillId : PuffishTalentSkillMap.skillIds(path)) {
            if (PuffishSkillsIntegration.isSkillUnlocked(categoryId, skillId, player)) rank++;
        }
        return rank;
    }

    public static boolean hasTalent(ServerPlayerEntity player, Identifier talentId) {
        return talentRank(player, talentId) > 0;
    }

    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
