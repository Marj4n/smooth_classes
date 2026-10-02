package org.marj4n.smooth_classes.content.lancer.runtime;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.marj4n.smooth_classes.SmoothClasses;
import org.marj4n.smooth_classes.content.lancer.LancerClass;
import org.marj4n.smooth_classes.entity.LancerImpaleEntity;
import org.marj4n.smooth_classes.registry.SmoothEntities;
import org.marj4n.smooth_classes.runtime.AbilityCooldowns;
import org.marj4n.smooth_classes.runtime.AbilityRuntime;
import org.marj4n.smooth_classes.runtime.CombatRuntime;
import org.marj4n.smooth_classes.runtime.ExecutionResult;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Lancer H special: Dragoon-style leap followed by a second-press dive. */
public final class LancerSpecialRuntime {
    public static final Identifier ID = SmoothClasses.id("class_special_high_jump");
    public static final int COOLDOWN_TICKS = 16 * 20;
    private static final int LEAP_WINDOW_TICKS = 60;

    private static final class State {
        long startedAt;
        boolean diving;
        int airTicks;
        ItemStack spearVisual = ItemStack.EMPTY;
    }

    private static final Map<UUID, State> STATES = new HashMap<>();
    private LancerSpecialRuntime() {}

    public static ExecutionResult activate(ServerPlayerEntity player) {
        if (!AbilityRuntime.isClass(player, LancerClass.ID))
            return ExecutionResult.failure("Only Lancer can use High Jump.");
        if (!LancerRuntime.isSpear(player.getMainHandStack()))
            return ExecutionResult.failure("High Jump requires a spear in your main hand.");

        State current = STATES.get(player.getUuid());
        if (current != null) {
            if (!current.diving && !player.isOnGround()) {
                current.diving = true;
                Vec3d look = player.getRotationVec(1.0F).normalize();
                double horizontal = 1.25D;
                player.setVelocity(look.x * horizontal, Math.min(-1.45D, look.y * 1.2D - 0.9D), look.z * horizontal);
                player.velocityModified = true;
                player.fallDistance = 0F;
                diveFx(player);
                player.sendMessage(Text.literal("Dragoon Dive").formatted(Formatting.AQUA), true);
                return ExecutionResult.success(1, "High Jump converted into Dragoon Dive.");
            }
            return ExecutionResult.failure(current.diving ? "Dragoon Dive is already descending." : "Get airborne before diving.");
        }

        if (!player.isOnGround()) return ExecutionResult.failure("High Jump must start from the ground.");
        if (!AbilityCooldowns.ready(player, ID))
            return ExecutionResult.failure("High Jump can be used again in " + seconds(AbilityCooldowns.remainingTicks(player, ID)) + "s.");

        State state = new State();
        state.startedAt = player.getServer().getTicks();
        state.spearVisual = player.getMainHandStack().copy();
        state.spearVisual.setCount(1);
        STATES.put(player.getUuid(), state);
        AbilityCooldowns.start(player, ID, COOLDOWN_TICKS);

        Vec3d look = player.getRotationVec(1.0F).normalize();
        player.setVelocity(look.x * 0.34D, 1.42D, look.z * 0.34D);
        player.velocityModified = true;
        player.fallDistance = 0F;
        leapFx(player);
        player.sendMessage(Text.literal("High Jump — press H again in the air to dive.").formatted(Formatting.AQUA), true);
        return ExecutionResult.success(1, "High Jump launched the Lancer.");
    }

    public static void tick(ServerPlayerEntity player) {
        State state = STATES.get(player.getUuid());
        if (state == null) return;
        if (!AbilityRuntime.isClass(player, LancerClass.ID) || !player.isAlive()) {
            STATES.remove(player.getUuid());
            return;
        }

        state.airTicks++;
        player.fallDistance = 0F;
        long elapsed = player.getServer().getTicks() - state.startedAt;

        if (state.diving) {
            if ((player.age % 2) == 0) {
                player.getServerWorld().spawnParticles(ParticleTypes.CLOUD,
                        player.getX(), player.getBodyY(0.45D), player.getZ(), 4, 0.22D, 0.25D, 0.22D, 0.03D);
                player.getServerWorld().spawnParticles(ParticleTypes.CRIT,
                        player.getX(), player.getBodyY(0.4D), player.getZ(), 3, 0.18D, 0.2D, 0.18D, 0.02D);
            }
            if (state.airTicks > 4 && player.isOnGround()) {
                impact(player, state);
                STATES.remove(player.getUuid());
            } else if (elapsed > 90L) {
                STATES.remove(player.getUuid());
            }
            return;
        }

        if (state.airTicks > 8 && player.isOnGround()) {
            STATES.remove(player.getUuid());
            return;
        }
        if (elapsed > LEAP_WINDOW_TICKS) STATES.remove(player.getUuid());
    }

