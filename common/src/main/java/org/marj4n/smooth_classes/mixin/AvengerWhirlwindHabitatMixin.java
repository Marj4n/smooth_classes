package org.marj4n.smooth_classes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

/** Preserve Aether whirlwind combat while adapting its temporary natural lifespan for bound souls. */
@Pseudo
@Mixin(targets = "com.aetherteam.aether.entity.monster.AbstractWhirlwind", remap = false)
public abstract class AvengerWhirlwindHabitatMixin {}
