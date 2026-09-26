package org.marj4n.smooth_classes.runtime;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.api.spell.registry.SpellRegistry;
import net.spell_engine.fx.ReleaseFx;
import net.spell_engine.utils.AnimationHelper;
import net.spell_engine.internals.casting.SpellCast;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.spell_engine.internals.SpellExecution;
import net.spell_engine.internals.delivery.SpellDelivery;
import net.spell_engine.internals.target.SpellIntents;
import net.spell_engine.internals.target.SpellTarget;
import net.spell_engine.utils.RegistryHelper;
import net.spell_power.api.SpellPower;

import java.util.List;

/** Server-authoritative casting for Smooth Classes' internal registry spells. */
public final class InternalSpellRuntime {
    private InternalSpellRuntime() {}

    public static RegistryEntry<Spell> entry(PlayerEntity p, Identifier id) {
        return RegistryHelper.getEntry(SpellRegistry.from(p.getWorld()), id).orElse(null);
    }

    public static boolean cast(PlayerEntity p, String id, List<Entity> targets, float multiplier) {
        if (p.getWorld().isClient) return false;
        RegistryEntry<Spell> e=entry(p,new Identifier(id));
        if(e==null)return false;
        Spell spell=e.value();
        SpellExecution.ImpactContext context=new SpellExecution.ImpactContext(
                multiplier,1F,null, SpellPower.getSpellPower(spell.school,p), SpellIntents.focusMode(spell),0);
        boolean delivered=SpellDelivery.resolveAndDeliver(p.getWorld(),p,e,SpellTarget.SearchResult.of(targets),context,null);
        if(delivered){
            ReleaseFx.send(p.getWorld(),p,e,1F);
            if(spell.release!=null && spell.release.animation!=null){
                AnimationHelper.sendAnimation(p, PlayerLookup.tracking(p),
                        SpellCast.Animation.RELEASE, spell.release.animation, 1.0F);
            }
        }
        return delivered;
    }
    public static boolean aoe(PlayerEntity p,String id,double radius,int chance,
                              boolean hostile,boolean friendly,float multiplier){
        java.util.ArrayList<Entity> targets=new java.util.ArrayList<>();
        var box=p.getBoundingBox().expand(radius);
        for(var entity:p.getWorld().getEntitiesByClass(net.minecraft.entity.LivingEntity.class,box,
                e->e!=p&&e.isAlive())){
            boolean ally=entity.isTeammate(p);
            if((hostile&&!ally)||(friendly&&ally)){
                if(p.getRandom().nextInt(100)<chance)targets.add(entity);
            }
        }
        if(targets.isEmpty())return false;
        return cast(p,id,targets,multiplier);
    }

    public static boolean dumbFire(PlayerEntity p,String id,float multiplier){return cast(p,id,List.of(),multiplier);}
    public static boolean target(PlayerEntity p,String id,Entity target,float multiplier){return cast(p,id,List.of(target),multiplier);}
    public static boolean atPosition(PlayerEntity p,String id,net.minecraft.util.math.Vec3d position,float multiplier){
        if (!(p.getWorld() instanceof net.minecraft.server.world.ServerWorld world)) return false;
        var marker=new org.marj4n.smooth_classes.entity.SpellTargetEntity(
                org.marj4n.smooth_classes.registry.SmoothEntities.SPELL_TARGET, world);
        marker.refreshPositionAndAngles(position.x,position.y,position.z,0,0);
        if (!world.spawnEntity(marker)) return false;
        boolean cast=target(p,id,marker,multiplier);
        if (!cast) marker.discard();
        return cast;
    }
}
