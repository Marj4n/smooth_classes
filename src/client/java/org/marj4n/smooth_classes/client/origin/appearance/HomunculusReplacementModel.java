package org.marj4n.smooth_classes.client.origin.appearance;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.*;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.marj4n.smooth_classes.origin.HomunculusAccessories;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Homunculus player replacement directly driven by the approved Blockbench bbmodel.
 * Geometry and key animations are loaded from the bundled resource model so the in-game
 * body matches the approved concept instead of the older temporary placeholder mesh.
 */
public final class HomunculusReplacementModel<T extends LivingEntity> extends EntityModel<T> {
    public static final float RENDER_SCALE = 1.0F;
    private static final String MODEL_RESOURCE = "/assets/smooth_classes/blockbench/homunculus.bbmodel";
    private static final Definition DEF = Definition.load();
    private static final float DEG = ((float) Math.PI / 180.0F);

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart neck;
    private final ModelPart head;
    private final ModelPart leftArm;
    private final ModelPart rightArm;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;
    private final ModelPart bodyMoss;
    private boolean weaponMounted;

    public HomunculusReplacementModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.neck = body.getChild("neck");
        this.head = neck.getChild("head");
        this.leftArm = body.getChild("l_arm");
        this.rightArm = body.getChild("r_arm");
        this.leftLeg = root.getChild("l_leg");
        this.rightLeg = root.getChild("r_leg");
        this.bodyMoss = body.getChild("body_mossstuffs");
    }

    public static TexturedModelData createData() {
        return DEF.createTexturedModelData();
    }

    public void copyFrom(PlayerEntityModel<?> ignoredParent, PlayerEntity player,
                         float limbAngle, float limbDistance, float animationProgress,
                         float headYaw, float headPitch) {
        this.weaponMounted = HomunculusAccessories.patched(player, HomunculusAccessories.WEAPON);

        resetBone(body, "body");
        resetBone(neck, "neck");
        resetBone(head, "head");
        resetBone(leftArm, "l_arm");
        resetBone(rightArm, "r_arm");
        resetBone(leftLeg, "l_leg");
        resetBone(rightLeg, "r_leg");
        resetBone(bodyMoss, "body_mossstuffs");

        String animName;
        float animTime;
        if (player.handSwingProgress > 0.01F) {
            animName = "attack";
            animTime = DEF.length("attack") * MathHelper.clamp(player.handSwingProgress, 0.0F, 1.0F);
        } else if (limbDistance > 0.08F) {
            animName = "walk";
            animTime = limbAngle * 0.65F;
        } else {
            animName = "idle";
            animTime = animationProgress / 20.0F;
        }

        applyAnimation(animName, animTime);

        // Preserve some reactive look direction on top of the bbmodel animation.
        this.head.yaw += headYaw * DEG * 0.35F;
        this.head.pitch += headPitch * DEG * 0.25F;

        if (player.isSneaking()) {
            this.body.pitch += 0.20F;
            this.leftLeg.pitch -= 0.15F;
            this.rightLeg.pitch -= 0.15F;
            this.body.pivotY += 1.5F;
        }
    }

    @Override
    public void setAngles(T entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
        // Animation is applied from copyFrom(...) because it needs player-specific state.
    }

    private void resetBone(ModelPart part, String boneName) {
        Transform transform = DEF.defaultTransform(boneName);
        part.pivotX = transform.pivotX;
        part.pivotY = transform.pivotY;
        part.pivotZ = transform.pivotZ;
        part.pitch = 0.0F;
        part.yaw = 0.0F;
        part.roll = 0.0F;
        part.visible = true;
    }

    private void applyAnimation(String animationName, float time) {
        applyBoneAnimation(body, "body", animationName, time);
        applyBoneAnimation(neck, "neck", animationName, time);
        applyBoneAnimation(head, "head", animationName, time);
        applyBoneAnimation(leftArm, "l_arm", animationName, time);
        applyBoneAnimation(rightArm, "r_arm", animationName, time);
        applyBoneAnimation(leftLeg, "l_leg", animationName, time);
        applyBoneAnimation(rightLeg, "r_leg", animationName, time);
        applyBoneAnimation(bodyMoss, "body_mossstuffs", animationName, time);
    }

    private void applyBoneAnimation(ModelPart part, String boneName, String animationName, float time) {
        Animation animation = DEF.animation(animationName);
        if (animation == null) return;
        BoneAnimation bone = animation.bones.get(boneName);
        if (bone == null) return;
        Sample rot = sample(bone.rotation, time, animation.length, animation.loop);
        Sample pos = sample(bone.position, time, animation.length, animation.loop);
        part.pitch += rot.x * DEG;
        part.yaw += rot.y * DEG;
        part.roll += rot.z * DEG;
        part.pivotX -= pos.x;
        part.pivotY -= pos.y;
        part.pivotZ += pos.z;
    }

    private Sample sample(List<Keyframe> keyframes, float time, float length, boolean loop) {
        if (keyframes == null || keyframes.isEmpty()) return Sample.ZERO;
        if (keyframes.size() == 1) return keyframes.get(0).sample;
        float t = time;
        if (loop && length > 0.0F) {
            t = t % length;
            if (t < 0.0F) t += length;
        } else {
            t = MathHelper.clamp(t, keyframes.get(0).time, keyframes.get(keyframes.size() - 1).time);
        }
        Keyframe previous = keyframes.get(0);
        Keyframe next = keyframes.get(keyframes.size() - 1);
        for (int i = 1; i < keyframes.size(); i++) {
            next = keyframes.get(i);
            if (t <= next.time) {
                previous = keyframes.get(i - 1);
                break;
            }
        }
        if (Math.abs(next.time - previous.time) < 0.0001F) return previous.sample;
        float delta = (t - previous.time) / (next.time - previous.time);
        return Sample.lerp(previous.sample, next.sample, delta);
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay,
                       float red, float green, float blue, float alpha) {
        root.render(matrices, vertices, light, overlay, red, green, blue, alpha);
    }

    /** Eyes are already painted into the approved skin. No separate fullbright pass. */
    public void renderEyes(MatrixStack matrices, VertexConsumer vertices, int overlay) {
    }

    /** Installed weapon is rendered from the left modular arm anchor. */
    public void renderWeaponArm(PlayerEntity player, MatrixStack matrices,
                                VertexConsumerProvider consumers, int light) {
        ItemStack mounted = HomunculusAccessories.stack(player, HomunculusAccessories.WEAPON);
        if (!HomunculusAccessories.valid(HomunculusAccessories.WEAPON, mounted)) return;
        matrices.push();
        body.rotate(matrices);
        leftArm.rotate(matrices);
        matrices.translate(0.0F, 18.0F / 16.0F, 0.0F);
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-90.0F));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(180.0F));
        matrices.scale(0.96F, 0.96F, 0.96F);
        MinecraftClient.getInstance().getItemRenderer().renderItem(mounted,
                ModelTransformationMode.FIXED, light, OverlayTexture.DEFAULT_UV,
                matrices, consumers, player.getWorld(), player.getId());
        matrices.pop();
    }

    private static final class Definition {
        private final int textureWidth;
        private final int textureHeight;
        private final Map<String, BoneDef> bones;
        private final Map<String, Animation> animations;

        private Definition(int textureWidth, int textureHeight, Map<String, BoneDef> bones, Map<String, Animation> animations) {
            this.textureWidth = textureWidth;
            this.textureHeight = textureHeight;
            this.bones = bones;
            this.animations = animations;
        }

        static Definition load() {
            try (var stream = HomunculusReplacementModel.class.getResourceAsStream(MODEL_RESOURCE)) {
                if (stream == null) throw new IllegalStateException("Missing Homunculus bbmodel resource: " + MODEL_RESOURCE);
                JsonObject root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();

                JsonObject texture = root.getAsJsonArray("textures").get(0).getAsJsonObject();
                int texWidth = texture.get("uv_width").getAsInt();
                int texHeight = texture.get("uv_height").getAsInt();

                Map<String, JsonObject> groupsById = new HashMap<>();
                for (JsonElement element : root.getAsJsonArray("groups")) {
                    JsonObject group = element.getAsJsonObject();
                    groupsById.put(group.get("uuid").getAsString(), group);
                }
                Map<String, JsonObject> elementsById = new HashMap<>();
                for (JsonElement element : root.getAsJsonArray("elements")) {
                    JsonObject cube = element.getAsJsonObject();
                    elementsById.put(cube.get("uuid").getAsString(), cube);
                }

                LinkedHashMap<String, BoneDef> bones = new LinkedHashMap<>();
                JsonArray outliner = root.getAsJsonArray("outliner");
                for (JsonElement element : outliner) {
                    if (!element.isJsonObject()) continue;
                    JsonObject node = element.getAsJsonObject();
                    JsonObject group = groupsById.get(node.get("uuid").getAsString());
                    if (group == null) continue;
                    String groupName = normalize(group.get("name").getAsString());
                    if ("base".equals(groupName)) {
                        parseChildren(node.getAsJsonArray("children"), null, groupsById, elementsById, bones);
                    } else {
                        parseGroup(node, null, groupsById, elementsById, bones);
                    }
                }

                Map<String, Animation> animations = new HashMap<>();
                for (JsonElement element : root.getAsJsonArray("animations")) {
                    JsonObject animationObject = element.getAsJsonObject();
                    String name = normalize(animationObject.get("name").getAsString());
                    float length = animationObject.get("length").getAsFloat();
                    String loopMode = animationObject.has("loop") ? animationObject.get("loop").getAsString() : "once";
                    boolean loop = "loop".equalsIgnoreCase(loopMode);
                    Map<String, BoneAnimation> boneAnimations = new HashMap<>();
                    JsonObject animators = animationObject.getAsJsonObject("animators");
                    for (Map.Entry<String, JsonElement> entry : animators.entrySet()) {
                        String uuid = entry.getKey();
                        JsonObject animator = entry.getValue().getAsJsonObject();
                        JsonObject group = groupsById.get(uuid);
                        if (group == null) continue;
                        String boneName = normalize(group.get("name").getAsString());
                        if (!bones.containsKey(boneName)) continue;
                        List<Keyframe> rotationFrames = new ArrayList<>();
                        List<Keyframe> positionFrames = new ArrayList<>();
                        JsonArray keyframes = animator.getAsJsonArray("keyframes");
                        if (keyframes != null) {
                            for (JsonElement keyframeElement : keyframes) {
                                JsonObject keyframe = keyframeElement.getAsJsonObject();
                                String channel = keyframe.get("channel").getAsString();
                                float t = keyframe.get("time").getAsFloat();
                                JsonObject point = keyframe.getAsJsonArray("data_points").get(0).getAsJsonObject();
                                Sample sample = new Sample(parseFloat(point, "x"), parseFloat(point, "y"), parseFloat(point, "z"));
                                if ("rotation".equals(channel)) rotationFrames.add(new Keyframe(t, sample));
                                else if ("position".equals(channel)) positionFrames.add(new Keyframe(t, sample));
                            }
                        }
                        rotationFrames.sort(Comparator.comparingDouble(k -> k.time));
                        positionFrames.sort(Comparator.comparingDouble(k -> k.time));
                        boneAnimations.put(boneName, new BoneAnimation(rotationFrames, positionFrames));
                    }
                    animations.put(name, new Animation(length, loop, boneAnimations));
                }

                return new Definition(texWidth, texHeight, bones, animations);
            } catch (Exception exception) {
                throw new RuntimeException("Failed to load Homunculus bbmodel", exception);
            }
        }

        private static void parseChildren(JsonArray children, String parentName,
                                          Map<String, JsonObject> groupsById,
                                          Map<String, JsonObject> elementsById,
                                          Map<String, BoneDef> bones) {
            for (JsonElement childElement : children) {
                if (childElement.isJsonObject()) {
                    parseGroup(childElement.getAsJsonObject(), parentName, groupsById, elementsById, bones);
                } else if (childElement.isJsonPrimitive()) {
                    String uuid = childElement.getAsString();
                    if (groupsById.containsKey(uuid)) {
                        JsonObject synthetic = new JsonObject();
                        synthetic.addProperty("uuid", uuid);
                        synthetic.add("children", new JsonArray());
                        parseGroup(synthetic, parentName, groupsById, elementsById, bones);
                    }
                }
            }
        }

        private static void parseGroup(JsonObject node, String parentName,
                                       Map<String, JsonObject> groupsById,
                                       Map<String, JsonObject> elementsById,
                                       Map<String, BoneDef> bones) {
            JsonObject group = groupsById.get(node.get("uuid").getAsString());
            if (group == null) return;
            String boneName = normalize(group.get("name").getAsString());
            JsonArray origin = group.getAsJsonArray("origin");
            float originX = origin.get(0).getAsFloat();
            float originY = origin.get(1).getAsFloat();
            float originZ = origin.get(2).getAsFloat();
            BoneDef parent = parentName == null ? null : bones.get(parentName);
            float pivotX = parent == null ? -originX : parent.originX - originX;
            float pivotY = parent == null ? 24.0F - originY : parent.originY - originY;
            float pivotZ = parent == null ? originZ : originZ - parent.originZ;
            BoneDef bone = new BoneDef(boneName, parentName, originX, originY, originZ, new Transform(pivotX, pivotY, pivotZ));
            bones.put(boneName, bone);
            if (parent != null) parent.children.add(boneName);

            JsonArray children = node.getAsJsonArray("children");
            int cubeIndex = 0;
            if (children != null) {
                for (JsonElement childElement : children) {
                    if (childElement.isJsonObject()) {
                        parseGroup(childElement.getAsJsonObject(), boneName, groupsById, elementsById, bones);
                    } else if (childElement.isJsonPrimitive()) {
                        String uuid = childElement.getAsString();
                        JsonObject cube = elementsById.get(uuid);
                        if (cube != null) {
                            String cubeName = cube.has("name") ? cube.get("name").getAsString() : "cube";
                            cubeName = sanitize(cubeName) + "_" + cubeIndex++;
                            JsonArray from = cube.getAsJsonArray("from");
                            JsonArray to = cube.getAsJsonArray("to");
                            // Blockbench may omit uv_offset even when box_uv=true
                            // (notably the head and metal joints in our approved model).
                            // Recover the original box-UV origin from the baked per-face
                            // coordinates instead of crashing during resource reload.
                            int[] uv = resolveBoxUv(cube);
                            bone.cubes.add(new CubeDef(
                                    cubeName,
                                    uv[0],
                                    uv[1],
                                    from.get(0).getAsFloat() - originX,
                                    originY - to.get(1).getAsFloat(),
                                    from.get(2).getAsFloat() - originZ,
                                    to.get(0).getAsFloat() - from.get(0).getAsFloat(),
                                    to.get(1).getAsFloat() - from.get(1).getAsFloat(),
                                    to.get(2).getAsFloat() - from.get(2).getAsFloat()
                            ));
                        }
                    }
                }
            }
        }

        /**
         * Returns the origin of the standard Minecraft/Blockbench box UV layout.
         * Some correctly textured Blockbench cubes serialize only faces[].uv and
         * omit uv_offset. The north face begins one cube-depth after the origin,
         * while the up face starts at its top edge. This preserves the atlas
         * placement of the head and removable silver joints exactly.
         */
        private static int[] resolveBoxUv(JsonObject cube) {
            JsonArray uvOffset = cube.getAsJsonArray("uv_offset");
            if (uvOffset != null && uvOffset.size() >= 2) {
                return new int[]{uvOffset.get(0).getAsInt(), uvOffset.get(1).getAsInt()};
            }
            if (cube.has("box_uv") && !cube.get("box_uv").getAsBoolean()) {
                throw new IllegalArgumentException("Homunculus cube has per-face UV layout, not box UV: "
                        + cube.get("uuid").getAsString());
            }
            JsonObject faces = cube.getAsJsonObject("faces");
            JsonObject north = faces == null ? null : faces.getAsJsonObject("north");
            JsonObject up = faces == null ? null : faces.getAsJsonObject("up");
            if (north == null || up == null) {
                throw new IllegalArgumentException("Homunculus cube is missing UV faces: "
                        + cube.get("uuid").getAsString());
            }
            JsonArray northUv = north.getAsJsonArray("uv");
            JsonArray upUv = up.getAsJsonArray("uv");
            if (northUv == null || northUv.size() < 4 || upUv == null || upUv.size() < 4) {
                throw new IllegalArgumentException("Invalid UV face for Homunculus cube: "
                        + cube.get("uuid").getAsString());
            }
            JsonArray from = cube.getAsJsonArray("from");
            JsonArray to = cube.getAsJsonArray("to");
            int depth = Math.round(Math.abs(to.get(2).getAsFloat() - from.get(2).getAsFloat()));
            int u = Math.round(Math.min(northUv.get(0).getAsFloat(), northUv.get(2).getAsFloat())) - depth;
            int v = Math.round(Math.min(upUv.get(1).getAsFloat(), upUv.get(3).getAsFloat()));
            if (u < 0 || v < 0) {
                throw new IllegalArgumentException("Out-of-bounds derived UV for Homunculus cube: "
                        + cube.get("uuid").getAsString());
            }
            return new int[]{u, v};
        }

        TexturedModelData createTexturedModelData() {
            ModelData modelData = new ModelData();
            ModelPartData root = modelData.getRoot();
            for (BoneDef bone : bones.values()) {
                if (bone.parentName == null) buildBone(root, bone);
            }
            return TexturedModelData.of(modelData, textureWidth, textureHeight);
        }

        private void buildBone(ModelPartData parent, BoneDef bone) {
            ModelPartData bonePart = parent.addChild(bone.name, ModelPartBuilder.create(),
                    ModelTransform.pivot(bone.transform.pivotX, bone.transform.pivotY, bone.transform.pivotZ));
            for (CubeDef cube : bone.cubes) {
                bonePart.addChild(cube.name,
                        ModelPartBuilder.create().uv(cube.uvX, cube.uvY)
                                .cuboid(cube.x, cube.y, cube.z, cube.sizeX, cube.sizeY, cube.sizeZ),
                        ModelTransform.NONE);
            }
            for (String child : bone.children) {
                buildBone(bonePart, bones.get(child));
            }
        }

        Transform defaultTransform(String boneName) {
            BoneDef bone = bones.get(boneName);
            return bone == null ? Transform.ZERO : bone.transform;
        }

        Animation animation(String name) {
            return animations.get(normalize(name));
        }

        float length(String name) {
            Animation animation = animation(name);
            return animation == null ? 0.0F : animation.length;
        }

        private static float parseFloat(JsonObject obj, String key) {
            return obj.has(key) ? Float.parseFloat(obj.get(key).getAsString()) : 0.0F;
        }

        private static String normalize(String input) {
            return input.toLowerCase(Locale.ROOT).replace(' ', '_');
        }

        private static String sanitize(String input) {
            String out = normalize(input).replaceAll("[^a-z0-9_]+", "_");
            while (out.contains("__")) out = out.replace("__", "_");
            return out;
        }
    }

    private record Transform(float pivotX, float pivotY, float pivotZ) {
        private static final Transform ZERO = new Transform(0.0F, 0.0F, 0.0F);
    }

    private static final class BoneDef {
        private final String name;
        private final String parentName;
        private final float originX;
        private final float originY;
        private final float originZ;
        private final Transform transform;
        private final List<CubeDef> cubes = new ArrayList<>();
        private final List<String> children = new ArrayList<>();

        private BoneDef(String name, String parentName, float originX, float originY, float originZ, Transform transform) {
            this.name = name;
            this.parentName = parentName;
            this.originX = originX;
            this.originY = originY;
            this.originZ = originZ;
            this.transform = transform;
        }
    }

    private record CubeDef(String name, int uvX, int uvY,
                           float x, float y, float z,
                           float sizeX, float sizeY, float sizeZ) {
    }

    private record Keyframe(float time, Sample sample) {
    }

    private record BoneAnimation(List<Keyframe> rotation, List<Keyframe> position) {
    }

    private record Animation(float length, boolean loop, Map<String, BoneAnimation> bones) {
    }

    private record Sample(float x, float y, float z) {
        private static final Sample ZERO = new Sample(0.0F, 0.0F, 0.0F);

        private static Sample lerp(Sample a, Sample b, float delta) {
            return new Sample(
                    MathHelper.lerp(delta, a.x, b.x),
                    MathHelper.lerp(delta, a.y, b.y),
                    MathHelper.lerp(delta, a.z, b.z)
            );
        }
    }
}
