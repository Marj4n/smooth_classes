package org.marj4n.smooth_classes.content.avenger.dance;

import java.util.*;

/** Explicit anatomy allow-list. Unknown/modded models fail closed. */
public final class DanceProfiles {
    public static final List<String> SIGNATURES=List.of("warden","wither_skeleton","enderman","spider","wither","shulker","villager","skeleton","zombie","creeper","drowned","zombified_piglin","witch","piglin","piglin_brute","pillager","vindicator","evoker","iron_golem","blaze","ghast","allay","slime","horse","chicken","cow","ravager","cat");
    private static final Map<String,String> PROFILES=new HashMap<>();
    static {

        add("villager","evoker","illusioner","pillager","vindicator");
        add("skeleton","stray");add("zombie","husk","giant","zombie_villager");
        add("zombified_piglin","piglin","piglin_brute");add("spider","cave_spider");add("villager","wandering_trader");
        add("ground","frog","iron_golem","snow_golem","endermite","silverfish","slime","magma_cube");
        add("quadruped","cow","mooshroom","pig","sheep","goat","polar_bear","panda","turtle","hoglin","zoglin","camel","cat","chicken","donkey","fox","horse","llama","mule","ocelot","rabbit","sniffer","trader_llama","wolf","skeleton_horse","zombie_horse","ravager","strider");
        add("flying","allay","bat","blaze","ghast","phantom","vex","bee");
        add("aquatic","cod","salmon","tropical_fish","pufferfish","tadpole","dolphin","squid","glow_squid","axolotl","guardian","elder_guardian");
        for(String s:SIGNATURES)PROFILES.put(s,s);
    }
    private static void add(String profile,String... ids){for(String id:ids)PROFILES.put(id,profile);}
    public static String profile(String id){return id.startsWith("minecraft:")?PROFILES.get(id.substring(10)):null;}
    public static Map<String,String> all(){return Collections.unmodifiableMap(PROFILES);}
    private DanceProfiles(){}
}
