package org.marj4n.smooth_classes.origin.demon;

import static org.junit.jupiter.api.Assertions.*;
import java.io.ByteArrayInputStream;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import net.minecraft.nbt.*;
import org.junit.jupiter.api.Test;

class ForbiddenTreeResourceTest {
    private record Pos(int x, int y, int z) {}

    @Test
    void approvedStructureKeepsBirthSupportRecognitionAndFallClearance() throws Exception {
        byte[] bytes = Files.readAllBytes(Path.of("../common/src/main/resources/data/smooth_classes/structures/forbidden_apple_tree.nbt"));
        assertEquals(ForbiddenTreeAsset.HASH, HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)));
        NbtCompound root = NbtIo.readCompressed(new ByteArrayInputStream(bytes));
        NbtList size = root.getList("size", 3), palette = root.getList("palette", 10), blocks = root.getList("blocks", 10);
        assertEquals(3465, root.getInt("DataVersion"));
        assertEquals(List.of(64, 96, 37), List.of(size.getInt(0), size.getInt(1), size.getInt(2)));
        assertEquals(ForbiddenTreeAsset.PALETTE_COUNT, palette.size());
        assertEquals(ForbiddenTreeAsset.BLOCK_COUNT, blocks.size());
        assertTrue(root.getList("entities", 10).isEmpty());
        Map<Pos, Integer> cells = new HashMap<>();
        int highest = -1, recognition = 0;
        for (var entry : blocks) {
            NbtCompound block = (NbtCompound) entry;
            assertFalse(block.contains("nbt"), "No block entities are part of the tree");
            NbtList p = block.getList("pos", 3);
            Pos pos = new Pos(p.getInt(0), p.getInt(1), p.getInt(2));
            assertTrue(pos.x >= 0 && pos.x < 64 && pos.y >= 0 && pos.y < 85 && pos.z >= 0 && pos.z < 37);
            int state = block.getInt("state");
            assertTrue(state >= 0 && state < palette.size());
            assertNull(cells.put(pos, state), "Duplicate structure cell");
            highest = Math.max(highest, pos.y);
            String name = palette.getCompound(state).getString("Name");
            assertNotEquals("minecraft:air", name);
            if (name.equals("minecraft:diorite_stairs")) recognition++;
        }
        assertEquals(84, highest);
        // Regression: unchanged opening camera sightline intersected these R06 fork cells.
        for (Pos p : List.of(new Pos(32,41,20), new Pos(33,42,21), new Pos(34,42,22)))
            assertFalse(cells.containsKey(p), "Opening camera corridor must remain clear");
        assertEquals(1, recognition);
        NbtCompound support = palette.getCompound(cells.get(new Pos(33, 31, 28)));
        assertEquals("minecraft:acacia_wood", support.getString("Name"));
        assertEquals("y", support.getCompound("Properties").getString("axis"));
        assertEquals("minecraft:diorite_stairs", palette.getCompound(cells.get(new Pos(28, 9, 24))).getString("Name"));
        assertEquals("minecraft:blackstone", palette.getCompound(cells.get(new Pos(33, 0, 28))).getString("Name"));
        for (int y = 1; y <= 30; y++) assertFalse(cells.containsKey(new Pos(33, y, 28)), "Fruit fall and landing body column must remain clear");
        Set<Pos> remaining = new HashSet<>(cells.keySet());
        ArrayDeque<Pos> stack = new ArrayDeque<>();
        Pos seed = remaining.iterator().next(); remaining.remove(seed); stack.push(seed);
        int[][] directions = {{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}};
        while (!stack.isEmpty()) {
            Pos p = stack.pop();
            for (int[] d : directions) {
                Pos q = new Pos(p.x+d[0], p.y+d[1], p.z+d[2]);
                if (remaining.remove(q)) stack.push(q);
            }
        }
        assertTrue(remaining.isEmpty(), "Detached exported cells must not appear");
    }
}
