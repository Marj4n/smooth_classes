package org.marj4n.smooth_classes.content.avenger.runtime;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.marj4n.smooth_classes.util.ResourceIds;
import java.util.*;

/** Cosmetic ownership proof, synchronized by tracking events without altering vanilla entity metadata. */
public final class AvengerVisualBindings {
    public static final Identifier CHANNEL=ResourceIds.parse("smooth_classes:bound_visual");
    private static final Map<World,Set<UUID>> CLIENT=new WeakHashMap<>();
    private AvengerVisualBindings(){}
    public static void registerServer(){
        EntityTrackingEvents.START_TRACKING.register((entity,player)->{if(AvengerReworkRuntime.isAvengerSummon(entity))send(player,entity,true);});
        EntityTrackingEvents.STOP_TRACKING.register((entity,player)->{if(AvengerReworkRuntime.isAvengerSummon(entity))send(player,entity,false);});
    }
    private static void send(ServerPlayerEntity player,Entity entity,boolean bound){
        if(!ServerPlayNetworking.canSend(player,CHANNEL))return;
        var buffer=PacketByteBufs.create();buffer.writeString(entity.getWorld().getRegistryKey().getValue().toString());buffer.writeUuid(entity.getUuid());buffer.writeBoolean(bound);
        ServerPlayNetworking.send(player,CHANNEL,buffer);
    }
    public static void refreshTracking(Entity entity){
        if(!(entity.getWorld() instanceof ServerWorld))return;
        boolean bound=AvengerReworkRuntime.isAvengerSummon(entity);
        for(var player:PlayerLookup.tracking(entity))send(player,entity,bound);
    }
    public static boolean clientBound(Entity entity){return CLIENT.getOrDefault(entity.getWorld(),Set.of()).contains(entity.getUuid());}
    public static void updateClient(World world,UUID uuid,boolean bound){
        if(bound)CLIENT.computeIfAbsent(world,ignored->new HashSet<>()).add(uuid);
        else{var set=CLIENT.get(world);if(set!=null)set.remove(uuid);}
    }
    public static void clearClient(){CLIENT.clear();}
}
