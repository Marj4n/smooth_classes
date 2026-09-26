package org.marj4n.smooth_classes.registry;

import net.minecraft.util.Identifier;
import org.marj4n.smooth_classes.api.specialization.ClassSpecialization;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Registry foundation for future evolutions. Intentionally empty until actual specialization designs are chosen. */
public final class SpecializationRegistry {
    private static final Map<Identifier, ClassSpecialization> VALUES = new LinkedHashMap<>();
    private SpecializationRegistry() {}

    public static ClassSpecialization register(ClassSpecialization specialization) {
        if (VALUES.putIfAbsent(specialization.id(), specialization) != null)
            throw new IllegalStateException("Duplicate specialization id: " + specialization.id());
        return specialization;
    }
    public static Optional<ClassSpecialization> get(Identifier id) { return Optional.ofNullable(VALUES.get(id)); }
    public static Collection<ClassSpecialization> values() { return java.util.List.copyOf(VALUES.values()); }
}
