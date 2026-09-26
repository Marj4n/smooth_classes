package org.marj4n.smooth_classes.client;

import net.minecraft.entity.LivingEntity;
import net.spell_engine.api.effect.Synchronized;
import org.marj4n.smooth_classes.effects.SmoothEffects;

/** Use both the local effect state and Spell Engine's synced remote effect state. */
public final class StealthVisualState {
    private StealthVisualState() {}

    public static boolean active(LivingEntity entity) {
        return entity.hasStatusEffect(SmoothEffects.STEALTH)
                || Synchronized.effectsOf(entity).toString().contains("smooth_classes:stealth");
    }
}
