package org.marj4n.smooth_classes.runtime;

import net.minecraft.entity.Entity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.marj4n.smooth_classes.registry.SmoothSounds;

/** Server-side visual/audio helpers ported from SimplySkills Continued HelperMethods. */
public final class ContinuedFx {
    private ContinuedFx() {}

    public static void sound(Entity entity, String id, float volume, float pitch) {
        var event = SmoothSounds.get(id);
        if (event != null) entity.getWorld().playSoundFromEntity(null, entity, event, SoundCategory.PLAYERS, volume, pitch);
    }

    /** Continued HelperMethods.spawnParticle equivalent anchored to an entity. */
    public static void particle(Entity entity, ParticleEffect particle,
                                double vx, double vy, double vz) {
        if (!(entity.getWorld() instanceof ServerWorld world)) return;
        world.spawnParticles(particle, entity.getX(), entity.getY(), entity.getZ(),
                1, vx, vy, vz, 0);
    }

    public static void plane(Entity entity, ParticleEffect particle, BlockPos pos, int radius,
                             double vx, double vy, double vz) {
        if (!(entity.getWorld() instanceof ServerWorld world)) return;
        double x = pos.getX() - (radius + 1);
        double y = pos.getY();
        double z = pos.getZ() - (radius + 1);
        for (int i = radius * 2; i > 0; i--) for (int j = radius * 2; j > 0; j--) {
            double choose = world.random.nextDouble();
            world.spawnParticles(particle, x + i + choose, y, z + j + choose, 1, vx, vy, vz, 0);
        }
    }

    public static void orbit(Entity entity, ParticleEffect particle, double radius, int count) {
        if (!(entity.getWorld() instanceof ServerWorld world)) return;
        Vec3d c = entity.getPos();
        for (int i=0;i<count;i++) {
            double a = 2 * Math.PI * i / count;
            world.spawnParticles(particle, c.x + radius*Math.cos(a), c.y, c.z + radius*Math.sin(a), 1, 0,0,0,0);
        }
    }

    public static void beam(Entity from, Entity to, ParticleEffect particle, int count) {
        if (!(from.getWorld() instanceof ServerWorld world) || count < 2) return;
        Vec3d start=from.getPos().add(0,from.getHeight()/2.0,0);
        Vec3d end=to.getPos().add(0,to.getHeight()/2.0,0);
        Vec3d d=end.subtract(start);
        for(int i=0;i<count;i++) {
            Vec3d p=start.add(d.multiply((double)i/(count-1)));
            world.spawnParticles(particle,p.x,p.y,p.z,1,0,0,0,0);
        }
    }
}
