package org.marj4n.smooth_classes.origin.demon;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import com.mojang.datafixers.util.Either;
import net.minecraft.server.world.*;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.*;

/** Pollable, bounded FULL-chunk lease. All ticket mutations occur on the server thread. */
public final class ForbiddenChunkLoad implements AutoCloseable {
    private static final ChunkTicketType<Long> TICKET=ChunkTicketType.create("smooth_classes_forbidden_birth",Long::compare);
    private static final Set<ForbiddenChunkLoad> LIVE=new HashSet<>();
    private final ServerChunkManager manager;public final ChunkPos pos;
    private final CompletableFuture<Either<Chunk,ChunkHolder.Unloaded>> future;
    private boolean closed;
    public ForbiddenChunkLoad(ServerWorld world,int x,int z){
        manager=world.getChunkManager();pos=new ChunkPos(x,z);
        manager.addTicket(TICKET,pos,0,pos.toLong());LIVE.add(this);
        //1.20.1's public method managed-blocks when called ON the server thread.
        //Its documented off-thread branch schedules the factory on that thread and returns a future.
        //Do not call getChunk/join on an unfinished future from the server tick.
        future=CompletableFuture.supplyAsync(()->manager.getChunkFutureSyncOnMainThread(x,z,ChunkStatus.FULL,true)).thenCompose(f->f);
    }
    public boolean ready(){if(!future.isDone())return false;if(future.join().left().isEmpty())throw new IllegalStateException("Chunk lease unloaded: "+pos);return true;}
    @Override public void close(){if(closed)return;closed=true;manager.removeTicket(TICKET,pos,0,pos.toLong());LIVE.remove(this);}
    public static int active(){return LIVE.size();}
    public static void clear(){for(var lease:List.copyOf(LIVE))lease.close();}
}
