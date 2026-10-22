package org.marj4n.smooth_classes.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.util.Identifier;
import org.marj4n.smooth_classes.SmoothClasses;
import org.marj4n.smooth_classes.origin.OriginRuntime;
import org.marj4n.smooth_classes.origin.OriginType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * R5: Preserve the vanilla/Better Combat arm bones and change only the skin.
 *
 * Minecraft first person arms and Player Animator's Better Combat attack pass
 * both sample the local player's skin using AbstractClientPlayerEntity.
 * Returning the VSB-purple vanilla-UV arm atlas here makes both consistent.
 * The exact VSB Blockbench geometry/skin remains in charge of third-person.
 */
@Mixin(AbstractClientPlayerEntity.class)
public abstract class ManBatFirstPersonSkinMixin {
    private static final Identifier VSB_FPV_SKIN =
            SmoothClasses.id("textures/entity/origin/vsb_first_person_arm.png");
    private static final Identifier VSB_LORD_FPV_SKIN =
            SmoothClasses.id("textures/entity/origin/vsb_first_person_arm_lord.png");

    @Inject(method = "getSkinTexture", at = @At("HEAD"), cancellable = true)
    private void smooth_classes$skinForManBatFPV(CallbackInfoReturnable<Identifier> cir) {
        MinecraftClient mc = MinecraftClient.getInstance();
        AbstractClientPlayerEntity self = (AbstractClientPlayerEntity) (Object) this;
        if (mc.player != self || mc.currentScreen != null
                || !mc.options.getPerspective().isFirstPerson()) return;
        var state = OriginRuntime.state(self);
        if (state.origin() == OriginType.VAMPIRE && state.hasFlag("vampire.form.man_bat")) {
            cir.setReturnValue(state.hasFlag("vampire.evolution.lord") ? VSB_LORD_FPV_SKIN : VSB_FPV_SKIN);
        }
    }
}
