package org.marj4n.smooth_classes.origin.demon;

import java.util.*;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.item.*;
import net.minecraft.registry.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import net.minecraft.text.Text;
import org.marj4n.smooth_classes.origin.*;
import org.marj4n.smooth_classes.SmoothClasses;

/** Actual player remains the sole character. Fruit is client-only, not an item drop. */
public final class ForbiddenBirth {
    public static final Identifier START=Identifier.of("smooth_classes","forbidden_birth_start"),END=Identifier.of("smooth_classes","forbidden_birth_end"),SKIP=Identifier.of("smooth_classes","forbidden_birth_skip"),READY=Identifier.of("smooth_classes","forbidden_birth_ready");
    public static final Item FRUIT=org.marj4n.smooth_classes.platform.PlatformRegistry.register(org.marj4n.smooth_classes.platform.PlatformRegistries.ITEM,Identifier.of("smooth_classes","forbidden_apple_cinematic"),new Item(new Item.Settings().maxCount(1)));
    public record Scene(UUID player,UUID token,long start,Vec3d anchor,Vec3d fruit,Vec3d landing){}
    private static final Map<UUID,Scene> ACTIVE=new HashMap<>();
    private static final Map<UUID,Long> PREPARED_AT=new HashMap<>();
    private ForbiddenBirth(){}
    public static void register(){
        ForbiddenBirthConfig.load();
        ForbiddenBirthChecks.register();
        ServerPlayNetworking.registerGlobalReceiver(READY,(server,p,handler,buf,response)->{if(buf.readableBytes()<16)return;UUID token=buf.readUuid();server.execute(()->{var old=ACTIVE.get(p.getUuid());if(old==null||old.start()>=0||!old.token().equals(token)||p.getWorld().getRegistryKey()!=World.NETHER)return;var scene=new Scene(old.player(),old.token(),p.getServerWorld().getTime(),old.anchor(),old.fruit(),old.landing());ACTIVE.put(p.getUuid(),scene);for(var receiver:server.getPlayerManager().getPlayerList())send(receiver,scene);SmoothClasses.LOGGER.info("FORBIDDEN_BIRTH CAMERA_READY uuid={} time={}",p.getUuid(),scene.start());});});
        ServerPlayNetworking.registerGlobalReceiver(SKIP,(server,p,handler,buf,response)->{if(buf.readableBytes()<16)return;UUID token=buf.readUuid();server.execute(()->{var s=ACTIVE.get(p.getUuid());if(s!=null&&s.token().equals(token))finish(p,"skipped");});});
        net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register((d,r,e)->d.register(CommandManager.literal("smoothclasses").then(CommandManager.literal("demon").then(CommandManager.literal("tree").requires(src->src.hasPermissionLevel(2))
            .then(CommandManager.literal("status").executes(ctx->{var t=ForbiddenTreePlacement.record(ctx.getSource().getServer());ctx.getSource().sendFeedback(()->Text.literal("Forbidden Tree: "+t.phase+" anchor="+(t.anchored?t.x+","+t.y+","+t.z:"none")+" generated="+t.generated+" placements="+t.placements+" cursor="+t.cursor+" reason="+t.failure),false);return 1;}))
            .then(CommandManager.literal("repair").executes(ctx->{var p=ctx.getSource().getPlayerOrThrow();boolean ok=ForbiddenTreePlacement.repair(p);ctx.getSource().sendFeedback(()->Text.literal(ok?"Explicit repair queued at the SAME saved anchor. No new tree location.":"No anchored tree to repair, or a placement is already running."),false);return ok?1:0;}))
            .then(CommandManager.literal("retry").executes(ctx->{var p=ctx.getSource().getPlayerOrThrow();var t=ForbiddenTreePlacement.record(p.getServer());if(t.anchored||t.phase!=ForbiddenTreeRecord.Phase.FAILED)return 0;t.phase=ForbiddenTreeRecord.Phase.UNREQUESTED;t.attempt=0;ForbiddenTreePlacement.request(p);return 1;}))
        ))));
    }
    public static boolean active(ServerPlayerEntity p){return ACTIVE.containsKey(p.getUuid());}
    public static Collection<Scene> scenes(){return List.copyOf(ACTIVE.values());}
    public static boolean start(ServerPlayerEntity p){
        var ledger=DemonWorldState.get(p.getServer());if(ledger.born.contains(p.getUuid())||active(p))return false;
        var r=ledger.forbiddenTree;var w=p.getServer().getWorld(World.NETHER);if(w==null)return false;
        BlockPos a=ForbiddenTreeAsset.anchor(r);w.getChunk((a.getX()+33)>>4,(a.getZ()+28)>>4);
        if(!ForbiddenTreePlacement.recognizable(w,r))return false;
        Vec3d landing=null;for(int[] d:new int[][]{{0,0},{2,0},{-2,0},{0,2},{0,-2},{2,2},{-2,2},{2,-2},{-2,-2}}){
            Vec3d test=Vec3d.of(a).add(ForbiddenTreeAsset.LANDING).add(d[0],0,d[1]);BlockPos feet=BlockPos.ofFloored(test);
            if(!ForbiddenTreePlacement.safeFloor(w,feet.down())||!DemonProtection.allowCombat(p,w,feet)||!w.isSpaceEmpty(p,new Box(test.x-.35,test.y,test.z-.35,test.x+.35,test.y+2.1,test.z+.35)))continue;
            boolean occupied=false;for(var other:ACTIVE.values())if(other.landing().squaredDistanceTo(test)<2.25){occupied=true;break;}if(!occupied){landing=test;break;}
        }
        if(landing==null)return false;
        p.stopRiding();p.setVelocity(Vec3d.ZERO);p.fallDistance=0;p.teleport(w,landing.x,landing.y,landing.z,-36.87f,0);
        p.setHeadYaw(-36.87f);p.setBodyYaw(-36.87f);
        var state=OriginRuntime.state(p);state.unflag("demon.birth_pending");state.progress("demon.birth_wait",0);state.progress("demon.protection",10);
        ledger.born.add(p.getUuid());ledger.markDirty();ForbiddenTreePlacement.checkpoint(p.getServer());
        for(var existing:ACTIVE.values())send(p,existing);
        var scene=new Scene(p.getUuid(),UUID.randomUUID(),-1,Vec3d.of(a),Vec3d.of(a).add(ForbiddenTreeAsset.FRUIT),landing);ACTIVE.put(p.getUuid(),scene);PREPARED_AT.put(p.getUuid(),w.getTime());
        for(var receiver:p.getServer().getPlayerManager().getPlayerList())send(receiver,scene);
        SmoothClasses.LOGGER.info("FORBIDDEN_BIRTH START uuid={} token={} anchor={} landing={} treePlacements={}",p.getUuid(),scene.token(),a,landing,r.placements);
        return true;
    }
    private static void vector(PacketByteBuf b,Vec3d p){b.writeDouble(p.x);b.writeDouble(p.y);b.writeDouble(p.z);}
    private static void send(ServerPlayerEntity p,Scene s){if(!ServerPlayNetworking.canSend(p,START))return;var b=PacketByteBufs.create();b.writeUuid(s.player());b.writeUuid(s.token());b.writeLong(s.start());vector(b,s.anchor());vector(b,s.fruit());vector(b,s.landing());ServerPlayNetworking.send(p,START,b);}
    public static void join(ServerPlayerEntity p){for(var s:ACTIVE.values())send(p,s);if(DemonWorldState.get(p.getServer()).born.contains(p.getUuid()))OriginRuntime.state(p).unflag("demon.birth_pending");}
    public static void tick(ServerPlayerEntity p){
        var s=ACTIVE.get(p.getUuid());if(s==null)return;
        if(!p.isAlive()||p.getWorld().getRegistryKey()!=World.NETHER||OriginRuntime.state(p).origin()!=OriginType.DEMON){finish(p,"interrupted");return;}
        long elapsed=p.getServerWorld().getTime()-(s.start()<0?PREPARED_AT.getOrDefault(p.getUuid(),p.getServerWorld().getTime()):s.start());
        if(elapsed>=(s.start()<0?200:ForbiddenBirthTimeline.END)||elapsed<0){finish(p,"completed");return;}
        OriginRuntime.state(p).progress("demon.protection",10);p.setVelocity(Vec3d.ZERO);p.fallDistance=0;
        if(p.getPos().squaredDistanceTo(s.landing())>.0025)p.networkHandler.requestTeleport(s.landing().x,s.landing().y,s.landing().z,p.getYaw(),p.getPitch());
    }
    public static void finish(ServerPlayerEntity p,String reason){DemonBirth.release(p.getUuid());var s=ACTIVE.remove(p.getUuid());if(s==null)return;PREPARED_AT.remove(p.getUuid());OriginRuntime.state(p).progress("demon.protection",0);
        for(var receiver:p.getServer().getPlayerManager().getPlayerList())if(ServerPlayNetworking.canSend(receiver,END)){var b=PacketByteBufs.create();b.writeUuid(p.getUuid());b.writeUuid(s.token());ServerPlayNetworking.send(receiver,END,b);}
        if(reason.equals("completed")){p.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.TitleS2CPacket(Text.literal("APPLEBORN").formatted(net.minecraft.util.Formatting.DARK_RED)));p.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.SubtitleS2CPacket(Text.literal("Born from the Forbidden Tree.")));}
        SmoothClasses.LOGGER.info("FORBIDDEN_BIRTH END uuid={} reason={}",p.getUuid(),reason);
    }
    public static void clear(MinecraftServer s){for(var p:s.getPlayerManager().getPlayerList())finish(p,"server stopping");ACTIVE.clear();PREPARED_AT.clear();ForbiddenTreePlacement.clear();DemonBirth.clear();ForbiddenChunkLoad.clear();}
}
