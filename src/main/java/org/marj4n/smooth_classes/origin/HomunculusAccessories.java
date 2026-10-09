package org.marj4n.smooth_classes.origin;

import com.google.common.collect.Multimap;
import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.AccessoriesContainer;
import io.wispforest.accessories.api.events.CanEquipCallback;
import io.wispforest.accessories.api.slot.SlotReference;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Entity;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.util.ActionResult;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterials;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolItem;
import net.minecraft.item.TridentItem;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.marj4n.smooth_classes.SmoothClasses;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Five real, persistent Accessories slots. Only Homunculus can equip them.
 * Inner armor applies its normal numerical armor/toughness stats but never its
 * external mesh. Outer armor is rendered by Minecraft and armor compat as usual.
 */
public final class HomunculusAccessories {
    public static final String HEAD = "homunculus_head_patch";
    public static final String CHEST = "homunculus_chest_patch";
    public static final String LEGS = "homunculus_leg_patch";
    public static final String FEET = "homunculus_feet_patch";
    public static final String WEAPON = "homunculus_weapon_arm";
    private static final List<String> SLOTS = List.of(HEAD, CHEST, LEGS, FEET, WEAPON);

    private static final UUID ARMOR_ID = UUID.fromString("9c424ddc-a346-4c3c-a585-874c3aa68dd1");
    private static final UUID TOUGHNESS_ID = UUID.fromString("e47fabd2-9d62-4bb4-8a55-90d3e0d7ddea");
    private static final UUID KB_ID = UUID.fromString("8d439fff-7207-4a4b-b877-26406f3f56f9");
    private static final UUID WEAPON_DAMAGE_ID = UUID.fromString("98e8161b-e92c-43f5-8ffd-4b96deff659b");
    private static final UUID WEAPON_SPEED_ID = UUID.fromString("dbdb86eb-c0ec-4103-ac79-d93746d497f3");

    private HomunculusAccessories() {}

