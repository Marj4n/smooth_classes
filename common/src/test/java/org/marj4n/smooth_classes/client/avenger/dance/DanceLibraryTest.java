package org.marj4n.smooth_classes.client.avenger.dance;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.marj4n.smooth_classes.content.avenger.dance.DanceProfiles;
class DanceLibraryTest {
    @Test void everyRequestedSignatureHasRealTracksAndMatchingLoopEndpoints(){
        for(String id:DanceProfiles.SIGNATURES){var clip=DanceClip.load(id);assertFalse(clip.tracks.isEmpty(),id);assertFalse(clip.generic,id);for(var t:clip.tracks){assertEquals(0,t.keys()[0][0],id);assertEquals(1,t.keys()[t.keys().length-1][0],id);for(int axis=0;axis<3;axis++)assertEquals(t.sample(0,axis),t.sample(1,axis),.001,id+" "+t.bone());}}
    }
    @Test void noSignatureRegressesToAConstantPlaceholder(){
        for(String id:DanceProfiles.SIGNATURES){var clip=DanceClip.load(id);var poses=new HashSet<String>();for(int sample=0;sample<16;sample++){var pose=new StringBuilder();for(var t:clip.tracks)for(int axis=0;axis<3;axis++)pose.append(Math.round(t.sample(sample/16f,axis)*100)).append(',');poses.add(pose.toString());}assertTrue(poses.size()>=3,id+" needs distinct authored poses; this is a structural gate, not artistic approval");}
    }
    @Test void sharedMusicalIntervalsKeepAllPrimaryStylesAligned(){for(String id:DanceProfiles.SIGNATURES){var clip=DanceClip.load(id);assertEquals(0,clip.phase(1160,0,1000),.00001,id);assertEquals(clip.phase(1017,.5f,1000),clip.phase(2017,.5f,2000),.00001,id);}}
    @Test void subTickInterpolationIsContinuousAtTheLoopBoundary(){for(String id:DanceProfiles.SIGNATURES){var clip=DanceClip.load(id);float a=clip.phase(1000+(long)clip.loopTicks-1,.999f,1000);float b=clip.phase(1000+(long)clip.loopTicks,0,1000);assertTrue(a>.99,id);assertEquals(0,b,.00001,id);}}
    @Test void binarySearchRespectsHeldKeysAndExactEndpoints(){var t=new DanceClip.Track("head","rotation",true,new float[][]{{0,0,0,0},{.2f,30,0,0},{.8f,-20,0,0},{1,0,0,0}});assertEquals(0,t.sample(.199f,0));assertEquals(30,t.sample(.2f,0));assertEquals(-20,t.sample(.9f,0));assertEquals(0,t.sample(1,0));}
    @Test void easedTransitionsDoNotOvershootTheirPoses(){var t=new DanceClip.Track("head","rotation",false,true,new float[][]{{0,-35,0,0},{1,80,0,0}});for(int i=0;i<=100;i++){float v=t.sample(i/100f,0);assertTrue(v>=-35&&v<=80);}assertEquals(-35,t.sample(-1,0));assertEquals(80,t.sample(2,0));}
    @Test void nonBipedSignaturesUseTheirOwnAnatomy(){for(String id:List.of("spider","creeper","shulker","wither","blaze","ghast","slime","horse","chicken","cow","ravager")){var clip=DanceClip.load(id);assertFalse(clip.tracks.stream().anyMatch(t->t.bone().equals("right_arm")||t.bone().equals("left_arm")),id);}}
    @Test void bossAdaptersIncludeEveryRequiredOriginalPart(){var wither=DanceClip.load("wither");for(String name:List.of("center_head","left_head","right_head"))assertTrue(wither.tracks.stream().anyMatch(t->t.bone().equals(name)),name);var shulker=DanceClip.load("shulker");assertTrue(shulker.tracks.stream().anyMatch(t->t.bone().equals("lid")&&t.channel().equals("position")));assertTrue(shulker.tracks.stream().anyMatch(t->t.bone().equals("head")));}
    @Test void genericFallbacksRemainAvailableWithoutForcingBipedJoints(){for(String id:List.of("quadruped","flying","aquatic","ground")){var clip=DanceClip.load(id);assertFalse(clip.tracks.isEmpty(),id);assertTrue(clip.generic,id);assertTrue(clip.tracks.stream().anyMatch(t->t.bone().equals("__root")),id);}}
}
