package org.marj4n.smooth_classes.mixin;

import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;

/** Marker for precise owned-only adapters after all ServerWorld mixins have merged. */
@Mixin(ServerWorld.class)
public abstract class AvengerServerWorldCompatMixin {}
