package org.marj4n.smooth_classes.client.avenger.dance;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;

/** Captures vanilla geometry once, without accessing loader-private Quad or Vertex types. */
final class CuboidVertexCollector implements VertexConsumer {
    record Vertex(float x, float y, float z, float u, float v, float nx, float ny, float nz) {}

    private final List<Vertex> vertices = new ArrayList<>();
    private float x, y, z, u, v, nx, ny, nz;

    static List<Vertex> capture(ModelPart.Cuboid cube) {
        var collector = new CuboidVertexCollector();
        cube.renderCuboid(new MatrixStack().peek(), collector, 0, 0, 1, 1, 1, 1);
        return List.copyOf(collector.vertices);
    }

    @Override
    public VertexConsumer vertex(double x, double y, double z) {
        // Cuboid rendering emits block coordinates; the joint works in model pixels.
        this.x = (float) (x * 16);
        this.y = (float) (y * 16);
        this.z = (float) (z * 16);
        return this;
    }

    @Override
    public VertexConsumer texture(float u, float v) {
        this.u = u;
        this.v = v;
        return this;
    }

    @Override
    public VertexConsumer normal(float x, float y, float z) {
        nx = x;
        ny = y;
        nz = z;
        return this;
    }

    @Override
    public void next() {
        vertices.add(new Vertex(x, y, z, u, v, nx, ny, nz));
    }

    @Override
    public VertexConsumer color(int r, int g, int b, int a) {
        return this;
    }

    @Override
    public VertexConsumer overlay(int u, int v) {
        return this;
    }

    @Override
    public VertexConsumer light(int u, int v) {
        return this;
    }

    @Override
    public void fixedColor(int r, int g, int b, int a) {}

    @Override
    public void unfixColor() {}
}
