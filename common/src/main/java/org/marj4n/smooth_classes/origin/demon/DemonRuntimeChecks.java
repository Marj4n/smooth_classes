package org.marj4n.smooth_classes.origin.demon;

import java.util.*;
import java.nio.file.*;
import com.google.gson.*;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.command.CommandManager;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.block.*;
import net.minecraft.entity.*;
import net.minecraft.entity.mob.*;
import net.minecraft.item.*;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.nbt.*;
import net.minecraft.text.Text;
import net.minecraft.util.math.*;
import net.minecraft.util.Hand;
import net.minecraft.world.*;
import org.marj4n.smooth_classes.origin.*;
import org.marj4n.smooth_classes.SmoothClasses;

/** Opt-in test harness. Disabled unless JVM property smooth_classes.demonTest=true.
 * Operator-only commands mutate ONLY the explicitly selected disposable test player/world.
 * Not a Survival progression route; do not enable this flag on a player server. */
public final class DemonRuntimeChecks {
    public static final boolean ENABLED=Boolean.getBoolean("smooth_classes.demonTest");
    private static final List<Map<String,Object>> RESULTS=new ArrayList<>();
    private static final List<Long> TICKS=new ArrayList<>();
    private record SeatObservation(UUID vehicle,Vec3d position,long tick,int rotation) {}
    private static final Map<UUID,SeatObservation> SEAT_OBSERVATIONS=new HashMap<>();
    private static boolean benchmarking;private static long startTick,navNanos,navCalls;
    private static String inventoryBefore;private static int xpBefore;
    private static final BlockPos BASE=new BlockPos(128,160,0);
    private DemonRuntimeChecks(){}
    public static void register(){
        if(!ENABLED)return;
        ServerTickEvents.START_SERVER_TICK.register(s->{if(benchmarking)startTick=System.nanoTime();});
        ServerTickEvents.END_SERVER_TICK.register(s->{if(benchmarking)TICKS.add(System.nanoTime()-startTick);});
        CommandRegistrationCallback.EVENT.register((d,r,e)->d.register(CommandManager.literal("smoothclasses_demon_test")
            .requires(source->source.hasPermissionLevel(2))
            .then(CommandManager.argument("scenario",com.mojang.brigadier.arguments.StringArgumentType.word())
                .then(CommandManager.argument("player",EntityArgumentType.player()).executes(ctx->{
                    String mode=com.mojang.brigadier.arguments.StringArgumentType.getString(ctx,"scenario");
                    var p=EntityArgumentType.getPlayer(ctx,"player");
                    try{run(mode,p);write();ctx.getSource().sendFeedback(()->Text.literal("DEMON_TEST "+mode+" completed; "+RESULTS.size()+" assertions recorded."),false);return 1;}
                    catch(Exception|AssertionError ex){record(mode,false,ex.toString());SmoothClasses.LOGGER.error("Demon runtime test failed: "+mode,ex);write();ctx.getSource().sendError(Text.literal("DEMON_TEST FAILED "+mode+": "+ex));return 0;}
                })))));
    }
    public static void navigation(long nanos){if(benchmarking){navNanos+=nanos;navCalls++;}}
    private static void record(String name,boolean pass,String detail){RESULTS.add(Map.of("test",name,"pass",pass,"detail",detail));SmoothClasses.LOGGER.info("DEMON_TEST {} {} {}",pass?"PASS":"FAIL",name,detail);}
    private static void check(String name,boolean value){record(name,value,"");if(!value)throw new AssertionError(name);}
    private static void write(){
        try{Files.writeString(Path.of("demon-runtime-tests.json"),new GsonBuilder().setPrettyPrinting().create().toJson(RESULTS));}
        catch(java.io.IOException ex){throw new IllegalStateException("Cannot write runtime test report",ex);}
    }
    private static ServerWorld nether(ServerPlayerEntity p){return Objects.requireNonNull(p.getServer().getWorld(World.NETHER));}
    private static void platform(ServerWorld w,BlockPos anchor,int radius){for(int x=-radius;x<=radius;x++)for(int z=-radius;z<=radius;z++){w.setBlockState(anchor.add(x,0,z),Blocks.NETHERITE_BLOCK.getDefaultState(),2);for(int y=1;y<5;y++)w.setBlockState(anchor.add(x,y,z),Blocks.AIR.getDefaultState(),2);}}
    private static void arena(ServerPlayerEntity p){var w=nether(p);w.getChunk(BASE.getX()>>4,BASE.getZ()>>4);platform(w,BASE,8);p.stopRiding();p.teleport(w,BASE.getX()+.5,BASE.getY()+1,BASE.getZ()+.5,0,0);p.setVelocity(0,0,0);p.fallDistance=0;}
    private static void clearMobs(ServerPlayerEntity p){for(var w:p.getServer().getWorlds()){List<Entity> snapshot=new ArrayList<>();for(var e:w.iterateEntities())if(e!=null&&e.getCommandTags().contains("sc_demon_test"))snapshot.add(e);for(var e:snapshot)e.discard();}}
    private static MobEntity mob(ServerPlayerEntity p,EntityType<? extends MobEntity> type,double dx,double dz){
        var m=type.create(p.getServerWorld());if(m==null)throw new AssertionError("Entity factory failed");
        m.refreshPositionAndAngles(p.getX()+dx,p.getY(),p.getZ()+dz,0,0);m.addCommandTag("sc_demon_test");m.setPersistent();p.getServerWorld().spawnEntity(m);return m;
    }
    private static void placeThrone(ServerWorld w,BlockPos origin,int rotation){
        try(var input=DemonRuntimeChecks.class.getResourceAsStream("/data/smooth_classes/demon/black_throne_compact_v2.json")){
            if(input==null)throw new AssertionError("Approved blueprint resource is missing");
            JsonObject blueprint=JsonParser.parseReader(new java.io.InputStreamReader(input,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
            for(int y=0;y<4;y++)for(int z=0;z<3;z++)for(int x=0;x<3;x++)w.setBlockState(origin.add(x,y,z),Blocks.AIR.getDefaultState(),2);
            for(var cell:blueprint.getAsJsonArray("blocks")){var b=cell.getAsJsonObject();int[] rotated=DemonRules.rotate(b.get("x").getAsInt(),b.get("z").getAsInt(),rotation);var block=org.marj4n.smooth_classes.platform.PlatformRegistries.BLOCK.get(org.marj4n.smooth_classes.util.ResourceIds.parse(b.get("block").getAsString()));w.setBlockState(origin.add(rotated[0],b.get("y").getAsInt(),rotated[1]),block.getDefaultState(),2);}
        }catch(java.io.IOException ex){throw new IllegalStateException(ex);}
    }
    private static void run(String mode,ServerPlayerEntity p){
        if(!mode.equals("respawn")&&!mode.equals("fatal_dead_check")&&!mode.equals("cleanup")&&!p.isAlive())throw new IllegalStateException("Respawn the disposable test player before running "+mode);
        var s=OriginRuntime.state(p);
        switch(mode){
            case "shot_front","shot_back","shot_first","shot_peer","shot_walk","shot_run","shot_crouch","shot_head","shot_pulse","shot_hud"->{
                String shot=mode.substring(5);var buf=net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();buf.writeString(shot);net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(p,org.marj4n.smooth_classes.util.ResourceIds.parse("smooth_classes:demon_visual_test_capture"),buf);
            }
            case "streak_one"->{
                check("Streak fixture Lord",DemonKillStreak.eligible(p));long before=DemonKillStreak.stacks(p);float max=p.getMaxHealth();p.setHealth(max-4);float health=p.getHealth();
                var target=mob(p,EntityType.ZOMBIE,3,0);target.setAiDisabled(true);var source=p.getDamageSources().playerAttack(p);target.damage(source,10000);
                check("Confirmed actual death adds exactly one stack",!target.isAlive()&&DemonKillStreak.stacks(p)==before+1);
                check("One kill adds1 max HP",Math.abs(p.getMaxHealth()-max-1)<.001);
                check("One kill restores1 current HP",Math.abs(p.getHealth()-health-1)<.001);
                DemonKillStreak.confirmedDeath(target,source);check("Duplicate death credit rejected",DemonKillStreak.stacks(p)==before+1);
                record("Actual kill streak attributes",true,"stacks="+DemonKillStreak.stacks(p)+",maxHP="+p.getMaxHealth()+",currentHP="+p.getHealth()+",armor="+p.getAttributeValue(EntityAttributes.GENERIC_ARMOR));
            }
            case "streak_reset_check"->{check("66s server timeout clears stacks",DemonKillStreak.stacks(p)==0);record("Timeout attributes",true,"maxHP="+p.getMaxHealth()+",armor="+p.getAttributeValue(EntityAttributes.GENERIC_ARMOR));}
            case "streak_reset"->{DemonKillStreak.reset(p);check("Explicit fixture reset clears transient state",DemonKillStreak.stacks(p)==0);}

            case "visual_skin_one"->{s.unflag("demon.debug.no_mesh");s.unflag("demon.debug.no_skin");s.flag("demon.debug.skin_one");org.marj4n.smooth_classes.network.SmoothClassesNetworking.sendOriginState(p);}
            case "visual_walk"->{s.flag("demon.debug.walk");org.marj4n.smooth_classes.network.SmoothClassesNetworking.sendOriginState(p);}

            case "visual_no_mesh","visual_no_skin","visual_debug_clear"->{
                s.unflag("demon.debug.no_mesh");s.unflag("demon.debug.no_skin");s.unflag("demon.debug.skin_one");s.unflag("demon.debug.walk");
                if(mode.equals("visual_no_mesh"))s.flag("demon.debug.no_mesh");
                if(mode.equals("visual_no_skin"))s.flag("demon.debug.no_skin");
                org.marj4n.smooth_classes.network.SmoothClassesNetworking.sendOriginState(p);
            }

            case "visual_netherforged","visual_lord"->{
                OriginRuntime.setOrigin(p,OriginType.DEMON);s=OriginRuntime.state(p);
                s.flag("demon.world_ender");s.flag("demon.netherforged");s.unflag("demon.lord");
                if(mode.equals("visual_lord"))s.flag("demon.lord");
                p.setPose(EntityPose.STANDING);p.calculateDimensions();
                org.marj4n.smooth_classes.network.SmoothClassesNetworking.sendOriginState(p);
                check("Authoritative visual stage "+mode,DemonVisualStage.of(s)==(mode.equals("visual_lord")?DemonVisualStage.DEMON_LORD:DemonVisualStage.NETHERFORGED));
            }
            case "visual_dimensions"->{
                check("Lord fixture active",DemonBodyGeometry.applies(p));
                for(var pose:new EntityPose[]{EntityPose.STANDING,EntityPose.CROUCHING,EntityPose.SWIMMING,EntityPose.FALL_FLYING}){
                    p.setPose(pose);p.calculateDimensions();var b=p.getBoundingBox();float expected=DemonBodyGeometry.height(pose,false);
                    check("Lord actual box height "+pose,Math.abs(b.maxY-b.minY-expected)<.001);
                    check("Lord actual box width "+pose,Math.abs(b.maxX-b.minX-DemonBodyGeometry.WIDTH)<.001);
                    check("Lord actual eye inside body "+pose,p.getEyeHeight(pose)>0&&p.getEyeHeight(pose)<expected);
                    record("Lord measured "+pose,true,"height="+(b.maxY-b.minY)+", width="+(b.maxX-b.minX)+", eye="+p.getEyeHeight(pose));
                }
                p.setPose(EntityPose.STANDING);p.calculateDimensions();
            }
            case "visual_headroom"->{
                check("Lord fixture active",DemonBodyGeometry.applies(p));arena(p);var w=p.getServerWorld();
                // 2-block room admits crawl but not 2.5-block crouch or 3-block standing.
                for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)w.setBlockState(BASE.add(x,3,z),Blocks.STONE.getDefaultState(),2);
                s.unflag("demon.lord");p.setPose(EntityPose.STANDING);p.calculateDimensions();s.flag("demon.lord");
                DemonBodyGeometry.refresh(p);
                check("Growth under ceiling selects crawl",p.getPose()==EntityPose.SWIMMING);
                check("Grown body fits room",w.isSpaceEmpty(p,p.getBoundingBox()));
                for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)w.setBlockState(BASE.add(x,3,z),Blocks.AIR.getDefaultState(),2);
                p.setPose(EntityPose.STANDING);p.calculateDimensions();
                org.marj4n.smooth_classes.network.SmoothClassesNetworking.sendOriginState(p);
            }
            case "visual_goat"->{
                OriginRuntime.setOrigin(p,OriginType.DEMON);s=OriginRuntime.state(p);
                s.unflag("demon.netherforged");s.unflag("demon.lord");s.flag("demon.world_ender");
                p.calculateDimensions();
                org.marj4n.smooth_classes.network.SmoothClassesNetworking.sendOriginState(p);
                check("Visual fixture uses authoritative Goatborn flag",DemonVisualStage.of(s)==DemonVisualStage.WORLD_ENDER);
            }

            case "visual_base"->{
                OriginRuntime.setOrigin(p,OriginType.DEMON);s=OriginRuntime.state(p);
                s.unflag("demon.world_ender");s.unflag("demon.netherforged");s.unflag("demon.lord");
                int age=p.age;p.age=((age/10)+1)*10;DemonBirth.tick(p);p.age=age;
                org.marj4n.smooth_classes.network.SmoothClassesNetworking.sendOriginState(p);
                check("Visual fixture uses actual Demon Base state",DemonVisualStage.of(s)==DemonVisualStage.DEMON_BASE);
                check("Visual fixture is in actual Nether",p.getWorld().getRegistryKey()==World.NETHER);
            }

            case "inspect"->{for(var e:p.getServerWorld().iterateEntities())if(e instanceof MobEntity m&&e.getCommandTags().contains("sc_movement_probe"))record("Movement diagnostic",true,DemonArmy.inspect(m));}
            case "kick"->{for(var e:p.getServerWorld().iterateEntities())if(e instanceof MobEntity m&&e.getCommandTags().contains("sc_movement_probe")){m.getNavigation().tick();m.getMoveControl().tick();record("One native navigation step",true,DemonArmy.inspect(m));}}
            case "base"->{
                // Recreate a first-selection fixture even when rerunning this lab UUID.
                DemonWorldState.get(p.getServer()).born.remove(p.getUuid());
                OriginRuntime.setOrigin(p,OriginType.DEMON);s=OriginRuntime.state(p);
                check("Demon selectable",OriginType.DEMON.isV1Playable());
                int age=p.age; p.age=((age/10)+1)*10;DemonBirth.tick(p);p.age=age;
                check("Initial birth reaches Nether safely",p.getWorld().getRegistryKey()==World.NETHER&&DemonWorldState.get(p.getServer()).born.contains(p.getUuid()));
                ForbiddenBirth.finish(p,"base regression fixture completes asynchronous birth");
                Vec3d birth=p.getPos();DemonBirth.begin(p);check("Birth does not repeat for existing UUID",!s.hasFlag("demon.birth_pending")&&p.getPos().equals(birth));
                check("Normal food before path",!DemonDiet.applies(p));
                arena(p);p.changeGameMode(GameMode.SURVIVAL);p.setHealth(20);s.progress("demon.protection",0);
                DemonBiology.protectFire(p,p.getDamageSources().inFire());
                check("Cinder first fire protection is immediate",s.progress("demon.cinder_active")==13320&&p.hasStatusEffect(StatusEffects.FIRE_RESISTANCE));
                check("Cinder unlocks first passive",s.hasFlag("demon.cinder_unlocked"));
                s.progress("demon.cinder_active",1);DemonBiology.tick(p,s);
                check("Cinder cooldown starts after effect",s.progress("demon.cinder_active")==0&&s.progress("demon.cinder_recovery")==13320);
                check("Cinder cannot retrigger during recovery",!DemonBiology.protectFire(p,p.getDamageSources().inFire()));
                check("Coming Soon path cannot be selected",!DemonProgression.choose(p,"hellkeeper"));
                check("World-Ender can be selected",DemonProgression.choose(p,"world_ender"));
                check("Path selection is permanent",!DemonProgression.choose(p,"world_ender")&&!DemonProgression.choose(p,"divine_demon"));
                p.getHungerManager().setFoodLevel(10);p.getHungerManager().setSaturationLevel(0);ItemStack fungus=new ItemStack(Items.CRIMSON_FUNGUS,3);DemonDiet.consume(p.getWorld(),p,fungus);
                check("Fungus gives two hunger without duplicating items",p.getHungerManager().getFoodLevel()==12&&fungus.getCount()==2);
                check("Fungus gives 1.2 saturation points",Math.abs(p.getHungerManager().getSaturationLevel()-1.2F)<.001);
                ItemStack roots=new ItemStack(Items.WARPED_ROOTS,2);DemonDiet.consume(p.getWorld(),p,roots);
                check("Roots give one hunger and 0.6 saturation",p.getHungerManager().getFoodLevel()==13&&roots.getCount()==1&&Math.abs(p.getHungerManager().getSaturationLevel()-1.8F)<.001);
                for(Item item:new Item[]{Items.GOLDEN_APPLE,Items.ENCHANTED_GOLDEN_APPLE}){
                    p.removeStatusEffect(StatusEffects.REGENERATION);p.removeStatusEffect(StatusEffects.ABSORPTION);int food=p.getHungerManager().getFoodLevel();ItemStack stack=new ItemStack(item,2);stack.finishUsing(p.getWorld(),p);
                    check("Rejected apple retains item and grants no effects: "+item,stack.getCount()==2&&p.getHungerManager().getFoodLevel()==food&&!p.hasStatusEffect(StatusEffects.REGENERATION)&&!p.hasStatusEffect(StatusEffects.ABSORPTION));
                }
                check("Pork whitelist",DemonDiet.allowed(Items.PORKCHOP)&&DemonDiet.allowed(Items.COOKED_PORKCHOP)&&!DemonDiet.allowed(Items.BREAD));
                s.progress("demon.decay",6000);DemonBiology.tick(p,s);check("Nether recovery preserves most accumulated decay",s.progress("demon.decay")==5990);
                NbtCompound saved=new NbtCompound();s.write(saved);OriginState copied=new OriginState();copied.read(saved);check("Player state serializes active evolution and timers",copied.hasFlag("demon.world_ender")&&copied.progress("demon.decay")==5990&&copied.progress("demon.cinder_recovery")>0);
                s.progress("demon.decay",0);s.progress("demon.cinder_recovery",0);p.removeStatusEffect(StatusEffects.SLOWNESS);p.removeStatusEffect(StatusEffects.MINING_FATIGUE);p.removeStatusEffect(StatusEffects.HUNGER);
                BlockPos bed=BASE.add(5,1,0);p.getWorld().setBlockState(bed.down(),Blocks.NETHERITE_BLOCK.getDefaultState(),2);p.getWorld().setBlockState(bed.south().down(),Blocks.NETHERITE_BLOCK.getDefaultState(),2);
                p.getWorld().setBlockState(bed,Blocks.BLACK_BED.getDefaultState().with(BedBlock.FACING,Direction.SOUTH).with(BedBlock.PART,net.minecraft.block.enums.BedPart.FOOT),Block.NOTIFY_LISTENERS|Block.FORCE_STATE);
                p.getWorld().setBlockState(bed.south(),Blocks.BLACK_BED.getDefaultState().with(BedBlock.FACING,Direction.SOUTH).with(BedBlock.PART,net.minecraft.block.enums.BedPart.HEAD),Block.NOTIFY_LISTENERS|Block.FORCE_STATE);
                var result=net.fabricmc.fabric.api.event.player.UseBlockCallback.EVENT.invoker().interact(p,p.getWorld(),Hand.MAIN_HAND,new net.minecraft.util.hit.BlockHitResult(Vec3d.ofCenter(bed),Direction.UP,bed,false));
                check("Demon Nether sleep intercepts vanilla bed explosion",result==net.minecraft.util.ActionResult.SUCCESS&&p.isSleeping()&&p.getWorld().getBlockState(bed).isOf(Blocks.BLACK_BED));p.wakeUp(true,true);arena(p);
            }
            case "curse"->{
                clearMobs(p);arena(p);s.flag("demon.world_ender");s.flag("demon.brand");s.unflag("demon.sovereign");s.unflag("demon.netherforged");s.unflag("demon.lord");s.progress("demon.brand_cd",0);
                var target=mob(p,EntityType.HUSK,0,5);target.getAttributeInstance(EntityAttributes.GENERIC_ARMOR).setBaseValue(20);p.setYaw(0);p.setPitch(0);
                var castResult=DemonCurse.activate(p);record("Brand diagnostic",true,castResult.message()+" player="+p.getPos()+" pose="+p.getPose()+" yaw="+p.getYaw()+" pitch="+p.getPitch()+" target="+target.getPos()+" aim="+DemonCurse.target(p,32));
                check("Brand raycast selects living visible target",castResult.success()&&DemonEntityData.of(target).curseOwner!=null);
                check("Brand cast cooldown is 30 seconds",s.progress("demon.brand_cd")==600);
                check("Brand reduces armor by 25 percent",Math.abs(target.getAttributeValue(EntityAttributes.GENERIC_ARMOR)-15)<.001);
                check("Brand reduces outgoing damage by 20 percent",Math.abs(DemonCurse.outgoing(p.getDamageSources().mobAttack(target),10)-8)<.001);
                target.clearStatusEffects();check("Vanilla status cleansing cannot remove curse",DemonEntityData.of(target).curseOwner!=null&&Math.abs(target.getAttributeValue(EntityAttributes.GENERIC_ARMOR)-15)<.001);
                check("Recall is available during cast cooldown",DemonCurse.activate(p).success());
                check("Recall removes curse modifiers and clears cooldown",s.progress("demon.brand_cd")==0&&DemonEntityData.of(target).curseOwner==null&&Math.abs(target.getAttributeValue(EntityAttributes.GENERIC_ARMOR)-20)<.001);
                s.flag("demon.netherforged");DemonCurse.activate(p);target.setHealth(20);target.timeUntilRegen=0;DemonEntityData.of(target).dotTicks=DemonConfig.INSTANCE.curseDotInterval-1;DemonCurse.tick(target);
                check("Netherforged curse deals actual DoT",target.getHealth()<20);
                final var protectedTarget=target;var veto=new java.util.concurrent.atomic.AtomicBoolean(true);
                DemonProtection.register((owner,world,pos)->!veto.get()||!pos.equals(protectedTarget.getBlockPos()));
                float beforeProtected=target.getHealth();target.timeUntilRegen=0;DemonEntityData.of(target).dotTicks=DemonConfig.INSTANCE.curseDotInterval-1;DemonCurse.tick(target);
                check("Curse DoT respects a newly denied region",target.getHealth()==beforeProtected);veto.set(false);
                check("Future Angel cleansing hook removes real debuffs",DemonCurse.cleanse(target)&&Math.abs(target.getAttributeValue(EntityAttributes.GENERIC_ARMOR)-20)<.001);
                clearMobs(p);s.flag("demon.sovereign");s.progress("demon.brand_cd",0);target=mob(p,EntityType.HUSK,0,5);DemonCurse.activate(p);
                int oldBudget=DemonConfig.INSTANCE.propagationBudget;DemonConfig.INSTANCE.propagationBudget=3;var token=DemonEntityData.of(target).curseOwner;DemonWorldState.get(p.getServer()).casts.get(token).budget=3;
                for(int i=0;i<8;i++){var m=mob(p,EntityType.HUSK,(i%3)-1,7+(i/3));m.setHealth(20);}
                target.timeUntilRegen=0;target.damage(p.getDamageSources().playerAttack(p),1000);
                for(int i=0;i<8;i++)DemonCurse.tickBursts(p.getServer());
                int spread=0;for(var e:p.getServerWorld().iterateEntities())if(e instanceof LivingEntity l&&token.equals(DemonEntityData.of(l).curseOwner))spread++;
                check("Death burst propagates to exactly its finite budget",spread==3);DemonConfig.INSTANCE.propagationBudget=oldBudget;clearMobs(p);
            }
            case "domain_prepare"->{
                clearMobs(p);arena(p);s.flag("demon.pandemonium");s.flag("demon.netherforged");s.progress("demon.domain_cd",0);s.progress("demon.domain_active",0);DemonConfig.INSTANCE.multiplayerTerrainChanges=true;
                p.getWorld().setBlockState(BASE.add(0,1,4),Blocks.WATER.getDefaultState(),2);
                p.getWorld().setBlockState(BASE.add(1,1,4),Blocks.OAK_SLAB.getDefaultState().with(net.minecraft.state.property.Properties.WATERLOGGED,true),2);
                check("Pandemonium starts with exact duration",DemonDomain.activate(p).success()&&s.progress("demon.domain_active")==900);
                p.getWorld().setBlockState(BASE.add(0,1,5),Blocks.WATER.getDefaultState(),3);
                check("New water evaporates immediately",!p.getWorld().getFluidState(BASE.add(0,1,5)).isIn(net.minecraft.registry.tag.FluidTags.WATER));
                // Place existing water near the beginning of the deterministic scanner.
                p.getWorld().setBlockState(BASE.add(0,-62,0),Blocks.WATER.getDefaultState(),2);
                s.progress("demon.domain_active",1);DemonDomain.tickPlayer(p);
                check("Pandemonium cooldown starts only after end",s.progress("demon.domain_active")==0&&s.progress("demon.domain_cd")==1200);
                check("Evaporated water is not restored when domain ends",p.getWorld().getBlockState(BASE.add(0,1,5)).isAir());
                s.progress("demon.domain_cd",0);DemonDomain.activate(p); // Leave a real running domain for incremental/runtime checks.
            }
            case "domain_check"->{
                check("Existing water permanently evaporates",!p.getWorld().getFluidState(BASE.add(0,1,4)).isIn(net.minecraft.registry.tag.FluidTags.WATER));
                check("Waterlogged blocks survive drying",p.getWorld().getBlockState(BASE.add(1,1,4)).isOf(Blocks.OAK_SLAB)&&!p.getWorld().getFluidState(BASE.add(1,1,4)).isIn(net.minecraft.registry.tag.FluidTags.WATER));
                if(s.progress("demon.domain_active")==0){s.progress("demon.domain_cd",0);DemonDomain.activate(p);}DemonDomain.tickPlayer(p);
                check("Domain applies real additional armor",p.getAttributeValue(EntityAttributes.GENERIC_ARMOR)>=4);
                DemonDomain.end(p);check("Domain modifiers clean up",p.getAttributeValue(EntityAttributes.GENERIC_ARMOR)==0);
            }
            case "respect"->{
                clearMobs(p);arena(p);var nativeMob=mob(p,EntityType.ZOMBIFIED_PIGLIN,2,0);var visitor=mob(p,EntityType.HUSK,-2,0);var boss=EntityType.WITHER.create(p.getWorld());
                check("Nether natives retain identity",DemonMobIdentity.nativeMob(nativeMob));check("Overworld visitor in Nether is not native",!DemonMobIdentity.nativeMob(visitor));check("Boss is excluded",boss!=null&&!DemonMobIdentity.eligible(boss));
                s.unflag("demon.sovereign");s.flag("demon.recognition");nativeMob.setTarget(p);check("Tier I prevents first aggression",nativeMob.getTarget()==null);
                DemonEntityData.of(nativeMob).provokedBy=p.getUuid();nativeMob.setTarget(p);check("Tier I permits retaliation",nativeMob.getTarget()==p);
                s.flag("demon.sovereign");nativeMob.setTarget(null);nativeMob.setTarget(p);check("Tier II prevents retaliation",nativeMob.getTarget()==null);
                s.flag("demon.army");var order=DemonArmy.command(p,1);check("Army claims eligible native only",order.success()&&p.getUuid().equals(DemonEntityData.of(nativeMob).commander)&&DemonEntityData.of(visitor).commander==null);
                var data=new DemonEntityData();data.read(DemonEntityData.of(nativeMob).write());check("Army owner and order persist in mob NBT",p.getUuid().equals(data.commander)&&data.command==1);
                nativeMob.setTarget(visitor);check("Travel order suppresses stale native combat target",nativeMob.getTarget()==null);
                DemonEntityData.of(nativeMob).command=2;nativeMob.setTarget(visitor);check("Attack order preserves native combat targeting",nativeMob.getTarget()==visitor);
                DemonArmy.command(p,4);check("Withdraw disengages the real mob",nativeMob.getTarget()==null&&DemonEntityData.of(nativeMob).command==4);
                clearMobs(p);
            }
            case "conversion"->{
                clearMobs(p);arena(p);s.flag("demon.army");s.flag("demon.sovereign");
                var original=mob(p,EntityType.PIGLIN,2,0);var data=DemonEntityData.of(original);data.commander=p.getUuid();data.command=3;data.guard=BASE;DemonArmy.loaded(original);
                var converted=original.convertTo(EntityType.ZOMBIFIED_PIGLIN,true);
                check("Piglin conversion produces one live entity",converted!=null&&original.isRemoved()&&converted.isAlive());converted.addCommandTag("sc_demon_test");
                var kept=DemonEntityData.of(converted);
                check("Conversion preserves army owner and order",p.getUuid().equals(kept.commander)&&kept.command==3&&BASE.equals(kept.guard));
                check("Conversion preserves native provenance",kept.birthRecorded&&kept.nativeBirth&&DemonMobIdentity.nativeMob(converted));
                clearMobs(p);
            }
            case "multiplayer"->{
                var peer=p.getServer().getPlayerManager().getPlayer("DemonPeer");if(peer==null)throw new IllegalStateException("Second real client DemonPeer must be connected");
                clearMobs(p);arena(p);OriginRuntime.setOrigin(peer,OriginType.DEMON);peer.teleport(p.getServerWorld(),p.getX()+1,p.getY(),p.getZ(),0,0);
                var ps=OriginRuntime.state(peer);ps.unflag("demon.birth_pending");ps.flag("demon.sovereign");ps.flag("demon.army");s.flag("demon.sovereign");s.flag("demon.army");
                peer.changeGameMode(GameMode.SURVIVAL);ps.progress("demon.protection",0);
                check("Demon cannot receive another Demon's curse",!DemonCurse.validTarget(p,peer));
                var ally=mob(p,EntityType.ZOMBIFIED_PIGLIN,3,0);DemonArmy.command(p,1);UUID first=DemonEntityData.of(ally).commander;DemonArmy.command(peer,0);
                check("Two online Demons cannot steal an owned army",p.getUuid().equals(first)&&first.equals(DemonEntityData.of(ally).commander));
                peer.setHealth(20);peer.timeUntilRegen=0;peer.damage(peer.getDamageSources().mobAttack(ally),2);
                check("Another Demon's troop is not neutralized by Sovereign immunity",peer.getHealth()<20);
                p.setHealth(20);p.timeUntilRegen=0;p.damage(p.getDamageSources().mobAttack(ally),2);check("Own troop friendly fire is blocked",p.getHealth()==20);
                check("Other player's ownership survives NBT serialization",first.equals(roundTrip(ally).commander));clearMobs(p);
            }
            case "modded"->{
                clearMobs(p);arena(p);s.flag("demon.sovereign");s.flag("demon.army");
                for(String id:List.of("skarrier_mobs:hellamander","skarrier_mobs:crimlin","netherman:ghastly","cataclysm:ignited_berserker")){
                    var key=org.marj4n.smooth_classes.util.ResourceIds.parse(id);check("Original mod native registered: "+id,org.marj4n.smooth_classes.platform.PlatformRegistries.ENTITY_TYPE.containsId(key));
                    var e=org.marj4n.smooth_classes.platform.PlatformRegistries.ENTITY_TYPE.get(key).create(p.getWorld());check("Original mob factory creates a living native: "+id,e instanceof MobEntity);
                    var m=(MobEntity)e;m.setAiDisabled(true);m.refreshPositionAndAngles(p.getX()+3,p.getY(),p.getZ()+3,0,0);m.addCommandTag("sc_demon_test");p.getServerWorld().spawnEntity(m);
                    check("Original mod native is eligible: "+id,DemonMobIdentity.eligible(m));
                    DemonArmy.command(p,0);check("Original mob is claimed without replacement: "+id,p.getUuid().equals(DemonEntityData.of(m).commander));
                }
                for(String id:List.of("cataclysm:ignis","cataclysm:netherite_monstrosity","cataclysm:ignited_revenant","netherman:azazel","bosses_of_mass_destruction:gauntlet","soulsweapons:accursed_lord_boss")){
                    var key=org.marj4n.smooth_classes.util.ResourceIds.parse(id);check("Required original encounter registered: "+id,org.marj4n.smooth_classes.platform.PlatformRegistries.ENTITY_TYPE.containsId(key));
                    var e=org.marj4n.smooth_classes.platform.PlatformRegistries.ENTITY_TYPE.get(key).create(p.getWorld());check("Boss or miniboss cannot join army: "+id,e instanceof LivingEntity l&&DemonMobIdentity.boss(l)&&!DemonMobIdentity.eligible(l));
                }
                var human=org.marj4n.smooth_classes.platform.PlatformRegistries.ENTITY_TYPE.get(org.marj4n.smooth_classes.util.ResourceIds.parse("netherman:azazel_human")).create(p.getWorld());s.unflag("demon.boss.azazel");
                DemonWorldState.get(p.getServer()).historicalKills.computeIfAbsent(p.getUuid(),ignored->new HashSet<>()).remove("azazel");
                if(human instanceof LivingEntity living)DemonProgression.onKill(p,living);check("Human Azazel does not grant Nether encounter credit",!s.hasFlag("demon.boss.azazel"));clearMobs(p);
            }
            case "trial_fortress","trial_bastion"->{testNaturalLoot(p,mode.equals("trial_fortress")?"fortress":"bastion_remnant");}
            case "fatal_nether_prepare","fatal_invalid_prepare"->{
                clearMobs(p);DemonThrone.removeSeat(p);s.flag("demon.lord");s.flag("demon.sovereign");s.progress("demon.protection",0);s.progress("demon.tribute_cd",100);s.progress("demon.throne_cd",0);s.progress("demon.return_pending",0);
                if(mode.equals("fatal_nether_prepare"))arena(p);else{var w=p.getServer().getOverworld();platform(w,BASE,8);p.teleport(w,128.5,161,.5,0,0);nether(p).setBlockState(BASE,Blocks.STONE.getDefaultState(),2);}
                p.setHealth(20);p.timeUntilRegen=0;
                s.progress("demon.domain_cd",0);s.progress("demon.domain_active",0);DemonDomain.activate(p);
            }
            case "fatal_dead_check"->{check("Invalid throne or Nether death follows normal death flow",!p.isAlive()&&s.progress("demon.return_pending")==0&&s.progress("demon.throne_cd")==0);check("Actual player death ends active domain",s.progress("demon.domain_active")==0);check("Dead player cannot issue army commands",!DemonArmy.command(p,0).success());}
            case "respawn"->{if(!p.isAlive())p.networkHandler.onClientStatus(new net.minecraft.network.packet.c2s.play.ClientStatusC2SPacket(net.minecraft.network.packet.c2s.play.ClientStatusC2SPacket.Mode.PERFORM_RESPAWN));}
            case "water_prepare"->{
                clearMobs(p);DemonDomain.end(p);arena(p);s.flag("demon.pandemonium");s.flag("demon.netherforged");DemonConfig.INSTANCE.multiplayerTerrainChanges=true;
                // Existing water, placed while no domain is active, stresses actual incremental writes.
                for(int x=-20;x<=20;x++)for(int z=-20;z<=20;z++)for(int y=1;y<=5;y++)p.getWorld().setBlockState(BASE.add(x,y,z),Blocks.WATER.getDefaultState(),2);
                check("Water stress fixture contains 8405 existing sources",countWater(p)==8405);
                s.progress("demon.domain_cd",0);DemonDomain.activate(p);TICKS.clear();navNanos=0;navCalls=0;benchmarking=true;s.flag("demon.benchmark_active");
            }
            case "water_check"->{
                check("Incremental domain evaporates entire 8405-block pool",countWater(p)==0);
                benchmarking=false;s.unflag("demon.benchmark_active");writeBenchmark("demon-water-benchmark.json",8405);DemonDomain.end(p);
            }
            case "throne"->{
                clearMobs(p);DemonDomain.end(p);arena(p);s.flag("demon.sovereign");s.flag("demon.army");p.changeGameMode(GameMode.SURVIVAL);
                for(int r=0;r<4;r++){BlockPos origin=BASE.add(r*8,0,0);p.getServerWorld().getChunk(origin.getX()>>4,origin.getZ()>>4);placeThrone(p.getServerWorld(),origin,r);check("Exact approved throne rotation "+r,DemonThrone.validate(p.getServerWorld(),origin,r));p.getWorld().setBlockState(origin,Blocks.STONE.getDefaultState(),2);check("Wrong material rejected rotation "+r,!DemonThrone.validate(p.getServerWorld(),origin,r));p.getWorld().setBlockState(origin,Blocks.NETHERITE_BLOCK.getDefaultState(),2);}
                p.getWorld().setBlockState(BASE.add(1,1,1),Blocks.NETHERITE_BLOCK.getDefaultState(),2);check("Seat obstruction rejected",!DemonThrone.validate(p.getServerWorld(),BASE,0));p.getWorld().setBlockState(BASE.add(1,1,1),Blocks.AIR.getDefaultState(),2);
                DemonWorldState.get(p.getServer()).thrones.remove(p.getUuid());p.changeGameMode(GameMode.ADVENTURE);check("Adventure cannot bypass Survival-only throne binding",!DemonThrone.bind(p,BASE.add(1,2,2)));p.changeGameMode(GameMode.SURVIVAL);check("Survival Beacon binding succeeds",DemonThrone.bind(p,BASE.add(1,2,2)));
                check("Binding unlocks Lord and seats real player",s.hasFlag("demon.lord")&&p.getVehicle() instanceof org.marj4n.smooth_classes.entity.DemonSeatEntity);
                p.stopRiding();record("Dismount diagnostic",true,"pos="+p.getPos()+" box="+p.getBoundingBox()+" pose="+p.getPose());check("Standing does not trap player in Netherite",p.getServerWorld().isSpaceEmpty(p,p.getBoundingBox()));
                DemonThrone.bind(p,BASE.add(8+1,2,2));check("Second intact throne cannot replace active binding",DemonWorldState.get(p.getServer()).thrones.get(p.getUuid()).origin().equals(BASE));
                p.stopRiding();s.progress("demon.decay",6000);DemonBiology.tick(p,s);check("Lord removes Netherbound permanently",s.progress("demon.decay")==0);check("Lord retains Infernal Diet",DemonDiet.applies(p));
            }
            case "seat_prepare","seat_prepare_0","seat_prepare_1","seat_prepare_2","seat_prepare_3"->{
                check("Seat fixture requires an actual Demon Lord",DemonBodyGeometry.applies(p));
                int rotation=mode.equals("seat_prepare")?0:Integer.parseInt(mode.substring(mode.length()-1));
                DemonThrone.removeSeat(p);arena(p);placeThrone(p.getServerWorld(),BASE,rotation);
                s.flag("demon.sovereign");s.flag("demon.army");p.changeGameMode(GameMode.SURVIVAL);
                check("Seat fixture uses Survival binding",p.interactionManager.getGameMode()==GameMode.SURVIVAL);
                int[] beacon=DemonRules.rotate(1,2,rotation);
                check("Seat fixture blueprint rotation "+rotation,DemonThrone.validate(p.getServerWorld(),BASE,rotation));
                check("Survival bind interaction accepted rotation "+rotation,DemonThrone.bind(p,BASE.add(beacon[0],2,beacon[1])));
                check("Bind created real passenger relation rotation "+rotation,p.getVehicle() instanceof org.marj4n.smooth_classes.entity.DemonSeatEntity);
                var throne=new DemonWorldState.Throne(p.getUuid(),BASE,rotation);
                SEAT_OBSERVATIONS.put(p.getUuid(),new SeatObservation(p.getVehicle().getUuid(),DemonThrone.seat(throne),p.getServerWorld().getTime(),rotation));
            }
            case "seat_hold_check"->{
                var observed=Objects.requireNonNull(SEAT_OBSERVATIONS.get(p.getUuid()),"Run seat_prepare first");
                record("Seat measured rotation "+observed.rotation(),true,"mode="+p.interactionManager.getGameMode()+",elapsedTicks="+(p.getServerWorld().getTime()-observed.tick())+",vehicle="+p.getVehicle()+",pos="+p.getPos()+",box="+p.getBoundingBox());
                check("Seat held at least40 actual server ticks rotation "+observed.rotation(),p.getServerWorld().getTime()-observed.tick()>=40);
                check("Same real seat survives ticks rotation "+observed.rotation(),p.getVehicle() instanceof org.marj4n.smooth_classes.entity.DemonSeatEntity&&p.getVehicle().getUuid().equals(observed.vehicle()));
                check("Seat passenger stays at intended position rotation "+observed.rotation(),p.getPos().squaredDistanceTo(observed.position())<.000001);
                check("Seat feet above foundation rotation "+observed.rotation(),p.getY()>=BASE.getY()+1);
                check("Seat body is collision free rotation "+observed.rotation(),p.getServerWorld().isSpaceEmpty(p,p.getBoundingBox()));
                check("Mounted Lord collision height rotation "+observed.rotation(),Math.abs((p.getBoundingBox().maxY-p.getBoundingBox().minY)-DemonBodyGeometry.THRONE_HEIGHT)<.001);
                check("Mounted eyes stay inside collision body rotation "+observed.rotation(),p.getEyeHeight(p.getPose())>0&&p.getEyeHeight(p.getPose())<DemonBodyGeometry.THRONE_HEIGHT);
            }
            case "seat_dismount"->{check("Dismount starts mounted",p.getVehicle() instanceof org.marj4n.smooth_classes.entity.DemonSeatEntity);p.stopRiding();}
            case "seat_dismount_check"->{
                check("Dismounted player has no vehicle",!p.hasVehicle());
                record("Dismount measured after actual ticks",true,"pos="+p.getPos()+",box="+p.getBoundingBox()+",pose="+p.getPose());
                check("Dismounted feet above foundation",p.getY()>=BASE.getY()+1);
                check("Dismounted Lord collision is clear",p.getServerWorld().isSpaceEmpty(p,p.getBoundingBox()));
                check("Standing Lord height restored after dismount",Math.abs((p.getBoundingBox().maxY-p.getBoundingBox().minY)-DemonBodyGeometry.HEIGHT)<.001);
            }
            case "seat_creative_rejected"->{
                DemonThrone.removeSeat(p);arena(p);placeThrone(p.getServerWorld(),BASE,0);s.flag("demon.sovereign");s.flag("demon.army");p.changeGameMode(GameMode.CREATIVE);
                check("Creative throne binding is rejected",!DemonThrone.bind(p,BASE.add(1,2,2)));check("Creative rejection leaves no mount",!p.hasVehicle());p.changeGameMode(GameMode.SURVIVAL);
            }
            case "seat_obstructed_rejected"->{
                DemonThrone.removeSeat(p);arena(p);placeThrone(p.getServerWorld(),BASE,0);p.changeGameMode(GameMode.SURVIVAL);p.getServerWorld().setBlockState(BASE.add(1,1,1),Blocks.STONE.getDefaultState(),2);
                check("Obstructed seat refuses binding",!DemonThrone.bind(p,BASE.add(1,2,2)));check("Obstruction rejection leaves no mount",!p.hasVehicle());
            }
            case "seat_break"->{
                var observed=Objects.requireNonNull(SEAT_OBSERVATIONS.get(p.getUuid()));check("Break fixture starts mounted",p.hasVehicle());
                int[] cell=DemonRules.rotate(0,1,observed.rotation());p.getServerWorld().setBlockState(BASE.add(cell[0],1,cell[1]),Blocks.AIR.getDefaultState(),2);
            }
            case "seat_check"->{check("Seated player stays above foundation after actual ticks",p.getVehicle() instanceof org.marj4n.smooth_classes.entity.DemonSeatEntity&&p.getY()>=161&&p.getServerWorld().isSpaceEmpty(p,p.getBoundingBox()));p.stopRiding();check("Dismount has safe logical feet",p.getY()>=161&&p.getServerWorld().isSpaceEmpty(p,p.getBoundingBox()));}
            case "tribute_prepare","fatal_prepare"->{
                if(mode.equals("fatal_prepare")){
                    var bound=DemonWorldState.get(p.getServer()).thrones.get(p.getUuid());
                    if(bound==null){bound=new DemonWorldState.Throne(p.getUuid(),BASE,0);DemonWorldState.get(p.getServer()).thrones.put(p.getUuid(),bound);}
                    placeThrone(nether(p),bound.origin(),bound.rotation());
                    check("Fatal test fixture has an intact bound throne",DemonThrone.returnReady(p));
                }
                clearMobs(p);DemonThrone.removeSeat(p);s.flag("demon.lord");s.flag("demon.sovereign");s.progress("demon.return_pending",0);s.progress("demon.throne_cd",0);s.progress("demon.tribute_cd",mode.equals("tribute_prepare")?0:100);s.progress("demon.protection",0);
                var w=p.getServer().getOverworld();platform(w,BASE,8);p.teleport(w,BASE.getX()+.5,BASE.getY()+1,BASE.getZ()+.5,0,0);p.setHealth(20);p.timeUntilRegen=0;
                p.getInventory().setStack(0,new ItemStack(Items.DIAMOND,7));inventoryBefore=p.getInventory().writeNbt(new NbtList()).toString();xpBefore=p.totalExperience;
                if(mode.equals("tribute_prepare"))mob(p,EntityType.ZOMBIFIED_PIGLIN,2,0);
            }
            case "tribute_check"->{
                check("Last Tribute rescues without dimension transfer",p.isAlive()&&p.getWorld().getRegistryKey()==World.OVERWORLD&&p.getHealth()==6);
                check("Last Tribute starts only its own cooldown",s.progress("demon.tribute_cd")>3500&&s.progress("demon.throne_cd")==0);
                int living=0;for(var e:p.getServerWorld().iterateEntities())if(e.getCommandTags().contains("sc_demon_test")&&e.isAlive())living++;check("Sacrifice actually dies",living==0);
            }
            case "fatal_check"->{
                check("Fatal damage returns living player to Nether throne",p.isAlive()&&p.getWorld().getRegistryKey()==World.NETHER&&p.getVehicle() instanceof org.marj4n.smooth_classes.entity.DemonSeatEntity);
                check("Throne return has exactly three hearts",p.getHealth()==6);
                check("Throne cooldown is fifteen gameplay minutes",s.progress("demon.throne_cd")>17900&&s.progress("demon.throne_cd")<=18000);
                check("Fatal return preserves inventory and XP",inventoryBefore.equals(p.getInventory().writeNbt(new NbtList()).toString())&&xpBefore==p.totalExperience);
            }
            case "army_prepare"->{
                clearMobs(p);arena(p);s.flag("demon.sovereign");s.flag("demon.army");platform(p.getServerWorld(),BASE,56);
                for(int i=0;i<500;i++)mob(p,EntityType.ZOMBIFIED_PIGLIN,(i%25)*3-36,(i/25)*3-27);
            }
            case "army_start"->{
                var order=DemonArmy.command(p,1);check("Actual 500-entity army has no cap",order.success()&&order.message().contains("500"));
                TICKS.clear();navCalls=0;navNanos=0;benchmarking=true;s.flag("demon.benchmark_active");
            }
            case "army_finish"->{
                benchmarking=false;s.unflag("demon.benchmark_active");List<Long> samples=new ArrayList<>(TICKS);Collections.sort(samples);
                double mean=samples.stream().mapToLong(Long::longValue).average().orElse(0)/1e6;
                double p95=samples.isEmpty()?0:samples.get((int)Math.floor((samples.size()-1)*.95))/1e6;
                Map<String,Object> benchmark=new LinkedHashMap<>();benchmark.put("population",500);benchmark.put("ticks",samples.size());benchmark.put("mean_tick_ms",mean);benchmark.put("p95_tick_ms",p95);benchmark.put("estimated_tps_ceiling",Math.min(20,1000/Math.max(mean,.01)));benchmark.put("navigation_calls",navCalls);benchmark.put("navigation_cpu_ms",navNanos/1e6);benchmark.put("used_heap_bytes",Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory());benchmark.put("client_fps","not measured by server harness");
                try{Files.writeString(Path.of("demon-army-benchmark.json"),new GsonBuilder().setPrettyPrinting().create().toJson(benchmark));}catch(java.io.IOException ex){throw new IllegalStateException(ex);}
                check("Army benchmark sampled real server ticks",samples.size()>=200);clearMobs(p);
            }
            case "portal_prepare"->{
                clearMobs(p);DemonThrone.removeSeat(p);DemonDomain.end(p);s.flag("demon.lord");s.flag("demon.sovereign");s.flag("demon.army");
                ServerWorld over=p.getServer().getOverworld(),net=nether(p);
                BlockPos to=new BlockPos(512,161,128),from=new BlockPos(64,161,16);
                platform(over,to.down(),8);platform(net,from.down(),8);portal(over,to);portal(net,from);
                p.teleport(net,from.getX()+.5,from.getY(),from.getZ()+2.5,0,0);
                mob(p,EntityType.ZOMBIFIED_PIGLIN,0,1);mob(p,EntityType.ZOMBIFIED_PIGLIN,1,1);mob(p,EntityType.ZOMBIFIED_PIGLIN,-1,1);
                var ghast=EntityType.GHAST.create(net);check("Small portal rejects Ghast hitbox",ghast!=null&&!DemonPortals.fits(ghast,new DemonPortals.Portal(from,2,3,Direction.Axis.X)));
                check("Valid rectangular portal detected",DemonPortals.rectangle(net,from)!=null&&DemonPortals.rectangle(over,to)!=null);
                p.teleport(over,to.getX()+.5,to.getY(),to.getZ()+2.5,180,0);
            }
            case "portal_send"->{var nw=nether(p);var from=new BlockPos(64,161,16);record("Portal source diagnostic",true,"loaded="+org.marj4n.smooth_classes.util.LoadedChunks.contains(nw, from)+" rectangle="+DemonPortals.rectangle(nw,from)+" POIs="+nw.getPointOfInterestStorage().getInSquare(e->e.matchesKey(net.minecraft.world.poi.PointOfInterestTypes.NETHER_PORTAL),from,16,net.minecraft.world.poi.PointOfInterestStorage.OccupationStatus.ANY).count());var result=DemonPortals.backup(p);record("Hellgate diagnostic",true,result.message());check("Hellgate accepts an existing coordinate-linked portal pair",result.success());}
            case "portal_check"->{
                int source=0,destination=0;Set<UUID> ids=new HashSet<>();
                for(var world:p.getServer().getWorlds())for(var e:world.iterateEntities())if(e instanceof MobEntity mob&&e.getCommandTags().contains("sc_demon_test")&&e.isAlive()){
                    check("Transferred UUID is unique",ids.add(e.getUuid()));
                    if(world.getRegistryKey()==World.NETHER)source++;else if(world.getRegistryKey()==World.OVERWORLD){destination++;check("Transferred mob retains commander",p.getUuid().equals(DemonEntityData.of(mob).commander));check("Portal exit has no solid clipping",world.isSpaceEmpty(mob,mob.getBoundingBox()));}
                }
                check("Portal transfer does not duplicate or leave source copies",source==0&&destination==3);clearMobs(p);
            }
            case "cleanup"->{benchmarking=false;clearMobs(p);DemonDomain.end(p);DemonThrone.removeSeat(p);}
            default->throw new IllegalArgumentException("Unknown test scenario "+mode);
        }
    }
    private static DemonEntityData roundTrip(LivingEntity e){var result=new DemonEntityData();result.read(DemonEntityData.of(e).write());return result;}
    private static void testNaturalLoot(ServerPlayerEntity p,String name){
        var w=nether(p);var registry=w.getRegistryManager().get(net.minecraft.registry.RegistryKeys.STRUCTURE);var key=net.minecraft.registry.RegistryKey.of(net.minecraft.registry.RegistryKeys.STRUCTURE,net.minecraft.util.Identifier.of("minecraft",name));
        var entry=registry.entryOf(key);var found=w.getChunkManager().getChunkGenerator().locateStructure(w,net.minecraft.registry.entry.RegistryEntryList.of(entry),BlockPos.ORIGIN,64,false);
        check("Natural generated structure located: "+name,found!=null);var start=w.getChunk(found.getFirst()).getStructureStart(entry.value());
        check("Real structure start exists: "+name,start!=null&&start.hasChildren());var bounds=start.getBoundingBox();
        for(int x=bounds.getMinX()>>4;x<=bounds.getMaxX()>>4;x++)for(int z=bounds.getMinZ()>>4;z<=bounds.getMaxZ()>>4;z++){
            var chunk=w.getChunk(x,z);
            for(var entity:new ArrayList<>(chunk.getBlockEntities().values()))if(entity instanceof net.minecraft.block.entity.ChestBlockEntity chest&&bounds.contains(chest.getPos())&&chest.createNbt().contains("LootTable")){
                String expected=name.equals("fortress")?"fortress":"bastion";var s=OriginRuntime.state(p);s.flag("demon.world_ender");s.unflag("demon.trial."+expected);s.progress("demon.protection",600);p.stopRiding();p.teleport(w,chest.getPos().getX()+.5,chest.getPos().getY()+1,chest.getPos().getZ()+.5,0,0);p.fallDistance=0;
                p.openHandledScreen(chest);var handler=p.currentScreenHandler;boolean moved=false;
                for(int slot=0;slot<handler.slots.size();slot++)if(handler.slots.get(slot).inventory==chest&&handler.slots.get(slot).hasStack()){
                    handler.onSlotClick(slot,0,net.minecraft.screen.slot.SlotActionType.QUICK_MOVE,p);moved=true;break;
                }
                p.closeHandledScreen();check("Natural chest has real loot to transfer: "+name,moved);check("Real container transfer completes trial: "+name,s.hasFlag("demon.trial."+expected));
                s.unflag("demon.trial."+expected);BlockPos replaced=chest.getPos();w.setBlockState(replaced,Blocks.AIR.getDefaultState(),3);w.setBlockState(replaced,Blocks.CHEST.getDefaultState(),3);
                var replacement=(net.minecraft.block.entity.ChestBlockEntity)w.getBlockEntity(replaced);replacement.setStack(0,new ItemStack(Items.DIAMOND));p.openHandledScreen(replacement);p.currentScreenHandler.onSlotClick(0,0,net.minecraft.screen.slot.SlotActionType.QUICK_MOVE,p);p.closeHandledScreen();
                check("Player replacement chest inside actual structure cannot grant trial: "+name,!s.hasFlag("demon.trial."+expected));s.flag("demon.trial."+expected);return;
            }
        }
        throw new AssertionError("Located structure has no unlooted chest: "+name);
    }
    private static int countWater(ServerPlayerEntity p){int count=0;for(int x=-20;x<=20;x++)for(int z=-20;z<=20;z++)for(int y=1;y<=5;y++)if(p.getWorld().getFluidState(BASE.add(x,y,z)).isIn(net.minecraft.registry.tag.FluidTags.WATER))count++;return count;}
    private static void writeBenchmark(String filename,int population){
        List<Long> samples=new ArrayList<>(TICKS);Collections.sort(samples);Map<String,Object> report=new LinkedHashMap<>();report.put("work_items",population);report.put("ticks",samples.size());report.put("mean_tick_ms",samples.stream().mapToLong(Long::longValue).average().orElse(0)/1e6);report.put("p95_tick_ms",samples.isEmpty()?0:samples.get((int)((samples.size()-1)*.95))/1e6);report.put("used_heap_bytes",Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory());
        try{Files.writeString(Path.of(filename),new GsonBuilder().setPrettyPrinting().create().toJson(report));}catch(java.io.IOException ex){throw new IllegalStateException(ex);}
    }
    private static void portal(ServerWorld world,BlockPos bottom){
        for(int x=-1;x<=2;x++)for(int y=-1;y<=3;y++){
            boolean edge=x==-1||x==2||y==-1||y==3;
            world.setBlockState(bottom.add(x,y,0),edge?Blocks.OBSIDIAN.getDefaultState():Blocks.NETHER_PORTAL.getDefaultState().with(NetherPortalBlock.AXIS,Direction.Axis.X),Block.NOTIFY_LISTENERS|Block.FORCE_STATE);
        }
    }
}
