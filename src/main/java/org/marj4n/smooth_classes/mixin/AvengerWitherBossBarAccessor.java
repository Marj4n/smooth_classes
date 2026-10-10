package org.marj4n.smooth_classes.mixin;

import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.boss.WitherEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Accessor only: hide the BOUND Wither's boss bar, never an enemy Wither's. */
@Mixin(WitherEntity.class)
public interface AvengerWitherBossBarAccessor {
    @Accessor("bossBar")
    ServerBossBar smoothClasses$getBossBar();
}
