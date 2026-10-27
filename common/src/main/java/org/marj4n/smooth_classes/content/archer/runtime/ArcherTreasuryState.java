package org.marj4n.smooth_classes.content.archer.runtime;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Persistent Archer Treasury storage.
 *
 * Every Archer starts with four usable slots. Treasury Expansion talents can
 * raise the usable capacity to eight. The persistent backing store always has
 * eight slots so unlocking/losing a talent never deletes stored items.
 */
public final class ArcherTreasuryState extends PersistentState {
    public static final String SAVE_ID = "smooth_classes_archer_treasury";
    public static final int BASE_SLOT_COUNT = 4;
    public static final int MAX_SLOT_COUNT = 8;

    private final Map<UUID, ItemStack[]> players = new HashMap<>();

    public static ArcherTreasuryState get(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager().getOrCreate(
                ArcherTreasuryState::fromNbt,
                ArcherTreasuryState::new,
                SAVE_ID
        );
    }

    private ItemStack[] slots(UUID player) {
        return players.computeIfAbsent(player, ignored -> emptySlots());
    }

    public ItemStack get(UUID player, int slot) {
        if (slot < 0 || slot >= MAX_SLOT_COUNT) return ItemStack.EMPTY;
        ItemStack[] slots = players.get(player);
        if (slots == null || slots[slot].isEmpty()) return ItemStack.EMPTY;
        return slots[slot].copy();
    }

    public ItemStack[] snapshot(UUID player) {
        ItemStack[] source = players.get(player);
        ItemStack[] copy = emptySlots();
        if (source == null) return copy;
        for (int i = 0; i < MAX_SLOT_COUNT; i++) {
            copy[i] = source[i].isEmpty() ? ItemStack.EMPTY : source[i].copy();
        }
        return copy;
    }

    public boolean hasAny(UUID player, int capacity) {
        ItemStack[] slots = players.get(player);
        if (slots == null) return false;
        for (int i = 0; i < clampCapacity(capacity); i++) {
            if (!slots[i].isEmpty()) return true;
        }
        return false;
    }

    public int firstEmpty(UUID player, int capacity) {
        ItemStack[] slots = slots(player);
        for (int i = 0; i < clampCapacity(capacity); i++) {
            if (slots[i].isEmpty()) return i;
        }
        return -1;
    }

    public int deposit(UUID player, ItemStack stack, int capacity) {
        if (stack == null || stack.isEmpty()) return -1;
        int slot = firstEmpty(player, capacity);
        if (slot < 0) return -1;
        slots(player)[slot] = stack.copy();
        markDirty();
        return slot;
    }

    public boolean putAt(UUID player, int slot, ItemStack stack) {
        if (slot < 0 || slot >= MAX_SLOT_COUNT || stack == null || stack.isEmpty()) return false;
        ItemStack[] slots = slots(player);
        if (!slots[slot].isEmpty()) return false;
        slots[slot] = stack.copy();
        markDirty();
        return true;
    }

    public ItemStack withdraw(UUID player, int slot) {
        if (slot < 0 || slot >= MAX_SLOT_COUNT) return ItemStack.EMPTY;
        ItemStack[] slots = players.get(player);
        if (slots == null || slots[slot].isEmpty()) return ItemStack.EMPTY;
        ItemStack out = slots[slot].copy();
        slots[slot] = ItemStack.EMPTY;
        if (allEmpty(slots)) players.remove(player);
        markDirty();
        return out;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtCompound all = new NbtCompound();
        for (Map.Entry<UUID, ItemStack[]> entry : players.entrySet()) {
            NbtList list = new NbtList();
            ItemStack[] slots = entry.getValue();
            for (int i = 0; i < MAX_SLOT_COUNT; i++) {
                if (slots[i].isEmpty()) continue;
                NbtCompound slot = new NbtCompound();
                slot.putByte("Slot", (byte) i);
                slot.put("Stack", slots[i].writeNbt(new NbtCompound()));
                list.add(slot);
            }
            if (!list.isEmpty()) {
                NbtCompound player = new NbtCompound();
                player.put("Slots", list);
                all.put(entry.getKey().toString(), player);
            }
        }
        nbt.put("Players", all);
        return nbt;
    }

    public static ArcherTreasuryState fromNbt(NbtCompound nbt) {
        ArcherTreasuryState state = new ArcherTreasuryState();
        NbtCompound all = nbt.getCompound("Players");
        for (String key : all.getKeys()) {
            try {
                UUID uuid = UUID.fromString(key);
                ItemStack[] slots = emptySlots();
                NbtList list = all.getCompound(key).getList("Slots", NbtElement.COMPOUND_TYPE);
                for (int i = 0; i < list.size(); i++) {
                    NbtCompound slot = list.getCompound(i);
                    int index = slot.getByte("Slot") & 255;
                    if (index >= MAX_SLOT_COUNT) continue;
                    ItemStack stack = ItemStack.fromNbt(slot.getCompound("Stack"));
                    if (!stack.isEmpty()) slots[index] = stack;
                }
                if (!allEmpty(slots)) state.players.put(uuid, slots);
            } catch (IllegalArgumentException ignored) {
                // Ignore corrupt/foreign UUID keys while preserving the rest of the save.
            }
        }
        return state;
    }

    private static int clampCapacity(int capacity) {
        return Math.max(BASE_SLOT_COUNT, Math.min(MAX_SLOT_COUNT, capacity));
    }

    private static ItemStack[] emptySlots() {
        ItemStack[] slots = new ItemStack[MAX_SLOT_COUNT];
        java.util.Arrays.fill(slots, ItemStack.EMPTY);
        return slots;
    }

    private static boolean allEmpty(ItemStack[] slots) {
        for (ItemStack stack : slots) if (!stack.isEmpty()) return false;
        return true;
    }
}
