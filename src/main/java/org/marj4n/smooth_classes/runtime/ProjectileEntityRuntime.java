package org.marj4n.smooth_classes.runtime;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.spell_engine.entity.SpellProjectile;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.spell_engine.api.spell.registry.SpellRegistry;
import net.spell_engine.utils.RegistryHelper;
import org.marj4n.smooth_classes.effects.SmoothEffects;
import org.marj4n.smooth_classes.effects.SourceStatusEffectInstance;
import org.marj4n.smooth_classes.integration.PuffishSkillsIntegration;
import org.marj4n.smooth_classes.integration.SkillNodeIds;
import org.marj4n.smooth_classes.content.saber.runtime.SaberRuntime;

import java.util.Comparator;

/** Chapter 7: special projectile lifecycle layered on Spell Engine projectiles. */
public final class ProjectileEntityRuntime {
    private ProjectileEntityRuntime(){}

    public static void tickSpellProjectile(ServerPlayerEntity owner, SpellProjectile p, Identifier id){
        if(id==null)return;
        String spell=id.toString();

        // Sacred Orb: after its initial travel, acquire a nearby ally.
        if(spell.equals("smooth_classes:sacred_orb") && p.age>20 && p.getFollowedTarget()==null){
            nearest(p,owner,6,false).ifPresent(p::setFollowedTarget);
        }

        // Lightning Orb: player-following persistent projectile.
        if(spell.contains("lightning_ball") && has(owner,PuffishSkillsIntegration.CASTER,
                SkillNodeIds.wizardSpecialisationStaticDischargeLightningOrb)){
            p.setFollowedTarget(owner);
            p.range=512;
            if(p.age%20==0 && p.distanceTo(owner)>10){
                p.teleport(owner.getX(),owner.getY()+1,owner.getZ());
                p.velocityModified=true;
            }
        }

        // Elemental Artillery: clone the parent projectile's context/perks and spawn
        // a random elemental homing child exactly on 12-tick cadence.
        if(spell.contains("arrow_rain") && p.age>30 && p.age%12==0
                && has(owner,PuffishSkillsIntegration.ARCHER, SkillNodeIds.rangerSpecialisationArrowRainElementalArtillery)){
            spawnChild(owner,p,new Identifier("smooth_classes", switch(owner.getRandom().nextInt(3)){
                case 0 -> "frost_arrow_homing"; case 1 -> "fire_arrow_homing"; default -> "lightning_arrow_homing";}),20,35);
        }

        // Static Discharge Lightning Ball: emit lesser homing projectiles every 5 ticks.
        if((spell.contains("lightning_ball")||spell.contains("lightning_lesser")) && !spell.contains("ball_homing")
                && p.age>5 && p.age%5==0 && has(owner,PuffishSkillsIntegration.CASTER,
                SkillNodeIds.wizardSpecialisationStaticDischargeLightningBall)){
            p.mutablePerks().pierce = 132;
            spawnChild(owner,p,new Identifier("smooth_classes","lightning_lesser"),5,5);
        }

        // Ascendancy projectile states use their real spell assets already shipped in
        // the datapack. Their runtime state here provides movement/target behavior.
        if(spell.contains("righteous_hammer_projectile") && p.getFollowedTarget()==null)
            nearest(p,owner,12,true).ifPresent(p::setFollowedTarget);
        if (spell.contains("arcane_slash_projectile")) {
            boolean transcend = spell.endsWith("_3");
            ArcaneSlashVisuals.projectile(p, spell.endsWith("_2") || transcend);
            if (transcend) {
                // 60+ Arcane Slash has no artificial travel-distance or age cap.
                // Keep Spell Engine's range guard effectively out of the way, but
                // never force-load terrain: the slash dies before entering an
                // unloaded chunk.
                p.range = 1_000_000_000;
                // Spell Engine also has a generic ~60 second projectile lifetime.
                // Keep this specific transcendent slash perpetually young so its
                // only natural travel limit is loaded-world availability.
                p.age = 0;
                if (p.getWorld() instanceof net.minecraft.server.world.ServerWorld world) {
                    Vec3d next = p.getPos().add(p.getVelocity());
                    int nextChunkX = net.minecraft.util.math.MathHelper.floor(next.x) >> 4;
                    int nextChunkZ = net.minecraft.util.math.MathHelper.floor(next.z) >> 4;
                    long nextChunk = net.minecraft.util.math.ChunkPos.toLong(nextChunkX, nextChunkZ);
                    if (!world.isChunkLoaded(nextChunk)) {
                        p.discard();
                        return;
                    }
                }
            } else if (p.age > 30) {
                p.discard();
            }
        }
        if(spell.contains("rapidfire") && p.getFollowedTarget()==null)
            nearest(p,owner,16,true).ifPresent(p::setFollowedTarget);
        if(spell.equals("smooth_classes:passive_throw") && p.getFollowedTarget()==null)
            nearest(p,owner,12,true).ifPresent(p::setFollowedTarget);
    }

