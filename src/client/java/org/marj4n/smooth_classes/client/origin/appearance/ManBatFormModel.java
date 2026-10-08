package org.marj4n.smooth_classes.client.origin.appearance;

import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.lang.ref.WeakReference;

/**
 * Thin, public-facing adapter for the direct VSB Blockbench mesh/animation
 * interpreter. The exact model is bundled as an asset and is NOT recreated
 * through Minecraft's fixed-UV ModelPartBuilder.
 */
public final class ManBatFormModel<T extends PlayerEntity> extends EntityModel<T> {
    private static final Map<UUID, Integer> FIRST_RENDER_AGE = new HashMap<>();
    private static final Map<UUID, Boolean> PREVIOUS_ON_GROUND = new HashMap<>();
    private static final Map<UUID, Integer> LAST_LANDED_AGE = new HashMap<>();
    private static final Map<UUID, Float> SMOOTH_HEAD_YAW = new HashMap<>();
    private static final Map<UUID, Float> SMOOTH_HEAD_PITCH = new HashMap<>();
    private static final Map<UUID, VsbFiguraAvatarRenderer.Pose> EQUIPMENT_POSES = new HashMap<>();
    private static final Map<UUID, WeakReference<PlayerEntity>> TRACKED_ENTITIES = new HashMap<>();
    private static final Map<UUID, Float> WING_OPEN = new HashMap<>();
    private VsbFiguraAvatarRenderer.Pose pose = VsbFiguraAvatarRenderer.Pose.rest();

    public ManBatFormModel(ModelPart unusedLayerPart) {
        // ModelPart cannot store the exact per-face Figura texture rectangles.
    }

    /** A player with the same UUID after death is NOT the same animated entity. */
    public static void clearPoseCache(UUID playerId) {
        FIRST_RENDER_AGE.remove(playerId);
        PREVIOUS_ON_GROUND.remove(playerId);
        LAST_LANDED_AGE.remove(playerId);
        SMOOTH_HEAD_YAW.remove(playerId);
        SMOOTH_HEAD_PITCH.remove(playerId);
        EQUIPMENT_POSES.remove(playerId);
        WING_OPEN.remove(playerId);
        TRACKED_ENTITIES.remove(playerId);
    }

    public static TexturedModelData createData() {
        ModelData data = new ModelData();
        data.getRoot().addChild("native_figura_mesh", ModelPartBuilder.create(), ModelTransform.NONE);
        return TexturedModelData.of(data, 64, 64);
    }

    /** Keep Better Combat's Player Animator first-person model visible while
     * an animated weapon attack is drawn. Without this, the replacement-body
     * mixin removes the arms Better Combat needs to render its slash. */
    public static boolean useVanillaFirstPersonCombat(AbstractClientPlayerEntity player) {
        MinecraftClient mc = MinecraftClient.getInstance();
        return player != null && player == mc.player
                && mc.options.getPerspective().isFirstPerson()
                && VsbCombatAnimationBridge.isFirstPersonAnimationActive(
                    player, mc.getTickDelta());
    }

