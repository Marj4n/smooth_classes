package org.marj4n.smooth_classes.client.origin.demon;
import java.util.*;
import net.fabricmc.fabric.api.client.networking.v1.*;
import net.minecraft.client.MinecraftClient;
import org.marj4n.smooth_classes.origin.demon.DemonKillStreak;
/** Visual snapshot only; local countdown does not grant attributes or prolong the state. */
public final class DemonStreakClient {
    private record Snapshot(long stacks,int ticks,long receivedTick,long lastKillTick){}
    private static final Map<UUID,Snapshot> STATES=new HashMap<>();
    private DemonStreakClient(){}
    public static long stacks(UUID id){var s=STATES.get(id);return s==null?0:s.stacks;}
    public static int remaining(UUID id){var s=STATES.get(id);var w=MinecraftClient.getInstance().world;return s==null||w==null?0:(int)Math.max(0,s.ticks-Math.max(0,w.getTime()-s.receivedTick));}
    public static float crownIntensity(UUID id,float partial){var s=STATES.get(id);var w=MinecraftClient.getInstance().world;double time=w==null?0:w.getTime()+partial;float pulse=s==null?0:(float)Math.max(0,1-(time-s.lastKillTick)/10d);return Math.min(1,(s==null?.78f:.90f)+pulse*.10f);}
    public static void register(){
        ClientPlayNetworking.registerGlobalReceiver(DemonKillStreak.SYNC,(client,handler,buf,sender)->{UUID id=buf.readUuid();long n=buf.readVarLong();int ticks=buf.readVarInt();client.execute(()->{if(n==0)STATES.remove(id);else {long now=client.world==null?0:client.world.getTime();Snapshot previous=STATES.get(id);STATES.put(id,new Snapshot(n,ticks,now,previous==null||n>previous.stacks?now:previous.lastKillTick));};});});
        ClientPlayConnectionEvents.DISCONNECT.register((handler,client)->STATES.clear());
    }
}
