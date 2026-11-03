package org.marj4n.smooth_classes.platform;

import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.server.world.ServerWorld;

/** Preserve loader spawn hooks without calling Forge's deprecated direct initializer. */
public final class PlatformSpawns {
    private PlatformSpawns() {}

    public static void initialize(MobEntity mob, ServerWorld world, SpawnReason reason) {
        mob.initialize(world, world.getLocalDifficulty(mob.getBlockPos()), reason, null, null);
    }
}
