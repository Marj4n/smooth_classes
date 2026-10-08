package org.marj4n.smooth_classes.origin;

import net.minecraft.server.network.ServerPlayerEntity;
import net.puffish.skillsmod.api.Skill;
import org.marj4n.smooth_classes.integration.PuffishSkillsIntegration;
import org.marj4n.smooth_classes.network.SmoothClassesNetworking;

import java.util.List;

/**
 * Operator-only Origin tree progression shortcut. This is NOT character XP or a
 * replacement for the real objective-based Origin advancement system.
 * Level 0 = Origin base; levels 1-7 = ordered tree milestone unlocks.
 * Branch skills have stable test ordering, and the natural quest system is untouched.
 */
public final class OriginDebugLevels {
    private OriginDebugLevels() { }

    private static final List<String> HUMAN = List.of(
            "survivor", "wayfarer", "indomitable", "endurance", "memory", "second_wind", "final");
    private static final List<String> VAMPIRE = List.of(
            "bat_form", "man_bat", "blood_sense", "blood_flask", "nobility", "sun_tolerance", "final");
    private static final List<String> MERMAID = List.of(
            "siren_voice", "drowned_song", "sea_kinship", "oceans_favor", "royal_court", "landwalker", "final");
    private static final List<String> SLIME = List.of(
            "humanoid", "elastic_core", "squeeze", "fragment", "perfect_gel", "size_control", "final");

    public static List<String> milestones(OriginType origin) {
        if (origin == null) return List.of();
        return switch (origin) {
            case HUMAN -> HUMAN;
            case VAMPIRE -> VAMPIRE;
            case MERMAID -> MERMAID;
            case SLIME -> SLIME;
            default -> List.of();
        };
    }

    public static int maxLevel(OriginType origin) { return milestones(origin).size(); }

    /** Level is highest unlocked milestone, including partial branches unlocked by quests. */
    public static int level(ServerPlayerEntity player) {
        OriginType origin = OriginRuntime.state(player).origin();
        List<String> steps = milestones(origin);
        if (steps.isEmpty()) return 0;
        var category = PuffishSkillsIntegration.category(origin.categoryId()).orElse(null);
        if (category == null) return 0;
        int level = 0;
        for (int i = 0; i < steps.size(); i++) {
            String skillId = steps.get(i);
            if (category.getSkill(skillId).map(skill -> skill.getState(player) == Skill.State.UNLOCKED)
                    .orElse(false)) level = i + 1;
        }
        return level;
    }

    /** Grants all preceding milestones, never revokes legitimate achievements. */
    public static boolean advanceTo(ServerPlayerEntity player, int target) {
        OriginType origin = OriginRuntime.state(player).origin();
        List<String> steps = milestones(origin);
        if (steps.isEmpty() || target < 0 || target > steps.size() || target < level(player)) return false;
        var category = PuffishSkillsIntegration.category(origin.categoryId()).orElse(null);
        if (category == null || !category.isUnlocked(player)) return false;
        // Pre-validate before changing any state, so a datapack mismatch cannot half-grant a tier.
        for (int i = 0; i < target; i++) {
            if (category.getSkill(steps.get(i)).isEmpty()) return false;
        }
        for (int i = 0; i < target; i++) {
            String milestone = steps.get(i);
            category.getSkill(milestone).ifPresent(skill -> {
                if (skill.getState(player) != Skill.State.UNLOCKED) {
                    skill.unlock(player);
                    OriginAdvancementNotices.onMilestoneUnlocked(player, origin, milestone);
                }
            });
        }
        PuffishSkillsIntegration.invalidateRuntimeCache(player);
        player.calculateDimensions();
        SmoothClassesNetworking.sendOriginState(player);
        SmoothClassesNetworking.sendAbilityState(player);
        return true;
    }
}
