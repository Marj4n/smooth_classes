package org.marj4n.smooth_classes.platform;
public final class PlatformItems {
    public static int enchantmentLevel(net.minecraft.enchantment.Enchantment enchantment, net.minecraft.item.ItemStack stack) { return stack.getEnchantmentLevel(enchantment); }
    public static net.minecraft.item.FoodComponent food(net.minecraft.item.ItemStack stack, net.minecraft.entity.LivingEntity entity) { return stack.getFoodProperties(entity); }
    private PlatformItems() {}
}
