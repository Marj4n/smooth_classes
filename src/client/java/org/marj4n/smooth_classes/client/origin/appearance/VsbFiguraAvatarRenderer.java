package org.marj4n.smooth_classes.client.origin.appearance;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Direct, source-driven native renderer for the vampire avatar shipped inside
 * the user-supplied Vampires Strike Back 1.8.1 archive.
 *
 * Nothing is reconstructed with ModelPartBuilder: a ModelPart cuboid has a fixed
 * UV net and cannot preserve the arbitrary UV rectangle/mirroring assigned to
 * EACH FACE in a Figura Blockbench model. Every corner, face UV, origin, rotation,
 * animation keyframe and hierarchy comes directly from the supplied .bbmodel.
 *
 * Figura is NOT loaded; only its geometry/animation data are interpreted. This
 * never executes VSB Lua, skills or gameplay behavior. Minecraft's vanilla player
 * renderer still owns movement, equipment and the existing Smooth Classes logic.
 */
final class VsbFiguraAvatarRenderer {
    private static final String PATH = "/assets/smooth_classes/vsb_vampire_model.bbmodel";
    private static final float PIXEL = 1.0F / 16.0F;
    private static final Avatar AVATAR = load();

    private VsbFiguraAvatarRenderer() {}

    static void render(MatrixStack stack, VertexConsumer vertices, int light, int overlay,
                       float red, float green, float blue, float alpha, Pose pose) {
        stack.push();
        // The vanilla player render origin is model Y=0 at the upper body and
        // Y=24 at the soles. Figura is authored with the soles at Y=0 and Y UP.
        stack.translate(0.0D, 24.0D / 16.0D, 0.0D);
        drawGroup(AVATAR.root, new Vec3(0, 0, 0), stack, vertices, light, overlay,
                red, green, blue, alpha, pose);
        stack.pop();
    }

    /**
     * Applies the exact nested Figura pivots to an attached item. No geometry is
     * approximated: this follows the same group tree used to draw the avatar.
     */
    static void atBone(MatrixStack matrices, String name, Pose pose, Runnable renderer) {
        matrices.push();
        matrices.translate(0.0D, 24.0D / 16.0D, 0.0D);
        findBone(AVATAR.root, new Vec3(0, 0, 0), name, pose, matrices, renderer);
        matrices.pop();
    }

    private static boolean findBone(Group group, Vec3 parentOrigin, String wanted,
                                    Pose pose, MatrixStack matrices, Runnable renderer) {
        matrices.push();
        transformGroup(group, parentOrigin, pose, matrices);
        if (wanted.equals(group.name)) {
            renderer.run();
            matrices.pop();
            return true;
        }
        for (Object child : group.children) {
            if (child instanceof Group nested
                    && findBone(nested, group.origin, wanted, pose, matrices, renderer)) {
                matrices.pop();
                return true;
            }
        }
        matrices.pop();
        return false;
    }

    static Vec3 boneRotation(String name, Pose pose) {
        Group group = AVATAR.byName.get(name);
        if (group == null) return Vec3.ZERO;
        Track track = AVATAR.tracks.getOrDefault(pose.clip, Map.of()).get(group.uuid);
        Vec3 clipRotation = track == null ? Vec3.ZERO : track.sample("rotation", pose.clipTime);
        Vec3 value = group.rotation.add(clipRotation);
        if (name.equals("head")) {
            // The source head has an authored -10 degree rest tilt and
            // independent clip rotations. Do not stack them on top of vanilla:
            // arms/legs already work by copying their finished player rig pose.
            // One source for every clip (idle, walk, attack, inventory, flight).
            return pose.combatRotations.getOrDefault("head", Vec3.ZERO);
        }
        if (name.equals("rightwinganchor") || name.equals("leftwinganchor")) {
            // VSB's idle animation erroneously animates *leftwinganchor* under
            // the name "leftear" while rightwinganchor has no matching track.
            // The wings already animate below, so the left anchor was DOUBLE
            // rotated. Neutralize both anchors and let mirrored wing bones drive.
            return group.rotation;
        }
        // Mirrored wing math: pitch matches; spread and flap invert for the
        // opposite side. Do NOT stack the source Figura idle wing tracks.
        if (name.equals("rightwing") || name.equals("leftwing")) {
            ManBatWingSymmetry.WingRotation wing = ManBatWingSymmetry.rotation(
                    name.equals("leftwing"), pose.wingOpen, pose.wingBeat, pose.wingDive);
            return new Vec3(wing.pitch(), wing.yaw(), wing.roll());
        }

        // The live vanilla/Better Combat model supplies finished limb rotations.
        // Replace only the animated arm TRACK (not the source resting angle):
        // otherwise VSB walk/idle rotations stack a second time and invert
        // greatsword/claymore swings. Keep the original +/-5 degree arm roll.
        Vec3 external = pose.combatRotations.get(name);
        if (external != null && (name.equals("LeftArm") || name.equals("RightArm")
                || name.equals("LeftLeg") || name.equals("RightLeg"))) {
            value = group.rotation.add(external);
        }
        if (name.equals("RightArm") && pose.armSwing > 0F) {
            value = value.add(new Vec3(MathHelper.sin(pose.armSwing * (float) Math.PI) * 65F, 0, 0));
        }
        return value;
    }

