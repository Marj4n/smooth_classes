package org.marj4n.smooth_classes.origin.demon;
import java.util.UUID;
import net.minecraft.nbt.NbtCompound;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ForbiddenTreeRecordTest {
    @Test void simultaneousRequestsBecomeOneServerThreadJob(){var r=new ForbiddenTreeRecord();UUID a=UUID.randomUUID(),b=UUID.randomUUID();assertTrue(r.request(a));assertFalse(r.request(b));assertEquals(a,r.requestedBy);}
    @Test void reservationCannotMoveWhenAnotherPlayerRequests(){var r=new ForbiddenTreeRecord();r.request(UUID.randomUUID());r.phase=ForbiddenTreeRecord.Phase.VALIDATING;assertTrue(r.reserve(128,31,256,"original"));assertFalse(r.reserve(512,31,768,"replacement"));assertFalse(r.request(UUID.randomUUID()));assertEquals(128,r.x);assertEquals("original",r.hash);}
    @Test void partialCrashResumesSameAnchorFromBeginningIdempotently(){var r=new ForbiddenTreeRecord();r.phase=ForbiddenTreeRecord.Phase.VALIDATING;r.reserve(64,31,96,"source");r.cursor=512;var restored=ForbiddenTreeRecord.read(r.write());assertTrue(restored.anchored);assertEquals(ForbiddenTreeRecord.Phase.PLACING,restored.phase);assertEquals(0,restored.cursor);assertEquals(64,restored.x);assertEquals(96,restored.z);assertEquals("source",restored.hash);}
    @Test void repairNeverCountsAsSecondGeneratedTree(){var r=new ForbiddenTreeRecord();r.phase=ForbiddenTreeRecord.Phase.VALIDATING;r.reserve(-32,31,160,"source");r.complete();assertEquals(1,r.placements);r.fail("destroyed");assertTrue(r.repair());r.complete();assertEquals(1,r.placements);assertEquals(-32,r.x);assertTrue(r.generated);}
    @Test void legacyDataDoesNotInventGeneration(){var r=ForbiddenTreeRecord.read(new NbtCompound());assertEquals(ForbiddenTreeRecord.Phase.UNREQUESTED,r.phase);assertFalse(r.generated);assertFalse(r.anchored);}
    @Test void completedTreeSurvivesSaveReloadWithoutSecondSearch(){var r=new ForbiddenTreeRecord();r.phase=ForbiddenTreeRecord.Phase.VALIDATING;r.reserve(16,32,-16,"source");r.complete();var restored=ForbiddenTreeRecord.read(r.write());assertEquals(ForbiddenTreeRecord.Phase.READY,restored.phase);assertTrue(restored.generated);assertFalse(restored.request(UUID.randomUUID()));assertEquals(1,restored.placements);}
}
