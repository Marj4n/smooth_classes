package org.marj4n.smooth_classes.origin.demon;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class DemonGripGeometryTest {
 @Test void normalMinecraftArmsKeepTheirOriginalGrip(){for(boolean left:new boolean[]{false,true}){float sign=left?1:-1;var p=DemonGripGeometry.offset(new float[]{sign*6,12,-2},new float[]{sign*5,22,0},left);assertEquals(0,p.x());assertEquals(0,p.y());assertEquals(0,p.z());}}
 @Test void longSteveArmsPlaceAllHeldItemsAtWristNotHalfwayDownArm(){for(boolean left:new boolean[]{false,true}){float sign=left?1:-1;var p=DemonGripGeometry.offset(new float[]{sign*7.5f,22,-2.5f},new float[]{sign*6.25f,42,0},left);assertEquals(sign*.25f,p.x());assertEquals(10,p.y());assertEquals(-.5,p.z());}}
 @Test void slimAlexGripUsesItsActualNarrowHandCenter(){var left=DemonGripGeometry.offset(new float[]{6.875f,22,-2.5f},new float[]{6.25f,42,0},true);var right=DemonGripGeometry.offset(new float[]{-6.875f,22,-2.5f},new float[]{-6.25f,42,0},false);assertEquals(-.375,left.x());assertEquals(.375,right.x());assertEquals(10,left.y());assertEquals(10,right.y());}
}