    static final class Pose {
        final boolean crouching;
        final String clip;
        final float clipTime;
        final float armSwing;
        final Map<String, Vec3> combatRotations;
        final Map<String, Vec3> vanillaArmOffsets;
        final float wingOpen;
        final float wingBeat;
        final float wingDive;

        Pose(boolean crouching, String clip, float clipTime,
             float armSwing, Map<String, Vec3> combatRotations,
             Map<String, Vec3> vanillaArmOffsets,
             float wingOpen, float wingBeat, float wingDive) {
            this.crouching = crouching;
            this.clip = clip;
            this.clipTime = clipTime;
            this.armSwing = armSwing;
            this.combatRotations = combatRotations;
            this.vanillaArmOffsets = vanillaArmOffsets;
            this.wingOpen = wingOpen;
            this.wingBeat = wingBeat;
            this.wingDive = wingDive;
        }

        static Pose rest() {
            return new Pose(false, "idle", 0F, 0F, Map.of(), Map.of(), .10F, 0F, 0F);
        }
    }

    private static void drawGroup(Group group, Vec3 parentOrigin, MatrixStack stack,
                                  VertexConsumer out, int light, int overlay,
                                  float red, float green, float blue, float alpha, Pose pose) {
        stack.push();
        transformGroup(group, parentOrigin, pose, stack);

        for (Object child : group.children) {
            if (child instanceof Group nested) {
                drawGroup(nested, group.origin, stack, out, light, overlay,
                        red, green, blue, alpha, pose);
            } else if (child instanceof Cube cube) {
                drawCube(cube, group.origin, stack, out, light, overlay,
                        red, green, blue, alpha);
            }
        }
        stack.pop();
    }

    private static void transformGroup(Group group, Vec3 parentOrigin, Pose pose, MatrixStack stack) {
        Vec3 local = group.origin.subtract(parentOrigin);
        float x = local.x, y = local.y, z = local.z;
        if (pose.crouching) {
            if (group.name.equals("head")) y -= 4.0F;
            if ((group.name.equals("LeftLeg") || group.name.equals("RightLeg"))
                    && !pose.vanillaArmOffsets.containsKey(group.name)) z -= 4.5F;
        }
        Track track = AVATAR.tracks.getOrDefault(pose.clip, Map.of()).get(group.uuid);
        Vec3 position = track == null ? Vec3.ZERO : track.sample("position", pose.clipTime);
        Vec3 vanillaOffset = pose.vanillaArmOffsets.get(group.name);
        if (vanillaOffset != null) position = position.add(vanillaOffset);
        stack.translate((x + position.x) * PIXEL, -(y + position.y) * PIXEL,
                (z + position.z) * PIXEL);
        rotate(stack, boneRotation(group.name, pose));
        // Enlarge the former ear membranes into actual back wings.
        if (group.name.equals("rightwing") || group.name.equals("leftwing")) {
            stack.scale(1.25F, 1.36F, 1.26F);
        }
        Vec3 scale = track == null ? new Vec3(1F, 1F, 1F) : track.sampleScale(pose.clipTime);
        stack.scale(scale.x, scale.y, scale.z);
    }

