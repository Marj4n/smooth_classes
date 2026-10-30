package org.marj4n.smooth_classes.origin.demon;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import org.marj4n.smooth_classes.SmoothClasses;

public final class DemonConfig {
    public static DemonConfig INSTANCE = new DemonConfig();
    public double exhaustionMultiplier=.5, fungusSaturation=1.2, plantSaturation=.6;
    public double curseSpeedReduction=.30,curseArmorReduction=.25,curseOutgoingReduction=.20;
    public int[] domainExposureSeconds={8,6,4,2},domainFireChanceDivisors={1024,512,341,256};
    public double[] domainArmor={4,6,8,10},domainAttack={3,3,6,6};
    public int curseDotInterval=40, propagationBudget=8, propagationRadius=6;
    public int curseBurstWorkBudget=64;
    public float curseDotDamage=1, lordDotDamage=2, explosionDamage=4, lordExplosionDamage=7;
    public int domainScanBudget=8192, domainBlockBudget=2048, armyUpdateBudget=48, armyTickInterval=10;
    public int teleportProtectionTicks=60, domainFireInterval=40;
    public boolean multiplayerTerrainChanges=false, domainFire=true;
    public List<String> nativeEntities = new ArrayList<>(), excludedEntities = new ArrayList<>();
    // Verified against local original JAR translations and registry bytecode; never load their classes.
    public Map<String,String> bossEntities = new LinkedHashMap<>(Map.of(
            "nether_gauntlet","bosses_of_mass_destruction:gauntlet", "decaying_king","soulsweapons:accursed_lord_boss",
            "azazel","netherman:azazel", "ignis","cataclysm:ignis", "netherite_monstrosity","cataclysm:netherite_monstrosity"));
    public void validate() {
        exhaustionMultiplier=finite(exhaustionMultiplier,.5,0,1);
        curseSpeedReduction=finite(curseSpeedReduction,.30,0,.95);curseArmorReduction=finite(curseArmorReduction,.25,0,.95);curseOutgoingReduction=finite(curseOutgoingReduction,.20,0,.95);
        domainExposureSeconds=stages(domainExposureSeconds,new int[]{8,6,4,2},1,1200);domainFireChanceDivisors=stages(domainFireChanceDivisors,new int[]{1024,512,341,256},64,65536);
        domainArmor=stages(domainArmor,new double[]{4,6,8,10},0,64);domainAttack=stages(domainAttack,new double[]{3,3,6,6},0,60);
        fungusSaturation=finite(fungusSaturation,1.2,0,20); plantSaturation=finite(plantSaturation,.6,0,20);
        curseDotInterval=clamp(curseDotInterval,10,1200); propagationBudget=clamp(propagationBudget,1,128);
        curseBurstWorkBudget=clamp(curseBurstWorkBudget,1,1024);
        propagationRadius=clamp(propagationRadius,1,16);domainScanBudget=clamp(domainScanBudget,128,32768);
        domainBlockBudget=clamp(domainBlockBudget,1,4096);armyUpdateBudget=clamp(armyUpdateBudget,1,512);
        armyTickInterval=clamp(armyTickInterval,5,100);teleportProtectionTicks=clamp(teleportProtectionTicks,1,100);
        domainFireInterval=clamp(domainFireInterval,20,1200);
        curseDotDamage=(float)finite(curseDotDamage,1,0,20);lordDotDamage=(float)finite(lordDotDamage,2,0,40);
        explosionDamage=(float)finite(explosionDamage,4,0,40);lordExplosionDamage=(float)finite(lordExplosionDamage,7,0,60);
        if(nativeEntities==null)nativeEntities=new ArrayList<>();if(excludedEntities==null)excludedEntities=new ArrayList<>();
        if(bossEntities==null)bossEntities=new DemonConfig().bossEntities;
    }
    private static int clamp(int n,int a,int b){return Math.max(a,Math.min(b,n));}
    private static int[] stages(int[] values,int[] fallback,int min,int max){if(values==null||values.length!=4)return fallback;for(int i=0;i<4;i++)values[i]=clamp(values[i],min,max);return values;}
    private static double[] stages(double[] values,double[] fallback,double min,double max){if(values==null||values.length!=4)return fallback;for(int i=0;i<4;i++)values[i]=finite(values[i],fallback[i],min,max);return values;}
    private static double finite(double n,double fallback,double a,double b){return Double.isFinite(n)?Math.max(a,Math.min(b,n)):fallback;}
    public static void load() {
        Path p=org.marj4n.smooth_classes.platform.PlatformLoader.getInstance().getConfigDir().resolve("smooth_classes-demon.json");
        Gson gson=new GsonBuilder().setPrettyPrinting().create();
        try {
            if(Files.exists(p)) {
                DemonConfig read=gson.fromJson(Files.readString(p),DemonConfig.class);
                if(read==null)throw new IOException("Empty Demon configuration");INSTANCE=read;
            }
            INSTANCE.validate();
            if(!Files.exists(p))Files.writeString(p,gson.toJson(INSTANCE));
        } catch(IOException|com.google.gson.JsonParseException ex) {
            throw new IllegalStateException("Cannot load "+p+"; fix the Demon config before starting the world",ex);
        }
        SmoothClasses.LOGGER.info("Demon balancing loaded; multiplayer terrain changes={}",INSTANCE.multiplayerTerrainChanges);
    }
}