    public static void onSpellProjectileHit(ServerPlayerEntity owner,SpellProjectile p,Identifier id,LivingEntity target){
        if(id==null)return;
        String spell=id.toString();

        if(spell.equals("smooth_classes:physical_heavensmiths_call")) SaberRuntime.onHeavensmithImpact(owner,target);
        if(spell.equals("smooth_classes:lightning_ball") || spell.equals("smooth_classes:lightning_lesser"))
            org.marj4n.smooth_classes.content.caster.runtime.CasterRuntime.onStaticChargeHit(owner,target);

        // Sacred Orb impact establishes the gameplay bond on both sides.
        if(spell.equals("smooth_classes:sacred_orb")){
            target.addStatusEffect(new SourceStatusEffectInstance(SmoothEffects.VITALITY_BOND,500,0,false,false,true,owner));
            owner.addStatusEffect(new SourceStatusEffectInstance(SmoothEffects.VITALITY_BOND,500,0,false,false,true,owner));
        }

        // Homing lightning/physical daggers remain live through impact in this runtime.
        if(spell.contains("lightning_ball_homing") || spell.contains("physical_dagger_homing")){
            target.timeUntilRegen=0;
        }
    }

    public static boolean ignoreBlockHit(Identifier id){
        if(id==null)return false;
        String s=id.toString();
        return s.contains("lightning_ball_homing") || s.contains("physical_dagger_homing")
                || s.contains("sacred_orb_lesser") || s.contains("righteous_hammer_projectile");
    }

    private static java.util.Optional<LivingEntity> nearest(Entity center,ServerPlayerEntity owner,double radius,boolean hostile){
        Box box=center.getBoundingBox().expand(radius);
        return center.getWorld().getEntitiesByClass(LivingEntity.class,box,e->{
            if(!e.isAlive()||e==owner)return false;
            boolean ally=e.isTeammate(owner);
            return hostile?!ally:ally;
        }).stream().min(Comparator.comparingDouble(center::squaredDistanceTo));
    }

    private static void spawnChild(ServerPlayerEntity owner,SpellProjectile parent,Identifier spellId,double radius,int chance){
        var entry=RegistryHelper.getEntry(SpellRegistry.from(parent.getWorld()),spellId).orElse(null);
        if(entry==null||parent.getImpactContext()==null)return;
        var child=new SpellProjectile(parent.getWorld(),owner,parent.getX(),parent.getY(),parent.getZ(),
                parent.getBehaviour(),entry,parent.getImpactContext(),parent.mutablePerks().copy());
        child.setVelocity(parent.getVelocity().multiply(spellId.getPath().equals("lightning_lesser")?5:1));
        child.range=parent.range; ProjectileUtil.setRotationFromVelocity(child,0.2F);
        Box searchBox;
        if(spellId.getPath().endsWith("_arrow_homing")){
            searchBox=new Box(parent.getX()+radius,parent.getY()+radius*3,parent.getZ()+radius,
                    parent.getX()-radius,parent.getY()-radius*3,parent.getZ()-radius);
        } else searchBox=new Box(parent.getX()+radius,parent.getY()+radius/2,parent.getZ()+radius,
                parent.getX()-radius,parent.getY()-radius/2,parent.getZ()-radius);
        var candidates=parent.getWorld().getEntitiesByClass(LivingEntity.class,searchBox,
                x->x!=owner&&x.isAlive()&&!x.isTeammate(owner));
        LivingEntity chosen=null;
        for(LivingEntity target:candidates)if(owner.getRandom().nextInt(100)<chance){chosen=target;child.setFollowedTarget(target);break;}
        if(spellId.getPath().equals("lightning_lesser") && chosen==null)return;
        parent.getWorld().spawnEntity(child);
        if(chosen!=null && spellId.getPath().equals("lightning_lesser")){
            CombatEventRuntime.onSpellCast(owner,java.util.List.of(chosen),parent.getSpellEntry().value().school);
            org.marj4n.smooth_classes.content.caster.runtime.CasterRuntime.onStaticChargeHit(owner,chosen);
        }
    }

    private static boolean has(ServerPlayerEntity p,Identifier category,String node){
        return PuffishSkillsIntegration.isSkillUnlocked(category,node,p);
    }
}
