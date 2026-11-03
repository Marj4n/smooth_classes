package org.marj4n.smooth_classes.client.avenger.dance;

import java.util.*;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Vector3f;
import org.marj4n.smooth_classes.mixin.client.*;

/** One temporary cosmetic joint, deforming the ORIGINAL Cat foreleg faces/UVs.
 * No model replacement, texture replacement, entity mutation or global default edits. */
public final class CatForepawJoint {
    private static final Map<ModelPart, Float> ACTIVE = new IdentityHashMap<>();
    private static final Map<ModelPart.Cuboid, List<Face>> CACHE = new WeakHashMap<>();

    record V(float x, float y, float z, float u, float v) {}

    record Face(List<V> vertices, float nx, float ny, float nz, boolean lower) {}

    public static float contactError;

    /** Keep contact when vanilla STAY/look control turns the real head toward its owner. */
    public static void pose(DanceRig rig, float weight) {
        var head = rig.part("head_0");
        var arm = rig.part("body_3");
        if (head == null || arm == null) return;
        double x = .45,
                y = 1.35,
                z = -4.65,
                c = Math.cos(head.pitch),
                s = Math.sin(head.pitch),
                v = y;
        y = v * c - z * s;
        z = v * s + z * c;
        c = Math.cos(head.yaw);
        s = Math.sin(head.yaw);
        v = x;
        x = v * c + z * s;
        z = -v * s + z * c;
        c = Math.cos(head.roll);
        s = Math.sin(head.roll);
        v = x;
        x = v * c - y * s;
        y = v * s + y * c;
        x += head.pivotX - arm.pivotX;
        y += head.pivotY - arm.pivotY;
        z += head.pivotZ - arm.pivotZ;
        double d = x * x + y * y + z * z;
        double bend =
                Math.max(
                        -2.7,
                        Math.min(
                                0,
                                Math.atan2(10, 50)
                                        - Math.acos(
                                                Math.max(
                                                        -1,
                                                        Math.min(1, (d - 51) / Math.sqrt(2600))))));
        double ly = 5 + 5 * Math.cos(bend),
                lz = 1 + 5 * Math.sin(bend),
                pitch = Math.atan2(-Math.hypot(x, z), y) - Math.atan2(lz, ly),
                yaw = Math.atan2(-x, -z);
        arm.pitch += (float) (pitch - arm.pitch) * weight;
        arm.yaw += (float) (yaw - arm.yaw) * weight;
        arm.roll *= 1 - weight;
        set(arm, (float) bend * weight);
        double py = 5 + 5 * Math.cos(bend),
                pz = 1 + 5 * Math.sin(bend),
                ey = py * Math.cos(pitch) - pz * Math.sin(pitch),
                ez = py * Math.sin(pitch) + pz * Math.cos(pitch),
                ex = ez * Math.sin(yaw);
        ez *= Math.cos(yaw);
        contactError =
                (float) Math.sqrt((ex - x) * (ex - x) + (ey - y) * (ey - y) + (ez - z) * (ez - z));
    }

    public static void set(ModelPart p, float radians) {
        if (p != null && Float.isFinite(radians) && Math.abs(radians) < 3) ACTIVE.put(p, radians);
    }

    public static void clear(ModelPart p) {
        if (p != null) ACTIVE.remove(p);
    }

    private static List<V> clip(List<V> polygon, boolean lower) {
        var out = new ArrayList<V>();
        if (polygon.isEmpty()) return out;
        V a = polygon.get(polygon.size() - 1);
        boolean ai = lower ? a.y >= 5 : a.y <= 5;
        for (V b : polygon) {
            boolean bi = lower ? b.y >= 5 : b.y <= 5;
            if (ai != bi) {
                float t = (5 - a.y) / (b.y - a.y);
                out.add(
                        new V(
                                a.x + (b.x - a.x) * t,
                                5,
                                a.z + (b.z - a.z) * t,
                                a.u + (b.u - a.u) * t,
                                a.v + (b.v - a.v) * t));
            }
            if (bi) out.add(b);
            a = b;
            ai = bi;
        }
        return out;
    }

    static List<Face> faces(ModelPart.Cuboid cube) {
        var faces = new ArrayList<Face>();
        var vertices = CuboidVertexCollector.capture(cube);
        for (int i = 0; i < vertices.size(); i += 4) {
            var normal = vertices.get(i);
            var original = new ArrayList<V>(4);
            for (int j = 0; j < 4; j++) {
                var p = vertices.get(i + j);
                original.add(new V(p.x(), p.y(), p.z(), p.u(), p.v()));
            }
            for (boolean lower : new boolean[] {false, true}) {
                var cut = clip(original, lower);
                if (cut.size() >= 3)
                    faces.add(
                            new Face(
                                    List.copyOf(cut),
                                    normal.nx(),
                                    normal.ny(),
                                    normal.nz(),
                                    lower));
            }
        }
        return List.copyOf(faces);
    }

    public static boolean draw(
            ModelPart part,
            MatrixStack.Entry entry,
            VertexConsumer out,
            int light,
            int overlay,
            float r,
            float g,
            float b,
            float a) {
        if (ACTIVE.isEmpty()) return false;
        Float bend = ACTIVE.get(part);
        if (bend == null) return false;
        float c = (float) Math.cos(bend), s = (float) Math.sin(bend);
        var position = new Vector3f();
        var normal = new Vector3f();
        for (var cube : ((AvengerDancePartAccessor) (Object) part).smoothClasses$danceCuboids())
            for (var face : CACHE.computeIfAbsent(cube, CatForepawJoint::faces)) {
                float ny = face.ny, nz = face.nz;
                if (face.lower) {
                    float t = ny;
                    ny = t * c - nz * s;
                    nz = t * s + nz * c;
                }
                normal.set(face.nx, ny, nz).mul(entry.getNormalMatrix());
                // Clipping an axis-aligned vanilla quad at the elbow yields a quad.
                // Triangles, if encountered, repeat their last vertex for QUADS output.
                for (int i = 0; i < 4; i++) {
                    var v = face.vertices.get(Math.min(i, face.vertices.size() - 1));
                    float y = v.y, z = v.z;
                    if (face.lower) {
                        float dy = y - 5, dz = z - 1;
                        y = 5 + dy * c - dz * s;
                        z = 1 + dy * s + dz * c;
                    }
                    position.set(v.x / 16, y / 16, z / 16).mulPosition(entry.getPositionMatrix());
                    out.vertex(
                            position.x,
                            position.y,
                            position.z,
                            r,
                            g,
                            b,
                            a,
                            v.u,
                            v.v,
                            overlay,
                            light,
                            normal.x,
                            normal.y,
                            normal.z);
                }
            }
        return true;
    }

    private CatForepawJoint() {}
}
