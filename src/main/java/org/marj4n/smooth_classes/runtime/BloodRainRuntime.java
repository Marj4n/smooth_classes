package org.marj4n.smooth_classes.runtime;

import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.EntityType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;
import org.marj4n.smooth_classes.SmoothClasses;
import org.marj4n.smooth_classes.entity.BloodRainEntity;
import org.marj4n.smooth_classes.registry.SmoothEntities;
import org.marj4n.smooth_classes.network.SmoothClassesNetworking;

public final class BloodRainRuntime {
    private static final Map<UUID,BloodRainEntity> ACTIVE=new HashMap<>();
    private BloodRainRuntime(){}
    public static boolean active(ServerPlayerEntity p){return ACTIVE.containsKey(p.getUuid());}
    public static void track(BloodRainEntity e){if(e.owner!=null){var old=ACTIVE.putIfAbsent(e.owner,e);if(old!=null&&old!=e)e.discard();}}
    public static void ended(BloodRainEntity e){
        if(e.owner==null||!ACTIVE.remove(e.owner,e))return;
        var p=e.getServer().getPlayerManager().getPlayer(e.owner);
        if(p!=null){AbilityCooldowns.start(p,SmoothClasses.id("ascendancy_magic_circle"),1200);SmoothClassesNetworking.sendAbilityState(p);}
    }
    public static void register(){
        ServerTickEvents.END_SERVER_TICK.register(s->{for(var e:new ArrayList<>(ACTIVE.values()))if(e.isRemoved()||!e.valid()){ended(e);e.discard();}});
        ServerPlayConnectionEvents.DISCONNECT.register((h,s)->{var e=ACTIVE.get(h.player.getUuid());if(e!=null){ended(e);AbilityCooldowns.start(h.player,SmoothClasses.id("ascendancy_magic_circle"),1200);e.discard();}});
        ServerLifecycleEvents.SERVER_STOPPING.register(s->{for(var e:new ArrayList<>(ACTIVE.values())){ended(e);e.discard();}});
        ServerLifecycleEvents.SERVER_STOPPED.register(s->ACTIVE.clear());
    }
    public static ExecutionResult cast(ServerPlayerEntity p,int points){
        if(active(p))return ExecutionResult.failure("Raining Blood is still active.");
        var world=p.getServerWorld();
        if((world.getRegistryKey()!=World.OVERWORLD&&world.getRegistryKey()!=World.END)||world.getDimension().hasCeiling())
            return ExecutionResult.failure("Raining Blood requires the Overworld or The End.");
        if(!BloodRainEntity.exposed(p))return ExecutionResult.failure("You must stand under the open sky.");
        var e=SmoothEntities.BLOOD_RAIN.create(world);
        if(e==null)return ExecutionResult.failure("Cannot summon blood rain here.");
        e.setPosition(p.getPos());e.configure(p.getUuid(),points>=30);
        if(!world.spawnEntity(e))return ExecutionResult.failure("Cannot summon blood rain here.");
        track(e);
        // Cosmetic lightning: no incidental vanilla damage, fire, or block changes.
        var hit=p.raycast(64,1,false);
        var bolt=EntityType.LIGHTNING_BOLT.create(world);
        if(bolt!=null){bolt.setPosition(hit.getPos());bolt.setCosmetic(true);world.spawnEntity(bolt);}
        return ExecutionResult.success(1,"magic_circle");
    }
}
