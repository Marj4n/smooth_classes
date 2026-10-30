package org.marj4n.smooth_classes.origin.demon;
import com.google.gson.*;
import java.nio.file.*;

public final class ForbiddenBirthConfig {
    public static ForbiddenBirthConfig INSTANCE=new ForbiddenBirthConfig();
    public boolean enabled=true;
    public int searchAttempts=12,searchRadius=1024,validationBudget=2048,placementBudget=256,chunkLoadsPerTick=1,waitTicks=1200,fallbackAttempts=48;
    public static void load(){var p=org.marj4n.smooth_classes.platform.PlatformLoader.getInstance().getConfigDir().resolve("smooth_classes-forbidden-birth.json");var g=new GsonBuilder().setPrettyPrinting().create();try{if(Files.exists(p)){var read=g.fromJson(Files.readString(p),ForbiddenBirthConfig.class);if(read==null)throw new IllegalArgumentException("Empty birth config");INSTANCE=read;}INSTANCE.validate();if(!Files.exists(p))Files.writeString(p,g.toJson(INSTANCE));}catch(Exception ex){throw new IllegalStateException("Cannot load "+p,ex);}}
    private static int bound(int x,int a,int b){return Math.max(a,Math.min(b,x));}
    public void validate(){searchAttempts=bound(searchAttempts,1,128);searchRadius=bound(searchRadius,128,8192);validationBudget=bound(validationBudget,128,16384);placementBudget=bound(placementBudget,16,2048);chunkLoadsPerTick=bound(chunkLoadsPerTick,1,2);waitTicks=bound(waitTicks,200,3600);fallbackAttempts=bound(fallbackAttempts,1,128);}
}
