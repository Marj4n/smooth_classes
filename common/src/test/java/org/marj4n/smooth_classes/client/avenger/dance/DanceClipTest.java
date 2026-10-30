package org.marj4n.smooth_classes.client.avenger.dance;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class DanceClipTest {
    @Test void linearTrackInterpolatesExactEndpoints(){var t=new DanceClip.Track("head","rotation",false,new float[][]{{0,0,0,0},{.5f,10,20,30},{1,0,0,0}});assertEquals(5,t.sample(.25f,0));assertEquals(20,t.sample(.5f,1));assertEquals(0,t.sample(1,2));}
    @Test void roboticStepsDoNotBecomeRandomSmoothMotion(){var t=new DanceClip.Track("head","rotation",true,new float[][]{{0,0,0,0},{.5f,20,0,0},{1,0,0,0}});assertEquals(0,t.sample(.25f,0));assertEquals(20,t.sample(.75f,0));}
}
