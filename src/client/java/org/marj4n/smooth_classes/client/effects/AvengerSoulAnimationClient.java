package org.marj4n.smooth_classes.client.effects;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.marj4n.smooth_classes.content.avenger.runtime.AvengerSoulAnimationRuntime;
import org.marj4n.smooth_classes.network.SmoothClassesNetworking;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * A purely client-side cinematic layer. The living entity stays in its safe
 * server position; its model is translated down through the floor on emergence
 * or pulled into the caster's right hand on recall. No collision, NBT or AI
 * data are adjusted by render code.
 */
public final class AvengerSoulAnimationClient {
    private static final Map<Integer, Playback> PLAYBACKS = new HashMap<>();
    private static final Map<UUID, CastPose> CASTERS = new HashMap<>();
    private static ClientWorld currentWorld;
    private static boolean registered;

    private AvengerSoulAnimationClient() {}

    public static void register() {
        if (registered) return;
        registered = true;
        ClientPlayNetworking.registerGlobalReceiver(SmoothClassesNetworking.AVENGER_SOUL_ANIMATION,
                (client, handler, buf, responseSender) -> {
                    int entityId = buf.readVarInt();
                    UUID caster = buf.readUuid();
                    byte action = buf.readByte();
                    boolean ground = buf.readBoolean();
                    int duration = buf.readVarInt();
                    Vec3d position = new Vec3d(buf.readDouble(), buf.readDouble(), buf.readDouble());
                    client.execute(() -> {
                        if (client.world == null || duration < 1 || duration > 80) return;
                        if (client.world != currentWorld) clear(client.world);
                        long now = client.world.getTime();
                        PLAYBACKS.put(entityId, new Playback(entityId, caster, action, ground, duration, now, position));
                        CASTERS.put(caster, new CastPose(action, duration, now));
                    });
                });
        ClientTickEvents.END_CLIENT_TICK.register(AvengerSoulAnimationClient::tick);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(() -> clear(null)));
    }

    private static void clear(ClientWorld world) {
        PLAYBACKS.clear();
        CASTERS.clear();
        currentWorld = world;
    }

    public static float casterPose(AbstractClientPlayerEntity player, float delta) {
        if (player == null || player.getWorld() != currentWorld) return 0F;
        CastPose pose = CASTERS.get(player.getUuid());
        if (pose == null) return 0F;
        float t = ((player.getWorld().getTime() - pose.start) + delta) / pose.duration;
        if (t < 0F || t >= 1F) return 0F;
        float enter = MathHelper.clamp(t / 0.22F, 0F, 1F);
        float leave = MathHelper.clamp((1F - t) / 0.28F, 0F, 1F);
        return smooth(enter) * smooth(leave);
    }

    public static byte casterAction(AbstractClientPlayerEntity player) {
        if (player == null || player.getWorld() != currentWorld) return -1;
        CastPose pose = CASTERS.get(player.getUuid());
        return pose == null ? -1 : pose.action;
    }

    /** Used by the entity renderer's matrix stack, never by entity physics. */
    public static void transform(LivingEntity entity, float delta, net.minecraft.client.util.math.MatrixStack matrices) {
        if (entity == null || entity.getWorld() != currentWorld) return;
        Playback p = PLAYBACKS.get(entity.getId());
        if (p == null) return;
        float time = ((entity.getWorld().getTime() - p.start) + delta) / p.duration;
        if (time < 0F || time >= 1F) return;
        float ease = smooth(time);
        if (p.action == AvengerSoulAnimationRuntime.EMERGE) {
            if (p.ground) {
                // Visual-only burial: no server-side suffocation/pathfinding bugs.
                matrices.translate(0D, -entity.getHeight() * 0.92D * (1F - ease), 0D);
            } else {
                float scale = 0.18F + 0.82F * ease;
                matrices.translate(0D, entity.getHeight() * 0.45D * (1F - ease), 0D);
                matrices.scale(scale, scale, scale);
            }
        } else {
            Vec3d hand = handPosition(entity.getWorld(), p.caster);
            if (hand != null) {
                Vec3d from = entity.getPos();
                Vec3d shift = hand.subtract(from).multiply(ease * ease);
                matrices.translate(shift.x, shift.y, shift.z);
            }
            float scale = Math.max(0.035F, (1F - ease) * (1F - ease));
            matrices.scale(scale, scale, scale);
        }
    }

    private static void tick(MinecraftClient client) {
        ClientWorld world = client.world;
        if (world != currentWorld) clear(world);
        if (world == null || client.isPaused()) return;
        long now = world.getTime();
        CASTERS.entrySet().removeIf(entry -> now - entry.getValue().start > entry.getValue().duration + 2);
        Iterator<Map.Entry<Integer, Playback>> iterator = PLAYBACKS.entrySet().iterator();
        while (iterator.hasNext()) {
            Playback p = iterator.next().getValue();
            long elapsed = now - p.start;
            if (elapsed > p.duration + 2) {
                iterator.remove();
                continue;
            }
            if (elapsed < 0 || elapsed >= p.duration) continue;
            Entity current = world.getEntityById(p.id);
            Vec3d base = current == null ? p.position : current.getPos();
            double height = current == null ? 1.8D : current.getHeight();
            double progress = MathHelper.clamp((double) elapsed / p.duration, 0D, 1D);
            if (p.action == AvengerSoulAnimationRuntime.EMERGE) {
                spawnEmergence(world, p, base, height, progress, now);
            } else {
                spawnAbsorption(world, p, base, height, progress, now);
            }
        }
    }

    private static void spawnEmergence(ClientWorld world, Playback p, Vec3d base,
                                       double height, double progress, long tick) {
        // Low particle budget: visually dense near the ground, cheap during battles.
        double radius = Math.min(0.72D, 0.32D + height * 0.12D);
        for (int j = 0; j < 3; j++) {
            double angle = ((tick * 0.19D + j / 3D) * Math.PI * 2D);
            double x = base.x + Math.cos(angle) * radius;
            double z = base.z + Math.sin(angle) * radius;
            double y = base.y + (p.ground ? 0.05D : height * 0.45D * (1D - progress));
            world.addParticle(ParticleTypes.SOUL, x, y + 0.08D, z,
                    Math.cos(angle) * 0.025D, 0.045D, Math.sin(angle) * 0.025D);
            if (p.ground && j < 2) {
                BlockPos floor = BlockPos.ofFloored(x, base.y - 0.25D, z);
                BlockState block = world.getBlockState(floor);
                if (!block.isAir()) world.addParticle(new BlockStateParticleEffect(ParticleTypes.BLOCK, block),
                        x, y + 0.05D, z, Math.cos(angle) * 0.06D, 0.13D, Math.sin(angle) * 0.06D);
            } else if (!p.ground) {
                world.addParticle(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 0, 0.014D, 0);
            }
        }
    }

    private static void spawnAbsorption(ClientWorld world, Playback p, Vec3d base,
                                        double height, double progress, long tick) {
        Vec3d hand = handPosition(world, p.caster);
        if (hand == null) return;
        Vec3d origin = base.add(0D, height * 0.6D, 0D);
        // Cubic trajectory with a corkscrew that contracts toward the palm.
        for (int i = 0; i < 5; i++) {
            double t = Math.min(1D, Math.max(0D, progress * 0.93D + i * 0.055D));
            Vec3d onLine = origin.lerp(hand, t);
            double spin = tick * 0.62D + i * 1.7D;
            double spread = (1D - t) * 0.26D;
            world.addParticle(i % 3 == 0 ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.SOUL,
                    onLine.x + Math.cos(spin) * spread,
                    onLine.y + Math.sin(spin * 1.2D) * spread,
                    onLine.z + Math.sin(spin) * spread,
                    (hand.x - onLine.x) * 0.08D,
                    (hand.y - onLine.y) * 0.08D,
                    (hand.z - onLine.z) * 0.08D);
        }
        if (progress > 0.82D) {
            world.addParticle(ParticleTypes.REVERSE_PORTAL, hand.x, hand.y, hand.z,
                    0D, 0.012D, 0D);
        }
    }

    private static Vec3d handPosition(net.minecraft.world.World world, UUID uuid) {
        if (!(world instanceof ClientWorld clientWorld)) return null;
        for (AbstractClientPlayerEntity player : clientWorld.getPlayers()) {
            if (!player.getUuid().equals(uuid)) continue;
            double yaw = Math.toRadians(player.getYaw());
            Vec3d right = new Vec3d(Math.cos(yaw), 0D, Math.sin(yaw));
            Vec3d forward = new Vec3d(-Math.sin(yaw), 0D, Math.cos(yaw));
            return player.getPos().add(0D, player.isSneaking() ? 0.72D : 1.02D, 0D)
                    .add(right.multiply(0.44D)).add(forward.multiply(0.40D));
        }
        return null;
    }

    private static float smooth(float value) {
        float t = MathHelper.clamp(value, 0F, 1F);
        return t * t * (3F - 2F * t);
    }

    private record Playback(int id, UUID caster, byte action, boolean ground,
                            int duration, long start, Vec3d position) {}
    private record CastPose(byte action, int duration, long start) {}
}
