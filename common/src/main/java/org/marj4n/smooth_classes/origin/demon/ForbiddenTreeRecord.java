package org.marj4n.smooth_classes.origin.demon;

import net.minecraft.nbt.NbtCompound;
import java.util.UUID;

/** One durable reservation per save. All mutations belong to the server thread. */
public final class ForbiddenTreeRecord {
    public enum Phase { UNREQUESTED, SEARCHING, VALIDATING, PLACING, READY, FAILED }
    public Phase phase=Phase.UNREQUESTED;
    public boolean anchored,generated,repair;
    public int x,y,z,rotation,attempt,cursor,placements;
    public String dimension="minecraft:the_nether",version="psemata-face-v1-1.20.1",hash="",failure="";
    public UUID requestedBy;
    public boolean request(UUID owner){if(anchored||phase!=Phase.UNREQUESTED)return false;requestedBy=owner;phase=Phase.SEARCHING;return true;}
    public boolean reserve(int x,int y,int z,String hash){
        if(anchored||phase!=Phase.VALIDATING)return false;
        this.x=x;this.y=y;this.z=z;this.hash=hash;anchored=true;cursor=0;phase=Phase.PLACING;return true;
    }
    public boolean repair(){if(!anchored||phase==Phase.PLACING)return false;repair=true;cursor=0;phase=Phase.PLACING;return true;}
    public void complete(){if(!anchored||phase!=Phase.PLACING)throw new IllegalStateException("No reserved placement");if(!generated)placements++;generated=true;repair=false;phase=Phase.READY;failure="";}
    public void fail(String reason){phase=Phase.FAILED;failure=reason;}
    public NbtCompound write(){NbtCompound n=new NbtCompound();n.putString("Phase",phase.name());n.putBoolean("Anchored",anchored);n.putBoolean("Generated",generated);n.putBoolean("Repair",repair);n.putInt("X",x);n.putInt("Y",y);n.putInt("Z",z);n.putInt("Rotation",rotation);n.putInt("Attempt",attempt);n.putInt("Cursor",cursor);n.putInt("Placements",placements);n.putString("Dimension",dimension);n.putString("Version",version);n.putString("Hash",hash);n.putString("Failure",failure);if(requestedBy!=null)n.putUuid("RequestedBy",requestedBy);n.putInt("SizeX",64);n.putInt("SizeY",85);n.putInt("SizeZ",37);return n;}
    public static ForbiddenTreeRecord read(NbtCompound n){
        ForbiddenTreeRecord r=new ForbiddenTreeRecord();try{r.phase=Phase.valueOf(n.getString("Phase"));}catch(IllegalArgumentException ignored){}
        r.anchored=n.getBoolean("Anchored");r.generated=n.getBoolean("Generated");r.repair=n.getBoolean("Repair");r.x=n.getInt("X");r.y=n.getInt("Y");r.z=n.getInt("Z");r.rotation=n.getInt("Rotation");r.attempt=Math.max(0,n.getInt("Attempt"));r.cursor=Math.max(0,n.getInt("Cursor"));r.placements=Math.max(0,n.getInt("Placements"));r.hash=n.getString("Hash");r.failure=n.getString("Failure");if(n.containsUuid("RequestedBy"))r.requestedBy=n.getUuid("RequestedBy");
        if(n.contains("Dimension"))r.dimension=n.getString("Dimension");if(n.contains("Version"))r.version=n.getString("Version");
        // An anchored job can only resume at its saved site; no new search after crash.
        if(r.anchored&&(r.phase==Phase.SEARCHING||r.phase==Phase.VALIDATING))r.phase=Phase.PLACING;
        if(r.phase==Phase.PLACING)r.cursor=0; // Replay same blocks idempotently; chunks can lag the ledger.
        if(r.phase==Phase.VALIDATING)r.phase=Phase.SEARCHING;
        return r;
    }
}
