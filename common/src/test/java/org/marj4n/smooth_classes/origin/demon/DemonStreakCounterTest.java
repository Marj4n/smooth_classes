package org.marj4n.smooth_classes.origin.demon;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class DemonStreakCounterTest {
 @Test void firstKillAndFiveHundredKillsHaveExactUncappedBonuses(){var c=new DemonStreakCounter();for(int i=1;i<=500;i++){c.kill(i);assertEquals(i,c.stacks());assertEquals(i,c.healthBonus());assertEquals(i*.5,c.armorBonus());assertEquals(1320,c.remaining(i));}}
 @Test void deadlineRefreshAndExactTimeout(){var c=new DemonStreakCounter();c.kill(0);assertFalse(c.expire(1319));c.kill(1319);assertEquals(2,c.stacks());assertFalse(c.expire(2638));assertTrue(c.expire(2639));assertEquals(0,c.stacks());assertEquals(0,c.remaining(2639));assertEquals(0,c.healthBonus());}
 @Test void expiredKillStartsFreshAndInstancesAreIndependent(){var a=new DemonStreakCounter();var b=new DemonStreakCounter();a.kill(4);b.kill(10);a.kill(1324);assertEquals(1,a.stacks());assertEquals(1,b.stacks());a.clear();assertEquals(1,b.stacks());assertEquals(0,a.armorBonus());}
 @Test void longRunningServerDoesNotUseIntegerTickOverflow(){var c=new DemonStreakCounter();long now=(long)Integer.MAX_VALUE+5000;c.kill(now);assertEquals(1320,c.remaining(now));assertTrue(c.expire(now+1320));}
 @Test void sessionResetClearsAllBonuses(){var c=new DemonStreakCounter();c.kill(5);c.clear();assertEquals(0,c.stacks());assertEquals(0,c.healthBonus());assertEquals(0,c.armorBonus());}
}
