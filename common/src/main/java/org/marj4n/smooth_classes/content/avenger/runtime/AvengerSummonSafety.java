package org.marj4n.smooth_classes.content.avenger.runtime;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.BossBar;
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
    // Dragon's boss bar lives in EnderDragonFight rather than on the entity.
    // All other entities use their actual inherited/generic boss-bar fields.
    private static final Set<String> BLOCKED = Set.of("minecraft:ender_dragon");
    private static final Map<Class<?>, Boolean> BOSS_BAR_CACHE = new ConcurrentHashMap<>();

    private AvengerSummonSafety() {}

    public static boolean bannedId(String entityId) {
        return entityId == null || BLOCKED.contains(entityId);
    }

    public static boolean allowed(LivingEntity entity) {
        if (entity == null) return false;
        String id =
                org.marj4n.smooth_classes.platform.PlatformRegistries.ENTITY_TYPE
                        .getId(entity.getType())
                        .toString();
        if ("minecraft:wither".equals(id)) return true;
        if (bannedId(id)) return false;
        // Content tags also classify high-health mobs such as Warden/Elder Guardian
        // as "bosses" even though they have no boss bar. Only an actual bar counts.
        return !BOSS_BAR_CACHE.computeIfAbsent(
                entity.getClass(), AvengerSummonSafety::hasBossBarField);
    }

    private static boolean hasBossBarField(Class<?> entityClass) {
        try {
            for (Class<?> cls = entityClass;
                    cls != null && cls != Object.class;
                    cls = cls.getSuperclass()) {
                for (Field field : cls.getDeclaredFields()) {
                    if (BossBar.class.isAssignableFrom(field.getType())) return true;
                    if (bossBarType(field.getGenericType())) return true;
                }
            }
        } catch (SecurityException ignored) {
            // Block ambiguous entities rather than potentially allowing a boss bar.
            return true;
        }
        return false;
    }

    private static boolean bossBarType(java.lang.reflect.Type type) {
        if (type instanceof Class<?> cls) return BossBar.class.isAssignableFrom(cls);
        if (type instanceof java.lang.reflect.ParameterizedType parameterized) {
            for (var argument : parameterized.getActualTypeArguments())
                if (bossBarType(argument)) return true;
        }
        if (type instanceof java.lang.reflect.WildcardType wildcard) {
            for (var upper : wildcard.getUpperBounds()) if (bossBarType(upper)) return true;
        }
        if (type instanceof java.lang.reflect.GenericArrayType array)
            return bossBarType(array.getGenericComponentType());
        return false;
    }
}
