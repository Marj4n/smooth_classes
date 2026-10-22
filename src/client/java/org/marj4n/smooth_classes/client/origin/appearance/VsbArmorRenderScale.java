package org.marj4n.smooth_classes.client.origin.appearance;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.EquipmentSlot;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Scale vanilla armor parts around their own animated pivots for the longer
 * VSB limbs (16px instead of 12px). Minecraft 1.20.1 ModelPart already has
 * per-part yScale; no global renderer mixin or matrix manipulation is needed.
 *
 * Original yScale values are restored after EACH armor slot render, even when
 * the underlying armor model is reused for a normal player in the next frame.
 */
public final class VsbArmorRenderScale {
    private static final float LONG_LIMB_Y_SCALE = 16F / 12F;
    private static final ThreadLocal<Map<ModelPart, Float>> ORIGINAL =
            ThreadLocal.withInitial(IdentityHashMap::new);

    private VsbArmorRenderScale() {}

    public static void begin(BipedEntityModel<?> model, EquipmentSlot slot) {
        end();
        if (slot == EquipmentSlot.CHEST) {
            extend(model.rightArm);
            extend(model.leftArm);
        } else if (slot == EquipmentSlot.LEGS || slot == EquipmentSlot.FEET) {
            extend(model.rightLeg);
            extend(model.leftLeg);
        }
    }

    private static void extend(ModelPart part) {
        ORIGINAL.get().put(part, part.yScale);
        part.yScale *= LONG_LIMB_Y_SCALE;
    }

    public static void end() {
        Map<ModelPart, Float> saved = ORIGINAL.get();
        saved.forEach((part, previous) -> part.yScale = previous);
        saved.clear();
    }
}
