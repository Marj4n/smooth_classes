package org.marj4n.smooth_classes.compat;

import moriyashiine.bewitchment.common.item.BottleOfBloodItem;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import org.marj4n.smooth_classes.network.SmoothClassesNetworking;
import org.marj4n.smooth_classes.origin.OriginRuntime;
import org.marj4n.smooth_classes.origin.OriginState;
import org.marj4n.smooth_classes.origin.OriginType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Optional compatibility with Bewitchment's Bottle of Blood.
 *
 * Smooth Classes Vampires are deliberately NOT Bewitchment Vampires, so the
 * unmodified Bewitchment item would apply Poison + Nausea instead of filling
 * Smooth Classes' blood gauge. Keep the real drinking animation and vanilla
 * potion stack/bottle handling, but redirect the blood reward to our Origin.
 *
 * This mixin is only applied by BewitchmentMixinPlugin when Bewitchment exists.
 */
@Mixin(value = BottleOfBloodItem.class, remap = false)
public abstract class BewitchmentBloodBottleMixin {
    @Unique
    private static final int smooth_classes$BLOOD_PER_BOTTLE = 20;

    // Bewitchment is an unmapped mod target. Match both development (Yarn)
    // and production (Fabric intermediary) names without asking Mixin to map
    // the third-party class: exactly one alias exists in any given runtime.
    @Inject(method = {"finishUsing", "method_7861"}, at = @At("HEAD"),
            cancellable = true, remap = false)
    private void smooth_classes$drinkBloodForOriginVampire(ItemStack stack, World world,
                                                            LivingEntity consumer,
                                                            CallbackInfoReturnable<ItemStack> cir) {
        if (!(consumer instanceof PlayerEntity player)) return;
        OriginState state = OriginRuntime.state(player);
        if (state.origin() != OriginType.VAMPIRE) return;

        // Bewitchment would consider our Vampire a non-vampire and poison them.
        // Bypass only that logic, retaining the normal potion consumption result.
        if (!world.isClient && player instanceof ServerPlayerEntity serverPlayer) {
            int cap = OriginRuntime.vampireBloodCapacity(serverPlayer);
            int before = state.blood();
            int restored = Math.min(smooth_classes$BLOOD_PER_BOTTLE, Math.max(0, cap - before));
            if (restored > 0) {
                state.blood(before + restored);
                if (state.blood() >= 100) state.flag("vampire.filled_blood_once");
                SmoothClassesNetworking.sendOriginState(serverPlayer);
            }
        }

        // Keep animation/use-time handled by Bewitchment, but make inventory
        // mutation server-authoritative. PotionItem.finishUsing on a foreign
        // bottle stack can leave an extra client-side replacement/ghost bottle.
        if (world.isClient || player.isCreative()) {
            cir.setReturnValue(stack);
            return;
        }
        stack.decrement(1);
        ItemStack empty = new ItemStack(Items.GLASS_BOTTLE);
        if (stack.isEmpty()) {
            // Single bottle: replace held item with the empty bottle.
            cir.setReturnValue(empty);
        } else {
            // Stacked bottle: retain remaining blood bottles, return exactly
            // one glass bottle to inventory, or drop if inventory is full.
            if (!player.getInventory().insertStack(empty)) player.dropItem(empty, false);
            cir.setReturnValue(stack);
        }
    }
}
