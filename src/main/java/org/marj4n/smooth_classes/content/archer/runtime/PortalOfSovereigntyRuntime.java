package org.marj4n.smooth_classes.content.archer.runtime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.marj4n.smooth_classes.content.archer.ArcherClass;
import org.marj4n.smooth_classes.entity.BladePortalEntity;
import org.marj4n.smooth_classes.entity.ProjectedDaggerEntity;
import org.marj4n.smooth_classes.integration.PuffishSkillsIntegration;
import org.marj4n.smooth_classes.registry.SmoothEntities;
import org.marj4n.smooth_classes.runtime.AbilityRuntime;
import org.marj4n.smooth_classes.runtime.ExecutionResult;

/**
 * Portal of Sovereignty: a dense rear-gate barrage built from short-lived random
 * portal pulses. Charging is character-only; gates are created only after release.
 */
public final class PortalOfSovereigntyRuntime {
    private static final Map<UUID, Session> ACTIVE = new HashMap<>();
    public static final int CHARGE_TICKS = 16;
    private static final int DISSIPATE_TICKS = 30;
    private static final int PORTAL_OPEN_TICKS = 4;
    private static final int PORTAL_CLOSE_TICKS = 5;
    private static final int BASE_ACTIVE_PORTALS = 5;
    private static final int MAX_ACTIVE_PORTALS = 10;
    private static final Map<UUID, Integer> HELD_AT = new HashMap<>();
    private static final net.minecraft.util.Identifier ABILITY = org.marj4n.smooth_classes.SmoothClasses.id("unlimited_blade_works");

    private static final class PortalPulse {
        final BladePortalEntity portal;
        final int[] shotAges;
        final int closeStartAge;
        final int totalAge;
        int age;
        int shotIndex;

        PortalPulse(BladePortalEntity portal, int[] shotAges) {
            this.portal = portal;
            this.shotAges = shotAges;
            this.closeStartAge = shotAges.length == 0
                    ? PORTAL_OPEN_TICKS + 3
                    : shotAges[shotAges.length - 1] + 1;
            this.totalAge = closeStartAge + PORTAL_CLOSE_TICKS;
        }

        boolean tick(Session session, int ticksLeft) {
            age++;
            portal.followCaster();

            float opening;
            if (age <= PORTAL_OPEN_TICKS) {
                opening = age / (float) PORTAL_OPEN_TICKS;
            } else if (age <= closeStartAge) {
                opening = 1F;
            } else {
                opening = 1F - (age - closeStartAge) / (float) PORTAL_CLOSE_TICKS;
            }
            if (ticksLeft <= DISSIPATE_TICKS) {
                opening *= MathHelper.clamp(ticksLeft / (float) DISSIPATE_TICKS, 0F, 1F);
            }
            portal.setOpening(opening);

            while (shotIndex < shotAges.length && age >= shotAges[shotIndex]) {
                if (ticksLeft > 0) fireFromPortal(session, portal, ticksLeft);
                shotIndex++;
            }

            if (age > totalAge || opening <= 0.01F) {
                portal.discard();
                return true;
            }
            return false;
        }
    }

    private static final class Session {
        final ServerPlayerEntity player;
        final ServerWorld world;
        final long end;
        final float damage;
        final double range;
        final int pierce;
        final boolean focused;
        final boolean rapid;
        final int activePortalLimit;
        final List<PortalPulse> pulses = new ArrayList<>();
        final List<ProjectedDaggerEntity> projectiles = new ArrayList<>();
        int chargeTicks;
        boolean released;
        int nextPortalIn;
        boolean cancelApplied;

        Session(ServerPlayerEntity player, ServerWorld world,
                long end, float damage, double range, int pierce,
                boolean focused, boolean rapid, int activePortalLimit) {
            this.player = player;
            this.world = world;
            this.end = end;
            this.damage = damage;
            this.range = range;
            this.pierce = pierce;
            this.focused = focused;
            this.rapid = rapid;
            this.activePortalLimit = MathHelper.clamp(activePortalLimit, BASE_ACTIVE_PORTALS, MAX_ACTIVE_PORTALS);
            this.nextPortalIn = 0;
        }
    }

    public static void hold(ServerPlayerEntity player, boolean held) {
        if (held) HELD_AT.put(player.getUuid(), player.getServer().getTicks());
        else {
            HELD_AT.remove(player.getUuid());
            Session s = ACTIVE.get(player.getUuid());
            if (s != null && !s.released) stop(player.getUuid());
        }
    }

