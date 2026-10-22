package org.marj4n.smooth_classes.origin;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Authoritative blood portions for non-hostile mobs, including modded MobEntity types.
 * The portions are tracked to clients (for the bite reticle), persisted with the mob,
 * and regenerate only if the victim is left alive before the final portion is taken.
 */
public final class VampireBloodReserve {
    public static final int MAX_PIPS = 5;
    // Regeneration is intentionally slow. Taking the LAST portion is fatal.
    private static final long VILLAGER_REGEN = 75L * 20L;
    private static final long PASSIVE_REGEN = 55L * 20L;
    private static final TagKey<EntityType<?>> ALLOW = TagKey.of(RegistryKeys.ENTITY_TYPE,
            new Identifier("smooth_classes", "vampire_blood_sources"));
    private static final TagKey<EntityType<?>> DENY = TagKey.of(RegistryKeys.ENTITY_TYPE,
            new Identifier("smooth_classes", "vampire_blood_excluded"));

    private VampireBloodReserve() {}

    public interface Holder {
        /** This value is backed by entity tracked data and synchronizes to clients. */
        int smooth_classes$getBloodPips();
        long smooth_classes$getBloodUpdatedAt();
        void smooth_classes$setBloodReserve(int pips, long timestamp);
    }

    public static boolean isFeedable(LivingEntity target) {
        if (!target.isAlive() || target.getType().isIn(DENY)) return false;
        if (target.getType().isIn(ALLOW)) return true;
        return target instanceof MobEntity && !(target instanceof HostileEntity);
    }

    public static int available(LivingEntity target) {
        if (!(target instanceof Holder holder) || !isFeedable(target)) return 0;
        int count = Math.max(0, Math.min(MAX_PIPS, holder.smooth_classes$getBloodPips()));
        // Client is view-only: the tracked number is delivered by the server and
        // must not be mutated locally when the crosshair HUD queries it.
        if (target.getWorld().isClient) return count;

        long now = target.getWorld().getTime();
        long last = holder.smooth_classes$getBloodUpdatedAt();
        if (last <= 0L || last > now) {
            holder.smooth_classes$setBloodReserve(count, now);
            return count;
        }
        if (count >= MAX_PIPS) return count;

        long period = target instanceof VillagerEntity ? VILLAGER_REGEN : PASSIVE_REGEN;
        long gained = (now - last) / period;
        if (gained > 0L) {
            count = (int) Math.min(MAX_PIPS, count + gained);
            // Carry leftover time so a partially depleted animal can continue
            // recovering at the same interval, independent of feeding attempts.
            holder.smooth_classes$setBloodReserve(count,
                    count == MAX_PIPS ? now : last + gained * period);
        }
        return count;
    }

    /**
     * Drain one real portion. The player gets credit for the kill when the last
     * portion is taken. A partially drained survivor can regenerate over time.
     */
    public static boolean consume(LivingEntity target, ServerPlayerEntity vampire) {
        if (target.getWorld().isClient || !isFeedable(target)
                || !(target instanceof Holder holder)) return false;
        int pips = available(target);
        if (pips <= 0) return false;

        int remaining = pips - 1;
        holder.smooth_classes$setBloodReserve(remaining, target.getWorld().getTime());
        if (remaining == 0) {
            // Real damage rather than Entity.discard/kill: loot, death events and
            // player attribution remain consistent with Minecraft combat.
            target.damage(vampire.getDamageSources().playerAttack(vampire), Float.MAX_VALUE);
        }
        return true;
    }
}
