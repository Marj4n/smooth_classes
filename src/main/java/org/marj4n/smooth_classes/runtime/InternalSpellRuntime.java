package org.marj4n.smooth_classes.runtime;

import net.minecraft.entity.Entity;
import net.minecraft.entity.attribute.EntityAttributes;
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
import net.spell_power.api.SpellSchools;

import java.util.List;

/** Server-authoritative casting for Smooth Classes' internal registry spells. */
public final class InternalSpellRuntime {
    private InternalSpellRuntime() {}

    public static RegistryEntry<Spell> entry(PlayerEntity p, Identifier id) {
        return RegistryHelper.getEntry(SpellRegistry.from(p.getWorld()), id).orElse(null);
    }

    public static boolean cast(PlayerEntity p, String id, List<Entity> targets, float multiplier) {
        return cast(p, id, targets, multiplier, false, null);
    }

    public static boolean castUniversal(PlayerEntity p, String id, List<Entity> targets, float multiplier) {
        return cast(p, id, targets, multiplier, true, null);
    }

    public static boolean castUsingPowerSchool(PlayerEntity p, String id, List<Entity> targets,
                                               float multiplier, net.spell_power.api.SpellSchool powerSchool) {
        return cast(p, id, targets, multiplier, false, powerSchool);
    }

    private static boolean cast(PlayerEntity p, String id, List<Entity> targets, float multiplier, boolean universalPower, net.spell_power.api.SpellSchool powerSchool) {
        if (p.getWorld().isClient) return false;
        RegistryEntry<Spell> e=entry(p,new Identifier(id));
        if(e==null)return false;
        Spell spell=e.value();
        var power=SpellPower.getSpellPower(spell.school,p);
        if (powerSchool != null) {
            var selected = SpellPower.getSpellPower(powerSchool, p);
            power = new SpellPower.Result(spell.school, selected.baseValue(), selected.criticalChance(), selected.criticalDamage());
        } else if (universalPower) {
            double best=Math.max(0.0D,p.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE));
            double critChance=power.criticalChance();
            double critDamage=power.criticalDamage();
            if (power.baseValue()>best) best=power.baseValue();
            for (var school : SpellSchools.all()) {
                try {
                    var candidate=SpellPower.getSpellPower(school,p);
                    double base=candidate.baseValue();
                    if (Double.isFinite(base) && base>best) {
                        best=base;
                        critChance=candidate.criticalChance();
                        critDamage=candidate.criticalDamage();
                    }
                } catch (RuntimeException ignored) {
                    // Optional schools may not expose a usable power source for every player.
                }
            }
            // Keep the delivered spell's school/damage identity while borrowing the
            // strongest offensive magnitude. Vanilla Attack Damage is a real fallback,
            // so a pure melee build can use every offensive Ascendancy as advertised.
            power=new SpellPower.Result(spell.school,best,critChance,critDamage);
        }
        SpellExecution.ImpactContext context=new SpellExecution.ImpactContext(
                multiplier,1F,null,power,SpellIntents.focusMode(spell),0);
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
        return aoeUsingPowerSchool(p,id,radius,chance,hostile,friendly,multiplier,null);
    }

    public static boolean aoeUsingPowerSchool(PlayerEntity p,String id,double radius,int chance,
                                               boolean hostile,boolean friendly,float multiplier,
                                               net.spell_power.api.SpellSchool powerSchool){
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
        return powerSchool==null?cast(p,id,targets,multiplier):castUsingPowerSchool(p,id,targets,multiplier,powerSchool);
    }

    public static boolean dumbFire(PlayerEntity p,String id,float multiplier){return cast(p,id,List.of(),multiplier);}
    public static boolean target(PlayerEntity p,String id,Entity target,float multiplier){return cast(p,id,List.of(target),multiplier);}
    public static boolean dumbFireUniversal(PlayerEntity p,String id,float multiplier){return castUniversal(p,id,List.of(),multiplier);}
    public static boolean targetUniversal(PlayerEntity p,String id,Entity target,float multiplier){return castUniversal(p,id,List.of(target),multiplier);}
    public static boolean dumbFireUsingPowerSchool(PlayerEntity p,String id,float multiplier,net.spell_power.api.SpellSchool school){
        return castUsingPowerSchool(p,id,List.of(),multiplier,school);
    }
    public static boolean targetUsingPowerSchool(PlayerEntity p,String id,Entity target,float multiplier,net.spell_power.api.SpellSchool school){
        return castUsingPowerSchool(p,id,List.of(target),multiplier,school);
    }
    public static boolean atPosition(PlayerEntity p,String id,net.minecraft.util.math.Vec3d position,float multiplier){
        return atPosition(p,id,position,multiplier,false);
    }
    public static boolean atPositionUniversal(PlayerEntity p,String id,net.minecraft.util.math.Vec3d position,float multiplier){
        return atPosition(p,id,position,multiplier,true);
    }
    private static boolean atPosition(PlayerEntity p,String id,net.minecraft.util.math.Vec3d position,float multiplier,boolean universalPower){
        if (!(p.getWorld() instanceof net.minecraft.server.world.ServerWorld world)) return false;
        var marker=new org.marj4n.smooth_classes.entity.SpellTargetEntity(
                org.marj4n.smooth_classes.registry.SmoothEntities.SPELL_TARGET, world);
        marker.refreshPositionAndAngles(position.x,position.y,position.z,0,0);
        if (!world.spawnEntity(marker)) return false;
        boolean cast=universalPower?targetUniversal(p,id,marker,multiplier):target(p,id,marker,multiplier);
        if (!cast) marker.discard();
        return cast;
    }
}
