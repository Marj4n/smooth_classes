package org.marj4n.smooth_classes.origin;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.puffish.skillsmod.api.Skill;
import org.marj4n.smooth_classes.integration.PuffishSkillsIntegration;

/** Server-authoritative objective unlocks for the four Origins shipped in V1. */
public final class OriginProgressUnlocks {
    private OriginProgressUnlocks() {}

    public static void sync(ServerPlayerEntity player, OriginState state) {
        OriginType origin = state.origin();
        if (origin == null) return;

        switch (origin) {
            case HUMAN -> syncHuman(player, state);
            case VAMPIRE -> syncVampire(player, state);
            case MERMAID -> syncMermaid(player, state);
            case SLIME -> syncSlime(player, state);
            default -> { }
        }
    }

    private static void syncHuman(ServerPlayerEntity player, OriginState state) {
        int mastered = state.countFlags("human.environment_mastered.");
        int dimensions = state.countFlags("human.dimension_visited.");
        int majors = state.progress("human.major_kills");

        unlockIf(player, OriginType.HUMAN, "survivor", mastered >= 3);
        unlockIf(player, OriginType.HUMAN, "wayfarer", dimensions >= 3);
        unlockIf(player, OriginType.HUMAN, "indomitable", mastered >= 3 && dimensions >= 3 && majors >= 3);
        if (isUnlocked(player, OriginType.HUMAN, "indomitable")) {
            unlockIf(player, OriginType.HUMAN, "endurance", mastered >= 4);
            unlockIf(player, OriginType.HUMAN, "memory", dimensions >= 3 && mastered >= 4);
            unlockIf(player, OriginType.HUMAN, "second_wind", majors >= 5);
        }
        unlockIf(player, OriginType.HUMAN, "final",
                mastered >= 5
                        && isUnlocked(player, OriginType.HUMAN, "endurance")
                        && isUnlocked(player, OriginType.HUMAN, "memory")
                        && isUnlocked(player, OriginType.HUMAN, "second_wind"));
    }

    private static void syncVampire(ServerPlayerEntity player, OriginState state) {
        unlockIf(player, OriginType.VAMPIRE, "bat_form",
                state.progress("vampire.bats_killed") >= 10
                        && state.hasFlag("vampire.filled_blood_once")
                        && state.hasFlag("vampire.full_night_survived"));

        unlockIf(player, OriginType.VAMPIRE, "man_bat",
                isUnlocked(player, OriginType.VAMPIRE, "bat_form")
                        && state.progress("vampire.humanoid_blood") >= 100
                        && state.progress("vampire.night_kills") >= 20);

        if (isUnlocked(player, OriginType.VAMPIRE, "man_bat")) {
            boolean bloodStudy = state.progress("vampire.lifetime_blood") >= 300
                    && state.countFlags("vampire.blood_category.") >= 4;
            unlockIf(player, OriginType.VAMPIRE, "blood_sense", bloodStudy);
            unlockIf(player, OriginType.VAMPIRE, "blood_flask",
                    state.progress("vampire.lifetime_blood") >= 400);
        }

        unlockIf(player, OriginType.VAMPIRE, "nobility",
                isUnlocked(player, OriginType.VAMPIRE, "blood_sense")
                        && isUnlocked(player, OriginType.VAMPIRE, "blood_flask")
                        && state.progress("vampire.lifetime_blood") >= 500
                        && state.countFlags("vampire.blood_category.") >= 5);
        unlockIf(player, OriginType.VAMPIRE, "sun_tolerance",
                isUnlocked(player, OriginType.VAMPIRE, "nobility")
                        && state.hasFlag("vampire.full_night_survived")
                        && state.progress("vampire.night_kills") >= 50);
        unlockIf(player, OriginType.VAMPIRE, "final",
                isUnlocked(player, OriginType.VAMPIRE, "nobility")
                        && isUnlocked(player, OriginType.VAMPIRE, "sun_tolerance")
                        && state.progress("vampire.lifetime_blood") >= 1000);
    }

