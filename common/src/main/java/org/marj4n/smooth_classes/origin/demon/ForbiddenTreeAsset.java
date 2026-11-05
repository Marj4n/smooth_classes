package org.marj4n.smooth_classes.origin.demon;

import java.util.*;
import net.minecraft.block.*;
import net.minecraft.nbt.*;
import net.minecraft.registry.Registries;
import net.minecraft.state.property.Property;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.*;

/** Original Sanguine Widow structure exported from independently authored native geometry. */
public final class ForbiddenTreeAsset {
    public static final String HASH =
            "942240f35dee4f9c428595dcf08bace0e4f9afdbfac5000ae9acaaa02a943557";
    public static final int WIDTH = 64, HEIGHT = 85, LENGTH = 37;
    public static final int BLOCK_COUNT = 33094, PALETTE_COUNT = 8, SELECTION_HEIGHT = 96;

    public record Cell(BlockPos offset, BlockState state) {}

    public static final Vec3d FRUIT = new Vec3d(33.5, 30.4, 28.5),
            LANDING = new Vec3d(33.5, 1, 28.5),
            FACE = new Vec3d(28.5, 11.5, 23.8),
            CENTER = new Vec3d(32, 42, 18.5);
    private static List<Cell> cells;
    private static int paletteCount;

    private ForbiddenTreeAsset() {}

    public static List<Cell> cells() {
        if (cells != null) return cells;
        try (var in =
                ForbiddenTreeAsset.class.getResourceAsStream(
                        "/data/smooth_classes/structures/forbidden_apple_tree.nbt")) {
            if (in == null)
                throw new IllegalStateException(
                        "Missing original Forbidden Tree resource");
            var encoded = in.readAllBytes();
            var digest = java.util.HexFormat.of().formatHex(
                    java.security.MessageDigest.getInstance("SHA-256").digest(encoded));
            if (!HASH.equals(digest)) throw new IllegalStateException("Unapproved Forbidden Tree bytes");
            var n = NbtIo.readCompressed(new java.io.ByteArrayInputStream(encoded));
            var size = n.getList("size", 3);
            if (size.size() != 3 || size.getInt(0) != WIDTH
                    || size.getInt(1) != SELECTION_HEIGHT || size.getInt(2) != LENGTH)
                throw new IllegalStateException("Wrong Forbidden Tree selection dimensions");
            var palette = new ArrayList<BlockState>();
            for (var e : n.getList("palette", 10)) {
                var p = (NbtCompound) e;
                Identifier id = Identifier.tryParse(p.getString("Name"));
                if (id == null
                        || !org.marj4n.smooth_classes.platform.PlatformRegistries.BLOCK.containsId(
                                id)) throw new IllegalStateException("Unknown1.20.1 block " + id);
                var block = org.marj4n.smooth_classes.platform.PlatformRegistries.BLOCK.get(id);
                BlockState state = block.getDefaultState();
                var props = p.getCompound("Properties");
                for (String key : props.getKeys()) {
                    Property<?> property = block.getStateManager().getProperty(key);
                    if (property == null)
                        throw new IllegalStateException("Unknown block property " + id + "/" + key);
                    state = property(state, property, props.getString(key));
                }
                palette.add(state);
            }
            paletteCount = palette.size();
            var list = new ArrayList<Cell>();
            for (var e : n.getList("blocks", 10)) {
                var b = (NbtCompound) e;
                var pos = b.getList("pos", 3);
                int i = b.getInt("state");
                if (i < 0 || i >= palette.size()) throw new IllegalStateException("Palette index");
                BlockPos offset = new BlockPos(pos.getInt(0), pos.getInt(1), pos.getInt(2));
                if (offset.getX() < 0
                        || offset.getX() >= WIDTH
                        || offset.getY() < 0
                        || offset.getY() >= HEIGHT
                        || offset.getZ() < 0
                        || offset.getZ() >= LENGTH)
                    throw new IllegalStateException("Tree coordinate outside actual source bounds");
                list.add(new Cell(offset, palette.get(i)));
            }
            if (list.size() != BLOCK_COUNT || paletteCount != PALETTE_COUNT || !n.getList("entities", 10).isEmpty())
                throw new IllegalStateException("Wrong tree variant or incomplete conversion");
            cells = List.copyOf(list);
            return cells;
        } catch (java.io.IOException | java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Cannot read Forbidden Tree", ex);
        }
    }

    private static <T extends Comparable<T>> BlockState property(
            BlockState s, Property<T> p, String value) {
        return s.with(
                p,
                p.parse(value)
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "Unknown1.20.1 state "
                                                        + p.getName()
                                                        + "="
                                                        + value)));
    }

    public static int paletteCount() {
        cells();
        return paletteCount;
    }

    public static BlockPos anchor(ForbiddenTreeRecord r) {
        return new BlockPos(r.x, r.y, r.z);
    }

    public static Box bounds(BlockPos a) {
        return new Box(
                a.getX(),
                a.getY(),
                a.getZ(),
                a.getX() + WIDTH,
                a.getY() + HEIGHT,
                a.getZ() + LENGTH);
    }
}
