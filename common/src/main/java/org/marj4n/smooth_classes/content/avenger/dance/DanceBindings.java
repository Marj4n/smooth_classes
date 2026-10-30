package org.marj4n.smooth_classes.content.avenger.dance;

import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;

/** Server-issued Death List provenance. Entity UUID is the entity-NBT identity;
 * a copied scoreboard tag cannot manufacture an entry in this world ledger. */
public final class DanceBindings extends PersistentState {
    public record Binding(UUID owner,String type) {}
    private final Map<UUID,Binding> entries=new HashMap<>();
    private final Map<UUID,Boolean> ownerEligibility=new HashMap<>();
    public static DanceBindings get(MinecraftServer server){return server.getOverworld().getPersistentStateManager().getOrCreate(DanceBindings::read,DanceBindings::new,"smooth_classes_avenger_dance_bindings");}
    public Binding get(UUID id){return entries.get(id);}
    public void issue(UUID entity,UUID owner,String type){entries.put(entity,new Binding(owner,type));ownerEligibility.put(owner,true);markDirty();}
    public void recordOwner(UUID owner,boolean eligible){if(!Objects.equals(ownerEligibility.put(owner,eligible),eligible))markDirty();}
    public boolean ownerEligible(UUID owner){return ownerEligibility.getOrDefault(owner,false);}
    public long count(UUID owner,String type){return entries.values().stream().filter(b->b.owner().equals(owner)&&b.type().equals(type)).count();}
    public void forget(UUID entity){if(entries.remove(entity)!=null)markDirty();}
    public static DanceBindings read(NbtCompound n){var s=new DanceBindings();var list=n.getList("Bindings",NbtElement.COMPOUND_TYPE);for(int i=0;i<list.size();i++){var e=list.getCompound(i);if(e.containsUuid("Entity")&&e.containsUuid("Owner"))s.entries.put(e.getUuid("Entity"),new Binding(e.getUuid("Owner"),e.getString("Type")));}var owners=n.getList("OwnerEligibility",NbtElement.COMPOUND_TYPE);for(int i=0;i<owners.size();i++){var e=owners.getCompound(i);if(e.containsUuid("Owner"))s.ownerEligibility.put(e.getUuid("Owner"),e.getBoolean("Eligible"));}return s;}
    @Override public NbtCompound writeNbt(NbtCompound n){var list=new NbtList();entries.forEach((id,b)->{var e=new NbtCompound();e.putUuid("Entity",id);e.putUuid("Owner",b.owner());e.putString("Type",b.type());list.add(e);});n.put("Bindings",list);var owners=new NbtList();ownerEligibility.forEach((id,eligible)->{var e=new NbtCompound();e.putUuid("Owner",id);e.putBoolean("Eligible",eligible);owners.add(e);});n.put("OwnerEligibility",owners);return n;}
}
