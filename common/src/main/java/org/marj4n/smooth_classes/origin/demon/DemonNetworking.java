package org.marj4n.smooth_classes.origin.demon;

import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.text.Text;
import org.marj4n.smooth_classes.network.SmoothClassesNetworking;

public final class DemonNetworking {
    public static final Identifier OPEN_PATH=org.marj4n.smooth_classes.util.ResourceIds.parse("smooth_classes:demon_open_path"),CHOOSE_PATH=org.marj4n.smooth_classes.util.ResourceIds.parse("smooth_classes:demon_choose_path"),OPEN_ARMY=org.marj4n.smooth_classes.util.ResourceIds.parse("smooth_classes:demon_open_army"),ARMY=org.marj4n.smooth_classes.util.ResourceIds.parse("smooth_classes:demon_army");
    private DemonNetworking(){}
    public static void register(){
        ServerPlayNetworking.registerGlobalReceiver(CHOOSE_PATH,(server,player,handler,buf,sender)->{
            String path=buf.readString(32);server.execute(()->{if(!accept(player))return;if(!DemonProgression.choose(player,path))player.sendMessage(Text.literal("This evolutionary path is unavailable."),true);});
        });
        ServerPlayNetworking.registerGlobalReceiver(ARMY,(server,player,handler,buf,sender)->{
            int command=buf.readVarInt();server.execute(()->{if(!accept(player))return;var r=DemonArmy.command(player,command);player.sendMessage(Text.literal(r.message()),true);});
        });
    }
    public static void openPath(ServerPlayerEntity p){ServerPlayNetworking.send(p,OPEN_PATH,PacketByteBufs.empty());}
    private static boolean accept(ServerPlayerEntity p){
        if(!p.isAlive()||!DemonProgression.demon(p))return false;
        var state=org.marj4n.smooth_classes.origin.OriginRuntime.state(p);long now=p.getServer().getOverworld().getTime();
        if(now<state.longProgress("demon.packet_after"))return false;state.longProgress("demon.packet_after",now+5);return true;
    }
    public static void openArmy(ServerPlayerEntity p){ServerPlayNetworking.send(p,OPEN_ARMY,PacketByteBufs.empty());}
}
