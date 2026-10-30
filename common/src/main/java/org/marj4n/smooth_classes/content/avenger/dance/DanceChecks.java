package org.marj4n.smooth_classes.content.avenger.dance;
import java.util.*;
import com.google.gson.Gson;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.JukeboxBlockEntity;
import net.minecraft.entity.*;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.*;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import org.marj4n.smooth_classes.content.avenger.runtime.*;
import org.marj4n.smooth_classes.integration.PuffishSkillsIntegration;

/** Explicit opt-in disposable-world fixture. Never registered on ordinary launches. */
public final class DanceChecks {
    public static final boolean ENABLED=Boolean.getBoolean("smooth_classes.danceLab");
    public static final Identifier ACTION=Identifier.of("smooth_classes","avenger_dance_lab");
    private static final Gson JSON=new Gson();
    public static void register(){if(!ENABLED)return;CommandRegistrationCallback.EVENT.register((d,r,e)->d.register(CommandManager.literal("dance_lab").requires(s->s.hasPermissionLevel(2)).then(CommandManager.argument("args",StringArgumentType.greedyString()).executes(ctx->{try{var result=run(ctx.getSource(),StringArgumentType.getString(ctx,"args").split(" +"));ctx.getSource().sendFeedback(()->Text.literal(JSON.toJson(result)),false);return 1;}catch(Exception|AssertionError ex){ctx.getSource().sendError(Text.literal("DANCE_TEST FAILED "+ex));return 0;}}))));}
    private static void require(boolean b,String message){if(!b)throw new AssertionError(message);}
    private static Object run(ServerCommandSource src,String[] a)throws Exception{
        var server=src.getServer();require(Set.of("avenger-dance-lab","avenger-dance-v2-lab").contains(server.getSaveProperties().getLevelName()),"Disposable dance world only");var w=server.getOverworld();String mode=a[0];
        if(mode.equals("setup")){var p=server.getPlayerManager().getPlayer(a[1]);require(p!=null,"owner online");PuffishSkillsIntegration.category(Identifier.of("smooth_classes","avenger")).orElseThrow().unlock(p);PuffishSkillsIntegration.invalidateRuntimeCache(p);AvengerDeathListState.get(server).awaken(p.getUuid());p.changeGameMode(net.minecraft.world.GameMode.SPECTATOR);p.teleport(w,0.5,-58,17.5,180,10);return Map.of("setup",p.getUuid().toString());}
        if(mode.equals("summon")){
            var p=server.getPlayerManager().getPlayer(a[1]);String id="minecraft:"+a[2];int count=Integer.parseInt(a[3]);boolean stay=a.length<5||!a[4].equals("follow");var recipe=AvengerSummonRecipes.recipe(id);require(recipe!=null&&count<=300,"supported bounded recipe");var state=AvengerDeathListState.get(server);var added=new ArrayList<String>();
            // Calls actual shared summon/cost path. Seeded test souls/materials/charges are explicitly fixture-only.
            var method=AvengerReworkRuntime.class.getDeclaredMethod("summonFromHands",ServerPlayerEntity.class,boolean.class);method.setAccessible(true);
            for(int i=0;i<count;i++){
                p.teleport(w,0.5,-60,8.5,0,-70);state.recordKill(p.getUuid(),id);state.refreshCharges(p.getUuid(),System.currentTimeMillis()+1_000_000_000L);
                p.setStackInHand(Hand.MAIN_HAND,new ItemStack(org.marj4n.smooth_classes.platform.PlatformRegistries.ITEM.get(org.marj4n.smooth_classes.util.ResourceIds.parse(recipe.mainItemId())),2));p.setStackInHand(Hand.OFF_HAND,recipe.offhandItemId()==null?ItemStack.EMPTY:new ItemStack(org.marj4n.smooth_classes.platform.PlatformRegistries.ITEM.get(org.marj4n.smooth_classes.util.ResourceIds.parse(recipe.offhandItemId())),2));
                Set<UUID> before=new HashSet<>();for(var e:snapshot(w))before.add(e.getUuid());Object result=method.invoke(null,p,stay);
                MobEntity mob=null;for(var e:snapshot(w))if(!before.contains(e.getUuid())&&e instanceof MobEntity m&&org.marj4n.smooth_classes.platform.PlatformRegistries.ENTITY_TYPE.getId(e.getType()).toString().equals(id)){mob=m;break;}require(mob!=null,"actual summon failed "+result);
                int n=total(w);double x=(n%20-9.5)*.6,z=(n/20-7)*.6;mob.refreshPositionAndAngles(x,-60,z,180,0);
                if(stay){ // Fixture repositions the existing STAY post, keeping production AI logic untouched.
                    for(String tag:new HashSet<>(mob.getCommandTags()))if(tag.startsWith("smooth_classes_avenger_stay_"))mob.removeScoreboardTag(tag);
                    mob.addCommandTag("smooth_classes_avenger_stay_"+BlockPos.ofFloored(x,-60,z).asLong());mob.addCommandTag("smooth_classes_avenger_stay_facing_180.0");
                }
                mob.addCommandTag("dance_lab");mob.setInvulnerable(true);mob.setSilent(true);added.add(mob.getUuid().toString());require(DanceBindings.get(server).get(mob.getUuid())!=null,"no issued binding");
            }
            p.teleport(w,.5,-57,18.5,180,8);return Map.of("summoned",added,"count",added.size(),"fixture_seeded_resources",true,"production_spawn_cost_path",true);
        }
        if(mode.equals("box")){BlockPos pos=new BlockPos(Integer.parseInt(a[2]),-60,Integer.parseInt(a[3]));if(a[1].equals("destroy")){w.breakBlock(pos,false);return Map.of("destroyed",pos.toString());}w.setBlockState(pos,Blocks.JUKEBOX.getDefaultState());var be=(JukeboxBlockEntity)w.getBlockEntity(pos);if(a[1].equals("stop")){be.removeStack(0);AvengerDanceRuntime.observe(be);}else{be.setStack(0,new ItemStack(Items.MUSIC_DISC_CAT));be.startPlaying();}return Map.of("playing",be.isPlayingRecord(),"at",pos.toString());}
        if(mode.equals("status")){var rows=new ArrayList<Object>();for(var e:snapshot(w))if(e instanceof MobEntity m&&m.getCommandTags().contains("dance_lab")){var b=DanceBindings.get(server).get(m.getUuid());rows.add(Map.of("uuid",m.getUuid().toString(),"id",m.getId(),"type",org.marj4n.smooth_classes.platform.PlatformRegistries.ENTITY_TYPE.getId(m.getType()).toString(),"owner",String.valueOf(AvengerReworkRuntime.summonOwnerUuid(m)),"authorized",AvengerDanceRuntime.authorized(m),"combat",AvengerDanceRuntime.combat(m),"stay",AvengerReworkRuntime.isStaying(m),"noAI",m.isAiDisabled(),"binding",b!=null,"position",List.of(m.getX(),m.getY(),m.getZ())));}
            var dances=new ArrayList<Object>();for(var d:AvengerDanceRuntime.states(w))dances.add(Map.of("uuid",d.uuid().toString(),"profile",d.profile(),"start",d.song().start(),"end",d.song().end(),"box",d.song().pos().asLong()));var result=new LinkedHashMap<String,Object>();result.put("entities",rows);result.put("dances",dances);result.put("active",dances.size());result.put("tick",w.getTime());result.put("module_last_ms",AvengerDanceRuntime.tickNanos/1e6);result.put("module_max_ms",AvengerDanceRuntime.maxTickNanos/1e6);result.put("server_tick_average_ms",java.util.Arrays.stream(server.lastTickLengths).average().orElse(0)/1e6);result.put("packets",AvengerDanceRuntime.packets);result.put("packet_bytes",AvengerDanceRuntime.packetBytes);return result;
        }
        if(mode.equals("inspect")){
            var m=(MobEntity)w.getEntity(UUID.fromString(a[1]));var result=new LinkedHashMap<String,Object>();result.put("uuid",m.getUuid().toString());result.put("pose",m.getPose().toString());result.put("target",String.valueOf(m.getTarget()));result.put("noAI",m.isAiDisabled());result.put("authorized",AvengerDanceRuntime.authorized(m));result.put("busy",AvengerDanceRuntime.combat(m));result.put("attack_flag",m.isAttacking());result.put("navigation_idle",m.getNavigation().isIdle());result.put("hurt_time",m.hurtTime);result.put("fall_distance",m.fallDistance);result.put("on_fire",m.isOnFire());result.put("soul_busy",AvengerSoulAnimationRuntime.busy(m));result.put("width",m.getWidth());result.put("height",m.getHeight());
            if(m instanceof net.minecraft.entity.boss.WitherEntity b){result.put("invul",b.getInvulnerableTimer());result.put("head_targets",List.of(b.getTrackedEntityId(0),b.getTrackedEntityId(1),b.getTrackedEntityId(2)));result.put("charged",b.shouldRenderOverlay());}
            if(m instanceof net.minecraft.entity.mob.WardenEntity b)result.put("anger",b.getAnger());
            return result;
        }
        if(mode.equals("pose")){var m=(MobEntity)w.getEntity(UUID.fromString(a[1]));m.setPose(EntityPose.valueOf(a[2]));return Map.of("test_initiated_tracked_pose",m.getPose().toString());}
        if(mode.equals("invul")){var m=(net.minecraft.entity.boss.WitherEntity)w.getEntity(UUID.fromString(a[1]));m.setInvulTimer(Integer.parseInt(a[2]));return Map.of("test_initiated_invul",m.getInvulnerableTimer());}
        if(mode.equals("headtarget")){var m=(net.minecraft.entity.boss.WitherEntity)w.getEntity(UUID.fromString(a[1]));int head=Integer.parseInt(a[2]);require(head>=0&&head<3,"valid head index");m.setTrackedEntityId(head,a[3].equals("clear")?0:w.getEntity(UUID.fromString(a[3])).getId());return Map.of("test_initiated_head_target",m.getTrackedEntityId(head));}
        if(mode.equals("shoot")){var m=(net.minecraft.entity.boss.WitherEntity)w.getEntity(UUID.fromString(a[1]));var target=(LivingEntity)w.getEntity(UUID.fromString(a[2]));m.attack(target,0);return Map.of("test_initiated_vanilla_wither_attack",true);}
        if(mode.equals("anger")){var m=(net.minecraft.entity.mob.WardenEntity)w.getEntity(UUID.fromString(a[1]));var target=(LivingEntity)w.getEntity(UUID.fromString(a[2]));m.increaseAngerAt(target,150,true);m.updateAttackTarget(target);return Map.of("test_initiated_vanilla_anger",true);}
        if(mode.equals("nav")){var m=(MobEntity)w.getEntity(UUID.fromString(a[1]));return Map.of("idle",m.getNavigation().isIdle(),"busy",AvengerDanceRuntime.combat(m),"noAI",m.isAiDisabled(),"position",List.of(m.getX(),m.getY(),m.getZ()));}
        if(mode.equals("legacy")){var m=(MobEntity)w.getEntity(UUID.fromString(a[1]));DanceBindings.get(server).forget(m.getUuid());return Map.of("legacy_authorized",AvengerDanceRuntime.authorized(m),"proof_reissued",DanceBindings.get(server).get(m.getUuid())!=null);}
        if(mode.equals("clearwild")){int n=0;for(var e:snapshot(w))if(e.getCommandTags().contains("dance_lab")&&DanceBindings.get(server).get(e.getUuid())==null){e.discard();n++;}return Map.of("removed_test_wild",n);}
        if(mode.equals("order")){var p=server.getPlayerManager().getPlayer(a[1]);var m=(MobEntity)w.getEntity(UUID.fromString(a[2]));p.teleport(w,m.getX(),m.getY(),m.getZ()+5,180,0);double dy=m.getBodyY(.6)-p.getEyeY();p.setPitch((float)-Math.toDegrees(Math.atan2(dy,5)));var result=AvengerReworkRuntime.selectSummonOrder(p,Integer.parseInt(a[3]));return Map.of("result",result.toString(),"stay",AvengerReworkRuntime.isStaying(m),"guard",AvengerReworkRuntime.isGuarding(m),"noAI",m.isAiDisabled(),"owner",String.valueOf(AvengerReworkRuntime.summonOwnerUuid(m)));}
        if(mode.equals("clear")){int n=0;for(var e:snapshot(w))if(e.getCommandTags().contains("dance_lab")){e.discard();n++;}return Map.of("removed",n);}
        if(mode.equals("client")){var p=server.getPlayerManager().getPlayer(a[1]);require(p!=null,"client online");var b=PacketByteBufs.create();b.writeString(String.join(" ",Arrays.copyOfRange(a,2,a.length)));ServerPlayNetworking.send(p,ACTION,b);return Map.of("sent",a[1]);}
        if(mode.equals("target")){var m=(MobEntity)w.getEntity(UUID.fromString(a[1]));if(a[2].equals("clear")){m.setTarget(null);m.setAttacker(null);m.setAttacking(false);}else if(a[2].equals("wild")){var target=EntityType.ZOMBIE.create(w);target.refreshPositionAndAngles(10,-60,10,0,0);target.setAiDisabled(true);target.addCommandTag("dance_lab");w.spawnEntity(target);m.setTarget(target);m.setAttacker(target);}else m.setTarget(server.getPlayerManager().getPlayer(a[2]));return Map.of("target",String.valueOf(m.getTarget()));}
        if(mode.equals("move")){var m=(MobEntity)w.getEntity(UUID.fromString(a[1]));double x=Double.parseDouble(a[2]),z=Double.parseDouble(a[3]);m.refreshPositionAndAngles(x,-60,z,m.getYaw(),0);for(String t:new HashSet<>(m.getCommandTags()))if(t.startsWith("smooth_classes_avenger_stay_")&&!t.startsWith("smooth_classes_avenger_stay_facing_"))m.removeScoreboardTag(t);if(m.isAiDisabled())m.addCommandTag("smooth_classes_avenger_stay_"+BlockPos.ofFloored(x,-60,z).asLong());return Map.of("moved",m.getUuid().toString());}
        if(mode.equals("wild")){var type=org.marj4n.smooth_classes.platform.PlatformRegistries.ENTITY_TYPE.get(Identifier.of("minecraft",a[1]));var e=(MobEntity)type.create(w);e.refreshPositionAndAngles(3,-60,3,180,0);e.setAiDisabled(true);e.setPersistent();e.addCommandTag("dance_lab");if(a.length>2){var owner=server.getPlayerManager().getPlayer(a[2]);e.addCommandTag("smooth_classes_avenger_summon");e.addCommandTag("smooth_classes_avenger_owner_"+owner.getUuid());}w.spawnEntity(e);return Map.of("wild",e.getUuid().toString(),"authorized",AvengerDanceRuntime.authorized(e));}
        throw new IllegalArgumentException("Unknown lab mode");
    }
    private static List<Entity> snapshot(net.minecraft.server.world.ServerWorld w){var list=new ArrayList<Entity>();for(var entity:w.iterateEntities())if(entity!=null)list.add(entity);return list;}
    private static int total(net.minecraft.server.world.ServerWorld w){int n=0;for(var e:snapshot(w))if(e.getCommandTags().contains("dance_lab"))n++;return n;}
    private DanceChecks(){}
}