    private static void drawCube(Cube cube, Vec3 parentOrigin, MatrixStack stack, VertexConsumer out,
                                 int light, int overlay, float red, float green, float blue, float alpha) {
        stack.push();
        Vec3 localOrigin = cube.origin.subtract(parentOrigin);
        stack.translate(localOrigin.x * PIXEL, -localOrigin.y * PIXEL, localOrigin.z * PIXEL);
        rotate(stack, cube.rotation);

        float x0 = cube.from.x - cube.inflate - cube.origin.x;
        float x1 = cube.to.x + cube.inflate - cube.origin.x;
        float y0 = cube.from.y - cube.inflate - cube.origin.y;
        float y1 = cube.to.y + cube.inflate - cube.origin.y;
        float z0 = cube.from.z - cube.inflate - cube.origin.z;
        float z1 = cube.to.z + cube.inflate - cube.origin.z;

        // Coordinates and per-face UVs are taken directly from the file, not
        // reconstructed from Minecraft's fixed biped/box UV layout.
        face(cube, "north", stack, out, light, overlay, red, green, blue, alpha,
                new float[][]{{x0,y1,z0},{x1,y1,z0},{x1,y0,z0},{x0,y0,z0}}, 0,0,-1);
        face(cube, "south", stack, out, light, overlay, red, green, blue, alpha,
                new float[][]{{x1,y1,z1},{x0,y1,z1},{x0,y0,z1},{x1,y0,z1}}, 0,0,1);
        face(cube, "east", stack, out, light, overlay, red, green, blue, alpha,
                new float[][]{{x1,y1,z0},{x1,y1,z1},{x1,y0,z1},{x1,y0,z0}}, 1,0,0);
        face(cube, "west", stack, out, light, overlay, red, green, blue, alpha,
                new float[][]{{x0,y1,z1},{x0,y1,z0},{x0,y0,z0},{x0,y0,z1}}, -1,0,0);
        face(cube, "up", stack, out, light, overlay, red, green, blue, alpha,
                new float[][]{{x0,y1,z0},{x1,y1,z0},{x1,y1,z1},{x0,y1,z1}}, 0,-1,0);
        face(cube, "down", stack, out, light, overlay, red, green, blue, alpha,
                new float[][]{{x0,y0,z1},{x1,y0,z1},{x1,y0,z0},{x0,y0,z0}}, 0,1,0);
        stack.pop();
    }

    private static void face(Cube cube, String side, MatrixStack stack, VertexConsumer out,
                             int light, int overlay, float red, float green, float blue,
                             float alpha, float[][] points, float nx, float ny, float nz) {
        Face face = cube.faces.get(side);
        if (face == null || face.texture != 0) return; // Texture 0 is the exact base atlas.
        float[] uv = face.uv;
        float[] us = {uv[0], uv[2], uv[2], uv[0]};
        float[] vs = {uv[1], uv[1], uv[3], uv[3]};
        MatrixStack.Entry entry = stack.peek();
        for (int i = 0; i < 4; i++) {
            float[] p = points[i];
            out.vertex(entry.getPositionMatrix(), p[0] * PIXEL, -p[1] * PIXEL, p[2] * PIXEL)
                    .color(red, green, blue, alpha)
                    .texture(us[i] / AVATAR.textureWidth, vs[i] / AVATAR.textureHeight)
                    .overlay(overlay).light(light)
                    .normal(entry.getNormalMatrix(), nx, ny, nz).next();
        }
    }

