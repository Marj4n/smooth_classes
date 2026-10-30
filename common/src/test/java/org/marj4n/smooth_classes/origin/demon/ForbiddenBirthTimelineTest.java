package org.marj4n.smooth_classes.origin.demon;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ForbiddenBirthTimelineTest {
    @Test void appleFallsContinuouslyBeforePlayerReveal(){assertEquals(0,ForbiddenBirthTimeline.fall(95));assertEquals(.25,ForbiddenBirthTimeline.fall(125),1e-9);assertEquals(1,ForbiddenBirthTimeline.fall(155));assertTrue(ForbiddenBirthTimeline.hidden(154));assertTrue(ForbiddenBirthTimeline.hidden(184));}
    @Test void revealedPlayerGrowsAtSameGroundOrigin(){assertEquals(0,ForbiddenBirthTimeline.emergence(185));assertTrue(ForbiddenBirthTimeline.emergence(200)>.49);assertEquals(1,ForbiddenBirthTimeline.emergence(215));assertFalse(ForbiddenBirthTimeline.hidden(185));}
    @Test void outOfRangeFramesNeverOvershoot(){for(double t:new double[]{-100,0,1000,Double.MAX_VALUE}){double fall=ForbiddenBirthTimeline.fall(t),scale=ForbiddenBirthTimeline.emergence(t);assertTrue(fall>=0&&fall<=1);assertTrue(scale>=0&&scale<=1);}}
    @Test void timeoutExceedsAllCinematicPhases(){assertTrue(ForbiddenBirthTimeline.IMPACT<ForbiddenBirthTimeline.REVEAL);assertTrue(ForbiddenBirthTimeline.FIRST_BREATH<ForbiddenBirthTimeline.END);assertTrue(ForbiddenBirthTimeline.END<ForbiddenBirthTimeline.TIMEOUT);}
}
