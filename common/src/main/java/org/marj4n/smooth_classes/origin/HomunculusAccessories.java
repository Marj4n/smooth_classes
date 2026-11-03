package org.marj4n.smooth_classes.origin;

import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.AccessoriesCapability;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.marj4n.smooth_classes.SmoothClasses;
import java.util.List;
import java.util.UUID;

/** Migration-only compatibility for retired Homunculus body patches and weapon arm. */
public final class HomunculusAccessories {
    public static final String HEAD = "homunculus_head_patch",
            CHEST = "homunculus_chest_patch",
            LEGS = "homunculus_leg_patch",
            FEET = "homunculus_feet_patch",
            WEAPON = "homunculus_weapon_arm";
    private static final List<String> SLOTS = List.of(HEAD, CHEST, LEGS, FEET, WEAPON);
    private static final String[] LEGACY_IDS = {
        "9c424ddc-a346-4c3c-a585-874c3aa68dd1",
        "e47fabd2-9d62-4bb4-8a55-90d3e0d7ddea",
        "8d439fff-7207-4a4b-b877-26406f3f56f9",
        "98e8161b-e92c-43f5-8ffd-4b96deff659b",
        "dbdb86eb-c0ec-4103-ac79-d93746d497f3"
    };
    private static final UUID[] MODIFIERS =
            java.util.Arrays.stream(LEGACY_IDS).map(UUID::fromString).toArray(UUID[]::new);
    private static final EntityAttribute[] LEGACY_ATTRIBUTES = {
        EntityAttributes.GENERIC_ARMOR,
        EntityAttributes.GENERIC_ARMOR_TOUGHNESS,
        EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE,
        EntityAttributes.GENERIC_ATTACK_DAMAGE,
        EntityAttributes.GENERIC_ATTACK_SPEED
    };

    private HomunculusAccessories() {}

    public static void register() {
        // Old datapacks may still refer to this validator. Reject new insertion safely.
        AccessoriesAPI.registerPredicate(
                SmoothClasses.id("homunculus_only"),
                (world, slot, index, stack) ->
                        SLOTS.contains(slot.name()) ? TriState.FALSE : TriState.DEFAULT);
        ServerPlayConnectionEvents.JOIN.register(
                (handler, sender, server) -> returnItems(handler.player));
    }

    public static boolean valid(String slot, ItemStack stack) {
        return false;
    }

    public static ItemStack stack(PlayerEntity player, String slot) {
        return ItemStack.EMPTY;
    }

    public static boolean patched(PlayerEntity player, String slot) {
        return false;
    }

    public static boolean hasWeaponArm(PlayerEntity player) {
        return false;
    }

    public static boolean shouldUseWeaponArmFollowup(PlayerEntity player) {
        return false;
    }

    public static boolean isCompanionWeapon(ItemStack stack) {
        return false;
    }

    public static double weaponArmDamage(ItemStack weapon) {
        return 0;
    }

    public static double weaponArmAttackSpeed(ItemStack weapon) {
        return 0;
    }

    public static void returnItems(ServerPlayerEntity player) {
        AccessoriesCapability.getOptionally(player)
                .ifPresent(
                        capability -> {
                            for (String slot : SLOTS) {
                                var container = capability.getContainers().get(slot);
                                if (container == null) continue;
                                recover(player, container.getAccessories());
                                recover(player, container.getCosmeticAccessories());
                                container.markChanged();
                            }
                        });
        updateAttributes(player);
        // Accessories itself recovers Items AND Cosmetics from removed saved bindings
        // through AccessoriesHolderImpl.invalidStacks. Never discard those queues.
    }

    private static void recover(ServerPlayerEntity player, Inventory inventory) {
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack found = inventory.getStack(i);
            if (found.isEmpty()) continue;
            ItemStack remainder = found.copy();
            inventory.setStack(i, ItemStack.EMPTY);
            player.getInventory().insertStack(remainder);
            if (!remainder.isEmpty()) player.dropItem(remainder, false);
        }
    }

    public static void updateAttributes(ServerPlayerEntity player) {
        for (int i = 0; i < LEGACY_ATTRIBUTES.length; i++) {
            var instance = player.getAttributeInstance(LEGACY_ATTRIBUTES[i]);
            if (instance != null) instance.removeModifier(MODIFIERS[i]);
        }
    }
}