    private static boolean held(ServerPlayerEntity player) {
        Integer tick = HELD_AT.get(player.getUuid());
        return tick != null && player.getServer().getTicks() - tick <= 12;
    }

    private static void animation(ServerPlayerEntity player, String name) {
        var entry = org.marj4n.smooth_classes.runtime.InternalSpellRuntime.entry(player,
                org.marj4n.smooth_classes.SmoothClasses.id(name));
        if (entry == null || entry.value().release == null || entry.value().release.animation == null) return;
        var viewers = new ArrayList<>(net.fabricmc.fabric.api.networking.v1.PlayerLookup.tracking(player));
        if (!viewers.contains(player)) viewers.add(player);
        net.spell_engine.utils.AnimationHelper.sendAnimation(player, viewers,
                net.spell_engine.internals.casting.SpellCast.Animation.RELEASE,
                entry.value().release.animation, 1F);
    }

    private static void cancelCharge(Session s) {
        if (s.cancelApplied || s.released) return;
        s.cancelApplied = true;
        org.marj4n.smooth_classes.network.SmoothClassesNetworking.sendChargeState(
                s.player, false, false, "unlimited_blade_works", s.chargeTicks, CHARGE_TICKS);
        org.marj4n.smooth_classes.runtime.AbilityCooldowns.start(s.player, ABILITY, 80);
        animation(s.player, "blade_cancel");
        org.marj4n.smooth_classes.network.SmoothClassesNetworking.sendAbilityState(s.player);
    }

