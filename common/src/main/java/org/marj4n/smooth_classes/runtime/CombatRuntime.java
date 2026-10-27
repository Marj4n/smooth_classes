package org.marj4n.smooth_classes.runtime;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.List;

/** Shared, server-authoritative combat primitives used by every class runtime. */
public final class CombatRuntime {
    private CombatRuntime() {}

    public static List<LivingEntity> nearbyEnemies(ServerPlayerEntity player, double radius) {
        return player.getWorld().getEntitiesByClass(
                LivingEntity.class,
                player.getBoundingBox().expand(radius),
                entity -> entity != player && entity.isAlive() && !player.isTeammate(entity)
        );
    }

    public static int damageNearby(ServerPlayerEntity player, double radius, float damage) {
        int hits = 0;
        for (LivingEntity target : nearbyEnemies(player, radius)) {
            if (target.damage(player.getDamageSources().playerAttack(player), damage)) hits++;
        }
        return hits;
    }

    public static int damageNearbyWithEffect(ServerPlayerEntity player, double radius, float damage,
                                             StatusEffect effect, int duration, int amplifier) {
        int hits = 0;
        for (LivingEntity target : nearbyEnemies(player, radius)) {
            if (target.damage(player.getDamageSources().playerAttack(player), damage)) hits++;
            target.addStatusEffect(new StatusEffectInstance(effect, duration, amplifier));
        }
        return hits;
    }

    public static void buff(ServerPlayerEntity player, StatusEffect effect, int duration, int amplifier) {
        player.addStatusEffect(new StatusEffectInstance(effect, duration, amplifier));
    }

    public static void heal(ServerPlayerEntity player, float amount) {
        player.heal(Math.max(0.0F, amount));
    }

    public static void launchForward(ServerPlayerEntity player, double horizontal, double vertical) {
        Vec3d look = player.getRotationVec(1.0F).normalize();
        player.setVelocity(look.x * horizontal, vertical, look.z * horizontal);
    }

    public static void launchBackward(ServerPlayerEntity player, double horizontal, double vertical) {
        Vec3d look = player.getRotationVec(1.0F).normalize();
        player.setVelocity(-look.x * horizontal, vertical, -look.z * horizontal);
    }
}
