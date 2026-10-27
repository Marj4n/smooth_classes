package org.marj4n.smooth_classes.client.effects;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.spell_engine.api.effect.CustomModelStatusEffect;

/** Model visuals disabled; Crimson Revenant now uses particles + first-person overlay. */
public final class CrimsonRevenantChargeRenderer implements CustomModelStatusEffect.Renderer {
    @Override
    public void renderEffect(long startTime, int amplifier, LivingEntity entity, float delta,
                             MatrixStack matrices, VertexConsumerProvider consumers, int light) {
        // no-op on purpose
    }
}
