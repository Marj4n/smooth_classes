package org.marj4n.smooth_classes.gameplay;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.marj4n.smooth_classes.runtime.AbilityCooldowns;
import org.marj4n.smooth_classes.runtime.AscendancyRuntime;
import org.marj4n.smooth_classes.runtime.ExecutionResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Cast surface for all thirteen ascendancy abilities. */
public final class AscendancyAbilityDispatcher {
    private AscendancyAbilityDispatcher() {}

    public record DispatchResult(boolean success,String ability,String message) {
        static DispatchResult fail(String message){return new DispatchResult(false,"none",message);}
        public static DispatchResult failPublic(String message){return fail(message);}
    }

    public static DispatchResult castNamed(ServerPlayerEntity p,String raw){
        if(org.marj4n.smooth_classes.runtime.WhenOnHighRuntime.active(p))return DispatchResult.fail("When On High is still channeling.");
        String ability=raw.toLowerCase(Locale.ROOT);
        if(!AscendancyRuntime.unlocked(p,ability))return DispatchResult.fail(ability+" is not unlocked in Puffish Ascendancy.");
        Identifier id=new Identifier("smooth_classes","ascendancy_"+ability);
        long remaining=AbilityCooldowns.remainingTicks(p,id);
        if(remaining>0)return DispatchResult.fail(ability+" cooldown "+String.format(Locale.ROOT,"%.1f",remaining/20D)+"s");
        ExecutionResult r=AscendancyRuntime.cast(p,ability);
        if(!r.success())return DispatchResult.fail(r.detail());
        if(!"magic_circle".equals(ability)&&!"agony".equals(ability)) AbilityCooldowns.start(p,id,"torment".equals(ability) ? 800 : effectiveCooldownTicks(p,ability));
        return new DispatchResult(true,ability,r.detail());
    }

    /** HUD slot selection: the unlocked ascendancy ability appears on the R slot. */
    public static String selectedAbility(ServerPlayerEntity p){
        List<String> values=unlocked(p);
        return values.isEmpty()?"":values.get(0);
    }

    public static List<String> unlocked(ServerPlayerEntity p){
        List<String> out=new ArrayList<>();
        for(String a:AscendancyRuntime.ABILITIES)if(AscendancyRuntime.unlocked(p,a))out.add(a);
        return out;
    }

    public static int effectiveCooldownTicks(ServerPlayerEntity p,String ability){
        int base=cooldownTicks(ability);
        if("torment".equals(ability)||"magic_circle".equals(ability)||"agony".equals(ability))return base;
        return Math.max((int)Math.ceil(base*.65),AbilityCooldowns.adjustedTicks(p,base));
    }
    public static int cooldownTicks(String ability){
        return org.marj4n.smooth_classes.runtime.AscendancyBalance.cooldownTicks(ability);
    }
}
