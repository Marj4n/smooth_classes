package org.marj4n.smooth_classes.content.avenger.runtime;

import java.util.*;

/** Deterministic ingredient allocation. A pair always identifies exactly one soul. */
public final class AvengerRecipeAllocator {
    private AvengerRecipeAllocator() {}
    public record Request(String entity, String main, String off, boolean preserve) {}
    public record Entry(String entity, String main, String off) {}
    public static List<Entry> allocate(List<Request> requests, List<String> discriminators) {
        Map<String,Long> mainCounts=new HashMap<>();
        for(var r:requests) mainCounts.merge(r.main(),1L,Long::sum);
        Set<String> pairs=new HashSet<>(),entities=new HashSet<>();
        List<Entry> result=new ArrayList<>();
        for(var r:requests){
            if(!entities.add(r.entity()))throw new IllegalArgumentException("Duplicate entity: "+r.entity());
            String off=r.off();
            if(!r.preserve()&&mainCounts.get(r.main())==1)off=null;
            else if(!r.preserve()&&off==null)off=pick(r,pairs,discriminators);
            if(pairs.contains(key(r.main(),off))||reserved(r.main(),off))off=pick(r,pairs,discriminators);
            if(!pairs.add(key(r.main(),off)))throw new IllegalStateException("Ambiguous ingredients");
            result.add(new Entry(r.entity(),r.main(),off));
        }
        return List.copyOf(result);
    }
    private static String pick(Request r,Set<String> pairs,List<String> pool){
        if(pool.isEmpty())throw new IllegalArgumentException("No discriminator items");
        int start=Math.floorMod(r.entity().hashCode(),pool.size());
        for(int i=0;i<pool.size();i++){
            String off=pool.get((start+i)%pool.size());
            if(!reserved(r.main(),off)&&!pairs.contains(key(r.main(),off)))return off;
        }
        throw new IllegalStateException("Not enough offhand ingredients for "+r.main());
    }
    private static boolean reserved(String main,String off){return main.equals("minecraft:emerald")&&"minecraft:enchanted_book".equals(off);}
    private static String key(String main,String off){return main+'|'+(off==null?"":off);}
}
