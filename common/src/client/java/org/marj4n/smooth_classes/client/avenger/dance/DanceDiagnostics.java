package org.marj4n.smooth_classes.client.avenger.dance;
import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.model.ModelPart;
import net.minecraft.entity.*;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.marj4n.smooth_classes.content.avenger.dance.DanceProfiles;
import org.marj4n.smooth_classes.mixin.client.AvengerDancePartAccessor;

/** Opt-in asset extraction: copies actual baked vanilla geometry for authoring only. */
public final class DanceDiagnostics {
    private static boolean exported;
    private static boolean registered;
    private static String recording;private static int recordTicks,shotIndex,shotCount=16,shotInterval=5;
    private static int capturePreviousFps; private static String frameRecording; private static long frameStart,frameEnd,frameNext; private static int frameIndex; private static final java.util.ArrayList<Object> captureFrames=new java.util.ArrayList<>();
    private static long lastFrame,benchStart,benchRender,benchCalls,benchGc;public static String benchmark;private static final java.util.ArrayList<Double> frameMs=new java.util.ArrayList<>();
    private static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
    public static void register(){if(registered||!org.marj4n.smooth_classes.content.avenger.dance.DanceChecks.ENABLED)return;registered=true;
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(org.marj4n.smooth_classes.content.avenger.dance.DanceChecks.ACTION,(c,h,b,r)->{String text=b.readString(256);c.execute(()->action(c,text.split(" +")));});
    }
    private static long gc(){long t=0;for(var bean:java.lang.management.ManagementFactory.getGarbageCollectorMXBeans())t+=Math.max(0,bean.getCollectionTime());return t;}
    private static void write(MinecraftClient c,String name,Object data){try{Files.writeString(c.runDirectory.toPath().resolve(name+".json"),JSON.toJson(data));}catch(Exception ex){throw new IllegalStateException(ex);}}
    private static void action(MinecraftClient c,String[] a){if(c.world==null||c.player==null)return;
        switch(a[0]){
            case "view"->{c.setScreen(null);c.options.hudHidden=true;c.player.setYaw(Float.parseFloat(a[1]));c.player.setPitch(Float.parseFloat(a[2]));}
            case "snapshot"->{snapshot(c,a[1]);}
            case "export"->{exported=false;}
            case "record"->{recording=a[1];recordTicks=0;shotIndex=0;shotCount=16;shotInterval=5;}
            case "capture"->{recording=a[1];recordTicks=0;shotIndex=0;shotCount=Math.min(640,Integer.parseInt(a[2]));shotInterval=1;}
            case "rendercapture"->{capturePreviousFps=c.options.getMaxFps().getValue();c.options.getMaxFps().setValue(60);frameRecording=a[1];frameStart=System.nanoTime();frameNext=frameStart;frameEnd=frameStart+(long)(Double.parseDouble(a[2])*1e9);frameIndex=0;captureFrames.clear();}
            case "bench_start"->{benchmark=a[1];frameMs.clear();lastFrame=0;benchStart=System.nanoTime();benchRender=AvengerDanceClient.renderNanos;benchCalls=AvengerDanceClient.renderCalls;benchGc=gc();}
            case "bench_stop"->{double elapsed=(System.nanoTime()-benchStart)/1e9;var sorted=new java.util.ArrayList<>(frameMs);sorted.sort(java.util.Comparator.reverseOrder());int count=Math.max(1,(int)Math.ceil(sorted.size()*.01));double slow=sorted.stream().limit(count).mapToDouble(Double::doubleValue).average().orElse(Double.NaN);write(c,"benchmark-"+benchmark,Map.of("seconds",elapsed,"frames",frameMs.size(),"fps",frameMs.size()/elapsed,"slowest_1_percent_frame_average_ms",slow,"one_percent_low_fps",1000/slow,"overlay_cpu_ms",(AvengerDanceClient.renderNanos-benchRender)/1e6,"overlay_render_calls",AvengerDanceClient.renderCalls-benchCalls,"client_gc_ms",gc()-benchGc,"frame_method","Actual GameRenderer render intervals; includes two-client contention and focus effects"));benchmark=null;}
            case "quit"->c.scheduleStop();
        }
    }
    public static void frame(float delta){if(!org.marj4n.smooth_classes.content.avenger.dance.DanceChecks.ENABLED)return;long now=System.nanoTime();if(benchmark!=null&&lastFrame!=0&&frameMs.size()<100000)frameMs.add((now-lastFrame)/1e6);lastFrame=now;
        if(frameRecording!=null){var c=MinecraftClient.getInstance();
            if(c.world==null||c.player==null){c.options.getMaxFps().setValue(capturePreviousFps);frameRecording=null;captureFrames.clear();return;}
            if(now>=frameEnd){write(c,"rendercapture-"+frameRecording,Map.of("method","Actual GameRenderer framebuffer frames, no interpolation","requested_max_fps",60,"elapsed_seconds",(now-frameStart)/1e9,"frames",captureFrames));c.options.getMaxFps().setValue(capturePreviousFps);frameRecording=null;captureFrames.clear();}
            else{
                String name="render-"+frameRecording+"-"+String.format("%04d",frameIndex++);
                var rows=new java.util.ArrayList<Object>();for(var p:AvengerDanceClient.playbacks())rows.add(Map.of("uuid",p.uuid().toString(),"profile",p.profile(),"start",p.start(),"phase",AvengerDanceClient.phase(p,c.world.getTime(),delta)));
                captureFrames.add(Map.of("file",name+".png","elapsed_seconds",(now-frameStart)/1e9,"tick",c.world.getTime(),"delta",delta,"active",rows));
                net.minecraft.client.util.ScreenshotRecorder.saveScreenshot(c.runDirectory,name+".png",c.getFramebuffer(),t->{});
                // Keep the schedule on the real timeline; skipped deadlines remain honest gaps.
                frameNext=frameStart+((now-frameStart)/16_666_667L+1)*16_666_667L;
            }
        }
    }
    private static void snapshot(MinecraftClient c,String name){var rows=new java.util.ArrayList<Object>();for(var p:AvengerDanceClient.playbacks()){var e=c.world.getEntityById(p.id());rows.add(Map.of("uuid",p.uuid().toString(),"profile",p.profile(),"start",p.start(),"phase",AvengerDanceClient.phase(p,c.world.getTime(),0),"render_eligible",e instanceof LivingEntity l&&AvengerDanceClient.playback(l)!=null));}write(c,"dance-"+name,Map.of("client",c.player.getUuid().toString(),"tick",c.world.getTime(),"active",rows,"restore_checks",AvengerDanceClient.restoreChecks,"restore_errors",AvengerDanceClient.restoreErrors,"render_calls",AvengerDanceClient.renderCalls,"overlay_cpu_ms",AvengerDanceClient.renderNanos/1e6,"render_probes",AvengerDanceClient.probes,"pose_details",AvengerDanceClient.poseDetails));net.minecraft.client.util.ScreenshotRecorder.saveScreenshot(c.runDirectory,"dance-"+name+".png",c.getFramebuffer(),t->{});}
    public static void tick(MinecraftClient c){
        if(recording!=null&&c.world!=null&&c.player!=null&&++recordTicks%shotInterval==0){snapshot(c,recording+"-"+String.format(shotInterval==1?"%03d":"%02d",shotIndex++));if(shotIndex>=shotCount)recording=null;}

        String output=System.getProperty("smooth_classes.danceExport");if(output==null||exported||c.world==null||c.player==null)return;
        try{Path dir=Path.of(output);Files.createDirectories(dir);int count=0;
            for(String id:java.util.stream.Stream.concat(DanceProfiles.SIGNATURES.stream(),java.util.stream.Stream.of("cow","ghast","cod","cat")).distinct().toList()){var type=Registries.ENTITY_TYPE.get(new Identifier("minecraft",id));Entity entity=type.create(c.world);var renderer=c.getEntityRenderDispatcher().getRenderer(entity);if(!(renderer instanceof LivingEntityRenderer<?,?> living))throw new IllegalStateException("No living renderer "+id);((net.minecraft.client.render.entity.model.EntityModel)living.getModel()).animateModel(entity,0,0,0);((net.minecraft.client.render.entity.model.EntityModel)living.getModel()).setAngles(entity,0,0,0,0,0);var rig=new DanceRig(living.getModel());if(rig.roots.isEmpty())throw new IllegalStateException("No root "+id);
                var model=new LinkedHashMap<String,Object>();model.put("id",id);model.put("source","Actual vanilla baked EntityRenderer model, Minecraft Java1.20.1; not a replacement");model.put("model_class",living.getModel().getClass().getName());var roots=new ArrayList<Object>();for(var e:rig.roots.entrySet())roots.add(part(e.getKey(),e.getValue()));model.put("roots",roots);
                model.put("texture",((LivingEntityRenderer)living).getTexture((LivingEntity)entity).toString());Files.writeString(dir.resolve(id+".json"),JSON.toJson(model));count++;
            }exported=true;Files.writeString(dir.resolve("PASS.json"),"{\"original_vanilla_rigs\":"+count+"}");org.marj4n.smooth_classes.SmoothClasses.LOGGER.info("AVENGER_DANCE exported {} original rigs",count);
        }catch(Exception e){exported=true;org.marj4n.smooth_classes.SmoothClasses.LOGGER.error("Dance rig export failed",e);}
    }
    private static Object part(String name,ModelPart p){var n=new LinkedHashMap<String,Object>();n.put("name",name);n.put("pivot",List.of(p.pivotX,p.pivotY,p.pivotZ));n.put("rotation",List.of(p.pitch,p.yaw,p.roll));n.put("visible",p.visible);var a=(AvengerDancePartAccessor)(Object)p;var cubes=new ArrayList<Object>();for(var c:a.smoothClasses$danceCuboids())cubes.add(List.of(c.minX,c.minY,c.minZ,c.maxX,c.maxY,c.maxZ));n.put("cubes",cubes);var children=new ArrayList<Object>();for(var e:a.smoothClasses$danceChildren().entrySet())children.add(part(e.getKey(),e.getValue()));n.put("children",children);return n;}
    private DanceDiagnostics(){}
}
