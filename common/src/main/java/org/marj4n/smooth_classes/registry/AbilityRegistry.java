package org.marj4n.smooth_classes.registry;
import org.marj4n.smooth_classes.api.ability.Ability;
import net.minecraft.util.Identifier;
import java.util.*;
public final class AbilityRegistry {
 private static final Map<Identifier, Ability> VALUES = new LinkedHashMap<>(); private AbilityRegistry() {}
 public static <T extends Ability> T register(T value){ if(VALUES.putIfAbsent(value.id(),value)!=null) throw new IllegalStateException("Duplicate ability: "+value.id()); return value; }
 public static Optional<Ability> get(Identifier id){ return Optional.ofNullable(VALUES.get(id)); }
 public static Collection<Ability> values(){ return Collections.unmodifiableCollection(VALUES.values()); }
}
