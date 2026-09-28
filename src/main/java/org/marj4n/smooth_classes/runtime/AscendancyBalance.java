package org.marj4n.smooth_classes.runtime;

/** Shared baseline tuning; durations use server ticks (20 ticks = 1 second). */
public final class AscendancyBalance {
    private AscendancyBalance(){}
    public static int points(int points){return Math.max(0,Math.min(40,points));}
    public static int boneCharges(int points){
        if(points>=60)return 12;
        return 4+points(points)/10;
    }
    public static float boneMultiplier(int points){return points>=60?.40F:points>=30?.60F:.70F;}
    public static int rapidfireDuration(int points){return points>=60?200:120+points(points);}
    public static int shieldTier(int charges){return charges>=15?4:charges>=10?3:charges>=5?2:1;}
    public static double multiSchoolScale(double highest,double sum){return sum<=0?0:(highest+.25D*(sum-highest))/sum;}
    public static int cooldownTicks(String ability){return 20*switch(ability){
        case "righteous_hammers" -> 40;
        case "bone_armor" -> 45;
        case "cyclonic_cleave" -> 18;
        case "magic_circle" -> 60;
        case "arcane_slash" -> 12;
        case "agony" -> 30;
        case "torment" -> 40;
        case "rapidfire" -> 24;
        case "cataclysm" -> 50;
        case "ghostwalk" -> 32;
        case "skyward_sunder" -> 20;
        case "righteous_shield" -> 8;
        case "chainbreaker" -> 32;
        default -> 25;
    };}
}
