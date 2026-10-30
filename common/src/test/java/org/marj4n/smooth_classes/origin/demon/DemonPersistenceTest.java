package org.marj4n.smooth_classes.origin.demon;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class DemonPersistenceTest {
    @Test void incompleteEvolutionArraysAndNonFiniteBalancingCannotCrashDomainTicks(){
        var c=new DemonConfig();c.domainExposureSeconds=new int[]{1};c.domainFireChanceDivisors=null;c.domainArmor=new double[]{Double.NaN,-100,999,10};c.domainAttack=new double[0];c.curseArmorReduction=Double.POSITIVE_INFINITY;c.validate();
        assertArrayEquals(new int[]{8,6,4,2},c.domainExposureSeconds);assertArrayEquals(new int[]{1024,512,341,256},c.domainFireChanceDivisors);assertArrayEquals(new double[]{4,0,64,10},c.domainArmor);assertArrayEquals(new double[]{3,3,6,6},c.domainAttack);assertEquals(.25,c.curseArmorReduction);
    }
    @Test void pendingDeathBurstsSurviveRestartWithSharedFiniteSpreadBudget(){
        var s=new DemonWorldState();UUID owner=UUID.randomUUID(),token=UUID.randomUUID(),target=UUID.randomUUID();
        var cast=new DemonWorldState.Cast(token,owner,null,2);cast.count=0;cast.pending=1;s.casts.put(token,cast);
        s.pendingBursts.add(new DemonWorldState.Burst(token,target,"minecraft:the_nether",3,-100,64,500));
        var restored=DemonWorldState.read(s.writeNbt(new NbtCompound()));
        assertEquals(1,restored.pendingBursts.size());assertEquals(1,restored.casts.get(token).pending);assertEquals(2,restored.casts.get(token).budget);
        assertEquals(target,restored.pendingBursts.peek().target());assertEquals(3,restored.pendingBursts.peek().tier());assertFalse(restored.mainCasts.containsKey(owner));
    }
    @Test void retaliationTracksSeveralPlayersWithoutOverwritingEarlierAggressor(){
        var d=new DemonEntityData();UUID a=UUID.randomUUID(),b=UUID.randomUUID();d.provoked.add(a);d.provoked.add(b);
        var restored=new DemonEntityData();restored.read(d.write());assertEquals(Set.of(a,b),restored.provoked);
    }
    @Test void throneBindingCurseTokensAndBirthLedgerSurviveRestart(){
        var s=new DemonWorldState();UUID owner=UUID.randomUUID(),root=UUID.randomUUID(),token=UUID.randomUUID();
        s.born.add(owner);s.thrones.put(owner,new DemonWorldState.Throne(owner,new BlockPos(-29999999,32,29999990),3));
        var c=new DemonWorldState.Cast(token,owner,root,7);c.count=3;s.casts.put(token,c);s.mainCasts.put(owner,token);
        s.naturalChests.put(new BlockPos(-55,64,1234).asLong(),"bastion");
        var restored=DemonWorldState.read(s.writeNbt(new NbtCompound()));
        assertTrue(restored.born.contains(owner));assertEquals(s.thrones.get(owner),restored.thrones.get(owner));
        assertEquals(token,restored.mainCasts.get(owner));assertEquals(7,restored.casts.get(token).budget);assertEquals(3,restored.casts.get(token).count);assertEquals(s.naturalChests,restored.naturalChests);
    }
    @Test void recalledCurseCannotReappearAfterUnloadedTargetReloads(){
        var s=new DemonWorldState();UUID owner=UUID.randomUUID(),token=UUID.randomUUID();var c=new DemonWorldState.Cast(token,owner,UUID.randomUUID(),5);
        c.cancelled=true;s.casts.put(token,c);s.mainCasts.put(owner,token);
        var restored=DemonWorldState.read(s.writeNbt(new NbtCompound()));assertTrue(restored.casts.get(token).cancelled);assertFalse(restored.mainCasts.containsKey(owner));
    }
    @Test void entityOwnershipOrdersProvocationAndCurseDoNotDependOnCurrentDimension(){
        var d=new DemonEntityData();d.commander=UUID.randomUUID();d.attackTarget=UUID.randomUUID();d.provokedBy=UUID.randomUUID();d.curseOwner=UUID.randomUUID();d.command=3;d.guard=new BlockPos(-22,-20,77);d.curseTier=3;d.dotTicks=23;d.propagated=true;d.birthRecorded=true;d.nativeBirth=true;
        var restored=new DemonEntityData();restored.read(d.write());
        assertEquals(d.commander,restored.commander);assertEquals(d.attackTarget,restored.attackTarget);assertEquals(d.provokedBy,restored.provokedBy);assertEquals(d.curseOwner,restored.curseOwner);assertEquals(d.guard,restored.guard);assertEquals(3,restored.curseTier);assertEquals(23,restored.dotTicks);assertTrue(restored.propagated);
        assertTrue(restored.birthRecorded&&restored.nativeBirth);
    }
    @Test void corruptEntityCountersCannotCreateInvalidCommandIndexes(){var n=new NbtCompound();n.putInt("Command",999);n.putInt("CurseTier",-10);n.putInt("DotTicks",-1);var d=new DemonEntityData();d.read(n);assertEquals(5,d.command);assertEquals(0,d.curseTier);assertEquals(0,d.dotTicks);}
    @Test void provisionalConfigRejectsNonFiniteMultipliersAndUnboundedPerTickWork(){var c=new DemonConfig();c.exhaustionMultiplier=Double.NaN;c.domainScanBudget=Integer.MAX_VALUE;c.armyUpdateBudget=-1;c.propagationBudget=Integer.MAX_VALUE;c.validate();assertEquals(.5,c.exhaustionMultiplier);assertEquals(32768,c.domainScanBudget);assertEquals(1,c.armyUpdateBudget);assertEquals(128,c.propagationBudget);}
}
