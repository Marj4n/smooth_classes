package org.marj4n.smooth_classes.integration;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

/**
 * Optional, server-side bridge for Smooth Progression's REAL RPG level.
 *
 * Smooth Progression 1.0.6 stores player levels in ProgressionManager and mob
 * levels as `smoothprogression.level.N` scoreboard tags. We use its own mob
 * load/scaling logic after a level change, so HP follows the existing configured
 * curve instead of stacking a second independent damage/health multiplier.
 * No hard Smooth Progression dependency is added to Smooth Classes.
 */
public final class SmoothProgressionSummonLevels {
    private static final String LEVEL_PREFIX = "smoothprogression.level.";
    private static final String ORIGINAL_LEVEL_PREFIX = "smooth_classes_avenger_original_level.";
    private static final UUID PROGRESSION_HEALTH_MODIFIER = UUID.fromString(
            "f6a2791b-857e-48c2-9ab9-df4e22b5f8d0");
    private static final UUID PROGRESSION_BOSS_HEALTH_MODIFIER = UUID.fromString(
            "2c9b713e-b6c1-4bfa-bd93-7b1ad9074d91");
    private static final boolean LOADED = FabricLoader.getInstance().isModLoaded("smooth_progression");

    private static boolean initialized;
    private static Method progressionGet;
    private static Method playerGetLevel;
    private static Method mobLevelReapply;
    private static Method bossLevelRefresh;

    private SmoothProgressionSummonLevels() {}

    /** Applies to any type of bound summon, including passive/modded and temporary Avenger minions. */
    public static void synchronize(ServerPlayerEntity owner, LivingEntity summon) {
        if (!LOADED || owner == null || summon == null || summon.getWorld().isClient || !summon.isAlive()) return;
        int level = getOwnerLevel(owner);
        if (level < 1) return;

        Set<String> tags = summon.getCommandTags();
        int previous = readLevel(tags);
        boolean singleCorrectTag = tags.contains(LEVEL_PREFIX + level);
        if (singleCorrectTag) {
            for (String tag : tags) {
                if (tag.startsWith(LEVEL_PREFIX) && !tag.equals(LEVEL_PREFIX + level)) {
                    singleCorrectTag = false;
                    break;
                }
            }
        }
        // A 1.0.5 world may already have the right scoreboard level but still
        // contain a *persistent* Smooth Progression boss HP modifier. Repair it
        // even when the level tag itself has not changed.
        if (singleCorrectTag && !hasStaleBossHealth(summon)) return;

        // Remember the normal area level before the FIRST binding, for Unbind.
        if (!hasOriginalLevel(tags)) summon.addCommandTag(ORIGINAL_LEVEL_PREFIX + previous);
        replaceLevel(summon, level);
        reapplyProgressionHealth(summon);
    }

    /** Called only when a manifested soul is unbound into an independent mob. */
    public static void restoreOnUnbind(LivingEntity summon) {
        if (!LOADED || summon == null || summon.getWorld().isClient) return;
        int originalLevel = -1;
        for (String tag : new ArrayList<>(summon.getCommandTags())) {
            if (!tag.startsWith(ORIGINAL_LEVEL_PREFIX)) continue;
            try {
                originalLevel = Integer.parseInt(tag.substring(ORIGINAL_LEVEL_PREFIX.length()));
            } catch (NumberFormatException ignored) {
                originalLevel = 0;
            }
            summon.removeScoreboardTag(tag);
        }
        if (originalLevel < 0 && !(summon instanceof WitherEntity)) return;
        // Older bound Withers may not have recorded their original level.
        // As an unbound boss, they must not keep the owner's level tag.
        replaceLevel(summon, Math.max(0, originalLevel));
        reapplyProgressionHealth(summon);
        // After the Avenger owner tags have been REMOVED, restore ordinary
        // Wither boss HP as well. Keep this optional via reflection.
        initialize();
        if (bossLevelRefresh != null && summon instanceof WitherEntity) {
            try { bossLevelRefresh.invoke(null, summon); }
            catch (ReflectiveOperationException | RuntimeException ignored) { /* no hard dependency */ }
        }
    }

