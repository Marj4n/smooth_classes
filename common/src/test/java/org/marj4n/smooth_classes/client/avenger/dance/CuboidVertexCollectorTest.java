package org.marj4n.smooth_classes.client.avenger.dance;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.util.EnumSet;
import java.util.Set;
import net.minecraft.client.model.ModelPart;
import net.minecraft.util.math.Direction;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CuboidVertexCollectorTest {
    @ParameterizedTest
    @CsvSource({"false,0", "true,0", "false,0.25", "true,0.25"})
    void preservesVanillaPositionsUvsWindingAndNormals(boolean mirror, float dilation)
            throws ReflectiveOperationException {
        verify(
                new ModelPart.Cuboid(
                        4,
                        7,
                        -1,
                        0,
                        -1,
                        2,
                        10,
                        2,
                        dilation,
                        dilation,
                        dilation,
                        mirror,
                        64,
                        32,
                        EnumSet.allOf(Direction.class)),
                24);
    }

    @Test
    void preservesSelectedFacesWithoutInventingMissingOnes() throws ReflectiveOperationException {
        verify(
                new ModelPart.Cuboid(
                        4,
                        7,
                        -1,
                        0,
                        -1,
                        2,
                        10,
                        2,
                        0,
                        0,
                        0,
                        false,
                        64,
                        32,
                        Set.of(Direction.NORTH, Direction.UP)),
                8);
    }

    @Test
    void elbowSplitPreservesSurfaceAreaAndKeepsUvsContinuous() {
        var cube =
                new ModelPart.Cuboid(
                        4,
                        7,
                        -1,
                        0,
                        -1,
                        2,
                        10,
                        2,
                        0,
                        0,
                        0,
                        false,
                        64,
                        32,
                        EnumSet.allOf(Direction.class));
        var faces = CatForepawJoint.faces(cube);
        assertEquals(10, faces.size());
        double area = 0;
        int seamVertices = 0;
        for (var face : faces) {
            var vertices = face.vertices();
            var a = vertices.get(0);
            for (int i = 1; i < vertices.size() - 1; i++) {
                var b = vertices.get(i);
                var c = vertices.get(i + 1);
                area +=
                        new Vector3f(b.x() - a.x(), b.y() - a.y(), b.z() - a.z())
                                        .cross(c.x() - a.x(), c.y() - a.y(), c.z() - a.z())
                                        .length()
                                / 2;
            }
            for (var vertex : vertices) {
                assertTrue(face.lower() ? vertex.y() >= 5 : vertex.y() <= 5);
                if (vertex.y() != 5) continue;
                seamVertices++;
                assertTrue(
                        faces.stream()
                                .filter(other -> other.lower() != face.lower())
                                .flatMap(other -> other.vertices().stream())
                                .anyMatch(other -> other.equals(vertex)),
                        "UV seam must meet on both halves");
            }
        }
        assertEquals(88, area, 1e-5);
        assertEquals(16, seamVertices);
    }

    private static void verify(ModelPart.Cuboid cube, int count)
            throws ReflectiveOperationException {
        var captured = CuboidVertexCollector.capture(cube);
        assertEquals(count, captured.size());
        Object[] quads = (Object[]) field(cube, "sides");
        int i = 0;
        for (Object quad : quads) {
            Vector3f normal = (Vector3f) field(quad, "direction");
            for (Object vertex : (Object[]) field(quad, "vertices")) {
                Vector3f position = (Vector3f) field(vertex, "pos");
                var actual = captured.get(i++);
                assertEquals(position.x, actual.x(), 1e-6);
                assertEquals(position.y, actual.y(), 1e-6);
                assertEquals(position.z, actual.z(), 1e-6);
                assertEquals((float) field(vertex, "u"), actual.u(), 1e-6);
                assertEquals((float) field(vertex, "v"), actual.v(), 1e-6);
                assertEquals(normal.x, actual.nx(), 1e-6);
                assertEquals(normal.y, actual.ny(), 1e-6);
                assertEquals(normal.z, actual.nz(), 1e-6);
            }
        }
    }

    // Reflection is confined to this oracle; production uses only the public render API.
    private static Object field(Object owner, String name) throws ReflectiveOperationException {
        Field field = owner.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(owner);
    }
}
