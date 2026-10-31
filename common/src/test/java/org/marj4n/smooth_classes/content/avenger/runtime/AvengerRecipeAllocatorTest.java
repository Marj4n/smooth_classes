package org.marj4n.smooth_classes.content.avenger.runtime;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class AvengerRecipeAllocatorTest {
    private static final List<String> POOL=List.of("minecraft:coal","minecraft:flint","minecraft:clay_ball","minecraft:emerald","minecraft:enchanted_book");
    @Test void duplicateVanillaFishFusionGetsDistinctOffhand(){
        var result=AvengerRecipeAllocator.allocate(List.of(
            new AvengerRecipeAllocator.Request("minecraft:axolotl","minecraft:tropical_fish","minecraft:water_bucket",true),
            new AvengerRecipeAllocator.Request("minecraft:tropical_fish","minecraft:tropical_fish","minecraft:water_bucket",true)),POOL);
        assertEquals("minecraft:water_bucket",result.get(0).off());assertNotEquals(result.get(0).off(),result.get(1).off());
    }
    @Test void uniqueNaturalDropNeedsOnlyMainHand(){
        var result=AvengerRecipeAllocator.allocate(List.of(new AvengerRecipeAllocator.Request("pack:crab","pack:crab_claw",null,false)),POOL);
        assertNull(result.get(0).off());
    }
    @Test void sharedDropsStayUniqueAndPreserveExistingMainOnlyRecipe(){
        var result=AvengerRecipeAllocator.allocate(List.of(
            new AvengerRecipeAllocator.Request("minecraft:zombie","minecraft:rotten_flesh",null,true),
            new AvengerRecipeAllocator.Request("pack:zombie_one","minecraft:rotten_flesh",null,false),
            new AvengerRecipeAllocator.Request("pack:zombie_two","minecraft:rotten_flesh",null,false)),POOL);
        assertNull(result.get(0).off());assertNotNull(result.get(1).off());assertNotEquals(result.get(1).off(),result.get(2).off());
    }
    @Test void librarianBookPairIsReserved(){
        var result=AvengerRecipeAllocator.allocate(List.of(new AvengerRecipeAllocator.Request("pack:trader","minecraft:emerald","minecraft:enchanted_book",true)),POOL);
        assertNotEquals("minecraft:enchanted_book",result.get(0).off());
    }
    @Test void rebuildIsDeterministic(){
        var requests=List.of(new AvengerRecipeAllocator.Request("a:one","minecraft:bone",null,false),new AvengerRecipeAllocator.Request("b:two","minecraft:bone",null,false));
        assertEquals(AvengerRecipeAllocator.allocate(requests,POOL),AvengerRecipeAllocator.allocate(requests,POOL));
    }
    @Test void refusesDuplicateEntitiesAndExhaustedIngredientSpace(){
        var request=new AvengerRecipeAllocator.Request("a:one","minecraft:bone",null,false);
        assertThrows(IllegalArgumentException.class,()->AvengerRecipeAllocator.allocate(List.of(request,request),POOL));
        var a=new AvengerRecipeAllocator.Request("a:one","minecraft:bone",null,false);var b=new AvengerRecipeAllocator.Request("b:two","minecraft:bone",null,false);
        assertThrows(IllegalStateException.class,()->AvengerRecipeAllocator.allocate(List.of(a,b),List.of("minecraft:coal")));
    }
}