    private static void initialize() {
        if (initialized) return;
        initialized = true;
        if (!LOADED) return;
        try {
            Class<?> manager = Class.forName("org.marj4n.smooth_progression.progression.ProgressionManager");
            Class<?> playerProgression = Class.forName("org.marj4n.smooth_progression.progression.PlayerProgression");
            progressionGet = manager.getMethod("get", ServerPlayerEntity.class);
            playerGetLevel = playerProgression.getMethod("getLevel");
        } catch (ReflectiveOperationException | LinkageError ignored) {
            // Cross-mod API unavailable: never crash summoning or assume vanilla XP level.
            progressionGet = null;
            playerGetLevel = null;
        }
        try {
            Class<?> manager = Class.forName("org.marj4n.smooth_progression.entity.MobLevelScaling");
            mobLevelReapply = manager.getDeclaredMethod("onEntityLoad", Entity.class, ServerWorld.class);
            mobLevelReapply.setAccessible(true);
        } catch (ReflectiveOperationException | LinkageError | SecurityException ignored) {
            mobLevelReapply = null;
        }
        try {
            Class<?> boss = Class.forName("org.marj4n.smooth_progression.boss.BossProgression");
            bossLevelRefresh = boss.getMethod("refreshEntity", Entity.class);
        } catch (ReflectiveOperationException | LinkageError ignored) {
            bossLevelRefresh = null;
        }
    }

    private static int getOwnerLevel(ServerPlayerEntity owner) {
        initialize();
        if (progressionGet == null || playerGetLevel == null) return 0;
        try {
            Object progression = progressionGet.invoke(null, owner);
            if (progression == null) return 0;
            Object value = playerGetLevel.invoke(progression);
            return value instanceof Number number ? Math.max(1, number.intValue()) : 0;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return 0;
        }
    }

    private static int readLevel(Set<String> tags) {
        for (String tag : tags) {
            if (!tag.startsWith(LEVEL_PREFIX)) continue;
            try {
                int level = Integer.parseInt(tag.substring(LEVEL_PREFIX.length()));
                if (level > 0) return level;
            } catch (NumberFormatException ignored) {
                // A modded or corrupted tag must not prevent synchronisation.
            }
        }
        return 0;
    }

    private static boolean hasOriginalLevel(Set<String> tags) {
        for (String tag : tags) if (tag.startsWith(ORIGINAL_LEVEL_PREFIX)) return true;
        return false;
    }

    private static void replaceLevel(LivingEntity summon, int level) {
        for (String tag : new ArrayList<>(summon.getCommandTags())) {
            if (tag.startsWith(LEVEL_PREFIX)) summon.removeScoreboardTag(tag);
        }
        if (level > 0) summon.addCommandTag(LEVEL_PREFIX + level);
    }

    private static boolean hasStaleBossHealth(LivingEntity entity) {
        if (!(entity instanceof WitherEntity)) return false;
        EntityAttributeInstance health = entity.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        return health != null && health.getModifier(PROGRESSION_BOSS_HEALTH_MODIFIER) != null;
    }

    private static void reapplyProgressionHealth(LivingEntity summon) {
        // Smooth Progression itself only scales hostile mobs. Passive souls receive
        // the level tag but retain their native + Avenger-talent health balance.
        if (!(summon instanceof HostileEntity hostile) || !(summon.getWorld() instanceof ServerWorld world)) return;
        initialize();
        if (mobLevelReapply == null) return;

        EntityAttributeInstance attribute = hostile.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (attribute == null) return;
        float previousMax = Math.max(1F, hostile.getMaxHealth());
        float healthFraction = Math.max(0F, Math.min(1F, hostile.getHealth() / previousMax));

        // The summoned Wither used to be categorized as a boss and may still
        // have a saved boss HP modifier. Strip ONLY Smooth Progression's known
        // modifier, preserving the health percentage and Avenger talent boosts.
        if (hasStaleBossHealth(summon)) {
            attribute.removeModifier(PROGRESSION_BOSS_HEALTH_MODIFIER);
        }

        // MobLevelScaling's on-load method will not replace an existing UUID,
        // so remove only its OWN modifier before recalculating with the new level.
        EntityAttributeModifier previousModifier = attribute.getModifier(PROGRESSION_HEALTH_MODIFIER);
        if (previousModifier != null) attribute.removeModifier(PROGRESSION_HEALTH_MODIFIER);
        try {
            mobLevelReapply.invoke(null, hostile, world);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // Preserve pre-existing HP scaling if a later Smooth Progression
            // release changes the private on-load method implementation.
            if (previousModifier != null && attribute.getModifier(PROGRESSION_HEALTH_MODIFIER) == null) {
                attribute.addPersistentModifier(previousModifier);
            }
        }
        hostile.setHealth(Math.max(0.01F, Math.min(hostile.getMaxHealth(), hostile.getMaxHealth() * healthFraction)));
    }
}
