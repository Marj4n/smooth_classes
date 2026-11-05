package org.marj4n.smooth_classes.client.origin.appearance;

import net.minecraft.client.network.AbstractClientPlayerEntity;

/**
 * Render-context guard for the vanilla PlayerEntityModel mesh.
 *
 * The original pre-pose visibility write was not stable against Player Animator:
 * another renderer can re-enable BipedModel parts after our renderer hook and
 * before AnimalModel.render actually draws them. Keep the original model's
 * transforms readable for Better Combat and the VSB avatar, but skip only the
 * vanilla player geometry during an active full replacement.
 */
public final class OriginVanillaBodyGuard {
    private static final ThreadLocal<Integer> SUPPRESS_DEPTH = ThreadLocal.withInitial(() -> 0);

    private static final ThreadLocal<Integer> SIREN_DEPTH = ThreadLocal.withInitial(() -> 0);
    private static boolean fullSirenTail(AbstractClientPlayerEntity player) {
        return player != null
                && org.marj4n.smooth_classes.origin.OriginRuntime.state(player).origin()
                    == org.marj4n.smooth_classes.origin.OriginType.MERMAID
                && org.marj4n.smooth_classes.client.origin.OriginMorphState.hideMermaidLegs(player);
    }
    public static boolean shouldSuppressSirenLegGeometry() { return SIREN_DEPTH.get() > 0; }
    private OriginVanillaBodyGuard() {}

    public static void begin(AbstractClientPlayerEntity player) {
        if (fullSirenTail(player)) SIREN_DEPTH.set(SIREN_DEPTH.get() + 1);
        if (OriginAppearanceFeatureRenderer.requiresFullReplacement(player)) {
            SUPPRESS_DEPTH.set(SUPPRESS_DEPTH.get() + 1);
        }
    }

    public static void end(AbstractClientPlayerEntity player) {
        if (fullSirenTail(player)) {
            int depth = SIREN_DEPTH.get() - 1;
            if (depth <= 0) SIREN_DEPTH.remove(); else SIREN_DEPTH.set(depth);
        }
        if (OriginAppearanceFeatureRenderer.requiresFullReplacement(player)) {
            int depth = SUPPRESS_DEPTH.get() - 1;
            if (depth <= 0) SUPPRESS_DEPTH.remove();
            else SUPPRESS_DEPTH.set(depth);
        }
    }

    public static boolean shouldSuppressVanillaPlayerGeometry() {
        return SUPPRESS_DEPTH.get() > 0;
    }
}