    private static void impact(ServerPlayerEntity player, State state) {
        float damage = (float) Math.max(4.0D, player.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE) * 1.65D);
        int hits = 0;
        for (LivingEntity target : CombatRuntime.nearbyEnemies(player, 4.0D)) {
            target.timeUntilRegen = 0;
            if (target.damage(player.getDamageSources().playerAttack(player), damage)) hits++;
            Vec3d away = target.getPos().subtract(player.getPos());
            if (away.lengthSquared() > 0.001D) {
                Vec3d n = away.normalize();
                target.addVelocity(n.x * 0.65D, 0.35D, n.z * 0.65D);
            }
        }
        LancerRuntime.grantSpecialMomentum(player, hits > 0 ? 12 : 4);
        var world = player.getServerWorld();
        spawnSpearEruption(player, state.spearVisual);
        world.spawnParticles(ParticleTypes.EXPLOSION, player.getX(), player.getY() + 0.15D, player.getZ(), 4, 1.25D, 0.15D, 1.25D, 0.0D);
        world.spawnParticles(ParticleTypes.CRIT, player.getX(), player.getY() + 0.2D, player.getZ(), 42, 1.8D, 0.35D, 1.8D, 0.12D);
        world.playSound(null, player.getBlockPos(), SoundEvents.ITEM_TRIDENT_THUNDER, SoundCategory.PLAYERS, 0.75F, 1.3F);
        player.sendMessage(Text.literal("Dragoon Impact" + (hits > 0 ? " — " + hits + " hit" : "")).formatted(Formatting.AQUA), true);
    }

    private static void spawnSpearEruption(ServerPlayerEntity player, ItemStack visualStack) {
        var world = player.getServerWorld();
        if (visualStack == null || visualStack.isEmpty()) return;

        // Golden-angle placement keeps the trap spread across the whole AOE instead
        // of stacking a dozen copies on top of each other.
        final int count = 14;
        final double goldenAngle = 2.399963229728653D;
        double phase = player.getRandom().nextDouble() * Math.PI * 2D;
        for (int i = 0; i < count; i++) {
            double normalized = (i + 0.55D) / count;
            double radius = 0.55D + Math.sqrt(normalized) * 3.15D;
            double angle = phase + i * goldenAngle;
            double x = player.getX() + Math.cos(angle) * radius;
            double z = player.getZ() + Math.sin(angle) * radius;
            double surfaceY = surfaceY(player, x, z);

            LancerImpaleEntity spear = new LancerImpaleEntity(SmoothEntities.LANCER_IMPALE, world);
            spear.setVisualStack(visualStack);
            spear.setGroundSpike(true);
            spear.setPosition(x, surfaceY - 1.12D, z);
            spear.setYaw((float) Math.toDegrees(angle));
            spear.setPitch(-90.0F);
            world.spawnEntity(spear);

            world.spawnParticles(ParticleTypes.CRIT, x, surfaceY + 0.05D, z, 3, 0.10D, 0.03D, 0.10D, 0.02D);
        }
    }

    private static double surfaceY(ServerPlayerEntity player, double x, double z) {
        double fallback = player.getY();
        Vec3d start = new Vec3d(x, fallback + 2.4D, z);
        Vec3d end = new Vec3d(x, fallback - 3.2D, z);
        var hit = player.getServerWorld().raycast(new RaycastContext(
                start, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, player));
        return hit.getType() == net.minecraft.util.hit.HitResult.Type.MISS ? fallback : hit.getPos().y;
    }

    private static void leapFx(ServerPlayerEntity player) {
        var world = player.getServerWorld();
        world.spawnParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.15D, player.getZ(), 28, 0.7D, 0.15D, 0.7D, 0.08D);
        world.spawnParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 0.25D, player.getZ(), 12, 0.45D, 0.15D, 0.45D, 0.06D);
        world.playSound(null, player.getBlockPos(), SoundEvents.ITEM_TRIDENT_RIPTIDE_1, SoundCategory.PLAYERS, 0.8F, 1.25F);
    }

    private static void diveFx(ServerPlayerEntity player) {
        var world = player.getServerWorld();
        world.spawnParticles(ParticleTypes.FLASH, player.getX(), player.getBodyY(0.5D), player.getZ(), 1, 0, 0, 0, 0);
        world.playSound(null, player.getBlockPos(), SoundEvents.ITEM_TRIDENT_RIPTIDE_2, SoundCategory.PLAYERS, 0.75F, 1.5F);
    }

    public static boolean active(ServerPlayerEntity player) { return STATES.containsKey(player.getUuid()); }
    public static int variant(ServerPlayerEntity player) {
        State state = STATES.get(player.getUuid());
        return state == null ? 0 : state.diving ? 2 : 1;
    }
    public static long remainingTicks(ServerPlayerEntity player) { return AbilityCooldowns.remainingTicks(player, ID); }
    public static int cooldownTicks() { return COOLDOWN_TICKS; }
    public static void cleanup(ServerPlayerEntity player) { STATES.remove(player.getUuid()); }
    public static void clear() { STATES.clear(); }
    private static int seconds(long ticks) { return (int) Math.ceil(Math.max(0L, ticks) / 20.0D); }
}
