package org.marj4n.smooth_classes.gameplay;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.marj4n.smooth_classes.runtime.AbilityCooldowns;
import org.marj4n.smooth_classes.runtime.AscendancyRuntime;
import org.marj4n.smooth_classes.runtime.ExecutionResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Cast surface for all thirteen Continued ascendancy abilities. */
public final class AscendancyAbilityDispatcher {
    private AscendancyAbilityDispatcher() {}

    public record DispatchResult(boolean success,String ability,String message) {
        static DispatchResult fail(String message){return new DispatchResult(false,"none",message);}
        public static DispatchResult failPublic(String message){return fail(message);}
    }

    public static DispatchResult castNamed(ServerPlayerEntity p,String raw){
        String ability=raw.toLowerCase(Locale.ROOT);
        if(!AscendancyRuntime.unlocked(p,ability))return DispatchResult.fail(ability+" is not unlocked in Puffish Ascendancy.");
        Identifier id=new Identifier("smooth_classes","ascendancy_"+ability);
        long remaining=AbilityCooldowns.remainingTicks(p,id);
        if(remaining>0)return DispatchResult.fail(ability+" cooldown "+String.format(Locale.ROOT,"%.1f",remaining/20D)+"s");
        ExecutionResult r=AscendancyRuntime.cast(p,ability);
        if(!r.success())return DispatchResult.fail(r.detail());
        AbilityCooldowns.start(p,id,AbilityCooldowns.adjustedTicks(p,cooldownTicks(ability)));
        return new DispatchResult(true,ability,r.detail());
    }

    /** Continued HUD slot selection: the unlocked ascendancy ability appears on the R slot. */
    public static String selectedAbility(ServerPlayerEntity p){
        List<String> values=unlocked(p);
        return values.isEmpty()?"":values.get(0);
    }

    public static List<String> unlocked(ServerPlayerEntity p){
        List<String> out=new ArrayList<>();
        for(String a:AscendancyRuntime.ABILITIES)if(AscendancyRuntime.unlocked(p,a))out.add(a);
        return out;
    }

    public static int cooldownTicks(String ability){
        return switch(ability){
            case "righteous_hammers" -> 60*20;
            case "bone_armor" -> 70*20;
            case "cyclonic_cleave" -> 15*20;
            case "magic_circle" -> 40*20;
            case "arcane_slash" -> 12*20;
            case "agony","torment" -> 40*20;
            case "rapidfire" -> 30*20;
            case "cataclysm" -> 60*20;
            case "ghostwalk" -> 30*20;
            case "skyward_sunder" -> 16*20;
            case "righteous_shield" -> 6*20;
            case "chainbreaker" -> 45*20;
            default -> 25*20;
        };
    }
}
