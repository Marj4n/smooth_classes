package org.marj4n.smooth_classes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

/** Optional Aether adapter; no Aether dependency, replacement entity or model. */
@Pseudo
@Mixin(targets = "com.aetherteam.aether.entity.monster.AechorPlant", remap = false)
public abstract class AvengerAechorHabitatMixin {}
