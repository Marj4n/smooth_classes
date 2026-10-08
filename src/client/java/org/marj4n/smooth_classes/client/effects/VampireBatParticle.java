package org.marj4n.smooth_classes.client.effects;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteBillboardParticle;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DefaultParticleType;

/** Very small animated bat-shaped particles emitted only while a vampire changes forms. */
public final class VampireBatParticle extends SpriteBillboardParticle {
    private final SpriteProvider sprites;
    private final float originalScale;

    private VampireBatParticle(ClientWorld world, double x, double y, double z,
                               double vx, double vy, double vz, SpriteProvider sprites) {
        super(world, x, y, z, vx, vy, vz);
        this.sprites = sprites;
        this.velocityX = vx * 0.62D + (world.random.nextDouble() - 0.5D) * 0.07D;
        this.velocityY = 0.045D + Math.abs(vy) * 0.40D;
        this.velocityZ = vz * 0.62D + (world.random.nextDouble() - 0.5D) * 0.07D;
        this.gravityStrength = 0.002F;
        this.velocityMultiplier = 0.92F;
        this.maxAge = 15 + world.random.nextInt(12);
        this.scale = 0.11F + world.random.nextFloat() * 0.09F;
        this.originalScale = this.scale;
        this.collidesWithWorld = false;
        this.setSpriteForAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.dead) return;
        // Soft flutter around the centre of the player's silhouette.
        this.velocityX += Math.sin(this.age * 0.91D + this.x * 3D) * 0.0019D;
        this.velocityZ += Math.cos(this.age * 0.87D + this.z * 3D) * 0.0019D;
        this.scale = originalScale * Math.min(1F, (maxAge - age) / 7F);
        this.alpha = Math.min(0.85F, Math.max(0F, (maxAge - age) / 7F));
        // Two frame sprites loop throughout the transformation, not merely once.
        this.setSprite(sprites.getSprite((age / 3) % 2, 1));
    }

    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static final class Factory implements ParticleFactory<DefaultParticleType> {
        private final SpriteProvider sprites;

        public Factory(SpriteProvider sprites) { this.sprites = sprites; }

        @Override
        public Particle createParticle(DefaultParticleType type, ClientWorld world,
                                       double x, double y, double z,
                                       double vx, double vy, double vz) {
            return new VampireBatParticle(world, x, y, z, vx, vy, vz, sprites);
        }
    }
}
