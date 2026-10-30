package org.marj4n.smooth_classes.origin.demon;
import java.util.*;
import java.nio.file.*;
import com.google.gson.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.nbt.NbtList;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import net.minecraft.block.Blocks;
import net.minecraft.util.Identifier;
import net.fabricmc.fabric.api.networking.v1.*;
import org.marj4n.smooth_classes.origin.*;

/** Explicit disposable-lab opt in only. No mission/Origin bypass registered in normal games. */
public final class ForbiddenBirthChecks {
    public static final boolean ENABLED=Boolean.getBoolean("smooth_classes.forbiddenBirthLab");
    public static final Identifier ACTION=new Identifier("smooth_classes","forbidden_birth_test_action");
    private record Inventory(String nbt,int xp,UUID id){}
    private static final Map<UUID,Inventory> INVENTORY=new HashMap<>();
    private static final List<Map<String,Object>> RESULTS=new ArrayList<>();
    private ForbiddenBirthChecks(){}
    public static void register(){if(!ENABLED)return;
        net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register((d,r,e)->d.register(CommandManager.literal("forbidden_birth_test").requires(s->s.hasPermissionLevel(2)).then(CommandManager.argument("mode",com.mojang.brigadier.arguments.StringArgumentType.word()).then(CommandManager.argument("player",EntityArgumentType.player()).executes(ctx->{
            var p=EntityArgumentType.getPlayer(ctx,"player");if(!p.getServer().getSaveProperties().getLevelName().toLowerCase(Locale.ROOT).contains("forbidden"))throw new IllegalStateException("Refusing test mutations outside named Forbidden birth lab world");
            String mode=com.mojang.brigadier.arguments.StringArgumentType.getString(ctx,"mode");try{String result=run(mode,p);ctx.getSource().sendFeedback(()->Text.literal(result),false);return 1;}catch(Exception|AssertionError ex){record(mode,false,ex.toString());ctx.getSource().sendError(Text.literal("FORBIDDEN_TEST FAILED "+mode+": "+ex));return 0;}
        })))));
    }
    private static void record(String test,boolean passed,String detail){RESULTS.add(Map.of("test",test,"passed",passed,"detail",detail));try{Files.writeString(Path.of("forbidden-birth-tests.json"),new GsonBuilder().setPrettyPrinting().create().toJson(RESULTS));}catch(Exception ex){throw new IllegalStateException(ex);}org.marj4n.smooth_classes.SmoothClasses.LOGGER.info("FORBIDDEN_TEST {} {} {}",passed?"PASS":"FAIL",test,detail);}
    private static void check(String test,boolean result){record(test,result,"");if(!result)throw new AssertionError(test);}
    public static void snapshot(ServerPlayerEntity p){INVENTORY.put(p.getUuid(),new Inventory(p.getInventory().writeNbt(new NbtList()).toString(),p.totalExperience,p.getUuid()));}
    private static void action(ServerPlayerEntity p,String value){if(!ServerPlayNetworking.canSend(p,ACTION))throw new IllegalStateException("Lab client channel unavailable");var b=net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();b.writeString(value);ServerPlayNetworking.send(p,ACTION,b);}
    public static String run(String mode,ServerPlayerEntity p){
        var r=ForbiddenTreePlacement.record(p.getServer());
        switch(mode){
            case "status"->{return new Gson().toJson(Map.of("phase",r.phase.name(),"anchored",r.anchored,"anchor",List.of(r.x,r.y,r.z),"placements",r.placements,"cursor",r.cursor,"attempt",r.attempt,"born",DemonWorldState.get(p.getServer()).born.size(),"scenes",ForbiddenBirth.scenes().size(),"player_active",ForbiddenBirth.active(p),"failure",r.failure));}
            case "select"->{snapshot(p);action(p,"select");return "Normal SELECT_ORIGIN client packet requested";}
            case "skip","respawn","reduced","invalid_skip"->{action(p,mode);return "Client lab action requested: "+mode;}
            case "metrics"->{return new Gson().toJson(Map.of("active_chunk_leases",ForbiddenChunkLoad.active(),"placement_tick_max_ms",ForbiddenTreePlacement.maxTickNanos/1000000d,"max_blocks_written_in_tick",ForbiddenTreePlacement.maxWrittenThisTick,"protection_ticks",OriginRuntime.state(p).progress("demon.protection"),"vanilla_invulnerable",p.getAbilities().invulnerable,"alive",p.isAlive(),"cinematic_scenes",ForbiddenBirth.scenes().size()));}
            case "tree"->{
                check("Actual1.20.1 registry validates53 palette states",ForbiddenTreeAsset.paletteCount()==53);
                check("Correct with-face original contains8271 blocks",ForbiddenTreeAsset.cells().size()==8271);
                check("Exactly one completed designated tree",r.anchored&&r.generated&&r.phase==ForbiddenTreeRecord.Phase.READY&&r.placements==1);
                var w=p.getServer().getWorld(World.NETHER);int matched=0,grassAged=0,face=0;for(var c:ForbiddenTreeAsset.cells()){var actual=w.getBlockState(ForbiddenTreeAsset.anchor(r).add(c.offset()));boolean aged=c.state().isOf(Blocks.GRASS_BLOCK)&&actual.isOf(Blocks.DIRT);if(actual.equals(c.state())||aged)matched++;if(aged)grassAged++;if(c.state().isOf(Blocks.DIORITE_STAIRS)&&actual.equals(c.state()))face++;}
                check("All8271 source cells preserved, allowing natural grass aging",matched==8271);check("All10 original diorite face detail cells preserved",face==10);
                record("Source structure measured",true,"anchor="+r.x+","+r.y+","+r.z+",matched="+matched+",naturallyAgedGrass="+grassAged+",sourceHash="+r.hash);return "FORBIDDEN_TEST tree passed";
            }
            case "pair"->{check("Two independent simultaneous birth sessions",ForbiddenBirth.scenes().size()==2);var list=new ArrayList<>(ForbiddenBirth.scenes());check("Independent actual player UUIDs",!list.get(0).player().equals(list.get(1).player()));check("One shared tree anchor",list.get(0).anchor().equals(list.get(1).anchor()));check("Separated safe emergence positions",list.get(0).landing().squaredDistanceTo(list.get(1).landing())>=2.25);check("One designated placement for two first births",r.placements==1);return "FORBIDDEN_TEST pair passed";}
            case "born"->{
                check("Actual player is alive",p.isAlive());check("Demon Origin retained",OriginRuntime.state(p).origin()==OriginType.DEMON);check("First-birth UUID recorded",DemonWorldState.get(p.getServer()).born.contains(p.getUuid()));check("No active/stuck server cinematic",!ForbiddenBirth.active(p));check("No pending first-birth replay",!OriginRuntime.state(p).hasFlag("demon.birth_pending"));check("No permanent birth invulnerability",!p.getAbilities().invulnerable&&OriginRuntime.state(p).progress("demon.protection")==0);check("Player body collision clear",p.getServerWorld().isSpaceEmpty(p,p.getBoundingBox()));
                var old=INVENTORY.get(p.getUuid());if(old!=null){check("Actual inventory unchanged",old.nbt().equals(p.getInventory().writeNbt(new NbtList()).toString()));check("Actual XP unchanged",old.xp()==p.totalExperience);check("Same real player UUID",old.id().equals(p.getUuid()));}return "FORBIDDEN_TEST born passed";
            }
            case "legacy"->{check("Legacy fixture already born",DemonWorldState.get(p.getServer()).born.contains(p.getUuid()));DemonBirth.begin(p);check("Legacy Demon never requeues birth",!OriginRuntime.state(p).hasFlag("demon.birth_pending")&&!ForbiddenBirth.active(p));check("Legacy login does not create second tree",r.placements==1);return "FORBIDDEN_TEST legacy passed";}
            case "break_branch"->{var w=p.getServer().getWorld(World.NETHER);w.setBlockState(ForbiddenTreeAsset.anchor(r).add(33,31,28),Blocks.AIR.getDefaultState(),2);check("Modified tree retains designated anchor",r.anchored&&r.generated);return "Designated branch removed in disposable lab; no auto repair requested";}
            case "fallback"->{check("Fallback still records Demon birth",OriginRuntime.state(p).origin()==OriginType.DEMON&&DemonWorldState.get(p.getServer()).born.contains(p.getUuid()));check("Fallback does not leave a scene",!ForbiddenBirth.active(p));check("Fallback does not generate duplicate",r.placements<=1);check("Fallback collision clear",p.getServerWorld().isSpaceEmpty(p,p.getBoundingBox()));return "FORBIDDEN_TEST fallback passed";}
            default->throw new IllegalArgumentException("Unknown lab mode "+mode);
        }
    }
}
