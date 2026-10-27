package org.marj4n.smooth_classes.registry;

import org.marj4n.smooth_classes.api.classsystem.PlayerClass;
import net.minecraft.util.Identifier;
import java.util.*;

public final class ClassRegistry {
    private static final Map<Identifier, PlayerClass> CLASSES = new LinkedHashMap<>();
    private ClassRegistry() {}
    public static <T extends PlayerClass> T register(T playerClass) {
        if (CLASSES.putIfAbsent(playerClass.id(), playerClass) != null) throw new IllegalStateException("Duplicate class: " + playerClass.id());
        return playerClass;
    }
    public static Optional<PlayerClass> get(Identifier id) { return Optional.ofNullable(CLASSES.get(id)); }
    public static Collection<PlayerClass> values() { return Collections.unmodifiableCollection(CLASSES.values()); }
}
