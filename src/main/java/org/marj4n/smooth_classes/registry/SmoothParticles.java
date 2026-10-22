package org.marj4n.smooth_classes.registry;

import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import org.marj4n.smooth_classes.SmoothClasses;

public final class SmoothParticles {
    public static final DefaultParticleType ARCANE_FLAME = FabricParticleTypes.simple();
    public static final DefaultParticleType BLACK_FLAME = FabricParticleTypes.simple();
    public static final DefaultParticleType VAMPIRE_BAT_SWARM = FabricParticleTypes.simple();
    private SmoothParticles() {}
    public static void register() {
        Registry.register(Registries.PARTICLE_TYPE, SmoothClasses.id("black_flame"), BLACK_FLAME);
        Registry.register(Registries.PARTICLE_TYPE, SmoothClasses.id("arcane_flame"), ARCANE_FLAME);
        Registry.register(Registries.PARTICLE_TYPE, SmoothClasses.id("vampire_bat_swarm"), VAMPIRE_BAT_SWARM);
    }
}
