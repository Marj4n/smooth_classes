package org.marj4n.smooth_classes.registry;
import org.marj4n.smooth_classes.api.talent.Talent;
import net.minecraft.util.Identifier;
import java.util.*;
public final class TalentRegistry {
 private static final Map<Identifier, Talent> VALUES = new LinkedHashMap<>(); private TalentRegistry() {}
 public static <T extends Talent> T register(T value){ if(VALUES.putIfAbsent(value.id(),value)!=null) throw new IllegalStateException("Duplicate talent: "+value.id()); return value; }
 public static Optional<Talent> get(Identifier id){ return Optional.ofNullable(VALUES.get(id)); }
 public static Collection<Talent> values(){ return Collections.unmodifiableCollection(VALUES.values()); }
}