    private static void syncMermaid(ServerPlayerEntity player, OriginState state) {
        unlockIf(player, OriginType.MERMAID, "siren_voice",
                state.hasFlag("mermaid.has_nautilus")
                        && state.progress("mermaid.drowned_kills") >= 10
                        && state.progress("mermaid.submerged_ticks") >= 10 * 60 * 20);
        unlockIf(player, OriginType.MERMAID, "drowned_song",
                isUnlocked(player, OriginType.MERMAID, "siren_voice")
                        && state.progress("mermaid.drowned_kills") >= 25
                        && state.progress("mermaid.trident_drowned_kills") >= 3);
        unlockIf(player, OriginType.MERMAID, "sea_kinship",
                isUnlocked(player, OriginType.MERMAID, "siren_voice")
                        && state.progress("mermaid.submerged_ticks") >= 20 * 60 * 20);
        unlockIf(player, OriginType.MERMAID, "oceans_favor",
                isUnlocked(player, OriginType.MERMAID, "drowned_song")
                        && isUnlocked(player, OriginType.MERMAID, "sea_kinship")
                        && state.progress("mermaid.elder_guardian_kills") >= 3);
        unlockIf(player, OriginType.MERMAID, "royal_court",
                isUnlocked(player, OriginType.MERMAID, "oceans_favor")
                        && state.progress("mermaid.guardian_kills") >= 20
                        && state.progress("mermaid.submerged_ticks") >= 40 * 60 * 20);
        unlockIf(player, OriginType.MERMAID, "landwalker",
                isUnlocked(player, OriginType.MERMAID, "oceans_favor")
                        && state.progress("mermaid.dry_ticks") >= 30 * 60 * 20);
        unlockIf(player, OriginType.MERMAID, "final",
                isUnlocked(player, OriginType.MERMAID, "royal_court")
                        && isUnlocked(player, OriginType.MERMAID, "landwalker")
                        && state.progress("mermaid.elder_guardian_kills") >= 5);
    }

    private static void syncSlime(ServerPlayerEntity player, OriginState state) {
        unlockIf(player, OriginType.SLIME, "humanoid",
                state.progress("slime.humanoid_residue") >= 20
                        && state.countFlags("slime.humanoid_species.") >= 4
                        && state.progress("slime.observe_ticks") >= 10 * 60 * 20);
        unlockIf(player, OriginType.SLIME, "elastic_core",
                isUnlocked(player, OriginType.SLIME, "humanoid")
                        && state.progress("slime.slimes_defeated") >= 20
                        && state.progress("slime.large_slimes_defeated") >= 3
                        && state.progress("slime.airborne_ticks") >= 5 * 60 * 20);
        unlockIf(player, OriginType.SLIME, "squeeze",
                isUnlocked(player, OriginType.SLIME, "elastic_core")
                        && state.progress("slime.squeeze_ticks") >= 2 * 60 * 20);
        unlockIf(player, OriginType.SLIME, "fragment",
                isUnlocked(player, OriginType.SLIME, "elastic_core")
                        && state.progress("slime.large_slimes_defeated") >= 8);
        unlockIf(player, OriginType.SLIME, "perfect_gel",
                isUnlocked(player, OriginType.SLIME, "squeeze")
                        && isUnlocked(player, OriginType.SLIME, "fragment")
                        && state.progress("slime.airborne_ticks") >= 10 * 60 * 20);
        unlockIf(player, OriginType.SLIME, "size_control",
                isUnlocked(player, OriginType.SLIME, "perfect_gel")
                        && state.progress("slime.slimes_defeated") >= 50);
        unlockIf(player, OriginType.SLIME, "final",
                isUnlocked(player, OriginType.SLIME, "perfect_gel")
                        && isUnlocked(player, OriginType.SLIME, "size_control")
                        && state.progress("slime.large_slimes_defeated") >= 15);
    }

    private static boolean isUnlocked(ServerPlayerEntity player, OriginType origin, String skillId) {
        return PuffishSkillsIntegration.category(origin.categoryId())
                .flatMap(category -> category.getSkill(skillId))
                .map(skill -> skill.getState(player) == Skill.State.UNLOCKED)
                .orElse(false);
    }

    private static void unlockIf(ServerPlayerEntity player, OriginType origin, String skillId, boolean complete) {
        if (!complete) return;
        PuffishSkillsIntegration.category(origin.categoryId()).ifPresent(category ->
                category.getSkill(skillId).ifPresent(skill -> {
                    if (skill.getState(player) == Skill.State.UNLOCKED) return;
                    skill.unlock(player);
                    PuffishSkillsIntegration.invalidateRuntimeCache(player);
                    player.sendMessage(Text.literal("Origin Milestone Unlocked: ")
                                    .formatted(Formatting.GOLD)
                                    .append(skillIdToTitle(skillId).formatted(Formatting.YELLOW)), true);
                }));
    }

    private static MutableText skillIdToTitle(String skillId) {
        StringBuilder out = new StringBuilder();
        for (String part : skillId.split("_")) {
            if (part.isEmpty()) continue;
            if (!out.isEmpty()) out.append(' ');
            out.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return Text.literal(out.toString());
    }
}
