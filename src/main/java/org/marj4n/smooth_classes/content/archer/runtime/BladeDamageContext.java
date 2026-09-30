package org.marj4n.smooth_classes.content.archer.runtime;

import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;

/** Keeps kill-time enchantment queries tied to the launched dagger, even after switching weapons. */
public final class BladeDamageContext {
    public record Hit(LivingEntity caster, ItemStack weapon) {}
    private static final ThreadLocal<Hit> CURRENT = new ThreadLocal<>();
    private BladeDamageContext() {}
    public static Hit current() { return CURRENT.get(); }
    public static Hit enter(LivingEntity caster, ItemStack weapon) {
        Hit previous = CURRENT.get();
        CURRENT.set(new Hit(caster, weapon));
        return previous;
    }
    public static void restore(Hit previous) {
        if (previous == null) CURRENT.remove(); else CURRENT.set(previous);
    }
}
