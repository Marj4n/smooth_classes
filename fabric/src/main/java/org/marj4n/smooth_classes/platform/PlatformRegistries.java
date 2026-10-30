package org.marj4n.smooth_classes.platform;
public final class PlatformRegistries {
    public static final RegistryAccess<net.minecraft.block.Block> BLOCK = wrap(net.minecraft.registry.Registries.BLOCK);
    public static final RegistryAccess<net.minecraft.item.Item> ITEM = wrap(net.minecraft.registry.Registries.ITEM);
    public static final RegistryAccess<net.minecraft.entity.effect.StatusEffect> STATUS_EFFECT = wrap(net.minecraft.registry.Registries.STATUS_EFFECT);
    public static final RegistryAccess<net.minecraft.entity.EntityType<?>> ENTITY_TYPE = wrap(net.minecraft.registry.Registries.ENTITY_TYPE);
    public static final RegistryAccess<net.minecraft.particle.ParticleType<?>> PARTICLE_TYPE = wrap(net.minecraft.registry.Registries.PARTICLE_TYPE);
    public static final RegistryAccess<net.minecraft.sound.SoundEvent> SOUND_EVENT = wrap(net.minecraft.registry.Registries.SOUND_EVENT);
    public static final RegistryAccess<net.minecraft.entity.attribute.EntityAttribute> ATTRIBUTE = wrap(net.minecraft.registry.Registries.ATTRIBUTE);
    private static <T> RegistryAccess<T> wrap(net.minecraft.registry.Registry<T> registry) {
        return new RegistryAccess<>(registry.getKey(),registry::get,registry::getId,registry::containsId,registry::getIds,
                (id,value)->net.minecraft.registry.Registry.register(registry,id,value));
    }
    private PlatformRegistries() {}
}
