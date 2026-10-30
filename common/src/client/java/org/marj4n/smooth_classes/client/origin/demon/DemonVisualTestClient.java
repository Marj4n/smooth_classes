package org.marj4n.smooth_classes.client.origin.demon;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.util.Identifier;
import org.marj4n.smooth_classes.SmoothClasses;
import java.nio.file.*;

/** Opt-in disposable localhost runtime fixture. Captures actual rendered framebuffer. */
public final class DemonVisualTestClient {
 private static String mode;
 private static int ticks;
 public static void register(){
  if(!Boolean.getBoolean("smooth_classes.demonTest"))return;
  ClientPlayNetworking.registerGlobalReceiver(org.marj4n.smooth_classes.util.ResourceIds.parse("smooth_classes:demon_visual_test_capture"),(client,handler,buf,sender)->{
   String requested=buf.readString(60);client.execute(()->{
    if(client.getCurrentServerEntry()==null||!client.getCurrentServerEntry().address.startsWith("127.0.0.1:"))return;
    mode=requested;ticks=requested.equals("pulse")?2:30;client.setScreen(null);client.options.hudHidden=!requested.equals("hud");
    client.options.setPerspective(requested.equals("back")?Perspective.THIRD_PERSON_BACK:requested.equals("first")||requested.equals("peer")?Perspective.FIRST_PERSON:Perspective.THIRD_PERSON_FRONT);
    client.options.forwardKey.setPressed(requested.equals("walk")||requested.equals("run"));client.options.sprintKey.setPressed(requested.equals("run"));client.options.sneakKey.setPressed(requested.equals("crouch"));
   });
  });
  ClientTickEvents.END_CLIENT_TICK.register(client->{if(mode==null||client.player==null||client.world==null)return;
   if(--ticks>0)return;String captured=mode;mode=null;
   String file="sovereign-"+captured+"-"+client.player.getUuid().toString().substring(0,8)+"-"+System.currentTimeMillis()+".png";
   ScreenshotRecorder.saveScreenshot(client.runDirectory,file,client.getFramebuffer(),message->{});
   client.options.forwardKey.setPressed(false);client.options.sprintKey.setPressed(false);client.options.sneakKey.setPressed(false);
   try{String json=new com.google.gson.Gson().toJson(java.util.Map.of("image",file,"mode",captured,"fps",client.getCurrentFps(),"streak",DemonStreakClient.stacks(client.player.getUuid()),"remaining_ticks",DemonStreakClient.remaining(client.player.getUuid()),"glow",DemonStreakClient.crownIntensity(client.player.getUuid(),0),"skin_model",client.player.getModel(),"skin_texture",client.player.getSkinTexture().toString(),"viewport",java.util.List.of(client.getWindow().getWidth(),client.getWindow().getHeight()),"source","Actual Minecraft1.20.1 framebuffer; controlled localhost fixture, shaders disabled in test profile"));Files.writeString(client.runDirectory.toPath().resolve("sovereign-captures.jsonl"),json+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);}catch(Exception ex){SmoothClasses.LOGGER.error("Cannot record visual fixture",ex);}
  });
 }
}
