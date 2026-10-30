package org.marj4n.smooth_classes.client.origin.demon;
import java.util.*;
import java.nio.file.*;
import com.google.gson.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.*;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.ScreenshotRecorder;
import org.marj4n.smooth_classes.network.SmoothClassesNetworking;
import org.marj4n.smooth_classes.origin.demon.*;

/** Native framebuffer evidence and normal selection packets; never desktop input automation. */
public final class ForbiddenBirthDiagnosticsClient {
    private static final Map<UUID,Set<Integer>> SHOTS=new HashMap<>();
    private static final Set<UUID> RESTORED=new HashSet<>();
    private static ForbiddenBirthClient.Scene last;private static int after,age,totalTicks;private static String lastScreen="";private static volatile int spState;private static long spLast;private static boolean previousPerspective;
    private ForbiddenBirthDiagnosticsClient(){}
    private static void select(){var b=PacketByteBufs.create();b.writeString("demon");ClientPlayNetworking.send(SmoothClassesNetworking.SELECT_ORIGIN,b);MinecraftClient.getInstance().setScreen(null);}
    private static void action(String action){var c=MinecraftClient.getInstance();switch(action){case "select"->select();case "skip"->ForbiddenBirthClient.skip();case "invalid_skip"->{var b=PacketByteBufs.create();b.writeUuid(UUID.randomUUID());ClientPlayNetworking.send(ForbiddenBirth.SKIP,b);}case "respawn"->{if(c.player!=null)c.player.requestRespawn();}case "reduced"->ForbiddenBirthClient.reducedMotion();}}
    public static void register(){if(!ForbiddenBirthChecks.ENABLED)return;
        ClientPlayNetworking.registerGlobalReceiver(ForbiddenBirthChecks.ACTION,(client,h,b,s)->{String a=b.readString(32);client.execute(()->action(a));});
        ClientTickEvents.END_CLIENT_TICK.register(c->{totalTicks++;if(c.player==null||c.world==null){
                if(c.currentScreen!=null){String title=c.currentScreen.getTitle().getString();if(!title.equals(lastScreen)){lastScreen=title;var buttons=new ArrayList<String>();for(var e:c.currentScreen.children())if(e instanceof net.minecraft.client.gui.widget.ButtonWidget b)buttons.add(b.getMessage().getString());write(c,"pre-world-screen",Map.of("title",title,"buttons",buttons));ScreenshotRecorder.saveScreenshot(c.runDirectory,"pre-world-"+totalTicks+".png",c.getFramebuffer(),m->{});}
                    if(System.getProperty("smooth_classes.forbiddenBirthMode","").startsWith("singleplayer"))for(var e:c.currentScreen.children())if(e instanceof net.minecraft.client.gui.widget.ButtonWidget b&&b.active&&b.visible&&(b.getMessage().getString().equals("Proceed")||b.getMessage().getString().equals("I know what I'm doing!"))){b.onPress();break;}
                }return;}age++;var scene=ForbiddenBirthClient.own();
            if(scene!=null){if(last==null||!last.token().equals(scene.token())){previousPerspective=c.options.getPerspective().isFirstPerson();last=scene;after=0;}else last=scene;
                if(scene.start()>=0){double t=c.world.getTime()-scene.start();var done=SHOTS.computeIfAbsent(scene.token(),k->new HashSet<>());for(int phase:new int[]{12,70,125,167,198,245})if(t>=phase&&done.add(phase)){String file="birth-"+c.player.getUuid().toString().substring(0,8)+"-"+scene.token().toString().substring(0,8)+"-phase"+phase+".png";ScreenshotRecorder.saveScreenshot(c.runDirectory,file,c.getFramebuffer(),m->{});write(c,"cinematic-frame",Map.of("image",file,"phase_target",phase,"actual_tick",t,"camera",c.gameRenderer.getCamera().getPos().toString(),"owner",scene.player().toString(),"uuid",c.player.getUuid().toString(),"hidden",ForbiddenBirthClient.hidden(c.player.getUuid()),"scale",ForbiddenBirthClient.scale(c.player.getUuid(),0),"fps",c.getCurrentFps()));break;}}
            }else if(last!=null&&++after>=20&&RESTORED.add(last.token())){boolean perspective=c.options.getPerspective().isFirstPerson()==previousPerspective;boolean clear=!(c.currentScreen instanceof ForbiddenBirthClient.BirthScreen);boolean camera=c.gameRenderer.getCamera().isThirdPerson()!=c.options.getPerspective().isFirstPerson();write(c,"restored",Map.of("uuid",c.player.getUuid().toString(),"perspective_unchanged",perspective,"screen_released",clear,"camera_matches_vanilla_perspective",camera,"server_origin",org.marj4n.smooth_classes.client.origin.OriginClientState.originId));}
            singleplayer(c);
        });
    }
    private static void write(MinecraftClient c,String kind,Map<String,Object> data){var all=new LinkedHashMap<String,Object>();all.put("kind",kind);all.put("data",data);all.put("source","Actual Minecraft1.20.1 client/framebuffer; phase may differ by one rendered frame");try{Files.writeString(c.runDirectory.toPath().resolve("forbidden-birth-client-evidence.jsonl"),new Gson().toJson(all)+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);}catch(Exception e){throw new IllegalStateException(e);}}
    private static void singleplayer(MinecraftClient c){String mode=System.getProperty("smooth_classes.forbiddenBirthMode","");if(!mode.startsWith("singleplayer")||c.getServer()==null||age<80)return;var server=c.getServer();var playerId=c.player.getUuid();if(!server.getSaveProperties().getLevelName().toLowerCase(Locale.ROOT).contains("forbidden"))return;
        if(spState==0){spState=1;if(mode.equals("singleplayer-first")){server.execute(()->{var p=server.getPlayerManager().getPlayer(playerId);if(p!=null){server.setDifficulty(net.minecraft.world.Difficulty.PEACEFUL,true);p.changeGameMode(net.minecraft.world.GameMode.SURVIVAL);ForbiddenBirthChecks.snapshot(p);}});select();}}
        if(spState!=1||c.world.getTime()-spLast<20)return;spLast=c.world.getTime();server.execute(()->{var p=server.getPlayerManager().getPlayer(playerId);if(p==null)return;var r=ForbiddenTreePlacement.record(server);if(r.phase!=ForbiddenTreeRecord.Phase.READY||ForbiddenBirth.active(p)||!DemonWorldState.get(server).born.contains(p.getUuid()))return;try{ForbiddenBirthChecks.run("tree",p);ForbiddenBirthChecks.run("born",p);if(mode.equals("singleplayer-reload"))ForbiddenBirthChecks.run("legacy",p);spState=2;Files.writeString(c.runDirectory.toPath().resolve(mode+"-pass.json"),new Gson().toJson(Map.of("passed",true,"actual_integrated_server",!server.isDedicated(),"tree_placements",r.placements,"anchor",List.of(r.x,r.y,r.z),"uuid",p.getUuid().toString(),"mode",mode)));server.save(false,true,true);c.execute(c::scheduleStop);}catch(Exception|AssertionError ex){spState=3;try{Files.writeString(c.runDirectory.toPath().resolve(mode+"-fail.txt"),ex.toString());}catch(Exception ignored){}}});
    }
}