    public static void register() {
        AccessoriesAPI.registerPredicate(SmoothClasses.id("homunculus_only"),
                (world, slot, index, stack) -> {
                    if (!SLOTS.contains(slot.name())) return TriState.DEFAULT;
                    return valid(slot.name(), stack) ? TriState.TRUE : TriState.FALSE;
                });
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (!world.isClient && player instanceof ServerPlayerEntity serverPlayer
                    && hand == net.minecraft.util.Hand.MAIN_HAND && entity instanceof LivingEntity target
                    && target.isAlive() && serverPlayer.getMainHandStack().isEmpty()) {
                ItemStack blade = stack(serverPlayer, WEAPON);
                if (valid(WEAPON, blade)) {
                    // The blade lives only in Accessories. Damage its actual stack,
                    // never materialize a duplicate in the vanilla inventory.
                    blade.damage(1, serverPlayer,
                            owner -> owner.sendEquipmentBreakStatus(EquipmentSlot.MAINHAND));
                    AccessoriesCapability.getOptionally(serverPlayer).ifPresent(cap -> {
                        AccessoriesContainer container = cap.getContainers().get(WEAPON);
                        if (container != null) container.markChanged();
                    });
                }
            }
            return ActionResult.PASS;
        });
        CanEquipCallback.EVENT.register((stack, reference) -> {
            if (!SLOTS.contains(reference.slotName())) return TriState.DEFAULT;
            if (!(reference.entity() instanceof PlayerEntity player)
                    || OriginRuntime.state(player).origin() != OriginType.HOMUNCULUS) return TriState.FALSE;
            return valid(reference.slotName(), stack) ? TriState.TRUE : TriState.FALSE;
        });
    }

    public static boolean valid(String slot, ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (WEAPON.equals(slot)) return stack.getItem() instanceof SwordItem;
        if (!(stack.getItem() instanceof ArmorItem armor)) return false;
        // Initial whitelist is vanilla-like. Never accept untested armor with custom
        // set bonuses, hidden equipment hooks, or arbitrary custom model renderers.
        Identifier key = Registries.ITEM.getId(stack.getItem());
        if (!"minecraft".equals(key.getNamespace())) return false;
        EquipmentSlot expected = switch (slot) {
            case HEAD -> EquipmentSlot.HEAD;
            case CHEST -> EquipmentSlot.CHEST;
            case LEGS -> EquipmentSlot.LEGS;
            case FEET -> EquipmentSlot.FEET;
            default -> null;
        };
        return expected != null && armor.getSlotType() == expected;
    }

    public static ItemStack stack(PlayerEntity player, String slot) {
        if (player == null || OriginRuntime.state(player).origin() != OriginType.HOMUNCULUS) return ItemStack.EMPTY;
        return rawStack(player, slot);
    }

    private static ItemStack rawStack(PlayerEntity player, String slot) {
        return AccessoriesCapability.getOptionally(player)
                .map(cap -> cap.getContainers().get(slot))
                .filter(container -> container.getSize() > 0)
                .map(container -> container.getAccessories().getStack(0))
                .orElse(ItemStack.EMPTY);
    }

    public static boolean patched(PlayerEntity player, String slot) {
        return valid(slot, stack(player, slot));
    }

    public static boolean hasWeaponArm(PlayerEntity player) {
        return patched(player, WEAPON);
    }

    public static boolean shouldUseWeaponArmFollowup(PlayerEntity player) {
        return hasWeaponArm(player) && isCompanionWeapon(player.getMainHandStack());
    }

    public static boolean isCompanionWeapon(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (stack.getItem() instanceof SwordItem || stack.getItem() instanceof ToolItem || stack.getItem() instanceof TridentItem) {
            return true;
        }
        Identifier key = Registries.ITEM.getId(stack.getItem());
        String path = key.getPath().toLowerCase(java.util.Locale.ROOT);
        return path.contains("sword") || path.contains("blade") || path.contains("dagger")
                || path.contains("knife") || path.contains("axe") || path.contains("spear")
                || path.contains("lance") || path.contains("halberd") || path.contains("scythe")
                || path.contains("katana") || path.contains("rapier") || path.contains("mace");
    }

    public static double weaponArmDamage(ItemStack weapon) {
        if (!valid(WEAPON, weapon)) return 0.0D;
        double damage = 0.0D;
        Multimap<EntityAttribute, EntityAttributeModifier> modifiers = weapon.getAttributeModifiers(EquipmentSlot.MAINHAND);
        for (EntityAttributeModifier modifier : modifiers.get(EntityAttributes.GENERIC_ATTACK_DAMAGE)) {
            if (modifier.getOperation() == EntityAttributeModifier.Operation.ADDITION) damage += modifier.getValue();
        }
        return damage;
    }

    public static double weaponArmAttackSpeed(ItemStack weapon) {
        if (!valid(WEAPON, weapon)) return 0.0D;
        double speed = 0.0D;
        Multimap<EntityAttribute, EntityAttributeModifier> modifiers = weapon.getAttributeModifiers(EquipmentSlot.MAINHAND);
        for (EntityAttributeModifier modifier : modifiers.get(EntityAttributes.GENERIC_ATTACK_SPEED)) {
            if (modifier.getOperation() == EntityAttributeModifier.Operation.ADDITION) speed += modifier.getValue();
        }
        return speed;
    }

    public static void returnItems(ServerPlayerEntity player) {
        var capability = AccessoriesCapability.getOptionally(player).orElse(null);
        if (capability == null) return;
        for (String slot : SLOTS) {
            AccessoriesContainer container = capability.getContainers().get(slot);
            if (container == null || container.getSize() <= 0) continue;
            ItemStack found = container.getAccessories().getStack(0);
            if (found.isEmpty()) continue;
            ItemStack removed = found.copy();
            // Clear the saved slot first; prevents repeated extraction on reconnect.
            container.getAccessories().setStack(0, ItemStack.EMPTY);
            container.markChanged();
            if (!player.getInventory().insertStack(removed)) player.dropItem(removed, false);
        }
        updateAttributes(player);
    }

    public static void updateAttributes(ServerPlayerEntity player) {
        boolean active = OriginRuntime.state(player).origin() == OriginType.HOMUNCULUS;
        int armorPoints = 0;
        double toughness = 0;
        double knockbackResistance = 0;
        double weaponDamage = 0;
        double weaponSpeed = 0;
        for (Map.Entry<String, EquipmentSlot> entry : Map.of(
                HEAD, EquipmentSlot.HEAD, CHEST, EquipmentSlot.CHEST,
                LEGS, EquipmentSlot.LEGS, FEET, EquipmentSlot.FEET).entrySet()) {
            ItemStack inner = active ? rawStack(player, entry.getKey()) : ItemStack.EMPTY;
            if (!valid(entry.getKey(), inner)) continue;
            ArmorItem armor = (ArmorItem) inner.getItem();
            armorPoints += armor.getProtection();
            toughness += armor.getToughness();
            if (armor.getMaterial() == ArmorMaterials.NETHERITE) knockbackResistance += 0.1D;
        }
        ItemStack weapon = active ? rawStack(player, WEAPON) : ItemStack.EMPTY;
        // An installed blade provides main-hand-like attack attributes only while
        // the actual main hand is EMPTY. With a real held weapon, never double-stack.
        if (valid(WEAPON, weapon) && player.getMainHandStack().isEmpty()) {
            weaponDamage += weaponArmDamage(weapon);
            weaponSpeed += weaponArmAttackSpeed(weapon);
        }
        apply(player, EntityAttributes.GENERIC_ARMOR, ARMOR_ID, armorPoints);
        apply(player, EntityAttributes.GENERIC_ARMOR_TOUGHNESS, TOUGHNESS_ID, toughness);
        apply(player, EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, KB_ID, knockbackResistance);
        apply(player, EntityAttributes.GENERIC_ATTACK_DAMAGE, WEAPON_DAMAGE_ID, weaponDamage);
        apply(player, EntityAttributes.GENERIC_ATTACK_SPEED, WEAPON_SPEED_ID, weaponSpeed);
        // Accessories supports per-slot renderer visibility. Inner patches are
        // drawn by the Homunculus body model instead of duplicate armor meshes.
        AccessoriesCapability.getOptionally(player).ifPresent(capability -> {
            for (String slot : SLOTS) {
                AccessoriesContainer container = capability.getContainers().get(slot);
                if (container == null || container.renderOptions().isEmpty()) continue;
                if (container.renderOptions().get(0)) {
                    container.renderOptions().set(0, false);
                    container.markChanged();
                }
            }
        });
    }

    private static void apply(ServerPlayerEntity player, EntityAttribute attribute, UUID id, double amount) {
        EntityAttributeInstance instance = player.getAttributeInstance(attribute);
        if (instance == null) return;
        EntityAttributeModifier previous = instance.getModifier(id);
        if (previous != null && Math.abs(previous.getValue() - amount) < 0.001D) return;
        instance.removeModifier(id);
        if (Math.abs(amount) > 0.001D) {
            instance.addTemporaryModifier(new EntityAttributeModifier(id, "Homunculus internal equipment",
                    amount, EntityAttributeModifier.Operation.ADDITION));
        }
    }
}
