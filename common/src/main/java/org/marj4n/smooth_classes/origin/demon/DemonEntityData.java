package org.marj4n.smooth_classes.origin.demon;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import java.util.UUID;
import java.util.Set;
import java.util.HashSet;

/** Persisted on the actual mob; no cloned minion entities or volatile ownership. */
public final class DemonEntityData {
    public UUID commander, curseOwner, attackTarget, provokedBy;
    public UUID damageCredit;
    public final Set<UUID> provoked=new HashSet<>();
    public int command, curseTier, exposure, dotTicks;
    public boolean propagated;
    public boolean streakDeathCredited; // Runtime-only confirmed-death idempotence.
    public boolean birthRecorded,nativeBirth;
    public boolean transforming;
    public BlockPos guard;
    public long scheduledAt;
    public NbtCompound write() {
        NbtCompound n=new NbtCompound();
        put(n,"Commander",commander);put(n,"CurseOwner",curseOwner);put(n,"AttackTarget",attackTarget);put(n,"ProvokedBy",provokedBy);
        var anger=new net.minecraft.nbt.NbtList();for(UUID id:provoked)anger.add(net.minecraft.nbt.NbtString.of(id.toString()));n.put("Provoked",anger);
        n.putInt("Command",command);n.putInt("CurseTier",curseTier);n.putInt("Exposure",exposure);n.putInt("DotTicks",dotTicks);
        n.putBoolean("Propagated",propagated);n.putBoolean("BirthRecorded",birthRecorded);n.putBoolean("NativeBirth",nativeBirth);if(guard!=null)n.putLong("Guard",guard.asLong());return n;
    }
    public void read(NbtCompound n) {
        commander=get(n,"Commander");curseOwner=get(n,"CurseOwner");attackTarget=get(n,"AttackTarget");provokedBy=get(n,"ProvokedBy");
        provoked.clear();if(provokedBy!=null)provoked.add(provokedBy);for(var entry:n.getList("Provoked",8))try{provoked.add(UUID.fromString(entry.asString()));}catch(IllegalArgumentException ex){org.marj4n.smooth_classes.SmoothClasses.LOGGER.warn("Ignoring invalid Demon provocation UUID {}",entry);}
        command=Math.max(0,Math.min(5,n.getInt("Command")));curseTier=Math.max(0,Math.min(3,n.getInt("CurseTier")));
        exposure=Math.max(0,n.getInt("Exposure"));dotTicks=Math.max(0,n.getInt("DotTicks"));propagated=n.getBoolean("Propagated");
        birthRecorded=n.getBoolean("BirthRecorded");nativeBirth=n.getBoolean("NativeBirth");
        guard=n.contains("Guard")?BlockPos.fromLong(n.getLong("Guard")):null;
    }
    private static void put(NbtCompound n,String key,UUID id){if(id!=null)n.putUuid(key,id);}
    private static UUID get(NbtCompound n,String key){return n.containsUuid(key)?n.getUuid(key):null;}
    public interface Holder { DemonEntityData smooth_classes$demon(); }
    public static DemonEntityData of(net.minecraft.entity.LivingEntity e){return ((Holder)e).smooth_classes$demon();}
}
