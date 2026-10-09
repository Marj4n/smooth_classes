package org.marj4n.smooth_classes.origin;

import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import org.marj4n.smooth_classes.registry.SmoothParticles;
import org.marj4n.smooth_classes.registry.SmoothSounds;

/** Shared for Bat Form, Man-Bat and Vampire Lord evolution theatrics. */
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

    public static void playLordEvolution(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();
        double x = player.getX();
        double y = player.getBodyY(0.50D);
        double z = player.getZ();

        for (int ring = 0; ring < 4; ring++) {
            world.spawnParticles(SmoothParticles.VAMPIRE_BAT_SWARM,
                    x, y + (ring - 1.5D) * 0.55D, z,
                    18, 0.55D, 0.24D, 0.55D, 0.08D);
        }
        world.spawnParticles(ParticleTypes.CRIMSON_SPORE, x, y + 0.70D, z,
                80, 0.70D, 1.10D, 0.70D, 0.02D);
        world.spawnParticles(ParticleTypes.WITCH, x, y + 0.60D, z,
                36, 0.45D, 0.95D, 0.45D, 0.04D);
        world.spawnParticles(ParticleTypes.ENCHANT, x, y + 0.40D, z,
                46, 0.55D, 0.85D, 0.55D, 0.12D);
        world.spawnParticles(ParticleTypes.CLOUD, x, y - 0.10D, z,
                28, 0.40D, 0.18D, 0.40D, 0.05D);
        world.spawnParticles(ParticleTypes.SMOKE, x, y + 0.15D, z,
                30, 0.45D, 0.65D, 0.45D, 0.03D);

        world.playSound(null, x, y, z, SoundEvents.ENTITY_ENDER_DRAGON_GROWL,
                SoundCategory.PLAYERS, 0.42F, 1.55F);
        world.playSound(null, x, y, z, SoundEvents.ENTITY_EVOKER_PREPARE_SUMMON,
                SoundCategory.PLAYERS, 0.70F, 0.72F);
        world.playSound(null, x, y, z, SoundEvents.ENTITY_WARDEN_HEARTBEAT,
                SoundCategory.PLAYERS, 0.65F, 0.75F);
    }
}
