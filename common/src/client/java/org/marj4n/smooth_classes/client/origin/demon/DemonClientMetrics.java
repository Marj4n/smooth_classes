package org.marj4n.smooth_classes.client.origin.demon;

import java.util.*;
import java.nio.file.*;
import net.minecraft.client.MinecraftClient;
import org.marj4n.smooth_classes.client.origin.OriginClientState;

/** Test-only real client FPS sampling; never infers client rendering from server TPS. */
public final class DemonClientMetrics {
    private static final List<Integer> FPS=new ArrayList<>();
    private static int tick;private static boolean wasActive;
    private DemonClientMetrics(){}
    public static void register(){
        if(!Boolean.getBoolean("smooth_classes.demonTest"))return;
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(client->{
            boolean active=client.world!=null&&OriginClientState.hasFlag("demon.benchmark_active");
            if(active&&(client.currentScreen instanceof DemonPathScreen||client.currentScreen instanceof org.marj4n.smooth_classes.client.origin.OriginSelectionScreen))client.setScreen(null);
            if(active&&++tick%20==0)FPS.add(client.getCurrentFps());
            if(wasActive&&!active&&!FPS.isEmpty()){
                List<Integer> values=new ArrayList<>(FPS);Collections.sort(values);
                Map<String,Object> report=new LinkedHashMap<>();report.put("samples",values.size());report.put("mean_fps",values.stream().mapToInt(Integer::intValue).average().orElse(0));report.put("minimum_fps",values.get(0));report.put("p05_fps",values.get((int)((values.size()-1)*.05)));report.put("window_width",client.getWindow().getWidth());report.put("window_height",client.getWindow().getHeight());report.put("render_distance",client.options.getViewDistance().getValue());report.put("caveat","Actual isolated client; not the entire Smooth Odyssey modpack. Modal screens/background throttling can affect FPS.");
                try{Files.writeString(client.runDirectory.toPath().resolve("demon-client-fps.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(report));}
                catch(java.io.IOException ex){org.marj4n.smooth_classes.SmoothClasses.LOGGER.error("Cannot save Demon client FPS report",ex);}FPS.clear();
            }
            wasActive=active;
        });
    }
}