    private static void rotate(MatrixStack stack, Vec3 deg) {
        // Minecraft ModelPart's canonical Euler composition is Z, Y, X.
        if (deg.z != 0F) stack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-deg.z));
        if (deg.y != 0F) stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(deg.y));
        if (deg.x != 0F) stack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-deg.x));
    }

    private static Avatar load() {
        try (InputStream input = VsbFiguraAvatarRenderer.class.getResourceAsStream(PATH)) {
            if (input == null) throw new IllegalStateException("Missing bundled original VSB avatar: " + PATH);
            JsonObject data = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonObject resolution = data.getAsJsonObject("resolution");
            int tw = resolution.get("width").getAsInt(), th = resolution.get("height").getAsInt();
            Map<String, Cube> cubes = new HashMap<>();
            for (JsonElement item : data.getAsJsonArray("elements")) {
                JsonObject node = item.getAsJsonObject();
                if (!"cube".equals(str(node, "type", "cube"))) continue;
                Map<String, Face> faces = new HashMap<>();
                for (Map.Entry<String, JsonElement> e : node.getAsJsonObject("faces").entrySet()) {
                    JsonObject f = e.getValue().getAsJsonObject();
                    if (!f.has("uv") || f.get("uv").isJsonNull()) continue;
                    faces.put(e.getKey(), new Face(array4(f.getAsJsonArray("uv")),
                            f.has("texture") && !f.get("texture").isJsonNull()
                                    ? f.get("texture").getAsInt() : -1));
                }
                cubes.put(node.get("uuid").getAsString(), new Cube(
                        vec(node, "from"), vec(node, "to"), vec(node, "origin"),
                        vecOpt(node, "rotation"), decimal(node, "inflate", 0F), faces));
            }
            Map<String, Group> names = new HashMap<>();
            Group root = readGroup(data.getAsJsonArray("outliner").get(0).getAsJsonObject(), cubes, names);
            Map<String, Map<String, Track>> animations = new HashMap<>();
            for (JsonElement item : data.getAsJsonArray("animations")) {
                JsonObject clip = item.getAsJsonObject();
                if (!clip.has("animators")) continue;
                Map<String, Track> tracks = new HashMap<>();
                for (Map.Entry<String, JsonElement> entry : clip.getAsJsonObject("animators").entrySet()) {
                    List<Key> keys = new ArrayList<>();
                    for (JsonElement keyJson : entry.getValue().getAsJsonObject().getAsJsonArray("keyframes")) {
                        JsonObject key = keyJson.getAsJsonObject();
                        JsonObject point = key.getAsJsonArray("data_points").get(0).getAsJsonObject();
                        keys.add(new Key(key.get("channel").getAsString(), key.get("time").getAsFloat(),
                                new Vec3(value(point,"x"), value(point,"y"), value(point,"z"))));
                    }
                    keys.sort(Comparator.comparingDouble(Key::time));
                    tracks.put(entry.getKey(), new Track(keys));
                }
                animations.put(clip.get("name").getAsString(), tracks);
            }
            return new Avatar(root, names, animations, tw, th);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot read exact user-provided VSB Blockbench avatar", e);
        }
    }

    private static Group readGroup(JsonObject obj, Map<String, Cube> cubes, Map<String, Group> names) {
        Group group = new Group(str(obj,"uuid",""), str(obj,"name",""),
                vecOpt(obj,"origin"), vecOpt(obj,"rotation"));
        names.put(group.name, group);
        for (JsonElement child : obj.getAsJsonArray("children")) {
            if (child.isJsonPrimitive()) {
                Cube cube = cubes.get(child.getAsString());
                if (cube != null) group.children.add(cube);
            } else if (child.isJsonObject()) {
                group.children.add(readGroup(child.getAsJsonObject(), cubes, names));
            }
        }
        return group;
    }

    private static float value(JsonObject obj, String key) {
        try { return obj.has(key) ? obj.get(key).getAsFloat() : 0F; }
        catch (RuntimeException ignored) { return 0F; }
    }
    private static float decimal(JsonObject obj, String key, float fallback) {
        return obj.has(key) ? obj.get(key).getAsFloat() : fallback;
    }
    private static String str(JsonObject obj, String key, String fallback) {
        return obj.has(key) ? obj.get(key).getAsString() : fallback;
    }
    private static Vec3 vec(JsonObject obj, String key) {
        JsonArray arr = obj.getAsJsonArray(key);
        return new Vec3(arr.get(0).getAsFloat(), arr.get(1).getAsFloat(), arr.get(2).getAsFloat());
    }
    private static Vec3 vecOpt(JsonObject obj, String key) {
        return obj.has(key) && obj.get(key).isJsonArray() ? vec(obj,key) : Vec3.ZERO;
    }
    private static float[] array4(JsonArray arr) {
        return new float[]{arr.get(0).getAsFloat(),arr.get(1).getAsFloat(),
                arr.get(2).getAsFloat(),arr.get(3).getAsFloat()};
    }

    private record Avatar(Group root, Map<String, Group> byName,
                          Map<String, Map<String, Track>> tracks, int textureWidth, int textureHeight) {}
    private static final class Group {
        final String uuid, name;
        final Vec3 origin, rotation;
        final List<Object> children = new ArrayList<>();
        Group(String uuid, String name, Vec3 origin, Vec3 rotation) {
            this.uuid=uuid; this.name=name; this.origin=origin; this.rotation=rotation;
        }
    }
    private record Cube(Vec3 from, Vec3 to, Vec3 origin, Vec3 rotation,
                        float inflate, Map<String, Face> faces) {}
    private record Face(float[] uv, int texture) {}
    private record Key(String channel, float time, Vec3 point) {}
    record Vec3(float x, float y, float z) {
        static final Vec3 ZERO = new Vec3(0,0,0);
        Vec3 add(Vec3 other) { return new Vec3(x+other.x,y+other.y,z+other.z); }
        Vec3 subtract(Vec3 other) { return new Vec3(x-other.x,y-other.y,z-other.z); }
        Vec3 interpolate(Vec3 other, float t) {
            return new Vec3(MathHelper.lerp(t,x,other.x), MathHelper.lerp(t,y,other.y),
                    MathHelper.lerp(t,z,other.z));
        }
    }
    private record Track(List<Key> keys) {
        Vec3 sample(String channel, float time) {
            Key prev = null;
            for (Key k : keys) {
                if (!k.channel.equals(channel)) continue;
                if (prev == null) {
                    prev = k;
                    if (time <= k.time) return k.point;
                } else if (time <= k.time) {
                    float d = k.time - prev.time;
                    return prev.point.interpolate(k.point, d <= 0F ? 0F : (time-prev.time)/d);
                } else {
                    prev = k;
                }
            }
            return prev == null ? Vec3.ZERO : prev.point;
        }
        Vec3 sampleScale(float time) {
            boolean found = keys.stream().anyMatch(k -> k.channel.equals("scale"));
            return found ? sample("scale",time) : new Vec3(1F,1F,1F);
        }
    }
}
