package org.marj4n.smooth_classes.origin;

import net.minecraft.advancement.Advancement;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.marj4n.smooth_classes.SmoothClasses;

/**
 * Grants real vanilla advancement toasts when an Origin objective node unlocks.
 * Stage advancements use TASK presentation, final evolutions use CHALLENGE.
 * Both are persisted by Minecraft's advancement tracker, so rejoining does not
 * replay notifications for milestones that have already been shown.
 */
public final class OriginAdvancementNotices {
    private static final String CRITERION = "earned";

    private OriginAdvancementNotices() {}

    public static void onOriginSelected(ServerPlayerEntity player, OriginType origin) {
        if (origin == null || !origin.isV1Playable()) return;
        ensureRoot(player, origin);
        grant(player, origin, "origin_base");
    }

    public static void onMilestoneUnlocked(ServerPlayerEntity player, OriginType origin, String skillId) {
        if (origin == null || !origin.isV1Playable()) return;
        ensureRoot(player, origin);
        grant(player, origin, skillId);
    }

    private static void ensureRoot(ServerPlayerEntity player, OriginType origin) {
        grant(player, origin, "root");
    }

    private static void grant(ServerPlayerEntity player, OriginType origin, String skillId) {
        MinecraftServer server = player.getServer();
        if (server == null) return;
        Identifier id = SmoothClasses.id("origins/" + origin.id() + "/" + skillId);
        Advancement advancement = server.getAdvancementLoader().get(id);
        if (advancement == null) return;
        var tracker = player.getAdvancementTracker();
        if (!tracker.getProgress(advancement).isDone()) {
            tracker.grantCriterion(advancement, CRITERION);
        }
    }
}
