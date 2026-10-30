package org.marj4n.smooth_classes.platform;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import java.util.Set;
import java.util.function.*;
/** Typed common view backed by the loader's actual registry, including mod entries. */
public record RegistryAccess<T>(RegistryKey<? extends Registry<T>> key,
        Function<Identifier,T> lookup, Function<T,Identifier> identifier,
        Predicate<Identifier> contains, Supplier<Set<Identifier>> keys,
        BiConsumer<Identifier,T> registrar) {
    public T get(Identifier id) { return lookup.apply(id); }
    public Identifier getId(T value) { return identifier.apply(value); }
    public boolean containsId(Identifier id) { return contains.test(id); }
    public Set<Identifier> getIds() { return keys.get(); }
    public RegistryKey<? extends Registry<T>> getKey() { return key; }
    public void register(Identifier id,T value) { registrar.accept(id,value); }
}