    public void prepare(T player, PlayerEntityModel<?> vanillaPose,
                        float limbAngle, float limbDistance, float animationProgress,
                        float headYaw, float headPitch) {
        UUID id = player.getUuid();
        WeakReference<PlayerEntity> previousEntity = TRACKED_ENTITIES.get(id);
        if (previousEntity == null || previousEntity.get() != player) {
            clearPoseCache(id);
            TRACKED_ENTITIES.put(id, new WeakReference<>(player));
        }
        // The supplied Figura head has a permanent -10 degree resting pitch.
        // Adding the full +/-60 degree player camera pitch on top of that bends
        // the Man-Bat neck unnaturally (particularly when looking down in flight).
        // Keep the approved mouse-driven inventory preview UNCHANGED; only bound
        // the actual in-world head pose to an anatomical range.
        var preview = MinecraftClient.getInstance().currentScreen;
        boolean inInventory = preview instanceof net.minecraft.client.gui.screen.ingame.InventoryScreen
                || preview instanceof net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
        boolean flying = player.getAbilities().flying;
        float clippedYaw = MathHelper.clamp(headYaw,
                inInventory ? -85.0F : -55.0F, inInventory ? 85.0F : 55.0F);
        float clippedPitch = MathHelper.clamp(headPitch,
                inInventory ? -60.0F : (flying ? -16.0F : -23.0F),
                inInventory ? 60.0F : (flying ? 18.0F : 28.0F));
        float smYaw = inInventory ? clippedYaw
                : MathHelper.lerp(.35F, SMOOTH_HEAD_YAW.getOrDefault(id, clippedYaw), clippedYaw);
        float smPitch = inInventory ? clippedPitch
                : MathHelper.lerp(.35F, SMOOTH_HEAD_PITCH.getOrDefault(id, clippedPitch), clippedPitch);
        SMOOTH_HEAD_YAW.put(id, smYaw);
        SMOOTH_HEAD_PITCH.put(id, smPitch);

        boolean onGround = player.isOnGround();
        boolean previous = PREVIOUS_ON_GROUND.getOrDefault(id, onGround);
        if (onGround && !previous) LAST_LANDED_AGE.put(id, player.age);
        PREVIOUS_ON_GROUND.put(id, onGround);

        int first = FIRST_RENDER_AGE.computeIfAbsent(id, ignored -> player.age);
        float spawnTime = (animationProgress - first) / 20F;
        String animation;
        float time;
        if (spawnTime >= 0F && spawnTime < .5F) {
            animation = "spawn";
            time = spawnTime;
        } else {
            Integer lastLand = LAST_LANDED_AGE.get(id);
            float landTime = lastLand == null ? 1F : (animationProgress - lastLand) / 20F;
            if (landTime >= 0F && landTime < .5F) {
                animation = "land";
                time = landTime;
            } else if (onGround && limbDistance > .02F) {
                animation = "walk";
                time = (animationProgress / 20F) % 1.0F;
            } else {
                animation = "idle";
                time = (animationProgress / 20F) % 6.0F;
            }
        }
        // The parent PlayerEntityModel already contains the final per-arm pose
        // applied by vanilla, Better Combat, and Player Animator for this frame.
        // Copy the ACTUAL rig angles rather than sampling a second animation
        // processor (which had different axes, handedness and interpolation).
        // Figura labels are mirrored: physical right (-X) is VSB LeftArm.
        // Vanilla's feature renderer uses this SAME pose to attach held items.
        var rigArms = VsbVanillaArmPose.capture(vanillaPose);
        var rigLegs = VsbVanillaLegPose.capture(vanillaPose);
        // Keep the approved R6 arm transforms untouched. Apply the same rig
        // mapping to both legs instead of playing a second VSB walk animation.
        Map<String, VsbFiguraAvatarRenderer.Vec3> rotations = new HashMap<>(rigArms.rotations());
        rotations.putAll(rigLegs.rotations());
        Map<String, VsbFiguraAvatarRenderer.Vec3> offsets = new HashMap<>(rigArms.offsets());
        offsets.putAll(rigLegs.offsets());
        // Stage-specific wings: folded at rest, wide during takeoff, deep heavy beats
        // in powered flight, slower braking beats during descent, then fold on landing.
        boolean airborne = !player.isOnGround();
        boolean powered = player.getAbilities().flying;
        double vertical = player.getVelocity().y;
        float targetOpen = powered ? 1.0F : airborne ? (vertical < -0.06D ? .72F : .90F) : .10F;
        if ("land".equals(animation)) targetOpen = .28F;
        float open = MathHelper.lerp(.17F, WING_OPEN.getOrDefault(id, targetOpen), targetOpen);
        WING_OPEN.put(id, open);
        float frequency = powered ? .56F : airborne ? (vertical > .05D ? .88F : .36F) : .11F;
        float beat = MathHelper.sin(animationProgress * frequency) * open;
        if (!airborne) beat *= .14F;
        float dive = airborne && vertical < -0.06D ? .32F : 0F;
        pose = new VsbFiguraAvatarRenderer.Pose(player.isSneaking(), animation, time,
                smYaw, smPitch, rigArms.rotations().isEmpty() ? player.handSwingProgress : 0F,
                rotations, offsets, open, beat, dive);
        EQUIPMENT_POSES.put(id, pose);
    }