    private PortalOfSovereigntyRuntime() {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> { if (!ACTIVE.isEmpty()) tick(); });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            Session old = ACTIVE.remove(handler.player.getUuid());
            if (old != null) {
                discardSession(old);
                if (!old.released && !old.cancelApplied) {
                    org.marj4n.smooth_classes.runtime.AbilityCooldowns.start(handler.player, ABILITY, 80);
                }
            }
            HELD_AT.remove(handler.player.getUuid());
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            ACTIVE.values().forEach(PortalOfSovereigntyRuntime::discardSession);
            ACTIVE.clear();
            HELD_AT.clear();
        });
    }

    public static ExecutionResult cast(ServerPlayerEntity player) {
        if (!AbilityRuntime.isClass(player, ArcherClass.ID)) return ExecutionResult.failure("Requires Archer.");
        if (ACTIVE.containsKey(player.getUuid())) return ExecutionResult.failure("Portal of Sovereignty is already active.");
        if (!DaggerCompatibility.isDagger(player.getMainHandStack()))
            return ExecutionResult.failure("Hold a dagger in your main hand to cast Portal of Sovereignty.");
        if (!held(player)) return ExecutionResult.failure("Hold the signature key until the portal finishes charging.");

        int duration = 200;
        for (String id : new String[]{"4q8hsjuia145trv7", "9hqz9uonnud6t9rw", "7i77726t2uhbe1oh"})
            if (has(player, id)) duration += 40;
        if (has(player, "dr852odvrafemtmw")) duration += 80;

        float damage = 1F;
        for (String id : new String[]{"ybvq1ijnytlldmmy", "inctw0dlvdx7wrf0", "xb84xgtz2cynhxe2"})
            if (has(player, id)) damage += .1F;
        if (has(player, "ydcjvatyvt2o7etu")) damage += .2F;

        double range = 24 + (has(player, "qerw03l9ypwl67cl") ? 4 : 0)
                + (has(player, "1sd5kv4wi1ylacx8") ? 8 : 0);

        int activePortalLimit = BASE_ACTIVE_PORTALS;
        if (has(player, "archer_blade_left_portal")) activePortalLimit += 2;
        if (has(player, "archer_blade_right_portal")) activePortalLimit += 3;
        activePortalLimit = Math.min(MAX_ACTIVE_PORTALS, activePortalLimit);
        boolean rapid = has(player, "5kcs5b8ne9c30zwn");

        ACTIVE.put(player.getUuid(), new Session(player, player.getServerWorld(),
                player.getWorld().getTime() + duration + CHARGE_TICKS,
                damage, range,
                has(player, "t5c5gzcskng8b8cz") ? 3 : 1,
                has(player, "fau41sxkpniul48c"),
                rapid, activePortalLimit));

        org.marj4n.smooth_classes.network.SmoothClassesNetworking.sendChargeState(
                player, true, false, "unlimited_blade_works", 0, CHARGE_TICKS);
        player.getServerWorld().playSound(null, player.getBlockPos(), SoundEvents.BLOCK_BEACON_ACTIVATE,
                SoundCategory.PLAYERS, .8F, 1.5F);
        org.marj4n.smooth_classes.runtime.SkillFx.sound(player, "magic_shamanic_power_12", .42F, 1.05F);
        org.marj4n.smooth_classes.runtime.SkillFx.sound(player, "energy_charge", .34F, .78F);
        animation(player, "blade_charge");
        return ExecutionResult.success(1, "Charging Portal of Sovereignty");
    }

    private static Vec3d aimedPoint(ServerPlayerEntity player, double range) {
        Vec3d eye = player.getEyePos();
        Vec3d end = eye.add(player.getRotationVec(1F).normalize().multiply(range));
        var block = player.getWorld().raycast(new net.minecraft.world.RaycastContext(eye, end,
                net.minecraft.world.RaycastContext.ShapeType.COLLIDER,
                net.minecraft.world.RaycastContext.FluidHandling.NONE, player));
        if (block.getType() != net.minecraft.util.hit.HitResult.Type.MISS) end = block.getPos();
        Vec3d nearest = end;
        for (var target : player.getWorld().getEntitiesByClass(net.minecraft.entity.LivingEntity.class,
                new net.minecraft.util.math.Box(eye, end).expand(.2), e -> ProjectedDaggerEntity.validTarget(e, player))) {
            var hit = target.getBoundingBox().expand(.1).raycast(eye, end);
            if (hit.isPresent() && eye.squaredDistanceTo(hit.get()) < eye.squaredDistanceTo(nearest)) nearest = hit.get();
        }
        return nearest;
    }

    private static boolean has(ServerPlayerEntity p, String id) {
        return PuffishSkillsIntegration.isSkillUnlocked(PuffishSkillsIntegration.ARCHER, id, p);
    }

    public static boolean owns(BladePortalEntity portal) {
        for (Session s : ACTIVE.values()) {
            for (PortalPulse pulse : s.pulses) {
                if (pulse.portal == portal) return true;
            }
        }
        return false;
    }

    private static void stop(UUID id) {
        Session old = ACTIVE.remove(id);
        if (old != null) {
            discardSession(old);
            cancelCharge(old);
        }
    }

    private static void discardSession(Session s) {
        for (PortalPulse pulse : s.pulses) pulse.portal.discard();
        s.pulses.clear();
        for (ProjectedDaggerEntity dagger : s.projectiles) dagger.discard();
        s.projectiles.clear();
    }

    private static double portalRadius(float scale) {
        return 1.16D * scale;
    }

    private static boolean fitsSpacing(Session s, double back, double lateral, double vertical, float scale) {
        double radius = portalRadius(scale);
        for (PortalPulse pulse : s.pulses) {
            BladePortalEntity other = pulse.portal;
            double dx = back - other.localBack();
            double dy = lateral - other.localLateral();
            double dz = vertical - other.localVertical();
            double minDistance = radius + portalRadius(other.scale()) + 0.55D;
            if ((dx * dx) + (dy * dy) + (dz * dz) < minDistance * minDistance) {
                return false;
            }
        }
        return true;
    }

    private static BladePortalEntity buildPortal(Session s, double back, double lateral,
                                                    double vertical, float scale) {
        BladePortalEntity portal = new BladePortalEntity(SmoothEntities.BLADE_PORTAL, s.world);
        portal.setCaster(s.player);
        portal.setSide(0);
        portal.setPlacement(back, lateral, vertical);
        portal.setScale(scale);
        portal.followCaster();
        return portal;
    }

    private static BladePortalEntity createSpacedPortal(Session s, double minBack, double backRange,
                                                        double lateralRange, double verticalMin, double verticalRange,
                                                        float minScale, float scaleRange) {
        var random = s.player.getRandom();
        double fallbackBack = 0D, fallbackLateral = 0D, fallbackVertical = 0D;
        float fallbackScale = 0F;
        double fallbackScore = Double.NEGATIVE_INFINITY;
        boolean hasFallback = false;

        // Sample primitive coordinates first; only allocate the Entity after a
        // position is accepted. Previously up to 24 discarded portal entities
        // were allocated for every gate when the formation was crowded.
        for (int attempt = 0; attempt < 24; attempt++) {
            double back = minBack + random.nextDouble() * backRange;
            double lateral = (random.nextDouble() * (lateralRange * 2D)) - lateralRange;
            if (Math.abs(lateral) < 0.70D) {
                lateral = Math.copySign(0.70D + random.nextDouble() * 0.80D,
                        lateral == 0 ? (random.nextBoolean() ? 1D : -1D) : lateral);
            }
            double vertical = verticalMin + random.nextDouble() * verticalRange;
            float scale = minScale + random.nextFloat() * scaleRange;

            if (fitsSpacing(s, back, lateral, vertical, scale)) {
                return buildPortal(s, back, lateral, vertical, scale);
            }

            double nearest = Double.POSITIVE_INFINITY;
            for (PortalPulse pulse : s.pulses) {
                BladePortalEntity other = pulse.portal;
                double dx = back - other.localBack();
                double dy = lateral - other.localLateral();
                double dz = vertical - other.localVertical();
                double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
                double clearance = distance - (portalRadius(scale) + portalRadius(other.scale()));
                nearest = Math.min(nearest, clearance);
            }
            if (nearest > fallbackScore) {
                fallbackBack = back;
                fallbackLateral = lateral;
                fallbackVertical = vertical;
                fallbackScale = scale;
                fallbackScore = nearest;
                hasFallback = true;
            }
        }
        return hasFallback && fallbackScore > 0.12D
                ? buildPortal(s, fallbackBack, fallbackLateral, fallbackVertical, fallbackScale)
                : null;
    }

    private static PortalPulse spawnPulse(Session s, int ticksLeft) {
        var random = s.player.getRandom();
        BladePortalEntity portal = createSpacedPortal(s,
                1.55D, 2.25D,
                5.10D,
                -0.10D, 2.25D,
                0.56F, 0.34F);
        if (portal == null || !s.world.spawnEntity(portal)) return null;

        int shots = 1;
        if (ticksLeft > 12) {
            double roll = random.nextDouble();
            if (s.rapid) shots = roll < 0.18D ? 1 : roll < 0.68D ? 2 : 3;
            else shots = roll < 0.48D ? 1 : roll < 0.86D ? 2 : 3;
            if (s.focused && roll > 0.58D) shots = Math.min(3, shots + 1);
        }
        if (ticksLeft <= DISSIPATE_TICKS) shots = Math.min(shots, 1 + random.nextInt(2));
        if (ticksLeft <= 8) shots = 1;

        int[] shotAges = new int[shots];
        int age = PORTAL_OPEN_TICKS + 1 + random.nextInt(2);
        for (int i = 0; i < shots; i++) {
            shotAges[i] = age;
            age += s.rapid ? 1 + random.nextInt(2) : 1 + random.nextInt(3);
        }

        s.world.playSound(null, s.player.getBlockPos(), SoundEvents.BLOCK_BEACON_AMBIENT,
                SoundCategory.PLAYERS, .13F, 1.45F + random.nextFloat() * .20F);
        return new PortalPulse(portal, shotAges);
    }

    private static void fireFromPortal(Session s, BladePortalEntity portal, int ticksLeft) {
        ServerPlayerEntity p = s.player;
        Vec3d look = p.getRotationVec(1F).normalize();
        Vec3d right = new Vec3d(Math.cos(Math.toRadians(p.getYaw())), 0, Math.sin(Math.toRadians(p.getYaw())));
        Vec3d up = look.crossProduct(right).normalize();
        Vec3d aim = aimedPoint(p, s.range);

        double angle = p.getRandom().nextDouble() * Math.PI * 2D;
        double radius = Math.sqrt(p.getRandom().nextDouble()) * .74D;
        Vec3d portalPlane = portal.getPos().add(right.multiply(Math.cos(angle) * radius))
                .add(up.multiply(Math.sin(angle) * radius));
        Vec3d start = portalPlane.subtract(look.multiply(ProjectedDaggerEntity.PORTAL_EMERGENCE_DEPTH));

        double spread = s.focused ? .10D : .48D;
        if (ticksLeft <= DISSIPATE_TICKS) spread *= 1.10D;
        Vec3d target = aim
                .add(right.multiply((p.getRandom().nextDouble() - .5D) * spread))
                .add(up.multiply((p.getRandom().nextDouble() - .5D) * spread));

        ProjectedDaggerEntity dagger = new ProjectedDaggerEntity(SmoothEntities.PROJECTED_DAGGER, s.world);
        dagger.configure(p, p.getMainHandStack(), (float) p.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE),
                s.damage, s.range + 3D, s.pierce);
        dagger.setPosition(start);
        dagger.setVelocity(target.subtract(start).normalize().multiply(s.focused ? 2.1D : 1.45D));
        dagger.faceVelocity();
        if (s.world.spawnEntity(dagger)) {
            s.projectiles.add(dagger);
            s.world.playSound(null, p.getBlockPos(), SoundEvents.ENTITY_ARROW_SHOOT,
                    SoundCategory.PLAYERS, .18F, 1.48F + p.getRandom().nextFloat() * .20F);
        }
    }

    private static void tick() {
        if (ACTIVE.isEmpty()) return;
        Iterator<Session> iterator = ACTIVE.values().iterator();
        while (iterator.hasNext()) {
            Session s = iterator.next();
            s.projectiles.removeIf(net.minecraft.entity.Entity::isRemoved);
            ServerPlayerEntity p = s.player;
            boolean invalid = !p.isAlive() || p.isRemoved() || p.isSpectator() || p.getServerWorld() != s.world
                    || !AbilityRuntime.isClass(p, ArcherClass.ID) || !has(p, "is053f9imz801s57")
                    || !DaggerCompatibility.isDagger(p.getMainHandStack());
            if (invalid) {
                discardSession(s);
                cancelCharge(s);
                iterator.remove();
                continue;
            }

            if (!s.released) {
                p.setSprinting(false);
                p.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 3, 3, false, false, false));

                if (!held(p)) {
                    discardSession(s);
                    cancelCharge(s);
                    iterator.remove();
                    continue;
                }

                s.chargeTicks++;
                if (s.chargeTicks < CHARGE_TICKS) continue;

                s.released = true;
                discardSession(s);
                org.marj4n.smooth_classes.network.SmoothClassesNetworking.sendChargeState(
                        p, false, true, "unlimited_blade_works", CHARGE_TICKS, CHARGE_TICKS);
                animation(p, "blade_release");
                org.marj4n.smooth_classes.runtime.SkillFx.sound(p, "spell_arcane_cast", .72F, .82F);
                org.marj4n.smooth_classes.runtime.SkillFx.sound(p, "spell_slash_02", .72F, 1.16F);
                org.marj4n.smooth_classes.integration.OptionalCompatRuntime.onSignatureAbility(p);
                org.marj4n.smooth_classes.runtime.AbilityCooldowns.start(p, ABILITY,
                        org.marj4n.smooth_classes.runtime.AbilityCooldowns.adjustedTicks(p,
                                org.marj4n.smooth_classes.integration.OptionalCompatRuntime.signatureCooldown(p,
                                        org.marj4n.smooth_classes.gameplay.SignatureCooldowns.ticks("unlimited_blade_works"))));
                org.marj4n.smooth_classes.network.SmoothClassesNetworking.sendAbilityState(p);
                s.world.playSound(null, p.getBlockPos(), SoundEvents.BLOCK_BEACON_POWER_SELECT,
                        SoundCategory.PLAYERS, .6F, 1.4F);
                s.nextPortalIn = 0;
            }

            int ticksLeft = (int) (s.end - s.world.getTime());

            Iterator<PortalPulse> pulseIterator = s.pulses.iterator();
            while (pulseIterator.hasNext()) {
                if (pulseIterator.next().tick(s, ticksLeft)) pulseIterator.remove();
            }

            if (ticksLeft <= 0) {
                if (s.pulses.isEmpty()) {
                    discardSession(s);
                    iterator.remove();
                }
                continue;
            }

            if (s.nextPortalIn > 0) s.nextPortalIn--;

            int activeLimit = s.activePortalLimit;
            if (ticksLeft <= DISSIPATE_TICKS) {
                float fade = MathHelper.clamp(ticksLeft / (float) DISSIPATE_TICKS, 0F, 1F);
                activeLimit = Math.max(1, Math.round(s.activePortalLimit * fade));
            }

            if (s.nextPortalIn <= 0 && s.pulses.size() < activeLimit) {
                int missing = activeLimit - s.pulses.size();
                int burstCap = s.rapid ? 4 : 3;
                int spawnCount = Math.min(missing, burstCap);
                for (int i = 0; i < spawnCount; i++) {
                    PortalPulse pulse = spawnPulse(s, ticksLeft);
                    if (pulse != null) s.pulses.add(pulse);
                }
                s.nextPortalIn = ticksLeft <= DISSIPATE_TICKS ? 2 : 1;
            }
        }
    }
}
