package org.marj4n.smooth_classes.client.effects;

import net.minecraft.client.particle.FlameParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DefaultParticleType;

/** Vanilla flame shape; its private atlas sprite is recolored violet on load. */
public final class ArcaneFlameFactory implements ParticleFactory<DefaultParticleType> {
    private final FlameParticle.Factory delegate;
    public ArcaneFlameFactory(SpriteProvider sprites) {
        delegate = new FlameParticle.Factory(sprites);
    }
    @Override
    public Particle createParticle(DefaultParticleType type, ClientWorld world,
                                   double x, double y, double z,
                                   double vx, double vy, double vz) {
        Particle particle = delegate.createParticle(type, world, x, y, z, vx, vy, vz);
        if (particle != null) {
            particle.setColor(1.0F, 1.0F, 1.0F);
            particle.scale(1.6F);
            particle.setMaxAge(24 + world.random.nextInt(12));
        }
        return particle;
    }
}
