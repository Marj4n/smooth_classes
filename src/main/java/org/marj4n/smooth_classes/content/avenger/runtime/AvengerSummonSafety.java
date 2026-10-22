package org.marj4n.smooth_classes.content.avenger.runtime;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.registry.Registries;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Boss-bar protection for the Death List. Only Wither is allowed.
 * Checks known monster boss IDs and the presence of a ServerBossBar field
 * in the runtime entity type (including subclasses such as ACBossEvent).
 * Does NOT reject ordinary high-health mobs or minibosses without a boss bar.
 */
public final class AvengerSummonSafety {
    private static final Set<String> BLOCKED = Set.of(
            "minecraft:ender_dragon",
            "alexscaves:luxtructosaurus", "alexscaves:tremorzilla", "alexscaves:hullbreaker",
            "alexsmobs:void_worm", "alexsmobs:warped_mosco",
            "illagerinvasion:invoker"
    );
    private static final Map<Class<?>, Boolean> BOSS_BAR_CACHE = new ConcurrentHashMap<>();

    private AvengerSummonSafety() {}

    public static boolean bannedId(String entityId) {
        return entityId == null || BLOCKED.contains(entityId);
    }

    public static boolean allowed(LivingEntity entity) {
        if (entity == null) return false;
        String id = Registries.ENTITY_TYPE.getId(entity.getType()).toString();
        if ("minecraft:wither".equals(id)) return true;
        if (bannedId(id)) return false;
        return !BOSS_BAR_CACHE.computeIfAbsent(entity.getClass(), AvengerSummonSafety::hasBossBarField);
    }

    private static boolean hasBossBarField(Class<?> entityClass) {
        try {
            for (Class<?> cls = entityClass; cls != null && cls != Object.class; cls = cls.getSuperclass()) {
                for (Field field : cls.getDeclaredFields()) {
                    if (ServerBossBar.class.isAssignableFrom(field.getType())) return true;
                }
            }
        } catch (SecurityException ignored) {
            // Block ambiguous entities rather than potentially allowing a boss bar.
            return true;
        }
        return false;
    }
}
