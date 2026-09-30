package org.marj4n.smooth_classes.runtime;

import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.*;
import net.minecraft.sound.*;
import net.spell_power.api.*;
import org.marj4n.smooth_classes.SmoothClasses;
import org.marj4n.smooth_classes.registry.SmoothEntities;
import org.marj4n.smooth_classes.entity.HighBeamEntity;
import org.marj4n.smooth_classes.network.SmoothClassesNetworking;

public final class WhenOnHighRuntime {
    private static final Map<UUID,Cast> ACTIVE=new HashMap<>();
    private static final Set<String> WARNED_SCHOOLS=new HashSet<>();
    public static final float PULSE_COEFFICIENT=.10F;
    private WhenOnHighRuntime(){}
    private static final class Cast {
        final ServerPlayerEntity caster;
        final ServerWorld world;
        final LivingEntity target;
        final Vec3d origin;
        final boolean oldGravity,oldInvulnerable;
        final int width;
        final int points;
        final HighCastSequence sequence=new HighCastSequence();
        Vec3d impact;
        HighBeamEntity beam;
        Cast(ServerPlayerEntity p,LivingEntity t,int points){caster=p;world=p.getServerWorld();target=t;origin=p.getPos();impact=t.getPos();oldGravity=p.hasNoGravity();oldInvulnerable=p.isInvulnerable();this.points=points;width=points>=60?7:points>=30?3:1;}
    }
    public static boolean active(ServerPlayerEntity p){return ACTIVE.containsKey(p.getUuid());}
    public static boolean ownsBeam(HighBeamEntity beam){var cast=ACTIVE.get(beam.owner);return cast!=null&&cast.beam==beam;}
    public static void register(){
        ServerTickEvents.END_SERVER_TICK.register(server->{
            if(ACTIVE.isEmpty())return;
            for(var cast:new ArrayList<>(ACTIVE.values())){
                try{tick(cast);}catch(RuntimeException failure){finish(cast,true);SmoothClasses.LOGGER.error("When On High channel interrupted",failure);}
            }
        });
        ServerPlayConnectionEvents.DISCONNECT.register((h,s)->finish(ACTIVE.get(h.player.getUuid()),false));
        ServerLifecycleEvents.SERVER_STOPPING.register(s->{for(var cast:new ArrayList<>(ACTIVE.values()))finish(cast,false);});
        ServerLifecycleEvents.SERVER_STOPPED.register(s->{ACTIVE.clear();WARNED_SCHOOLS.clear();});
    }
    private static boolean enemy(ServerPlayerEntity p,LivingEntity e){
        return e!=p&&e.isAlive()&&!e.isSpectator()&&!e.isTeammate(p)
            &&!(e instanceof TameableEntity tame&&p.getUuid().equals(tame.getOwnerUuid()));
    }
    public static ExecutionResult cast(ServerPlayerEntity p,int points){
        if(active(p))return ExecutionResult.failure("When On High is already channeling.");
        if(p.hasStatusEffect(org.marj4n.smooth_classes.effects.SmoothEffects.GHOSTWALK))return ExecutionResult.failure("Wait for Ghostwalk to finish before casting When On High.");
        if(p.hasVehicle()||p.isSpectator())return ExecutionResult.failure("Dismount before casting When On High.");
        Vec3d eye=p.getEyePos(),end=eye.add(p.getRotationVec(1).multiply(48));
        LivingEntity selected=null;double nearest=Double.MAX_VALUE;
        // Select along the aim ray, without roof/wall raycasts: the spell can pierce terrain.
        for(var e:p.getServerWorld().getEntitiesByClass(LivingEntity.class,p.getBoundingBox().stretch(end.subtract(eye)).expand(1),t->enemy(p,t))){
            var hit=e.getBoundingBox().expand(.3).raycast(eye,end);
            if(hit.isPresent()&&eye.squaredDistanceTo(hit.get())<nearest){selected=e;nearest=eye.squaredDistanceTo(hit.get());}
        }
        if(selected==null)return ExecutionResult.failure("Aim at a living enemy within 48 blocks.");
        var cast=new Cast(p,selected,points);ACTIVE.put(p.getUuid(),cast);
        p.setNoGravity(true);p.setInvulnerable(true);p.setVelocity(Vec3d.ZERO);p.fallDistance=0;p.setSprinting(false);
        p.getServerWorld().playSound(null,p.getBlockPos(),SoundEvents.BLOCK_BEACON_ACTIVATE,SoundCategory.PLAYERS,1F,1.25F);
        SmoothClassesNetworking.sendAbilityState(p);
        return ExecutionResult.success(1,"agony");
    }
    private static void tick(Cast cast){
        var p=cast.caster;
        if(!p.isAlive()||p.isRemoved()||p.getWorld()!=cast.world){finish(cast,true);return;}
        cast.sequence.tick();
        // Freeze horizontal movement and prevent knockback/fall accumulation for all three phases.
        p.setNoGravity(true);p.setInvulnerable(true);p.fallDistance=0;p.setVelocity(Vec3d.ZERO);p.velocityModified=true;
        p.requestTeleport(cast.origin.x,cast.origin.y+cast.sequence.height,cast.origin.z);
        if(cast.sequence.phase==HighCastSequence.Phase.BEAM){
            if(cast.target.isAlive()&&!cast.target.isRemoved()&&cast.target.getWorld()==cast.world)cast.impact=cast.target.getPos();
            if(cast.beam==null){
                cast.beam=SmoothEntities.HIGH_BEAM.create(cast.world);
                if(cast.beam==null){finish(cast,true);return;}
                cast.beam.owner=p.getUuid();cast.beam.setBeamWidth(cast.width);cast.beam.setPosition(cast.impact);
                if(!cast.world.spawnEntity(cast.beam)){finish(cast,true);return;}
            }
            cast.beam.setPosition(cast.impact);
            if(cast.sequence.pulse()||(cast.points>=60&&cast.sequence.beamTicks%2==0))pulse(cast);
            if(cast.sequence.beamTicks%20==0)cast.world.playSound(null,BlockPos.ofFloored(cast.impact),SoundEvents.BLOCK_BEACON_AMBIENT,SoundCategory.PLAYERS,1.1F,1.6F);
        }else if(cast.beam!=null){cast.beam.discard();cast.beam=null;}
        if(cast.sequence.phase==HighCastSequence.Phase.DONE)finish(cast,true);
    }
    private static void pulse(Cast cast){
        double half=cast.width/2D;var at=cast.impact;var p=cast.caster;
        var area=new Box(at.x-half,at.y-.5,at.z-half,at.x+half,at.y+4,at.z+half);
        double sum=0,highest=0;
        for(var school:SpellSchools.all()){
            if(school.archetype!=SpellSchool.Archetype.MAGIC)continue;
            if(school.damageType==null||cast.world.getRegistryManager().get(RegistryKeys.DAMAGE_TYPE).getOrEmpty(school.damageType).isEmpty())continue;
            try{double value=SpellPower.getSpellPower(school,p).baseValue();if(Double.isFinite(value)&&value>0){sum+=value;highest=Math.max(highest,value);}}
            catch(RuntimeException ignored){/* Optional school without a player attribute. */}
        }
        double schoolScale=AscendancyBalance.multiSchoolScale(highest,sum);
        for(var target:cast.world.getEntitiesByClass(LivingEntity.class,area,e->enemy(p,e))){
            boolean hit=false;
            for(var school:SpellSchools.all()){
                if(school.archetype!=SpellSchool.Archetype.MAGIC)continue;
                // Optional mod schools may lack a damage type/attribute on this player.
                // One incompatible school must never cancel the other schools or the channel.
                try {
                    if(school.damageType==null||cast.world.getRegistryManager().get(RegistryKeys.DAMAGE_TYPE).getOrEmpty(school.damageType).isEmpty())continue;
                    var power=SpellPower.getSpellPower(school,p);
                    float coefficient=cast.points>=60?PULSE_COEFFICIENT*2F:PULSE_COEFFICIENT;
                    float damage=(float)(power.randomValue(SpellPower.getVulnerability(target,school))*coefficient*schoolScale);
                    if(!Float.isFinite(damage)||damage<=0)continue;
                    int previousImmunity=target.timeUntilRegen;
                    try{target.timeUntilRegen=0;hit|=target.damage(SpellDamageSource.create(school,p),damage);}
                    finally{target.timeUntilRegen=previousImmunity;}
                } catch(RuntimeException incompatibleSchool) {
                    if(WARNED_SCHOOLS.add(String.valueOf(school.id)))
                        SmoothClasses.LOGGER.warn("When On High skipped incompatible spell school "+school.id,incompatibleSchool);
                }
                if(!target.isAlive())break;
            }
            if(hit){
                double y=target.getY()+target.getHeight()*.55;
                cast.world.spawnParticles(net.minecraft.particle.ParticleTypes.END_ROD,target.getX(),y,target.getZ(),24,target.getWidth()*.6,target.getHeight()*.35,target.getWidth()*.6,.16);
                cast.world.spawnParticles(net.minecraft.particle.ParticleTypes.ELECTRIC_SPARK,target.getX(),y,target.getZ(),20,.45,.65,.45,.22);
            }

        }
    }
    private static void finish(Cast cast,boolean sync){
        if(cast==null||!ACTIVE.remove(cast.caster.getUuid(),cast))return;
        var p=cast.caster;
        if(cast.beam!=null)cast.beam.discard();
        if(p.getWorld()==cast.world&&p.isAlive())p.requestTeleport(cast.origin.x,cast.origin.y,cast.origin.z);
        p.setNoGravity(cast.oldGravity);p.setInvulnerable(cast.oldInvulnerable);p.setVelocity(Vec3d.ZERO);p.fallDistance=0;
        AbilityCooldowns.start(p,SmoothClasses.id("ascendancy_agony"),600);
        if(sync)SmoothClassesNetworking.sendAbilityState(p);
    }
}
