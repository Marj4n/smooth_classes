package org.marj4n.smooth_classes.origin;

import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import org.marj4n.smooth_classes.registry.SmoothParticles;
import org.marj4n.smooth_classes.registry.SmoothSounds;

/** Shared for Bat Form, Man-Bat and all returns to human form (including timeout). */
public final class VampireTransformationEffects {
    private VampireTransformationEffects() {}

    public static void play(ServerPlayerEntity player, boolean entering) {
        ServerWorld world = player.getServerWorld();
        double x = player.getX();
        double y = player.getBodyY(0.50D);
        double z = player.getZ();

        // A brief halo of small animated bat silhouettes; never real bat entities.
        for (int i = 0; i < 3; i++) {
            world.spawnParticles(SmoothParticles.VAMPIRE_BAT_SWARM,
                    x, y + (i - 1) * 0.46D, z,
                    12, 0.35D, 0.29D, 0.35D, 0.075D);
        }
        world.spawnParticles(ParticleTypes.SMOKE, x, y, z,
                22, 0.35D, 0.68D, 0.35D, 0.025D);
        world.spawnParticles(ParticleTypes.CLOUD, x, y - 0.20D, z,
                14, 0.32D, 0.40D, 0.32D, 0.035D);
        world.playSound(null, x, y, z,
                entering ? SmoothSounds.get("origin_vampire_transform_in")
                         : SmoothSounds.get("origin_vampire_transform_out"),
                SoundCategory.PLAYERS, 0.95F, 1.0F);
    }
}
