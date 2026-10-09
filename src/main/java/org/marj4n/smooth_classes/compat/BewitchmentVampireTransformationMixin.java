package org.marj4n.smooth_classes.compat;

import moriyashiine.bewitchment.api.component.TransformationComponent;
import moriyashiine.bewitchment.api.registry.Transformation;
import moriyashiine.bewitchment.common.registry.BWTransformations;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Smooth Classes owns player Vampire progression when both mods are installed.
 * Do not touch Bewitchment's Werewolf transformation, coffin, or other features.
 * This optional mixin must not invoke methods on TransformationComponent directly:
 * that class implements Cardinal Components API interfaces, which are not part
 * of Smooth Classes' compile-time dependencies.
 */
@Mixin(value = TransformationComponent.class, remap = false)
public abstract class BewitchmentVampireTransformationMixin {
    @ModifyVariable(method = "setTransformation", at = @At("HEAD"), argsOnly = true, ordinal = 0, remap = false)
    private Transformation smoothClasses$disableBewitchmentVampire(Transformation incoming) {
        return incoming == BWTransformations.VAMPIRE ? BWTransformations.HUMAN : incoming;
    }

    /**
     * When an old world contains the removed Vampire form, normalize both its
     * transformation and its alternate-form flag BEFORE Bewitchment reads them.
     * This avoids calling the CCA-backed TransformationComponent API and leaves
     * unrelated transformations untouched.
     */
    @Inject(method = "readFromNbt", at = @At("HEAD"), remap = false)
    private void smoothClasses$resetOldVampireForm(NbtCompound nbt, CallbackInfo ci) {
        if ("bewitchment:vampire".equals(nbt.getString("Transformation"))) {
            nbt.putString("Transformation", "bewitchment:human");
            nbt.putBoolean("AlternateForm", false);
        }
    }
}
