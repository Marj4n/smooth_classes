package org.marj4n.smooth_classes.content.lancer.runtime;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Ownable;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.marj4n.smooth_classes.SmoothClasses;
import org.marj4n.smooth_classes.content.lancer.LancerClass;
import org.marj4n.smooth_classes.content.lancer.LancerContent;
import org.marj4n.smooth_classes.effects.SmoothEffects;
import org.marj4n.smooth_classes.entity.LancerImpaleEntity;
import org.marj4n.smooth_classes.registry.SmoothEntities;
import org.marj4n.smooth_classes.runtime.AbilityRuntime;
import org.marj4n.smooth_classes.runtime.ExecutionResult;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Server-authoritative Lancer passive and signature runtime. */
public final class LancerRuntime {
    public static final int BASE_MOMENTUM_PERCENT = 40;
    public static final int MAX_MOMENTUM_PERCENT = 400;
    private static final int BASE_HIT_GAIN = 2;
    private static final int BASE_KILL_GAIN = 10;

    private static final TagKey<Item> LANCER_SPEARS = TagKey.of(RegistryKeys.ITEM, SmoothClasses.id("lancer_spears"));
    private static final UUID ATTACK_DAMAGE_MODIFIER = UUID.fromString("26e16a71-8273-4d20-82e0-8ff04e08f08b");
    private static final UUID MOVE_SPEED_MODIFIER = UUID.fromString("cf2f66b9-6d63-4ad4-8652-63682d18cc95");
    private static final UUID KNOCKBACK_MODIFIER = UUID.fromString("e1981f91-d193-432d-bfa4-e8a4184fe46d");

    private static final Map<UUID, Integer> MOMENTUM = new HashMap<>();
    private static final Map<UUID, ThrustSession> THRUSTS = new HashMap<>();
    private static final Map<UUID, StormSession> STORMS = new HashMap<>();
    private static final List<ImpaleVisual> IMPALES = new ArrayList<>();

