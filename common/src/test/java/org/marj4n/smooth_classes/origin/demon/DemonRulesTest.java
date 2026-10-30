package org.marj4n.smooth_classes.origin.demon;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class DemonRulesTest {
    @Test void oldDimensionUnloadCannotEraseReplacementEntityWithTheSameUuid(){
        var q=new DemonWorkQueue<UUID,Object>();UUID id=UUID.randomUUID();Object oldEntity=new Object(),arrived=new Object();q.put(id,oldEntity);q.put(id,arrived);q.remove(id,oldEntity);assertEquals(1,q.size());assertSame(arrived,q.next());q.remove(id,arrived);assertEquals(0,q.size());
    }
    @Test void cinderProtects666SecondsThenRecovers666SecondsWithoutAnOffByOneGap(){
        var cycle=new DemonRules.Cycle(13320,0);
        for(int i=1;i<=13320;i++){
            cycle=DemonRules.advance(cycle.active(),cycle.cooldown());
            assertEquals(13320-i,cycle.active());
            assertEquals(i==13320?13320:0,cycle.cooldown());
        }
        for(int i=1;i<=13320;i++){cycle=DemonRules.advance(cycle.active(),cycle.cooldown());assertEquals(13320-i,cycle.cooldown());assertEquals(0,cycle.active());}
    }
    @ParameterizedTest @CsvSource({"0,0","2999,0","3000,1","4199,1","4200,2","5999,2","6000,3","6001,3"})
    void decayUsesDimensionTimeRatherThanWeather(int ticks,int stage){assertEquals(stage,DemonRules.decayStage(ticks));}
    @Test void oneTickPortalDipCannotResetDecay(){assertEquals(5990,DemonRules.decay(6000,true,false));assertEquals(5991,DemonRules.decay(5990,false,false));}
    @Test void maximumDecayRecoversOverThirtySecondsInNether(){int decay=6000;for(int i=0;i<599;i++)decay=DemonRules.decay(decay,true,false);assertEquals(10,decay);assertEquals(0,DemonRules.decay(decay,true,false));}
    @Test void lordHasNoDecayInAnyDimension(){assertEquals(0,DemonRules.decay(6000,false,true));assertEquals(0,DemonRules.decay(6000,true,true));}
    @Test void exposureCannotOverflowAfterDaysOutsideNether(){assertEquals(6000,DemonRules.decay(Integer.MAX_VALUE,false,false));}
    @Test void exactThroneAndEveryRotationAreBijectiveAndKeepOneVisibleCore(){
        for(int rotation=0;rotation<4;rotation++){
            int netherite=0,beacon=0;Set<String> cells=new HashSet<>();
            for(int y=0;y<4;y++)for(int z=0;z<3;z++)for(int x=0;x<3;x++){
                char c=DemonRules.throne(x,y,z);if(c=='N')netherite++;if(c=='B'){beacon++;assertEquals(1,x);assertEquals(2,y);assertEquals(2,z);}
                int[] point=DemonRules.rotate(x,z,rotation);assertTrue(point[0]>=0&&point[0]<3&&point[1]>=0&&point[1]<3);
                assertTrue(cells.add(point[0]+":"+y+":"+point[1]));
                int[] back=DemonRules.rotate(point[0],point[1],4-rotation);assertArrayEquals(new int[]{x,z},back);
                assertEquals(c,DemonRules.throne(2-x,y,z));
            }
            assertEquals(18,netherite);assertEquals(1,beacon);assertEquals(36,cells.size());
        }
    }
    @Test void throneSeatAndStandingBodyAreClearAndArmrestsRemainSolid(){
        for(int y=1;y<=2;y++){assertEquals('.',DemonRules.throne(1,y,1));assertEquals('.',DemonRules.throne(1,y,0));}
        assertEquals('N',DemonRules.throne(1,0,1));assertEquals('N',DemonRules.throne(0,1,1));assertEquals('N',DemonRules.throne(2,1,1));
        assertThrows(IllegalArgumentException.class,()->DemonRules.throne(1,4,1));
    }
    @Test void domainIsAnExactSphereAndDoesNotIncludeCubeCorners(){
        assertTrue(DemonRules.inSphere(64,0,0,64));assertTrue(DemonRules.inSphere(0,-64,0,64));
        assertFalse(DemonRules.inSphere(64.001,0,0,64));assertFalse(DemonRules.inSphere(64,64,0,64));assertFalse(DemonRules.inSphere(64,64,64,64));
    }
    @Test void armyQueueHasNoSizeCapAndDoesNotStarveTheTailWhenNewEntitiesLoad(){
        var queue=new DemonWorkQueue<Integer,Integer>();for(int i=0;i<10000;i++)queue.put(i,i);
        Set<Integer> serviced=new HashSet<>();
        for(int tick=0;tick<400;tick++){queue.put(10000+tick,10000+tick);for(int work=0;work<48;work++)serviced.add(queue.next());}
        assertEquals(10400,queue.size());for(int i=0;i<10000;i++)assertTrue(serviced.contains(i),"Starved entity "+i);
    }
    @Test void removalReloadAndRekeyDoNotDuplicateArmyJobs(){
        var q=new DemonWorkQueue<String,Integer>();q.put("a",1);q.put("b",2);q.remove("a");q.put("a",3);assertEquals(2,q.size());
        assertEquals(3,q.next());assertEquals(2,q.next());assertEquals(3,q.next());q.remove("a");q.remove("b");assertNull(q.next());q.put("c",4);assertEquals(4,q.next());
    }
    @Test void domainProcessingStartsAtCasterAndVisitsEverySphericalPositionOnce(){
        assertEquals(0,DemonDomainCursor.x(0));assertEquals(0,DemonDomainCursor.y(0));assertEquals(0,DemonDomainCursor.z(0));
        BitSet coordinates=new BitSet(129*129*129);int previous=0;
        for(int i=0;i<DemonDomainCursor.size();i++){
            int x=DemonDomainCursor.x(i),y=DemonDomainCursor.y(i),z=DemonDomainCursor.z(i);int distance=x*x+y*y+z*z;
            assertTrue(distance>=previous&&distance<=4096);previous=distance;
            int key=(x+64)*129*129+(y+64)*129+z+64;assertFalse(coordinates.get(key));coordinates.set(key);
        }
        assertEquals(DemonDomainCursor.size(),coordinates.cardinality());assertTrue(coordinates.get(128*129*129+64*129+64));
        assertFalse(coordinates.get(128*129*129+128*129+128));
    }
}
