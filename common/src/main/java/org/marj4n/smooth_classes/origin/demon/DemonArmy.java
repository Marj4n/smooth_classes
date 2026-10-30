package org.marj4n.smooth_classes.origin.demon;

import java.util.*;
import net.minecraft.entity.*;
import net.minecraft.entity.mob.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import org.marj4n.smooth_classes.origin.OriginAbilityDispatcher.Result;

/** No army-size cap. Only navigation work is budgeted, round-robin across loaded owned mobs. */
public final class DemonArmy {
    private static final DemonWorkQueue<UUID,MobEntity> LOADED=new DemonWorkQueue<>();
    private static final Set<MobEntity> ATTACHED=Collections.newSetFromMap(new WeakHashMap<>());
    private DemonArmy(){}
    public static void loaded(Entity e){if(e instanceof MobEntity m&&DemonEntityData.of(m).commander!=null){m.setPersistent();LOADED.put(e.getUuid(),m);if(ATTACHED.add(m))((DemonCommandGoal.Access)m).smooth_classes$demonGoals().add(0,new DemonCommandGoal(m));}}
    public static void unloaded(Entity e){if(e instanceof MobEntity m)LOADED.remove(e.getUuid(),m);}
    public static void clear(){LOADED.clear();ATTACHED.clear();}
    public static boolean available(MobEntity m,ServerPlayerEntity p){
        UUID owner=DemonEntityData.of(m).commander;
        return DemonMobIdentity.eligible(m)&&(owner==null||owner.equals(p.getUuid()))&&m.getWorld() instanceof ServerWorld w&&DemonProtection.allowCombat(p,w,m.getBlockPos());
    }
    public static Result command(ServerPlayerEntity p,int command){
        if(!p.isAlive()||p.isSpectator()||!DemonProgression.demon(p)||!DemonProgression.has(p,"army"))return Result.fail("A living Sovereign is required.");
        if(command<0||command>=DemonRules.COMMANDS.length)return Result.fail("Invalid Nether army command.");
        if(command==5&&p.getWorld().getRegistryKey()!=World.NETHER){
            if(!DemonProgression.has(p,"lord"))return Result.fail("Demon Lord is required to call troops across dimensions.");
            return DemonPortals.backup(p);
        }
        if(command!=5)DemonPortals.cancel(p.getUuid());
        LivingEntity target=command==2?DemonCurse.target(p,128):null;
        if(command==2&&target==null)return Result.fail("Mark a visible enemy within 128 blocks.");
        if(target instanceof MobEntity m&&p.getUuid().equals(DemonEntityData.of(m).commander))return Result.fail("Your army cannot attack its own forces.");
        BlockPos guard=p.getBlockPos();
        if(command==3){var hit=p.raycast(48,1F,false);if(hit.getType()!=net.minecraft.util.hit.HitResult.Type.MISS)guard=BlockPos.ofFloored(hit.getPos()).up();}
        int count=0;
        for(MobEntity m:p.getWorld().getEntitiesByClass(MobEntity.class,p.getBoundingBox().expand(128),m->m.squaredDistanceTo(p)<=16384&&available(m,p))){
            var d=DemonEntityData.of(m);d.commander=p.getUuid();d.command=command==5?0:command;d.attackTarget=target==null?null:target.getUuid();d.guard=guard;
            if(command!=2)m.setTarget(null);
            if(command==4){
                if(m instanceof net.minecraft.entity.mob.Angerable angerable)angerable.stopAnger();
                if(m.getBrain().hasMemoryModule(net.minecraft.entity.ai.brain.MemoryModuleType.ANGRY_AT))m.getBrain().forget(net.minecraft.entity.ai.brain.MemoryModuleType.ANGRY_AT);
                if(m.getBrain().hasMemoryModule(net.minecraft.entity.ai.brain.MemoryModuleType.ATTACK_TARGET))m.getBrain().forget(net.minecraft.entity.ai.brain.MemoryModuleType.ATTACK_TARGET);
            }
            loaded(m);count++;
        }
        return Result.ok(DemonRules.COMMANDS[command]+": "+count+" eligible Nether allies.");
    }
    public static void tick(MinecraftServer server){
        if(LOADED.size()==0)return;
        int budget=Math.min(DemonConfig.INSTANCE.armyUpdateBudget,LOADED.size());
        for(int i=0;i<budget;i++){
            MobEntity m=LOADED.next();if(m==null)return;if(m.isRemoved()||!m.isAlive()){LOADED.remove(m.getUuid());continue;}
            var d=DemonEntityData.of(m);long now=server.getTicks();if(now<d.scheduledAt)continue;
            d.scheduledAt=now+DemonConfig.INSTANCE.armyTickInterval;
            ServerPlayerEntity owner=server.getPlayerManager().getPlayer(d.commander);
            if(owner==null||!owner.isAlive()||!DemonProgression.demon(owner)||!DemonProgression.has(owner,"army")){m.setTarget(null);continue;}
            if(!available(m,owner))continue;
            if(m.getWorld()!=owner.getWorld())continue;
            advanceReachedNodes(m);
            LivingEntity target=null;
            if(d.command==2&&d.attackTarget!=null){var e=owner.getServerWorld().getEntity(d.attackTarget);if(e instanceof LivingEntity living&&validEnemy(owner,living))target=living;}
            if(d.command==3&&d.guard!=null){
                if(m.squaredDistanceTo(Vec3d.ofCenter(d.guard))<=144){
                    for(MobEntity hostile:m.getWorld().getEntitiesByClass(MobEntity.class,new Box(d.guard).expand(12),e->e.getTarget()==owner&&validEnemy(owner,e))){target=hostile;break;}
                }
            }
            if(target!=null){m.setTarget(target);if(m.getBrain().hasMemoryModule(net.minecraft.entity.ai.brain.MemoryModuleType.ATTACK_TARGET))m.getBrain().remember(net.minecraft.entity.ai.brain.MemoryModuleType.ATTACK_TARGET,target);if(m.squaredDistanceTo(target)>16)move(m,target.getPos());continue;}
            if(d.command==2){m.setTarget(null);d.attackTarget=null;d.command=1;}
            if(d.command!=2){m.setTarget(null);if(m.getBrain().hasMemoryModule(net.minecraft.entity.ai.brain.MemoryModuleType.ATTACK_TARGET))m.getBrain().forget(net.minecraft.entity.ai.brain.MemoryModuleType.ATTACK_TARGET);}
            // Hash-based rings separate ground/flying/large bodies; no identical destination pile.
            double angle=Math.floorMod(m.getUuid().hashCode(),360)*Math.PI/180;
            double radius=3+Math.floorMod(m.getId(),5)*2+Math.max(m.getWidth(),1);
            Vec3d goal=d.command==3&&d.guard!=null?Vec3d.ofCenter(d.guard):owner.getPos();
            goal=goal.add(Math.cos(angle)*radius,flying(m)?3+Math.floorMod(m.getId(),5):0,Math.sin(angle)*radius);
            if(m.squaredDistanceTo(goal)>9)move(m,goal);else if(d.command!=1)m.getNavigation().stop();
        }
    }
    private static void move(MobEntity m,Vec3d goal){
        long start=DemonRuntimeChecks.ENABLED?System.nanoTime():0;
        if(flying(m)){
            // Ghast keeps its own flight controller; never replace it with ground navigation.
            m.getMoveControl().moveTo(goal.x,goal.y,goal.z,.8);
        }else {
            // Respect short-range native pathfinders without changing their attributes.
            Vec3d delta=goal.subtract(m.getPos());double reach=Math.max(1,Math.min(12,m.getAttributeValue(net.minecraft.entity.attribute.EntityAttributes.GENERIC_FOLLOW_RANGE)*.6));
            if(delta.lengthSquared()>reach*reach)goal=m.getPos().add(delta.normalize().multiply(reach));
            if(m.getBrain().hasMemoryModule(net.minecraft.entity.ai.brain.MemoryModuleType.WALK_TARGET))m.getBrain().remember(net.minecraft.entity.ai.brain.MemoryModuleType.WALK_TARGET,new net.minecraft.entity.ai.brain.WalkTarget(BlockPos.ofFloored(goal),1.1F,2));
            m.getNavigation().startMovingTo(goal.x,goal.y,goal.z,1.1);
            advanceReachedNodes(m);
        }
        if(DemonRuntimeChecks.ENABLED)DemonRuntimeChecks.navigation(System.nanoTime()-start);
    }
    private static void advanceReachedNodes(MobEntity mob){
        // Some bridged navigators retain their start node at the mob's exact
        // feet. Consume only physically reached points; retain native path,
        // move control, collision and combat AI, and never move/teleport here.
        var path=mob.getNavigation().getCurrentPath();
        for(int work=0;work<4&&path!=null&&!path.isFinished()&&path.getNodePosition(mob).squaredDistanceTo(mob.getPos())<.0625;work++)path.next();
    }
    private static boolean flying(MobEntity m){return m instanceof FlyingEntity||m.getNavigation() instanceof net.minecraft.entity.ai.pathing.BirdNavigation||m.getMoveControl() instanceof net.minecraft.entity.ai.control.FlightMoveControl||m.hasNoGravity();}
    public static boolean validEnemy(ServerPlayerEntity owner,LivingEntity target){
        if(!target.isAlive()||target==owner||owner.isTeammate(target))return false;
        if(target instanceof ServerPlayerEntity q&&(!owner.getServer().isPvpEnabled()||!owner.shouldDamagePlayer(q)))return false;
        if(target instanceof MobEntity m&&owner.getUuid().equals(DemonEntityData.of(m).commander))return false;
        return DemonProtection.allowCombat(owner,owner.getServerWorld(),target.getBlockPos());
    }
    public static boolean friendly(LivingEntity victim,net.minecraft.entity.damage.DamageSource source){
        if(!(source.getAttacker() instanceof LivingEntity attacker))return false;
        var d=DemonEntityData.of(attacker);if(d.commander==null)return false;
        if(victim.getUuid().equals(d.commander))return true;
        return d.commander.equals(DemonEntityData.of(victim).commander);
    }
    public static boolean passiveOrder(MobEntity mob){
        var data=DemonEntityData.of(mob);if(data.commander==null||data.command==2||data.command==3||mob.getServer()==null||!DemonMobIdentity.eligible(mob))return false;
        var owner=mob.getServer().getPlayerManager().getPlayer(data.commander);
        return owner!=null&&owner.isAlive()&&owner.getWorld()==mob.getWorld()&&DemonProgression.demon(owner)&&DemonProgression.has(owner,"army");
    }
    public static String inspect(MobEntity m){
        if(!DemonRuntimeChecks.ENABLED)return "disabled";
        var d=DemonEntityData.of(m);var path=m.getNavigation().getCurrentPath();
        String counters="";for(var field:net.minecraft.entity.ai.pathing.EntityNavigation.class.getDeclaredFields())if(field.getType()==int.class)try{field.setAccessible(true);counters+=field.getName()+":"+field.getInt(m.getNavigation())+",";}catch(ReflectiveOperationException ex){throw new IllegalStateException("Cannot inspect navigation counter",ex);}
        return "type="+org.marj4n.smooth_classes.platform.PlatformRegistries.ENTITY_TYPE.getId(m.getType())+" managed="+(LOADED.get(m.getUuid())==m)+" aiDisabled="+m.isAiDisabled()+" eligible="+DemonMobIdentity.eligible(m)+" command="+d.command+" scheduled="+d.scheduledAt+" tick="+m.getServer().getTicks()+" nav="+m.getNavigation().getClass().getName()+" idle="+m.getNavigation().isIdle()+" path="+(path==null?"null":path.getTarget()+" node="+path.getCurrentNodeIndex()+"/"+path.getLength()+" next="+path.getNodePosition(m))+" counters="+counters+" control="+m.getMoveControl().getClass().getName()+" moving="+m.getMoveControl().isMoving()+" speed="+m.getMovementSpeed()+" follow="+m.getAttributeValue(net.minecraft.entity.attribute.EntityAttributes.GENERIC_FOLLOW_RANGE)+" forward="+m.forwardSpeed+" goals="+((DemonCommandGoal.Access)m).smooth_classes$demonGoals().getGoals().stream().map(g->g.getGoal().getClass().getSimpleName()+":"+g.getPriority()+":"+g.isRunning()).toList();
    }
}
