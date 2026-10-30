package org.marj4n.smooth_classes.origin.demon;
import net.minecraft.entity.EntityPose;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class DemonBodyGeometryTest {
    @Test void lordReallyHasThreeAndHalfBlockStandingBody(){assertEquals(3.5,DemonBodyGeometry.height(EntityPose.STANDING,false),.0001);assertEquals(.8,DemonBodyGeometry.WIDTH,.0001);assertEquals(3.5,56/16.0*.9375*DemonBodyGeometry.RENDER_MULTIPLIER,.0001);}
    @Test void lowAndMountedPosesHaveExplicitBodiesAndEyes(){assertEquals(2.75,DemonBodyGeometry.height(EntityPose.CROUCHING,false),.0001);assertEquals(.9,DemonBodyGeometry.height(EntityPose.SWIMMING,false),.0001);assertEquals(2.2,DemonBodyGeometry.height(EntityPose.STANDING,true),.0001);for(EntityPose p:new EntityPose[]{EntityPose.STANDING,EntityPose.CROUCHING,EntityPose.SWIMMING,EntityPose.FALL_FLYING,EntityPose.DYING})assertTrue(DemonBodyGeometry.eyeHeight(p,false)<DemonBodyGeometry.height(p,false));assertTrue(DemonBodyGeometry.eyeHeight(EntityPose.STANDING,true)<DemonBodyGeometry.THRONE_HEIGHT);}
}
