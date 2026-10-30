package org.marj4n.smooth_classes.client.avenger.dance;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.marj4n.smooth_classes.content.avenger.dance.DanceProfiles;

class CatDrownedCandidateTest {
    @Test void catHasItsOwnVanillaOnlySignature(){assertEquals("cat",DanceProfiles.profile("minecraft:cat"));assertNull(DanceProfiles.profile("other:cat"));assertEquals("quadruped",DanceProfiles.profile("minecraft:ocelot"));assertFalse(DanceClip.load("cat").generic);}
    @Test void catTargetsFourActualLegsAndBothTailSegments(){var clip=DanceClip.load("cat");for(String name:List.of("head_0","body_1","body_2","body_3","body_4","body_5","body_6"))assertTrue(clip.tracks.stream().anyMatch(t->t.bone().equals(name)&&t.channel().equals("rotation")),name);assertTrue(clip.tracks.stream().anyMatch(t->t.bone().equals("body_6")&&t.channel().equals("position")),"Sibling tail tip follows rotating base");assertFalse(clip.tracks.stream().anyMatch(t->t.bone().contains("arm")||t.channel().equals("scale")));}
    @Test void landAndWaterHaveExactlyCompatibleTrackOrderAndMusicPhase(){var land=DanceClip.load("drowned");var water=DanceClip.load("drowned_water");assertFalse(water.tracks.isEmpty());assertEquals(land.loopTicks,water.loopTicks);assertEquals(land.tracks.size(),water.tracks.size());for(int i=0;i<land.tracks.size();i++){assertEquals(land.tracks.get(i).bone(),water.tracks.get(i).bone());assertEquals(land.tracks.get(i).channel(),water.tracks.get(i).channel());}for(int tick=0;tick<200;tick++)assertEquals(land.phase(tick,.3f,0),water.phase(tick,.3f,0));}
    @Test void underwaterIsActuallyDifferentAndHasFiniteContinuousEndpoints(){var land=DanceClip.load("drowned");var water=DanceClip.load("drowned_water");boolean different=false;for(int i=0;i<water.tracks.size();i++)for(int axis=0;axis<3;axis++){var a=land.tracks.get(i);var b=water.tracks.get(i);assertEquals(b.sample(0,axis),b.sample(1,axis),.001);for(int k=0;k<=480;k++){float x=a.sample(k/480f,axis),y=b.sample(k/480f,axis);assertTrue(Float.isFinite(y));if(Math.abs(x-y)>.1)different=true;}}assertTrue(different);}
}
