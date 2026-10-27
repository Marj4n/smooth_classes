package org.marj4n.smooth_classes.origin;

import net.minecraft.entity.player.PlayerEntity;

/** Authoritative shared Vampire Bat Form lock; never affects Man-Bat or humanoid forms. */
public final class VampireBatAbilityLock {
    private VampireBatAbilityLock() { }

    public static boolean isLocked(PlayerEntity player) {
        if (player == null) return false;
        OriginState state = OriginRuntime.state(player);
        return state.origin() == OriginType.VAMPIRE && state.hasFlag("vampire.form.bat");
    }
}
