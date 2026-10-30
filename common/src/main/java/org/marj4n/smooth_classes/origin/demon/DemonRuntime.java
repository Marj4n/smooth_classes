package org.marj4n.smooth_classes.origin.demon;

import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.block.*;
import net.minecraft.block.enums.BedPart;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.world.World;
import org.marj4n.smooth_classes.origin.*;
import org.marj4n.smooth_classes.origin.OriginAbilityDispatcher.Result;

public final class DemonRuntime {
    public static boolean running;
    private DemonRuntime(){}
    public static void register(){
        DemonConfig.load();ForbiddenBirth.register();DemonNetworking.register();DemonCommands.register();
        ServerLifecycleEvents.SERVER_STARTED.register(s->{running=true;DemonProtection.initialize();DemonMobIdentity.rebuild(s);ForbiddenTreeAsset.cells();org.marj4n.smooth_classes.SmoothClasses.LOGGER.info("FORBIDDEN_TREE ASSET VALIDATED states={} blocks={}",ForbiddenTreeAsset.paletteCount(),ForbiddenTreeAsset.cells().size());});
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((s,manager,success)->{if(success)DemonMobIdentity.rebuild(s);});
        ServerTickEvents.END_SERVER_TICK.register(server->{ForbiddenTreePlacement.tick(server);DemonKillStreak.tick(server);DemonDomain.tickWorld(server);DemonCurse.tickBursts(server);DemonArmy.tick(server);DemonPortals.tick(server);});
        ServerEntityEvents.ENTITY_LOAD.register((entity,world)->{DemonMobIdentity.onLoad(entity,world);DemonArmy.loaded(entity);if(entity instanceof ServerPlayerEntity p)ForbiddenBirth.join(p);});
        ServerEntityEvents.ENTITY_UNLOAD.register((entity,world)->{DemonArmy.unloaded(entity);if(entity instanceof net.minecraft.entity.LivingEntity living&&entity.getRemovalReason()!=null&&entity.getRemovalReason().shouldDestroy()&&!DemonEntityData.of(living).transforming)DemonCurse.cleanse(living);});
        ServerPlayConnectionEvents.DISCONNECT.register((handler,server)->{ForbiddenBirth.finish(handler.player,"disconnect");DemonKillStreak.reset(handler.player);DemonDomain.disconnect(handler.player);DemonThrone.removeSeat(handler.player);});
        ServerLifecycleEvents.SERVER_STOPPED.register(s->{running=false;ForbiddenBirth.clear(s);DemonKillStreak.stop(s);DemonDomain.clear();DemonArmy.clear();DemonPortals.clear();DemonThrone.clear();});
        ServerPlayConnectionEvents.JOIN.register((handler,sender,server)->{DemonKillStreak.join(handler.player);ForbiddenBirth.join(handler.player);});
        UseBlockCallback.EVENT.register((player,world,hand,hit)->{
            if(!(player instanceof ServerPlayerEntity p)||!DemonProgression.demon(p))return ActionResult.PASS;
            var pos=hit.getBlockPos();var block=world.getBlockState(pos);
            if(block.isOf(Blocks.BEACON)&&DemonThrone.bind(p,pos))return ActionResult.SUCCESS;
            if(world.getRegistryKey()==World.NETHER&&block.getBlock() instanceof BedBlock){
                if(!player.isSleeping()&&!block.get(BedBlock.OCCUPIED)){
                    if(block.get(BedBlock.PART)==BedPart.FOOT)pos=pos.offset(block.get(BedBlock.FACING));
                    player.sleep(pos);return ActionResult.SUCCESS;
                }
                return ActionResult.FAIL;
            }
            return ActionResult.PASS;
        });
        // Metrics must run after our END_SERVER_TICK work, including navigation.
        if(DemonRuntimeChecks.ENABLED)DemonRuntimeChecks.register();
    }
    public static void tick(ServerPlayerEntity p,OriginState s){
        if(!p.isAlive())return;
        DemonBodyGeometry.refresh(p);
        DemonBiology.tick(p,s);DemonBirth.tick(p);DemonCurse.tickPlayer(p);DemonDomain.tickPlayer(p);DemonImmortality.tick(p);DemonThrone.tick(p);
    }
    public static void cleanup(ServerPlayerEntity p){
        ForbiddenBirth.finish(p,"origin changed");
        DemonKillStreak.reset(p);
        if(!DemonProgression.demon(p))return;
        DemonDomain.end(p);DemonThrone.removeSeat(p);
        var ledger=DemonWorldState.get(p.getServer());ledger.mainCasts.remove(p.getUuid());for(var cast:ledger.casts.values())if(cast.owner.equals(p.getUuid()))cast.cancelled=true;ledger.markDirty();
    }
    public static Result activate(ServerPlayerEntity p,int slot){
        if(ForbiddenBirth.active(p))return Result.fail("Your first breath is not complete.");
        if(!p.isAlive()||p.isSpectator())return Result.fail("Your soul cannot act right now.");
        String flag=switch(slot){case 0->"brand";case 1->"pandemonium";case 2->"army";default->"";};
        if(!DemonProgression.has(p,flag))return Result.fail("Complete the Demon evolution objective first.");
        return switch(slot){case 0->DemonCurse.activate(p);case 1->DemonDomain.activate(p);case 2->{DemonNetworking.openArmy(p);yield Result.ok("All Hell Shall Kneel.");}default->Result.fail("Invalid Origin slot.");};
    }
}
