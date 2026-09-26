package org.marj4n.smooth_classes.integration;

import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.puffish.skillsmod.api.SkillsAPI;
import net.puffish.skillsmod.api.Skill;
import org.marj4n.smooth_classes.SmoothClasses;
import org.marj4n.smooth_classes.network.SmoothClassesNetworking;

/** Server-authoritative feedback for skill purchases made through Puffish Skills. */
public final class PuffishSkillFeedback {
    private static final Identifier UNLOCK_SOUND_ID = SmoothClasses.id("fx_ui_unlock");

    private PuffishSkillFeedback() {}

    public static void register() {
        SkillsAPI.registerSkillUnlockEvent((player, categoryId, skillId) -> {
            if (!SmoothClasses.MOD_ID.equals(categoryId.getNamespace())) {
                return;
            }
            playUnlockSound(player);
            syncJunctions(player, categoryId, skillId);
            PuffishSkillsIntegration.ensureAscendancyUnlocked(player);
            SmoothClassesNetworking.sendAbilityState(player);
        });
    }

    private static void syncJunctions(ServerPlayerEntity player, Identifier categoryId, String skillId) {
        if (!PuffishSkillsIntegration.TREE.equals(categoryId)) return;
        String[] pair = switch (skillId) {
            case "p5czucfm7f3126hu", "o2is5ive25amfsq8" ->
                    new String[]{"p5czucfm7f3126hu", "o2is5ive25amfsq8"};
            case "fblggw09uwd4mk3d", "4jsg72vigi1qyj9j" ->
                    new String[]{"fblggw09uwd4mk3d", "4jsg72vigi1qyj9j"};
            default -> null;
        };
        if (pair == null) return;
        SkillsAPI.getCategory(categoryId).ifPresent(category -> {
            for (String node : pair) category.getSkill(node).ifPresent(skill -> {
                if (skill.getState(player) != Skill.State.UNLOCKED) skill.unlock(player);
            });
        });
    }

    private static void playUnlockSound(ServerPlayerEntity player) {
        SoundEvent sound = Registries.SOUND_EVENT.get(UNLOCK_SOUND_ID);
        player.getServerWorld().playSound(
                null,
                player.getBlockPos(),
                sound,
                SoundCategory.PLAYERS,
                0.85F,
                0.92F
        );
    }
}
