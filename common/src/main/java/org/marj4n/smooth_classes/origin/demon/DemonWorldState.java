package org.marj4n.smooth_classes.origin.demon;

import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;

/** Overworld ledger owns cross-dimensional UUIDs. No mob references are serialized. */
public final class DemonWorldState extends PersistentState {
    public record Throne(UUID owner,BlockPos origin,int rotation){}
    public static final class Cast {
        public UUID id,owner,root;public int budget,count,pending;public boolean cancelled;
        Cast(UUID id,UUID owner,UUID root,int budget){this.id=id;this.owner=owner;this.root=root;this.budget=budget;this.count=1;}
    }
    public final Map<UUID,Throne> thrones=new HashMap<>();
    public final Map<UUID,Cast> casts=new HashMap<>();
    public record Burst(UUID token,UUID target,String dimension,int tier,double x,double y,double z){}
    public final ArrayDeque<Burst> pendingBursts=new ArrayDeque<>();
    public final Map<UUID,UUID> mainCasts=new HashMap<>();
    public final Set<UUID> born=new HashSet<>();
    public ForbiddenTreeRecord forbiddenTree=new ForbiddenTreeRecord();
    // Natural loot provenance survives chest closing/reloads, but is erased if that chest is broken.
    public final Map<Long,String> naturalChests=new HashMap<>();
    public final Map<UUID,Set<String>> historicalKills=new HashMap<>();
    public static DemonWorldState get(MinecraftServer s){return s.getOverworld().getPersistentStateManager().getOrCreate(DemonWorldState::read,DemonWorldState::new,"smooth_classes_demon");}
    public static DemonWorldState read(NbtCompound n){
        DemonWorldState s=new DemonWorldState();
        if(n.contains("ForbiddenTree",10))s.forbiddenTree=ForbiddenTreeRecord.read(n.getCompound("ForbiddenTree"));
        for(NbtElement e:n.getList("Thrones",10)){NbtCompound t=(NbtCompound)e;if(t.containsUuid("Owner")){UUID o=t.getUuid("Owner");s.thrones.put(o,new Throne(o,BlockPos.fromLong(t.getLong("Origin")),Math.floorMod(t.getInt("Rotation"),4)));}}
        for(NbtElement e:n.getList("Casts",10)){NbtCompound t=(NbtCompound)e;if(!t.containsUuid("Id")||!t.containsUuid("Owner"))continue;
            Cast c=new Cast(t.getUuid("Id"),t.getUuid("Owner"),t.containsUuid("Root")?t.getUuid("Root"):null,Math.max(0,t.getInt("Budget")));
            c.count=Math.max(0,t.getInt("Count"));c.cancelled=t.getBoolean("Cancelled");s.casts.put(c.id,c);if(t.getBoolean("Main")&&!c.cancelled&&c.root!=null)s.mainCasts.put(c.owner,c.id);
        }
        for(NbtElement e:n.getList("Born",8))try{s.born.add(UUID.fromString(e.asString()));}catch(IllegalArgumentException ex){org.marj4n.smooth_classes.SmoothClasses.LOGGER.warn("Ignoring malformed Demon birth UUID {}",e);}
        for(NbtElement e:n.getList("Bursts",10)){NbtCompound t=(NbtCompound)e;if(!t.containsUuid("Token")||!t.containsUuid("Target"))continue;var c=s.casts.get(t.getUuid("Token"));if(c==null)continue;s.pendingBursts.add(new Burst(c.id,t.getUuid("Target"),t.getString("Dimension"),Math.max(0,Math.min(3,t.getInt("Tier"))),t.getDouble("X"),t.getDouble("Y"),t.getDouble("Z")));c.pending++;}
        for(NbtElement e:n.getList("Chests",10)){NbtCompound t=(NbtCompound)e;s.naturalChests.put(t.getLong("Pos"),t.getString("Kind"));}
        for(NbtElement e:n.getList("Kills",10)){NbtCompound t=(NbtCompound)e;if(!t.containsUuid("Owner"))continue;Set<String> kills=new HashSet<>();for(NbtElement k:t.getList("Bosses",8))kills.add(k.asString());s.historicalKills.put(t.getUuid("Owner"),kills);}
        return s;
    }
    @Override public NbtCompound writeNbt(NbtCompound n){
        n.put("ForbiddenTree",forbiddenTree.write());
        NbtList ts=new NbtList();for(Throne t:thrones.values()){NbtCompound x=new NbtCompound();x.putUuid("Owner",t.owner());x.putLong("Origin",t.origin().asLong());x.putInt("Rotation",t.rotation());ts.add(x);}n.put("Thrones",ts);
        NbtList cs=new NbtList();for(Cast c:casts.values()){NbtCompound x=new NbtCompound();x.putUuid("Id",c.id);x.putUuid("Owner",c.owner);if(c.root!=null)x.putUuid("Root",c.root);x.putInt("Budget",c.budget);x.putInt("Count",c.count);x.putBoolean("Cancelled",c.cancelled);x.putBoolean("Main",c.id.equals(mainCasts.get(c.owner)));cs.add(x);}n.put("Casts",cs);
        NbtList bs=new NbtList();for(UUID id:born)bs.add(NbtString.of(id.toString()));n.put("Born",bs);
        NbtList bursts=new NbtList();for(Burst hit:pendingBursts){NbtCompound t=new NbtCompound();t.putUuid("Token",hit.token());t.putUuid("Target",hit.target());t.putString("Dimension",hit.dimension());t.putInt("Tier",hit.tier());t.putDouble("X",hit.x());t.putDouble("Y",hit.y());t.putDouble("Z",hit.z());bursts.add(t);}n.put("Bursts",bursts);
        NbtList ch=new NbtList();naturalChests.forEach((pos,kind)->{NbtCompound x=new NbtCompound();x.putLong("Pos",pos);x.putString("Kind",kind);ch.add(x);});n.put("Chests",ch);
        NbtList kills=new NbtList();historicalKills.forEach((owner,set)->{NbtCompound x=new NbtCompound();x.putUuid("Owner",owner);NbtList entries=new NbtList();set.forEach(k->entries.add(NbtString.of(k)));x.put("Bosses",entries);kills.add(x);});n.put("Kills",kills);return n;
    }
}
