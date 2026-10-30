package org.marj4n.smooth_classes.client.avenger.dance;
import com.google.gson.*;
import java.util.*;
import java.io.*;

/** Authored vanilla-pivot tracks. Smoothstep transitions never overshoot held key poses. */
public final class DanceClip {
    public record Track(String bone,String channel,boolean step,boolean smooth,float[][] keys) {
        public Track(String bone,String channel,boolean step,float[][] keys){this(bone,channel,step,false,keys);}
        public float sample(float phase,int axis){
            if(keys.length==0)return 0;if(keys.length==1||phase<=keys[0][0])return keys[0][axis+1];if(phase>=keys[keys.length-1][0])return keys[keys.length-1][axis+1];
            int low=0,high=keys.length-1;while(high-low>1){int mid=(low+high)>>>1;if(phase<keys[mid][0])high=mid;else low=mid;}float[] a=keys[low],b=keys[high];float t=step?0:Math.max(0,Math.min(1,(phase-a[0])/(b[0]-a[0])));if(smooth)t=t*t*(3-2*t);return a[axis+1]+(b[axis+1]-a[axis+1])*t;
        }
    }
    public final List<Track> tracks;
    public final float loopTicks,tempoMultiplier,amplitude,offset,transitionTicks;
    public final boolean generic;
    private DanceClip(List<Track> tracks){this(tracks,40,1,1,0,6);}
    private DanceClip(List<Track> tracks,float loop,float tempo,float amplitude,float offset,float transition){this(tracks,loop,tempo,amplitude,offset,transition,false);}
    private DanceClip(List<Track> tracks,float loop,float tempo,float amplitude,float offset,float transition,boolean generic){this.tracks=List.copyOf(tracks);this.loopTicks=loop;this.tempoMultiplier=tempo;this.amplitude=amplitude;this.offset=offset;this.transitionTicks=transition;this.generic=generic;}
    private static float value(JsonObject j,String name,float fallback,float min,float max)throws IOException{float f=j.has(name)?j.get(name).getAsFloat():fallback;if(!Float.isFinite(f)||f<min||f>max)throw new IOException("Invalid style field "+name);return f;}
    public float phase(long now,float delta,long start){double tick=(now-start+delta)*tempoMultiplier+offset;return (float)(tick/loopTicks-Math.floor(tick/loopTicks));}
    public static DanceClip load(String profile){
        String path="/assets/smooth_classes/avenger_dance/"+profile+".json";
        try(var in=DanceClip.class.getResourceAsStream(path)){
            if(in==null)return new DanceClip(List.of());var j=JsonParser.parseReader(new InputStreamReader(in,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();var tracks=new ArrayList<Track>();
            for(var item:j.getAsJsonArray("tracks")){var t=item.getAsJsonObject();String bone=t.get("bone").getAsString(),channel=t.get("channel").getAsString();if(!Set.of("rotation","position","scale").contains(channel)||channel.equals("scale")&&!bone.equals("__root"))throw new IOException("Unsafe channel");if(bone.equals("__cat_right_elbow")&&(!profile.equals("cat")||!channel.equals("rotation")))throw new IOException("Cat-only cosmetic joint");var list=new ArrayList<float[]>();float last=-1;for(var k:t.getAsJsonArray("keys")){var a=k.getAsJsonArray();float[] key={a.get(0).getAsFloat(),a.get(1).getAsFloat(),a.get(2).getAsFloat(),a.get(3).getAsFloat()};for(float v:key)if(!Float.isFinite(v))throw new IOException("Nonfinite key");if(key[0]<=last||key[0]<0||key[0]>1)throw new IOException("Invalid timing");if(channel.equals("scale"))for(int axis=1;axis<=3;axis++)if(key[axis]<.65f||key[axis]>1.4f)throw new IOException("Unsafe scale");last=key[0];list.add(key);}if(list.size()<2||list.get(0)[0]!=0||last!=1)throw new IOException("Missing loop endpoints");for(int axis=1;axis<=3;axis++)if(Math.abs(list.get(0)[axis]-list.get(list.size()-1)[axis])>.001f)throw new IOException("Discontinuous loop");tracks.add(new Track(bone,channel,t.has("step")&&t.get("step").getAsBoolean(),t.has("smooth")&&t.get("smooth").getAsBoolean(),list.toArray(float[][]::new)));}
            return new DanceClip(tracks,value(j,"loop_ticks",40,20,640),value(j,"tempo_multiplier",1,.25f,4),value(j,"amplitude",1,0,2),value(j,"offset_ticks",0,0,640),value(j,"transition_ticks",6,0,20),Set.of("quadruped","ground","flying","aquatic").contains(profile));
        }catch(Exception e){org.marj4n.smooth_classes.SmoothClasses.LOGGER.error("Invalid cosmetic dance clip {}: {}",profile,e.toString());return new DanceClip(List.of());}
    }
}