    /**
     * The VSB model uses the opposite source names for the physical sides:
     * -X = LeftArm/LeftLeg, while Minecraft's rightArm/rightLeg also live at -X.
     * R9 attached the armor to the *opposite* source bone, so rotations and
     * positions disagreed with the visible VSB limbs during walking/combat.
     */
    public static void alignArmor(PlayerEntity player, EquipmentSlot slot, BipedEntityModel<?> model) {
        VsbFiguraAvatarRenderer.Pose pose = EQUIPMENT_POSES.getOrDefault(
                player.getUuid(), VsbFiguraAvatarRenderer.Pose.rest());
        setPart(model.head, "head", 0F, pose.crouching ? 2F : -2F, 0F, pose);
        setPart(model.hat, "head", 0F, pose.crouching ? 2F : -2F, 0F, pose);
        setPart(model.body, "body", 0F, -2F, 2F, pose);
        model.body.pitch = rad(17.5F);
        // Physical right (-X) uses Figura's LeftArm, not RightArm.
        setPart(model.rightArm, "LeftArm", -5F, -1F, 2F, pose);
        setPart(model.leftArm, "RightArm", 5F, -1F, 2F, pose);
        // Boots and leggings both rotate around the same VSB hip joint.
        alignLegArmor(model.rightLeg, "LeftLeg", -1.9F, 8F, pose);
        alignLegArmor(model.leftLeg, "RightLeg", 1.9F, 8F, pose);
    }

    private static void alignLegArmor(ModelPart part, String bone, float restX,
                                      float restY, VsbFiguraAvatarRenderer.Pose pose) {
        VsbFiguraAvatarRenderer.Vec3 delta = pose.vanillaArmOffsets.get(bone);
        float x = restX;
        float y = restY;
        float z = pose.crouching ? 1.5F : 6F;
        if (delta != null) {
            x += delta.x();
            y -= delta.y();
            z = 6F + delta.z();
        }
        setPart(part, bone, x, y, z, pose);
    }

    private static void setPart(ModelPart part, String bone, float x, float y, float z,
                                VsbFiguraAvatarRenderer.Pose pose) {
        VsbFiguraAvatarRenderer.Vec3 source = VsbFiguraAvatarRenderer.boneRotation(bone, pose);
        // Apply the same translated origin as the VSB geometry. This is
        // essential for Better Combat poses that move the shoulder itself.
        VsbFiguraAvatarRenderer.Vec3 offset = pose.vanillaArmOffsets.get(bone);
        if (offset != null && (bone.endsWith("Arm"))) {
            x += offset.x();
            y -= offset.y();
            z += offset.z();
        }
        part.pivotX = x;
        part.pivotY = y;
        part.pivotZ = z;
        part.pitch = rad(-source.x());
        part.yaw = rad(source.y());
        part.roll = rad(-source.z());
    }

    private static float rad(float degrees) {
        return degrees * ((float) Math.PI / 180.0F);
    }

    @Override
    public void setAngles(T entity, float limbAngle, float limbDistance, float animationProgress,
                          float headYaw, float headPitch) {
        prepare(entity, null, limbAngle, limbDistance, animationProgress, headYaw, headPitch);
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay,
                       float red, float green, float blue, float alpha) {
        VsbFiguraAvatarRenderer.render(matrices, vertices, light, overlay,
                red, green, blue, alpha, pose);
    }
}
