package org.marj4n.smooth_classes.content.archer.runtime;

import java.lang.reflect.Method;
import java.util.Locale;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import org.marj4n.smooth_classes.SmoothClasses;

/** Reads Better Combat's resolved category, including datapack and fallback assignments. */
public final class DaggerCompatibility {
    private static final TagKey<Item> DAGGERS = TagKey.of(RegistryKeys.ITEM, SmoothClasses.id("daggers"));
    private static boolean initialized;
    private static Method getAttributes;
    private static Method category;
    private DaggerCompatibility() {}

    public static boolean isDagger(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.isIn(DAGGERS)) return true;
        if (!initialized) {
            initialized = true;
            try {
                getAttributes = Class.forName("net.bettercombat.logic.WeaponRegistry")
                        .getMethod("getAttributes", ItemStack.class);
                category = Class.forName("net.bettercombat.api.WeaponAttributes").getMethod("category");
            } catch (ReflectiveOperationException | LinkageError ignored) {
                getAttributes = null;
            }
        }
        if (getAttributes != null) {
            try {
                Object attributes = getAttributes.invoke(null, stack);
                if (attributes != null) {
                    Object value = category.invoke(attributes);
                    // An explicit non-dagger category wins over guesses from the item name.
                    if (value instanceof String name && !name.isBlank()) return daggerWord(name);
                }
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                // Better Combat is optional; allow the documented ID fallback below.
            }
        }
        return daggerWord(Registries.ITEM.getId(stack.getItem()).getPath());
    }

    private static boolean daggerWord(String value) {
        for (String word : value.toLowerCase(Locale.ROOT).split("[^a-z]+")) {
            if (word.equals("dagger") || word.equals("daggers")) return true;
        }
        return false;
    }
}
