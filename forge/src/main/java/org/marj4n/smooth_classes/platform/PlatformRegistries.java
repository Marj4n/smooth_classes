package org.marj4n.smooth_classes.platform;
public final class PlatformRegistries {
    public static final RegistryAccess<net.minecraft.block.Block> BLOCK = wrap(net.minecraftforge.registries.ForgeRegistries.BLOCKS);
    public static final RegistryAccess<net.minecraft.item.Item> ITEM = wrap(net.minecraftforge.registries.ForgeRegistries.ITEMS);
    public static final RegistryAccess<net.minecraft.entity.effect.StatusEffect> STATUS_EFFECT = wrap(net.minecraftforge.registries.ForgeRegistries.MOB_EFFECTS);
    public static final RegistryAccess<net.minecraft.entity.EntityType<?>> ENTITY_TYPE = wrap(net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES);
    public static final RegistryAccess<net.minecraft.particle.ParticleType<?>> PARTICLE_TYPE = wrap(net.minecraftforge.registries.ForgeRegistries.PARTICLE_TYPES);
    public static final RegistryAccess<net.minecraft.sound.SoundEvent> SOUND_EVENT = wrap(net.minecraftforge.registries.ForgeRegistries.SOUND_EVENTS);
    public static final RegistryAccess<net.minecraft.entity.attribute.EntityAttribute> ATTRIBUTE = wrap(net.minecraftforge.registries.ForgeRegistries.ATTRIBUTES);
    private static <T> RegistryAccess<T> wrap(net.minecraftforge.registries.IForgeRegistry<T> registry) {
        return new RegistryAccess<>(registry.getRegistryKey(),registry::getValue,registry::getKey,registry::containsKey,registry::getKeys,
                registry::register);
    }
    private PlatformRegistries() {}
}
