package org.marj4n.smooth_classes.origin.demon;

import java.util.Arrays;

/** Shared near-first sphere ordering. One primitive array, not one queue per player. */
public final class DemonDomainCursor {
    private static final long[] POSITIONS=build();
    private DemonDomainCursor(){}
    private static long[] build(){
        long[] values=new long[129*129*129];int count=0;
        for(int x=-64;x<=64;x++)for(int y=-64;y<=64;y++)for(int z=-64;z<=64;z++){
            int distance=x*x+y*y+z*z;if(distance>4096)continue;
            values[count++]=((long)distance<<24)|((long)(x+64)<<16)|((long)(y+64)<<8)|(z+64);
        }
        values=Arrays.copyOf(values,count);Arrays.sort(values);return values;
    }
    public static int size(){return POSITIONS.length;}
    public static int x(int index){return (int)((POSITIONS[index]>>>16)&255)-64;}
    public static int y(int index){return (int)((POSITIONS[index]>>>8)&255)-64;}
    public static int z(int index){return (int)(POSITIONS[index]&255)-64;}
}
