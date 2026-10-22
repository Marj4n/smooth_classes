package org.marj4n.smooth_classes.client.effects;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import org.marj4n.smooth_classes.network.SmoothClassesNetworking;

/** A persistent red attack ping visible only to the Avenger issuing the order. */
public final class AvengerAttackMarkClient {
    private static int markedEntityId = -1;
    private static ClientWorld activeWorld;

    private AvengerAttackMarkClient() { }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(SmoothClassesNetworking.SYNC_AVENGER_ATTACK_MARK,
                (client, handler, buf, sender) -> {
                    int id = buf.readVarInt();
                    client.execute(() -> markedEntityId = id);
                });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.world != activeWorld || client.player == null) {
                activeWorld = client.world;
                markedEntityId = -1;
            }
        });
    }

    public static void render(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (markedEntityId < 0 || client.world == null || client.player == null
                || context.matrixStack() == null || context.consumers() == null) return;
        Entity entity = client.world.getEntityById(markedEntityId);
        if (!(entity instanceof LivingEntity target) || !target.isAlive()
                || client.player.squaredDistanceTo(target) > 128D * 128D) return;

        float delta = context.tickDelta();
        double x = MathHelper.lerp(delta, target.prevX, target.getX());
        double y = MathHelper.lerp(delta, target.prevY, target.getY()) + target.getHeight() + 0.45D
                + Math.sin((target.age + delta) * 0.11D) * 0.06D;
        double z = MathHelper.lerp(delta, target.prevZ, target.getZ());
        var camera = context.camera().getPos();
        MatrixStack matrices = context.matrixStack();
        TextRenderer text = client.textRenderer;
        matrices.push();
        matrices.translate(x - camera.x, y - camera.y, z - camera.z);
        matrices.multiply(context.camera().getRotation());
        matrices.scale(-0.028F, -0.028F, 0.028F);
        var matrix = matrices.peek().getPositionMatrix();
        // Bright, readable ping-wheel-like red cross and a compact ATTACK label.
        text.draw("X", -text.getWidth("X") / 2F, -15F, 0xFFFF3333,
                false, matrix, context.consumers(), TextRenderer.TextLayerType.SEE_THROUGH, 0, 0xF000F0);
        text.draw("ATTACK", -text.getWidth("ATTACK") / 2F, 0F, 0xFFFF5555,
                false, matrix, context.consumers(), TextRenderer.TextLayerType.SEE_THROUGH, 0, 0xF000F0);
        matrices.pop();
    }
}
