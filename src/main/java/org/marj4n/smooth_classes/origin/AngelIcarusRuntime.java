package org.marj4n.smooth_classes.origin;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;

/**
 * Gives Angel its innate Icarus wings through Icarus' real Trinkets cape slot.
 * The slot is server-authoritative: taking the bound wings out simply places
 * them back on the Angel on the next check.
 */
public final class AngelIcarusRuntime {
    private static final Identifier WHITE_FEATHERED_WINGS = new Identifier("icarus", "white_feathered_wings");
    private static final String BOUND_TAG = "SmoothClassesAngelWing";

    private AngelIcarusRuntime() {}

    public static void tick(ServerPlayerEntity player, OriginState state) {
        if (state.origin() != OriginType.ANGEL) return;
        if ((player.age % 5) != 0) return;
        ensureEquipped(player);
    }

    @SuppressWarnings("unchecked")
    private static void ensureEquipped(ServerPlayerEntity player) {
        Item wingItem = Registries.ITEM.get(WHITE_FEATHERED_WINGS);
        if (wingItem == null || wingItem == Items.AIR) return;

        try {
            Class<?> api = Class.forName("dev.emi.trinkets.api.TrinketsApi");
            Method getComponent = null;
            for (Method method : api.getMethods()) {
                if (!method.getName().equals("getTrinketComponent") || method.getParameterCount() != 1) continue;
                if (method.getParameterTypes()[0].isAssignableFrom(player.getClass())
                        || method.getParameterTypes()[0].isAssignableFrom(net.minecraft.entity.LivingEntity.class)
                        || net.minecraft.entity.LivingEntity.class.isAssignableFrom(method.getParameterTypes()[0])) {
                    getComponent = method;
                    break;
                }
            }
            if (getComponent == null) return;

            Object optionalObj = getComponent.invoke(null, player);
            if (!(optionalObj instanceof Optional<?> optional) || optional.isEmpty()) return;
            Object component = optional.get();
            Object inventoryObj = component.getClass().getMethod("getInventory").invoke(component);
            if (!(inventoryObj instanceof Map<?, ?> inventory)) return;
            Object chestObj = inventory.get("chest");
            if (!(chestObj instanceof Map<?, ?> chest)) return;
            Object capeInventory = chest.get("cape");
            if (capeInventory == null) return;

            Method getStack = capeInventory.getClass().getMethod("getStack", int.class);
            Method setStack = capeInventory.getClass().getMethod("setStack", int.class, ItemStack.class);
            ItemStack current = (ItemStack) getStack.invoke(capeInventory, 0);
            if (isBoundAngelWing(current, wingItem)) return;

            ItemStack boundFromInventory = takeBoundWingFromInventory(player, wingItem);
            ItemStack replacement = boundFromInventory.isEmpty() ? new ItemStack(wingItem) : boundFromInventory;
            replacement.getOrCreateNbt().putBoolean(BOUND_TAG, true);
            replacement.getOrCreateNbt().putBoolean("Unbreakable", true);

            if (current != null && !current.isEmpty()) {
                ItemStack displaced = current.copy();
                if (!player.getInventory().insertStack(displaced)) {
                    player.dropItem(displaced, false);
                }
            }
            setStack.invoke(capeInventory, 0, replacement);

            try {
                capeInventory.getClass().getMethod("markUpdate").invoke(capeInventory);
            } catch (ReflectiveOperationException ignored) {
            }
        } catch (Throwable ignored) {
            // Icarus is declared required, but reflection keeps the core mod
            // source decoupled from Trinkets' compile-time API.
        }
    }

    private static ItemStack takeBoundWingFromInventory(ServerPlayerEntity player, Item wingItem) {
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (!isBoundAngelWing(stack, wingItem)) continue;
            ItemStack out = stack.copy();
            player.getInventory().setStack(i, ItemStack.EMPTY);
            return out;
        }
        return ItemStack.EMPTY;
    }

    private static boolean isBoundAngelWing(ItemStack stack, Item wingItem) {
        return stack != null && !stack.isEmpty() && stack.isOf(wingItem)
                && stack.hasNbt() && stack.getNbt().getBoolean(BOUND_TAG);
    }
}
