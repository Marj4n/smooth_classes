package org.marj4n.smooth_classes.content.avenger.dance;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class DanceBindingTest {
    @Test void offlineOwnerRoleCannotBeReenabledByDisconnectOrRestart(){var s=new DanceBindings();UUID owner=UUID.randomUUID();s.issue(UUID.randomUUID(),owner,"minecraft:skeleton");assertTrue(s.ownerEligible(owner));s.recordOwner(owner,false);var restored=DanceBindings.read(s.writeNbt(new net.minecraft.nbt.NbtCompound()));assertFalse(restored.ownerEligible(owner));}
    @Test void copiedTagCannotIssueLedgerProof(){var s=new DanceBindings();assertNull(s.get(UUID.randomUUID()));}
    @Test void proofSurvivesSaveReloadWithSeparateOwners(){var s=new DanceBindings();UUID e1=UUID.randomUUID(),e2=UUID.randomUUID(),a=UUID.randomUUID(),b=UUID.randomUUID();s.issue(e1,a,"minecraft:skeleton");s.issue(e2,b,"minecraft:skeleton");var restored=DanceBindings.read(s.writeNbt(new net.minecraft.nbt.NbtCompound()));assertEquals(a,restored.get(e1).owner());assertEquals(b,restored.get(e2).owner());assertEquals("minecraft:skeleton",restored.get(e1).type());}
    @Test void removedDeadBindingCannotAuthorizeReusedIdentity(){var s=new DanceBindings();var id=UUID.randomUUID();s.issue(id,UUID.randomUUID(),"minecraft:zombie");s.forget(id);assertNull(s.get(id));}
    @Test void allTwentyEightSignaturesHaveExplicitAdapters(){assertEquals(28,DanceProfiles.SIGNATURES.size());for(String id:DanceProfiles.SIGNATURES)assertEquals(id,DanceProfiles.profile("minecraft:"+id));}
    @Test void moddedEntitiesNeverReceiveVanillaAdapters(){assertNull(DanceProfiles.profile("smooth_classes:wraith"));assertNull(DanceProfiles.profile("smooth_classes:dreadglare"));assertNull(DanceProfiles.profile("other:skeleton"));}
    @Test void unsafeAndUnknownModelsFailClosed(){assertNull(DanceProfiles.profile("minecraft:parrot"));assertEquals("wither",DanceProfiles.profile("minecraft:wither"));assertEquals("warden",DanceProfiles.profile("minecraft:warden"));assertEquals("shulker",DanceProfiles.profile("minecraft:shulker"));assertNull(DanceProfiles.profile("minecraft:unknown"));}
    @Test void provenanceBudgetKeepsOwnersAndSpeciesSeparate(){var s=new DanceBindings();UUID a=UUID.randomUUID(),b=UUID.randomUUID();s.issue(UUID.randomUUID(),a,"minecraft:skeleton");s.issue(UUID.randomUUID(),b,"minecraft:skeleton");s.issue(UUID.randomUUID(),a,"minecraft:zombie");assertEquals(1,s.count(a,"minecraft:skeleton"));assertEquals(0,s.count(b,"minecraft:zombie"));}
    @Test void fallbackAnatomyIsExplicit(){assertEquals("spider",DanceProfiles.profile("minecraft:cave_spider"));assertEquals("flying",DanceProfiles.profile("minecraft:bat"));assertEquals("aquatic",DanceProfiles.profile("minecraft:cod"));assertEquals("quadruped",DanceProfiles.profile("minecraft:pig"));}
}