    private LancerRuntime() {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            tickThrusts();
            tickStorms();
            tickImpales();
            if ((server.getTicks() % 10) == 0) {
                for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) tickPassiveAttributes(player);
            }
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> cleanupPlayer(handler.player, true));
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) cleanupPlayer(player, true);
            clearVisuals();
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            MOMENTUM.clear();
            THRUSTS.clear();
            STORMS.clear();
            IMPALES.clear();
        });
    }

    // ---------------------------------------------------------------------
    // Spear recognition
    // ---------------------------------------------------------------------

    public static boolean isSpear(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (stack.isOf(Items.TRIDENT) || stack.isIn(LANCER_SPEARS)) return true;

        Identifier id = Registries.ITEM.getId(stack.getItem());
        String path = id.getPath().toLowerCase(Locale.ROOT);
        if (containsWeaponWord(path, "spear") || containsWeaponWord(path, "lance")
                || containsWeaponWord(path, "pike") || containsWeaponWord(path, "javelin")) return true;

        return BetterCombatBridge.isSpear(stack);
    }

    private static boolean containsWeaponWord(String path, String word) {
        return path.equals(word) || path.startsWith(word + "_") || path.endsWith("_" + word)
                || path.contains("_" + word + "_") || path.contains("-" + word) || path.contains(word + "-");
    }

    private static final class BetterCombatBridge {
        private static boolean initialized;
        private static Method getAttributes;
        private static Method category;
        private static Method pose;
        private static Method attacks;
        private static Method attackAnimation;

        private static void init() {
            if (initialized) return;
            initialized = true;
            try {
                Class<?> registry = Class.forName("net.bettercombat.logic.WeaponRegistry");
                Class<?> attributes = Class.forName("net.bettercombat.api.WeaponAttributes");
                Class<?> attack = Class.forName("net.bettercombat.api.WeaponAttributes$Attack");
                getAttributes = registry.getMethod("getAttributes", ItemStack.class);
                category = attributes.getMethod("category");
                pose = attributes.getMethod("pose");
                attacks = attributes.getMethod("attacks");
                attackAnimation = attack.getMethod("animation");
            } catch (ReflectiveOperationException | LinkageError ignored) {
                getAttributes = null;
            }
        }

        static boolean isSpear(ItemStack stack) {
            init();
            if (getAttributes == null) return false;
            try {
                Object attributesObj = getAttributes.invoke(null, stack);
                if (attributesObj == null) return false;
                if (looksLikeSpear((String) category.invoke(attributesObj))) return true;
                if (looksLikeSpear((String) pose.invoke(attributesObj))) return true;
                Object attackArray = attacks.invoke(attributesObj);
                if (attackArray != null && attackArray.getClass().isArray()) {
                    int length = Array.getLength(attackArray);
                    for (int i = 0; i < length; i++) {
                        Object attackObj = Array.get(attackArray, i);
                        if (attackObj != null && looksLikeSpear((String) attackAnimation.invoke(attackObj))) return true;
                    }
                }
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                return false;
            }
            return false;
        }

        private static boolean looksLikeSpear(String value) {
            if (value == null) return false;
            String v = value.toLowerCase(Locale.ROOT);
            return v.contains("spear") || v.contains("lance") || v.contains("pike") || v.contains("javelin");
        }
    }

    // ---------------------------------------------------------------------
    // Momentum passive
    // ---------------------------------------------------------------------

    public static int momentumPercent(ServerPlayerEntity player) {
        if (!AbilityRuntime.isClass(player, LancerClass.ID)) return BASE_MOMENTUM_PERCENT;
        return MOMENTUM.computeIfAbsent(player.getUuid(), ignored -> BASE_MOMENTUM_PERCENT);
    }

    public static float momentumMultiplier(ServerPlayerEntity player) {
        int percent = momentumPercent(player);
        float multiplier = 1.0F + percent / 100.0F;
        if (percent >= MAX_MOMENTUM_PERCENT && has(player, LancerContent.PERFECT_MOMENTUM)) multiplier += 0.10F;
        return multiplier;
    }

    public static boolean prepareDirectSpearAttack(ServerPlayerEntity player) {
        removeAttackModifier(player);
        if (!AbilityRuntime.isClass(player, LancerClass.ID) || !isSpear(player.getMainHandStack())) return false;
        EntityAttributeInstance attack = player.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE);
        if (attack == null) return false;
        double bonus = momentumPercent(player) / 100.0D;
        if (momentumPercent(player) >= MAX_MOMENTUM_PERCENT && has(player, LancerContent.PERFECT_MOMENTUM)) bonus += 0.10D;
        attack.addTemporaryModifier(new EntityAttributeModifier(
                ATTACK_DAMAGE_MODIFIER, "Smooth Classes Lancer Momentum", bonus,
                EntityAttributeModifier.Operation.MULTIPLY_TOTAL));
        return true;
    }

    public static void finishDirectSpearAttack(ServerPlayerEntity player, boolean prepared, boolean landed) {
        removeAttackModifier(player);
        if (prepared && landed && player.isAlive() && AbilityRuntime.isClass(player, LancerClass.ID)) {
            addMomentum(player, hitGain(player), "Spear hit");
        }
    }

    private static void removeAttackModifier(ServerPlayerEntity player) {
        EntityAttributeInstance attack = player.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE);
        if (attack != null) attack.removeModifier(ATTACK_DAMAGE_MODIFIER);
    }

    public static void onKill(ServerPlayerEntity player, LivingEntity victim) {
        if (!AbilityRuntime.isClass(player, LancerClass.ID) || !isSpear(player.getMainHandStack())) return;
        addMomentum(player, killGain(player), "Kill");
        float healPercent = has(player, LancerContent.BATTLE_RECOVERY_II) ? 0.06F
                : has(player, LancerContent.BATTLE_RECOVERY_I) ? 0.03F : 0F;
        if (healPercent > 0F) player.heal(player.getMaxHealth() * healPercent);
    }

    public static void onDeath(ServerPlayerEntity player) {
        MOMENTUM.put(player.getUuid(), BASE_MOMENTUM_PERCENT);
        removePassiveAttributes(player);
        removeAttackModifier(player);
        THRUSTS.remove(player.getUuid());
        STORMS.remove(player.getUuid());
    }

    private static int hitGain(ServerPlayerEntity player) {
        int gain = BASE_HIT_GAIN;
        if (has(player, LancerContent.MOMENTUM_TRAINING_I)) gain++;
        if (has(player, LancerContent.MOMENTUM_TRAINING_II)) gain++;
        return gain;
    }

    private static int killGain(ServerPlayerEntity player) {
        return BASE_KILL_GAIN + (has(player, LancerContent.EXECUTION_RHYTHM) ? 5 : 0);
    }

    private static void addMomentum(ServerPlayerEntity player, int amount, String reason) {
        int old = momentumPercent(player);
        int next = Math.min(MAX_MOMENTUM_PERCENT, old + Math.max(0, amount));
        MOMENTUM.put(player.getUuid(), next);
        if (next != old) {
            player.sendMessage(Text.literal("Lancer Momentum: +" + next + "%").formatted(
                    next >= MAX_MOMENTUM_PERCENT ? Formatting.GOLD : Formatting.AQUA), true);
        }
    }

    private static void tickPassiveAttributes(ServerPlayerEntity player) {
        if (!AbilityRuntime.isClass(player, LancerClass.ID)) {
            MOMENTUM.remove(player.getUuid());
            removePassiveAttributes(player);
            return;
        }
        if (!isSpear(player.getMainHandStack())) {
            removePassiveAttributes(player);
            return;
        }

        EntityAttributeInstance move = player.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (move != null) {
            move.removeModifier(MOVE_SPEED_MODIFIER);
            double amount = has(player, LancerContent.FLEET_II) ? 0.10D : has(player, LancerContent.FLEET_I) ? 0.05D : 0D;
            if (amount > 0D) move.addTemporaryModifier(new EntityAttributeModifier(
                    MOVE_SPEED_MODIFIER, "Smooth Classes Fleet Lancer", amount,
                    EntityAttributeModifier.Operation.MULTIPLY_TOTAL));
        }
        EntityAttributeInstance knockback = player.getAttributeInstance(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE);
        if (knockback != null) {
            knockback.removeModifier(KNOCKBACK_MODIFIER);
            if (has(player, LancerContent.IRON_GRIP)) knockback.addTemporaryModifier(new EntityAttributeModifier(
                    KNOCKBACK_MODIFIER, "Smooth Classes Iron Grip", 0.20D,
                    EntityAttributeModifier.Operation.ADDITION));
        }
    }

    private static void removePassiveAttributes(ServerPlayerEntity player) {
        EntityAttributeInstance move = player.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (move != null) move.removeModifier(MOVE_SPEED_MODIFIER);
        EntityAttributeInstance knockback = player.getAttributeInstance(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE);
        if (knockback != null) knockback.removeModifier(KNOCKBACK_MODIFIER);
    }

    // ---------------------------------------------------------------------
    // Signature: Impaling Volley
    // ---------------------------------------------------------------------

    public static ExecutionResult executeImpalingVolley(ServerPlayerEntity player) {
        ExecutionResult requirement = requireSpear(player);
        if (requirement != null) return requirement;

        double range = 18D;
        LivingEntity selectedPrimary = aimedEnemy(player, range);
        if (selectedPrimary == null) selectedPrimary = nearestEnemy(player, 8D);
        if (selectedPrimary == null) return ExecutionResult.failure("Impaling Volley requires a living target within 18 blocks (or 8 blocks around you).");
        final LivingEntity primary = selectedPrimary;

        int cap = has(player, LancerContent.VOLLEY_NINE) ? 9
                : has(player, LancerContent.VOLLEY_SEVEN) ? 7
                : has(player, LancerContent.VOLLEY_FIVE) ? 5 : 3;

        List<LivingEntity> targets = new ArrayList<>();
        targets.add(primary);
        List<LivingEntity> nearby = player.getServerWorld().getEntitiesByClass(
                LivingEntity.class,
                primary.getBoundingBox().expand(10D),
                e -> enemy(player, e) && e != primary
        );
        nearby.sort(Comparator.comparingDouble(primary::squaredDistanceTo));
        for (LivingEntity target : nearby) {
            if (targets.size() >= cap) break;
            if (target.squaredDistanceTo(player) <= range * range) targets.add(target);
        }

        float damage = abilityDamage(player, 1.0F);
        int hits = 0;
        for (LivingEntity target : targets) {
            if (damageAbility(player, target, damage)) hits++;
            spawnSixImpales(player, target);
        }
        player.getServerWorld().playSound(null, player.getBlockPos(), SoundEvents.ITEM_TRIDENT_THROW,
                SoundCategory.PLAYERS, 1.0F, 1.15F);
        return hits > 0 ? ExecutionResult.success(hits, "Impaling Volley (" + hits + "/" + cap + " targets, Momentum +" + momentumPercent(player) + "%)")
                : ExecutionResult.failure("Impaling Volley found targets, but none could be damaged.");
    }

    private static LivingEntity aimedEnemy(ServerPlayerEntity player, double range) {
        Vec3d eye = player.getEyePos();
        Vec3d end = eye.add(player.getRotationVec(1F).normalize().multiply(range));
        LivingEntity selected = null;
        double best = Double.MAX_VALUE;
        Box scan = player.getBoundingBox().stretch(end.subtract(eye)).expand(1.25D);
        for (LivingEntity target : player.getServerWorld().getEntitiesByClass(LivingEntity.class, scan, e -> enemy(player, e))) {
            var hit = target.getBoundingBox().expand(0.35D).raycast(eye, end);
            if (hit.isEmpty()) continue;
            double dist = eye.squaredDistanceTo(hit.get());
            if (dist < best) { best = dist; selected = target; }
        }
        return selected;
    }

    private static LivingEntity nearestEnemy(ServerPlayerEntity player, double range) {
        return player.getServerWorld().getEntitiesByClass(LivingEntity.class, player.getBoundingBox().expand(range), e -> enemy(player, e))
                .stream().min(Comparator.comparingDouble(player::squaredDistanceTo)).orElse(null);
    }

    private static void spawnSixImpales(ServerPlayerEntity player, LivingEntity target) {
        ServerWorld world = player.getServerWorld();
        long startedAt = world.getTime();
        long expires = startedAt + 18L;
        ItemStack visualStack = player.getMainHandStack().copy();
        if (!visualStack.isEmpty()) visualStack.setCount(1);

        for (int i = 0; i < 6; i++) {
            double angle = Math.PI * 2D * i / 6D + (i % 2 == 0 ? 0D : 0.18D);
            double y = ((i % 3) - 1) * 0.31D;
            LancerImpaleEntity spear = new LancerImpaleEntity(SmoothEntities.LANCER_IMPALE, world);
            spear.setVisualStack(visualStack);
            positionImpale(spear, target, angle, y, 0.0F);
            if (world.spawnEntity(spear)) {
                IMPALES.add(new ImpaleVisual(target, spear, angle, y, startedAt, expires));
            }
        }

        Vec3d c = target.getPos().add(0D, target.getHeight() * 0.55D, 0D);
        world.spawnParticles(ParticleTypes.CRIT, c.x, c.y, c.z, 18, 0.45D, 0.6D, 0.45D, 0.05D);
        world.spawnParticles(ParticleTypes.END_ROD, c.x, c.y, c.z, 8, 0.35D, 0.45D, 0.35D, 0.02D);
        world.playSound(null, target.getBlockPos(), SoundEvents.ITEM_TRIDENT_HIT, SoundCategory.PLAYERS, 0.75F, 1.25F);
    }

    /**
     * progress 0 = spear starts well outside the victim.
     * progress 1 = spear centre is slightly past the victim centre, so the shaft visibly crosses the body.
     */
    private static void positionImpale(
            LancerImpaleEntity spear,
            LivingEntity target,
            double angle,
            double yOffset,
            float progress
    ) {
        Vec3d center = target.getPos().add(0D, target.getHeight() * 0.55D + yOffset, 0D);
        Vec3d outward = new Vec3d(Math.cos(angle), 0D, Math.sin(angle)).normalize();

        // Slam from 2.35 blocks away to 0.12 blocks PAST the target centre.
        // That final negative radius is intentional: it makes the spear visibly pierce through the body.
        double eased = 1.0D - Math.pow(1.0D - MathHelper.clamp(progress, 0F, 1F), 3.0D);
        double radius = 2.35D + (-0.12D - 2.35D) * eased;
        Vec3d pos = center.add(outward.multiply(radius));
        Vec3d travel = outward.negate();

        double horizontal = Math.sqrt(travel.x * travel.x + travel.z * travel.z);
        float yaw = (float) (MathHelper.atan2(travel.x, travel.z) * 180D / Math.PI);
        float pitch = (float) (-MathHelper.atan2(travel.y, horizontal) * 180D / Math.PI);
        spear.refreshPositionAndAngles(pos.x, pos.y, pos.z, yaw, pitch);
        spear.setVelocity(Vec3d.ZERO);
    }

    private static void tickImpales() {
        Iterator<ImpaleVisual> it = IMPALES.iterator();
        while (it.hasNext()) {
            ImpaleVisual visual = it.next();
            ServerWorld world = visual.target.getWorld() instanceof ServerWorld sw ? sw : null;
            if (world == null || world.getTime() >= visual.expiresAt || !visual.target.isAlive() || visual.spear.isRemoved()) {
                if (!visual.spear.isRemoved()) visual.spear.discard();
                it.remove();
                continue;
            }
            long elapsed = Math.max(0L, world.getTime() - visual.startedAt);
            float progress = MathHelper.clamp(elapsed / 4.0F, 0F, 1F);
            positionImpale(visual.spear, visual.target, visual.angle, visual.yOffset, progress);
        }
    }

    private record ImpaleVisual(
            LivingEntity target,
            LancerImpaleEntity spear,
            double angle,
            double yOffset,
            long startedAt,
            long expiresAt
    ) {}

    // ---------------------------------------------------------------------
    // Signature: Dragon Thrust
    // ---------------------------------------------------------------------

    public static ExecutionResult executeDragonThrust(ServerPlayerEntity player) {
        ExecutionResult requirement = requireSpear(player);
        if (requirement != null) return requirement;
        if (THRUSTS.containsKey(player.getUuid())) return ExecutionResult.failure("Dragon Thrust is already active.");

        Vec3d direction = player.getRotationVec(1F).normalize();
        int ticks = has(player, LancerContent.THRUST_REACH) ? 14 : 9;
        boolean pin = has(player, LancerContent.THRUST_PIN);
        boolean breaker = has(player, LancerContent.THRUST_BREAKER);
        THRUSTS.put(player.getUuid(), new ThrustSession(player, direction, ticks, pin, breaker));
        player.getServerWorld().playSound(null, player.getBlockPos(), SoundEvents.ITEM_TRIDENT_RIPTIDE_1,
                SoundCategory.PLAYERS, 0.9F, 1.15F);
        return ExecutionResult.success(0, "Dragon Thrust");
    }

    private static void tickThrusts() {
        Iterator<Map.Entry<UUID, ThrustSession>> iterator = THRUSTS.entrySet().iterator();
        while (iterator.hasNext()) {
            ThrustSession session = iterator.next().getValue();
            ServerPlayerEntity player = session.player;
            if (!player.isAlive() || player.isRemoved() || !AbilityRuntime.isClass(player, LancerClass.ID)
                    || !isSpear(player.getMainHandStack()) || session.ticksLeft-- <= 0) {
                iterator.remove();
                continue;
            }

            double speed = session.breaker ? 1.52D : 1.34D;
            player.setVelocity(session.direction.multiply(speed));
            player.velocityModified = true;
            player.fallDistance = 0F;
            ServerWorld world = player.getServerWorld();
            world.spawnParticles(ParticleTypes.CLOUD, player.getX(), player.getBodyY(0.45D), player.getZ(),
                    5, 0.28D, 0.28D, 0.28D, 0.025D);

            float damage = abilityDamage(player, session.breaker ? 1.75F : 1.35F);
            for (LivingEntity target : world.getEntitiesByClass(LivingEntity.class,
                    player.getBoundingBox().expand(1.25D), e -> enemy(player, e) && !session.hit.contains(e.getUuid()))) {
                session.hit.add(target.getUuid());
                damageAbility(player, target, damage);
                if (session.pin) target.addStatusEffect(new StatusEffectInstance(SmoothEffects.IMMOBILIZE, 30, 0));
                Vec3d push = session.direction.multiply(session.breaker ? 0.85D : 0.45D);
                target.addVelocity(push.x, session.breaker ? 0.30D : 0.16D, push.z);
                world.spawnParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getBodyY(0.5D), target.getZ(), 1,
                        0.05D, 0.05D, 0.05D, 0D);
            }
        }
    }

    private static final class ThrustSession {
        final ServerPlayerEntity player;
        final Vec3d direction;
        int ticksLeft;
        final boolean pin;
        final boolean breaker;
        final Set<UUID> hit = new HashSet<>();
        ThrustSession(ServerPlayerEntity player, Vec3d direction, int ticksLeft, boolean pin, boolean breaker) {
            this.player = player; this.direction = direction; this.ticksLeft = ticksLeft; this.pin = pin; this.breaker = breaker;
        }
    }

    // ---------------------------------------------------------------------
    // Signature: Spearstorm
    // ---------------------------------------------------------------------

    public static ExecutionResult executeSpearstorm(ServerPlayerEntity player) {
        ExecutionResult requirement = requireSpear(player);
        if (requirement != null) return requirement;
        if (STORMS.containsKey(player.getUuid())) return ExecutionResult.failure("Spearstorm is already active.");

        int pulses = has(player, LancerContent.STORM_PULSES) ? 5 : 3;
        double radius = has(player, LancerContent.STORM_RADIUS) ? 6.0D : 4.5D;
        boolean vortex = has(player, LancerContent.STORM_VORTEX);
        STORMS.put(player.getUuid(), new StormSession(player, pulses, radius, vortex));
        player.getServerWorld().playSound(null, player.getBlockPos(), SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP,
                SoundCategory.PLAYERS, 1.0F, 0.85F);
        return ExecutionResult.success(0, "Spearstorm");
    }

    private static void tickStorms() {
        Iterator<Map.Entry<UUID, StormSession>> iterator = STORMS.entrySet().iterator();
        while (iterator.hasNext()) {
            StormSession session = iterator.next().getValue();
            ServerPlayerEntity player = session.player;
            if (!player.isAlive() || player.isRemoved() || !AbilityRuntime.isClass(player, LancerClass.ID)
                    || !isSpear(player.getMainHandStack())) {
                iterator.remove();
                continue;
            }
            session.tick++;
            if ((session.tick % 4) != 1) continue;

            session.pulsesDone++;
            ServerWorld world = player.getServerWorld();
            float damage = abilityDamage(player, 0.55F);
            for (LivingEntity target : world.getEntitiesByClass(LivingEntity.class,
                    player.getBoundingBox().expand(session.radius), e -> enemy(player, e))) {
                if (target.squaredDistanceTo(player) > session.radius * session.radius) continue;
                damageAbility(player, target, damage);
                if (session.vortex) {
                    Vec3d pull = player.getPos().subtract(target.getPos());
                    if (pull.lengthSquared() > 0.01D) {
                        Vec3d n = pull.normalize().multiply(0.34D);
                        target.addVelocity(n.x, session.pulsesDone >= session.totalPulses ? 0.22D : 0.06D, n.z);
                    }
                }
            }
            double y = player.getBodyY(0.5D);
            world.spawnParticles(ParticleTypes.SWEEP_ATTACK, player.getX(), y, player.getZ(), 8,
                    session.radius * 0.5D, 0.35D, session.radius * 0.5D, 0D);
            world.spawnParticles(ParticleTypes.CRIT, player.getX(), y, player.getZ(), 20,
                    session.radius * 0.45D, 0.55D, session.radius * 0.45D, 0.08D);
            world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP,
                    SoundCategory.PLAYERS, 0.75F, 0.8F + session.pulsesDone * 0.08F);

            if (session.pulsesDone >= session.totalPulses) iterator.remove();
        }
    }

    private static final class StormSession {
        final ServerPlayerEntity player;
        final int totalPulses;
        final double radius;
        final boolean vortex;
        int tick;
        int pulsesDone;
        StormSession(ServerPlayerEntity player, int totalPulses, double radius, boolean vortex) {
            this.player = player; this.totalPulses = totalPulses; this.radius = radius; this.vortex = vortex;
        }
    }

    // ---------------------------------------------------------------------
    // Shared helpers
    // ---------------------------------------------------------------------

    private static ExecutionResult requireSpear(ServerPlayerEntity player) {
        if (!AbilityRuntime.isClass(player, LancerClass.ID)) return ExecutionResult.failure("You are not a Lancer.");
        if (!isSpear(player.getMainHandStack())) return ExecutionResult.failure("Lancer abilities require the spear in your main hand.");
        return null;
    }

    private static float abilityDamage(ServerPlayerEntity player, float coefficient) {
        float base = (float) player.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE);
        return Math.max(1.0F, base * coefficient * momentumMultiplier(player));
    }

    private static boolean damageAbility(ServerPlayerEntity player, LivingEntity target, float amount) {
        return target.damage(player.getDamageSources().playerAttack(player), amount);
    }

    private static boolean enemy(ServerPlayerEntity owner, LivingEntity e) {
        if (e == owner || !e.isAlive() || e.isSpectator() || owner.isTeammate(e)) return false;
        if (e instanceof TameableEntity tame && owner.getUuid().equals(tame.getOwnerUuid())) return false;
        if (e instanceof AbstractHorseEntity horse && owner.getUuid().equals(horse.getOwnerUuid())) return false;
        if (e instanceof Ownable ownable) {
            Entity master = ownable.getOwner();
            if (master != null && owner.getUuid().equals(master.getUuid())) return false;
        }
        return true;
    }

    private static boolean has(ServerPlayerEntity player, org.marj4n.smooth_classes.api.talent.Talent talent) {
        return AbilityRuntime.hasTalent(player, talent.id());
    }

    private static void cleanupPlayer(ServerPlayerEntity player, boolean resetMomentum) {
        THRUSTS.remove(player.getUuid());
        STORMS.remove(player.getUuid());
        removeAttackModifier(player);
        removePassiveAttributes(player);
        if (resetMomentum) MOMENTUM.remove(player.getUuid());
    }

    private static void clearVisuals() {
        for (ImpaleVisual visual : IMPALES) if (!visual.spear.isRemoved()) visual.spear.discard();
        IMPALES.clear();
    }
}
