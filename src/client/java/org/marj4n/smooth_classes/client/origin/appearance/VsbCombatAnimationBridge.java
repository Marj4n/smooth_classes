package org.marj4n.smooth_classes.client.origin.appearance;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.util.math.MathHelper;

import java.lang.reflect.Method;

/**
 * Optional bridge to the Player Animator library used by Better Combat.
 *
 * Only detects Player Animator's optional FIRST-PERSON animation pass.
 * Third-person poses come from the already-animated vanilla PlayerEntityModel;
 * sampling Player Animator a second time produces conflicting weapon angles.
 */
final class VsbCombatAnimationBridge {
    private static final Adapter ADAPTER = Adapter.load();

    private VsbCombatAnimationBridge() {}

    /** Whether Player Animator is drawing its animated third-person model as a
     * first-person Better Combat attack. In that pass we must not hide Steve's
     * bone structure: the animation library needs it to show the actual slash.
     */
    static boolean isFirstPersonAnimationActive(AbstractClientPlayerEntity player, float tickDelta) {
        if (ADAPTER == null || player == null) return false;
        try {
            if (!ADAPTER.animatedPlayerClass.isInstance(player)) return false;
            Object animator = ADAPTER.getAnimation.invoke(player);
            if (animator == null || !Boolean.TRUE.equals(ADAPTER.isActive.invoke(animator))) return false;
            ADAPTER.setTickDelta.invoke(animator, MathHelper.clamp(tickDelta, 0F, 1F));
            Object mode = ADAPTER.getFirstPersonMode.invoke(animator);
            return mode != null && Boolean.TRUE.equals(ADAPTER.isFirstPersonModeEnabled.invoke(mode));
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ex) {
            return false;
        }
    }

    private record Adapter(Class<?> animatedPlayerClass, Method getAnimation, Method isActive,
                           Method setTickDelta, Method getFirstPersonMode,
                           Method isFirstPersonModeEnabled) {
        private static Adapter load() {
            try {
                Class<?> animated = Class.forName("dev.kosmx.playerAnim.impl.IAnimatedPlayer");
                Class<?> processor = Class.forName("dev.kosmx.playerAnim.core.impl.AnimationProcessor");
                Class<?> firstPersonMode = Class.forName("dev.kosmx.playerAnim.api.firstPerson.FirstPersonMode");
                return new Adapter(animated, animated.getMethod("playerAnimator_getAnimation"),
                        processor.getMethod("isActive"), processor.getMethod("setTickDelta", float.class),
                        processor.getMethod("getFirstPersonMode"), firstPersonMode.getMethod("isEnabled"));
            } catch (ReflectiveOperationException | LinkageError | RuntimeException ex) {
                return null;
            }
        }
    }
}
