package org.marj4n.smooth_classes.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.marj4n.smooth_classes.entity.ShadowAnchorEntity;
import org.marj4n.smooth_classes.network.SmoothClassesNetworking;
import org.marj4n.smooth_classes.registry.SmoothEntities;

/** Client-only targeting/preview state for Assassin Shadow Technique. */
public final class ShadowAimClient {
    private static final String PREVIEW_TAG = "smooth_classes_shadow_preview";

    private static boolean aiming;
    private static LivingEntity highlighted;
    private static boolean highlightedWasGlowing;
    private static LivingEntity aimedEntity;
    private static Vec3d aimedBlock;
    private static Vec3d previewPos;
    private static float previewYaw;
    private static ShadowAnchorEntity previewEntity;

    private ShadowAimClient() {}

    public static boolean isAiming() { return aiming; }

    public static void begin(MinecraftClient client) {
        if (client.player == null || client.world == null) return;
        aiming = true;
        aimedBlock = null;
        update(client);
    }

    /**
     * Aim is live for the entire key hold. No mouse confirmation is required:
     * look at a mob or block, then release the signature key to commit.
     */
    public static void update(MinecraftClient client) {
        if (!aiming || client.player == null || client.world == null) return;
        if (AbilityHudState.shadowActive) {
            aimedEntity = null;
            aimedBlock = null;
            previewPos = null;
            clearTarget();
            return;
        }

        double range = Math.max(1, AbilityHudState.shadowRange);
        Vec3d eye = client.player.getEyePos();
        Vec3d end = eye.add(client.player.getRotationVec(1F).multiply(range));
        HitResult block = client.player.raycast(range, 1F, false);
        double blockDistanceSq = block.getType() == HitResult.Type.MISS
                ? eye.squaredDistanceTo(end) : eye.squaredDistanceTo(block.getPos());

        LivingEntity entity = findLivingTarget(client, eye, end, blockDistanceSq);
        aimedEntity = entity;
        setHighlighted(entity);

        if (entity != null) {
            aimedBlock = null;
            previewPos = previewBehindEntity(client, entity);
            previewYaw = entity.getYaw();
            return;
        }

        if (block.getType() != HitResult.Type.MISS) {
            aimedBlock = block.getPos();
            previewPos = previewDestination(client, aimedBlock);
            previewYaw = client.player.getYaw();
        } else {
            aimedBlock = null;
            previewPos = null;
        }
    }

    public static void release(MinecraftClient client) {
        if (!aiming || client.player == null) {
            reset();
            return;
        }

        PacketByteBuf out = PacketByteBufs.create();
        if (AbilityHudState.shadowActive) {
            out.writeByte(0); // return to owned shadow
            ClientPlayNetworking.send(SmoothClassesNetworking.SHADOW_AIM_CAST, out);
        } else if (aimedEntity != null && aimedEntity.isAlive()) {
            out.writeByte(1);
            out.writeInt(aimedEntity.getId());
            ClientPlayNetworking.send(SmoothClassesNetworking.SHADOW_AIM_CAST, out);
        } else if (aimedBlock != null) {
            out.writeByte(2);
            out.writeDouble(aimedBlock.x);
            out.writeDouble(aimedBlock.y);
            out.writeDouble(aimedBlock.z);
            ClientPlayNetworking.send(SmoothClassesNetworking.SHADOW_AIM_CAST, out);
        } else {
            client.player.sendMessage(Text.literal("Aim at a creature or block, then release Shadow Technique."), true);
        }
        reset();
    }

    public static void reset() {
        aiming = false;
        aimedBlock = null;
        previewPos = null;
        aimedEntity = null;
        clearTarget();
        previewEntity = null;
    }

    private static LivingEntity findLivingTarget(MinecraftClient client, Vec3d eye, Vec3d end, double maxDistanceSq) {
        double nearest = Math.min(eye.squaredDistanceTo(end), maxDistanceSq);
        LivingEntity best = null;
        Box search = new Box(eye, end).expand(1.0D);
        for (LivingEntity entity : client.world.getEntitiesByClass(LivingEntity.class, search,
                entity -> entity != client.player && entity.isAlive())) {
            var hit = entity.getBoundingBox().expand(.15D).raycast(eye, end);
            if (hit.isEmpty()) continue;
            double distance = eye.squaredDistanceTo(hit.get());
            if (distance < nearest) {
                nearest = distance;
                best = entity;
            }
        }
        return best;
    }

    private static Vec3d previewBehindEntity(MinecraftClient client, LivingEntity target) {
        double distance = Math.max(1.4D, (target.getWidth() + client.player.getWidth()) * .5D + .45D);
        Vec3d back = Vec3d.fromPolar(0F, target.getYaw()).multiply(-distance);
        return safePreview(client, target.getPos().add(back));
    }

    private static Vec3d previewDestination(MinecraftClient client, Vec3d hit) {
        Vec3d eye = client.player.getEyePos();
        Vec3d offset = hit.subtract(eye);
        if (offset.lengthSquared() < .001D) return null;
        return safePreview(client, hit.subtract(offset.normalize().multiply(.65D)));
    }

    private static Vec3d safePreview(MinecraftClient client, Vec3d requested) {
        if (free(client, requested)) return requested;
        Vec3d up = requested.add(0D, 1D, 0D);
        return free(client, up) ? up : null;
    }

    private static boolean free(MinecraftClient client, Vec3d pos) {
        Box box = client.player.getBoundingBox().offset(pos.subtract(client.player.getPos()));
        return client.world.getWorldBorder().contains(box)
                && pos.y >= client.world.getBottomY()
                && pos.y + client.player.getHeight() < client.world.getTopY()
                && client.world.isSpaceEmpty(client.player, box);
    }

    private static void setHighlighted(LivingEntity entity) {
        if (highlighted == entity) return;
        clearTarget();
        if (entity == null) return;
        highlighted = entity;
        highlightedWasGlowing = entity.isGlowing();
        entity.setGlowing(true);
    }

    private static void clearTarget() {
        if (highlighted != null && !highlighted.isRemoved()) highlighted.setGlowing(highlightedWasGlowing);
        highlighted = null;
        highlightedWasGlowing = false;
    }

    public static void render(WorldRenderContext context) {
        if (!aiming || previewPos == null || AbilityHudState.shadowActive) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null || context.matrixStack() == null || context.consumers() == null) return;

        if (previewEntity == null || previewEntity.getWorld() != client.world) {
            previewEntity = SmoothEntities.SHADOW_ANCHOR.create(client.world);
            if (previewEntity == null) return;
            previewEntity.addCommandTag(PREVIEW_TAG);
        }
        previewEntity.refreshPositionAndAngles(previewPos.x, previewPos.y, previewPos.z, previewYaw, 0F);

        Vec3d camera = context.camera().getPos();
        client.getEntityRenderDispatcher().render(previewEntity,
                previewPos.x - camera.x, previewPos.y - camera.y, previewPos.z - camera.z,
                previewYaw, context.tickDelta(), context.matrixStack(), context.consumers(), 0xF000F0);
    }
}
