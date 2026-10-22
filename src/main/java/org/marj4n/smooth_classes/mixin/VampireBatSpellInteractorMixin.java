package org.marj4n.smooth_classes.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.spell_engine.internals.casting.SpellCast;
import net.spell_engine.internals.casting.SpellCastInteractor;
import org.marj4n.smooth_classes.origin.VampireBatAbilityLock;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Cancel any in-flight spell before a Bat can finish a charged/channelled cast. */
@Mixin(SpellCastInteractor.class)
public abstract class VampireBatSpellInteractorMixin {
    @Shadow @Final private PlayerEntity player;
    @Shadow public abstract SpellCast.Process process();
    @Shadow public abstract void requestClear();

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true, remap = false)
    private void smooth_classes$clearBatCast(CallbackInfo ci) {
        if (!VampireBatAbilityLock.isLocked(player)) return;
        if (process() != null) requestClear();
        ci.cancel();
    }

    @Inject(method = "requestCast", at = @At("HEAD"), cancellable = true, remap = false)
    private void smooth_classes$noBatCastRequest(Identifier spellId, SpellCast.TargetSnapshot targets, CallbackInfo ci) {
        if (!VampireBatAbilityLock.isLocked(player)) return;
        if (process() != null) requestClear();
        ci.cancel();
    }

    @Inject(method = "requestEnd", at = @At("HEAD"), cancellable = true, remap = false)
    private void smooth_classes$noBatRelease(Identifier spellId, SpellCast.TargetSnapshot targets, CallbackInfo ci) {
        if (!VampireBatAbilityLock.isLocked(player)) return;
        if (process() != null) requestClear();
        ci.cancel();
    }
}
