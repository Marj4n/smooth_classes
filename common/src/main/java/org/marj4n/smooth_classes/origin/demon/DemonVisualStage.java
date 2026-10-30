package org.marj4n.smooth_classes.origin.demon;
import org.marj4n.smooth_classes.origin.OriginState;
import java.util.function.Predicate;
/** Four visual milestones; combat subtiers do not create additional forms. */
public enum DemonVisualStage {
    DEMON_BASE("appleborn"),WORLD_ENDER("goatborn"),NETHERFORGED("netherforged"),DEMON_LORD("demon_lord");
    private final String asset;
    DemonVisualStage(String asset){this.asset=asset;}
    public String asset(){return asset;}
    public static DemonVisualStage of(OriginState state){return of(state::hasFlag);}
    public static DemonVisualStage of(Predicate<String> has){return has.test("demon.lord")?DEMON_LORD:has.test("demon.netherforged")?NETHERFORGED:has.test("demon.world_ender")?WORLD_ENDER:DEMON_BASE;}
}
