package org.marj4n.smooth_classes.content.avenger.dance;
import org.junit.jupiter.api.Test;
import net.minecraft.entity.EntityPose;
import static org.junit.jupiter.api.Assertions.*;
class FrozenWardenPoseTest {
    @Test void frozenAmbientPosesWaitForFullVanillaClipAndOneSafetyTick(){assertTrue(DancePriority.frozenAmbientBusy(EntityPose.SNIFFING,4209,true));assertFalse(DancePriority.frozenAmbientBusy(EntityPose.SNIFFING,4210,true));assertTrue(DancePriority.frozenAmbientBusy(EntityPose.ROARING,4249,true));assertFalse(DancePriority.frozenAmbientBusy(EntityPose.ROARING,4250,true));}
    @Test void unknownClockOrNonAmbientPosesFailClosed(){assertTrue(DancePriority.frozenAmbientBusy(EntityPose.SNIFFING,-1,true));assertTrue(DancePriority.frozenAmbientBusy(EntityPose.ROARING,9999,false));assertTrue(DancePriority.frozenAmbientBusy(EntityPose.DIGGING,9999,true));assertTrue(DancePriority.frozenAmbientBusy(EntityPose.EMERGING,9999,true));}
}
