package org.marj4n.smooth_classes.origin.demon;
import org.junit.jupiter.api.Test;import java.util.Set;import static org.junit.jupiter.api.Assertions.*;
class DemonVisualStageTest {
 @Test void exactlyFourForms(){assertEquals(4,DemonVisualStage.values().length);}
 @Test void realMilestonesSelectForms(){assertEquals(DemonVisualStage.DEMON_BASE,DemonVisualStage.of(Set.<String>of()::contains));assertEquals(DemonVisualStage.WORLD_ENDER,DemonVisualStage.of(Set.of("demon.world_ender")::contains));assertEquals(DemonVisualStage.NETHERFORGED,DemonVisualStage.of(Set.of("demon.world_ender","demon.netherforged")::contains));assertEquals(DemonVisualStage.DEMON_LORD,DemonVisualStage.of(Set.of("demon.world_ender","demon.netherforged","demon.lord")::contains));}
 @Test void combatSubtiersDoNotChangeAnatomy(){assertEquals(DemonVisualStage.WORLD_ENDER,DemonVisualStage.of(Set.of("demon.world_ender","demon.recognition","demon.sovereign","demon.army")::contains));}
}
