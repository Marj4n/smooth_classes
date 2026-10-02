package org.marj4n.smooth_classes.runtime.base;

import org.marj4n.smooth_classes.config.SmoothBalance;
import org.marj4n.smooth_classes.runtime.InternalSpellRuntime;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.spell_power.api.SpellSchool;
import net.spell_power.api.SpellSchools;
import org.marj4n.smooth_classes.effects.SmoothEffects;
import org.marj4n.smooth_classes.integration.PuffishSkillsIntegration;
import org.marj4n.smooth_classes.integration.SkillNodeIds;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;

public final class BasePathRuntime {
    private BasePathRuntime() {}

    private static final Map<UUID, Float> EARTHSHAKER_FALL = new HashMap<>();
    private static final StatusEffect[] ATTUNEMENTS = {
            SmoothEffects.ARCANE_ATTUNEMENT, SmoothEffects.SOUL_ATTUNEMENT,
            SmoothEffects.HOLY_ATTUNEMENT, SmoothEffects.FIRE_ATTUNEMENT,
            SmoothEffects.FROST_ATTUNEMENT, SmoothEffects.LIGHTNING_ATTUNEMENT
    };

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) if (p.isAlive()) tick(p);
        });
    }

    private static boolean has(ServerPlayerEntity p, String id) {
        return PuffishSkillsIntegration.isSkillUnlocked(PuffishSkillsIntegration.TREE, id, p);
    }

    private static void tick(ServerPlayerEntity p) {
        int age = p.age;
        tickEarthshaker(p);

        // Everything below is scheduled on a multiple of five ticks. Keep the
        // stealth render flag responsive each tick, but avoid tree probes on 4/5 ticks.
        if ((age % 5) != 0) {
            syncStealthVisibility(p);
            return;
        }

        tickAttuned(p);

        // MAGIC (Initiate)
        if (age % 80 == 0 && has(p, SkillNodeIds.initiateNullification)) nullification(p);
        if (age % 500 == 0 && p.getWorld().isThundering()
                && has(p, SkillNodeIds.initiateLightningRod))
            inc(p,SmoothEffects.LIGHTNING_ATTUNEMENT,600,1,5);
        if (age % 40 == 0 && p.hasStatusEffect(SmoothEffects.SOULSHOCK)
                && has(p, SkillNodeIds.initiateLightningRod))
            lightningRodPulse(p);
        if (age % 20 == 0 && has(p, SkillNodeIds.wizardPath)) frail(p);

        // AGILITY (Wayfarer)
        if (age % 10 == 0 && p.isSneaking() && !p.hasStatusEffect(SmoothEffects.REVEALED)
                && has(p,SkillNodeIds.wayfarerStealth) && !targeted(p,20))
            p.addStatusEffect(new StatusEffectInstance(SmoothEffects.STEALTH,20,0,false,false,true));
        if (age % 10 == 0 && p.isSneaking() && has(p,SkillNodeIds.wayfarerSneak)) {
            p.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED,15,2,false,false,true));
            if (p.hasStatusEffect(SmoothEffects.STEALTH)) inc(p,SmoothEffects.MIGHT,15,1,22);
        }
        if (age % 800 == 0 && p.getOffHandStack().getItem() instanceof CrossbowItem
                && has(p,SkillNodeIds.wayfarerGuarding))
            inc(p,SmoothEffects.BARRIER,3400,1,3);
        if (age % 20 == 0) slender(p);

        // STRENGTH (Warrior)
        if (age % 20 == 0 && has(p,SkillNodeIds.warriorDeathDefy)) deathDefy(p);
        if (age % 15 == 0 && has(p,SkillNodeIds.warriorCarnage)) carnage(p);
        if (age % 10 == 0 && has(p,SkillNodeIds.bulwarkShieldMastery)) shieldMastery(p);

        syncStealthVisibility(p);
    }

    private static void syncStealthVisibility(ServerPlayerEntity p) {
        if (p.hasStatusEffect(SmoothEffects.STEALTH)) p.setInvisible(true);
        else if (!p.hasStatusEffect(StatusEffects.INVISIBILITY)) p.setInvisible(false);
    }

    public static void onMeleeHit(ServerPlayerEntity p, LivingEntity target) {
        if (p.hasStatusEffect(SmoothEffects.STEALTH)) breakStealth(p);
        if (has(p,SkillNodeIds.warriorFrenzy)) inc(p,SmoothEffects.EXHAUSTION,400,1,79);
        if (has(p,SkillNodeIds.warriorSpellbreaker) && p.getRandom().nextInt(100) < 25)
            p.addStatusEffect(new StatusEffectInstance(SmoothEffects.RAGING_JAVELIN,50,0,false,false,true));
        if (has(p,SkillNodeIds.warriorTwinstrike)) {
            int chance=p.hasStatusEffect(StatusEffects.HEALTH_BOOST)?30:15;
            if (p.getRandom().nextInt(100)<chance) {
                target.timeUntilRegen=0;
                target.damage(p.getDamageSources().playerAttack(p),(float)p.getAttributeValue(net.minecraft.entity.attribute.EntityAttributes.GENERIC_ATTACK_DAMAGE));
                target.timeUntilRegen=0;
            }
        }
        if (has(p,SkillNodeIds.warriorSwordfall)
                && (p.getMainHandStack().getItem() instanceof SwordItem || p.getMainHandStack().getItem() instanceof AxeItem)) {
            int chance = SmoothBalance.Strength.swordfallChance;
            if (p.hasStatusEffect(SmoothEffects.MIGHT)) chance *= 2;
            if (p.getRandom().nextInt(100) < chance) {
                InternalSpellRuntime.target(p, "smooth_classes:physical_swordfall", target, 1.0F);
            }
        }
        if (has(p,SkillNodeIds.wizardPath)) frail(p);
    }


    /** Incoming-damage hook, called by SmoothClasses ServerPlayerEntity mixin. */
    public static void onDamaged(ServerPlayerEntity p) {
        if (has(p, SkillNodeIds.warriorHeavyArmorMastery)
                || has(p, SkillNodeIds.warriorMediumArmorMastery)) armorMastery(p);
        if (has(p, SkillNodeIds.warriorSpellbreaker) && p.getRandom().nextInt(100) < 25)
            p.addStatusEffect(new StatusEffectInstance(SmoothEffects.SPELLBREAKING,100,0,false,false,true));
        if (has(p, SkillNodeIds.initiateHasty)) inc(p, StatusEffects.SLOWNESS,20,1,4);
        if (p.hasStatusEffect(SmoothEffects.STEALTH)) {
            if (has(p,SkillNodeIds.wayfarerReflexive) && p.getRandom().nextInt(100)<75)
                inc(p,SmoothEffects.EVASION,100,1,1);
            p.removeStatusEffect(SmoothEffects.STEALTH);
            p.addStatusEffect(new StatusEffectInstance(SmoothEffects.REVEALED,180,5,false,false,true));
            p.setInvisible(false);
        }
    }

    /** Shield-block hook for Bulwark Rebuke. */
    public static void onShieldHit(ServerPlayerEntity p, LivingEntity attacker) {
        if (has(p,SkillNodeIds.bulwarkRebuke) && p.getRandom().nextInt(100)<25)
            attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS,80,0,false,false,true));
    }

    /** Full-charge bow hook for Wayfarer Quickfire. */
    public static void onBowRelease(ServerPlayerEntity p, boolean fullCharge) {
        if (p.hasStatusEffect(SmoothEffects.STEALTH)) breakStealth(p);
        if (fullCharge && has(p,SkillNodeIds.wayfarerQuickfire))
            inc(p,SmoothEffects.MARKSMANSHIP,40,1,6);
    }

    /** Crossbow use hook for Wayfarer Unseen. */
    public static void onCrossbowUse(ServerPlayerEntity p, boolean charged) {
        if (!charged && has(p,SkillNodeIds.wayfarerUnseen)) {
            if (!p.hasStatusEffect(SmoothEffects.STEALTH))
                p.addStatusEffect(new StatusEffectInstance(SmoothEffects.STEALTH,45,0,false,false,true));
            else inc(p,SmoothEffects.MARKSMANSHIP,60,1,10);
        }
    }

    /** Spell Engine cast hook: completes the Initiate/Magic branch. */
    public static void onFallTick(ServerPlayerEntity p) {
        if (has(p,SkillNodeIds.initiateSlowfall) && p.fallDistance>3F && !p.hasStatusEffect(StatusEffects.SLOW_FALLING))
            p.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING,20,0,false,false,true));
        if (has(p,SkillNodeIds.warriorGoliath) && p.fallDistance>3F && !p.hasStatusEffect(StatusEffects.SLOW_FALLING)) {
            p.addStatusEffect(new StatusEffectInstance(SmoothEffects.EARTHSHAKER,200,0,false,false,true));
            if(has(p,SkillNodeIds.warriorBound)){
                p.addStatusEffect(new StatusEffectInstance(StatusEffects.JUMP_BOOST,80,2,false,false,true));
                p.addStatusEffect(new StatusEffectInstance(SmoothEffects.RAGING_JAVELIN,80,0,false,false,true));
            }
        }
    }

    public static void onSpellCast(ServerPlayerEntity p, List<Entity> targets, SpellSchool school) {
        if (has(p,SkillNodeIds.initiateEmpower) && school != null) empower(p, school);

        if (p.hasStatusEffect(SmoothEffects.STEALTH)) {
            breakStealth(p);
            if (has(p,SkillNodeIds.initiateWhisperedWizardry))
                inc(p,SmoothEffects.SPELLFORGED,80,1,5);
        } else if (has(p,SkillNodeIds.initiateSpellcloak) && !p.hasStatusEffect(SmoothEffects.REVEALED)) {
            p.addStatusEffect(new StatusEffectInstance(SmoothEffects.STEALTH,40,0,false,false,true));
        }

        if (has(p,SkillNodeIds.initiateOverload)) inc(p,SmoothEffects.OVERLOAD,160,1,9);

        if (targets != null && !targets.isEmpty()) {
            if (has(p,SkillNodeIds.initiateEldritchEnfeeblement)) eldritchEnfeeblement(p);
            if (has(p,SkillNodeIds.initiatePerilousPrecision)) perilousPrecision(p);
        }

        if (school != null && school.id.toString().contains("physical_ranged")
                && has(p,SkillNodeIds.wayfarerQuickfire))
            inc(p,SmoothEffects.MARKSMANSHIP,40,1,6);
    }

    private static void armorMastery(ServerPlayerEntity p) {
        if (p.getRandom().nextInt(100)>=10) return;
        if (p.getArmor()>=15 && has(p,SkillNodeIds.warriorHeavyArmorMastery))
            p.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH,100,0,false,false,true));
        else if (has(p,SkillNodeIds.warriorMediumArmorMastery))
            p.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION,100,0,false,false,true));
    }

    private static void empower(ServerPlayerEntity p, SpellSchool school) {
        StatusEffect effect=null;
        if (school==SpellSchools.ARCANE) effect=SmoothEffects.ARCANE_ATTUNEMENT;
        else if (school==SpellSchools.SOUL) effect=SmoothEffects.SOUL_ATTUNEMENT;
        else if (school==SpellSchools.HEALING) effect=SmoothEffects.HOLY_ATTUNEMENT;
        else if (school==SpellSchools.FIRE) effect=SmoothEffects.FIRE_ATTUNEMENT;
        else if (school==SpellSchools.FROST) effect=SmoothEffects.FROST_ATTUNEMENT;
        else if (school==SpellSchools.LIGHTNING) effect=SmoothEffects.LIGHTNING_ATTUNEMENT;
        if(effect!=null && p.getRandom().nextInt(100)<15) inc(p,effect,300,1,15);
    }

    private static void tickAttuned(ServerPlayerEntity p) {
        if (p.age%20!=0 || !has(p,SkillNodeIds.initiateAttuned)) return;
        for(StatusEffect fx:ATTUNEMENTS){
            StatusEffectInstance x=p.getStatusEffect(fx);
            if(x!=null && x.getAmplifier()>4){inc(p,SmoothEffects.PRECISION,150,1,15);dec(p,fx);break;}
        }
    }

    private static void eldritchEnfeeblement(ServerPlayerEntity p) {
        var critChance=Registries.ATTRIBUTE.get(new Identifier("spell_power","critical_chance"));
        var critDamage=Registries.ATTRIBUTE.get(new Identifier("spell_power","critical_damage"));
        if(critChance==null||critDamage==null)return;
        double chance=p.getAttributeValue(critChance), damage=p.getAttributeValue(critDamage);
        if(p.getRandom().nextInt(100)<chance)p.heal((float)Math.min(3,damage/100.0));
    }

    private static void perilousPrecision(ServerPlayerEntity p) {
        int chance=Math.max(1,50-(int)p.getMaxHealth());
        if(p.getRandom().nextInt(100)<chance)inc(p,SmoothEffects.BARRIER,60,1,10);
    }

    private static void lightningRodPulse(ServerPlayerEntity p) {
        float damage=Math.max(1.0F, org.marj4n.smooth_classes.runtime.SpellPowerRuntime.lightning(p,0.35));
        for(LivingEntity e:p.getWorld().getEntitiesByClass(LivingEntity.class,p.getBoundingBox().expand(5),e->e!=p&&e.isAlive()))
            if(!(e instanceof PlayerEntity other) || p.shouldDamagePlayer(other)) e.damage(p.getDamageSources().magic(),damage);
    }

    private static void tickEarthshaker(ServerPlayerEntity p) {
        if(!p.hasStatusEffect(SmoothEffects.EARTHSHAKER)){
            if(!EARTHSHAKER_FALL.isEmpty()) EARTHSHAKER_FALL.remove(p.getUuid());
            return;
        }
        if(!p.isOnGround()){
            EARTHSHAKER_FALL.merge(p.getUuid(),p.fallDistance,Math::max);
            return;
        }
        Float fall=EARTHSHAKER_FALL.remove(p.getUuid());
        if(fall==null||fall<=0)return;
        float damage=2.0F + (has(p,SkillNodeIds.warriorHeavyWeight)?fall*0.3F:0F);
        for(LivingEntity e:p.getWorld().getEntitiesByClass(LivingEntity.class,p.getBoundingBox().expand(4),e->e!=p&&e.isAlive())){
            if(e instanceof PlayerEntity other && !p.shouldDamagePlayer(other))continue;
            e.timeUntilRegen=0;e.damage(p.getDamageSources().playerAttack(p),damage);
            Vec3d away=e.getPos().subtract(p.getPos()).normalize().multiply(0.25);
            e.addVelocity(away.x,0.2,away.z);
        }
        p.removeStatusEffect(SmoothEffects.EARTHSHAKER);
    }

    private static void deathDefy(ServerPlayerEntity p) {
        float hp=p.getHealth()/p.getMaxHealth()*100F; if(hp>=30F)return;
        int regen=0; if(hp<20)regen++; if(hp<10)regen+=2;
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION,25,regen,false,false,true));
        if(regen>0)inc(p,SmoothEffects.EXHAUSTION,80,regen+1,49);
    }
    private static void carnage(ServerPlayerEntity p) {
        boolean ex=p.hasStatusEffect(SmoothEffects.EXHAUSTION)&&p.getStatusEffect(SmoothEffects.EXHAUSTION).getAmplifier()>=75;
        boolean rage=p.hasStatusEffect(SmoothEffects.RAGE)&&p.getStatusEffect(SmoothEffects.RAGE).getAmplifier()>=75;
        if(ex||rage){int a=(int)p.getMaxHealth()/10;p.addStatusEffect(new StatusEffectInstance(StatusEffects.HEALTH_BOOST,20,a,false,false,true));p.addStatusEffect(new StatusEffectInstance(SmoothEffects.IMMOBILIZING_AURA,20,a,false,false,true));}
    }
    private static void shieldMastery(ServerPlayerEntity p) {
        if(!(p.getOffHandStack().getItem() instanceof ShieldItem))return;
        int a=0;if(has(p,SkillNodeIds.bulwarkShieldMasterySkilled))a=2;else if(has(p,SkillNodeIds.bulwarkShieldMasteryProficient))a=1;
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE,15,a,false,false,true));
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS,15,0,false,false,true));
    }
    private static void frail(ServerPlayerEntity p) {
        if(p.getAttributeValue(net.minecraft.entity.attribute.EntityAttributes.GENERIC_ATTACK_DAMAGE)>5){
            p.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS,25,0,false,false,true));
            p.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE,25,3,false,false,true));
        }
    }
    private static void slender(ServerPlayerEntity p) {
        int armor=p.getArmor();if(armor>=35)return;
        if(has(p,SkillNodeIds.roguePath)||has(p,SkillNodeIds.rangerPath)||has(p,SkillNodeIds.wizardPath))
            p.addStatusEffect(new StatusEffectInstance(SmoothEffects.AGILE,25,(35-armor)/5,false,false,false));
    }
    private static void nullification(ServerPlayerEntity p) {
        Box box=p.getBoundingBox().expand(12,4,12);
        for(LivingEntity e:p.getWorld().getEntitiesByClass(LivingEntity.class,box,e->e!=p&&e.isAlive())){
            if(e instanceof PlayerEntity other && !p.shouldDamagePlayer(other))continue;if(e.isTeammate(p))continue;
            for(StatusEffectInstance x:new ArrayList<>(e.getStatusEffects()))if(x.getEffectType().isBeneficial()){dec(e,x.getEffectType());break;}
        }
    }
    private static void breakStealth(ServerPlayerEntity p) {
        if(has(p,SkillNodeIds.wayfarerReflexive)){inc(p,SmoothEffects.MIGHT,40,1,20);inc(p,SmoothEffects.MARKSMANSHIP,40,1,20);}
        p.removeStatusEffect(SmoothEffects.STEALTH);if(p.hasStatusEffect(StatusEffects.INVISIBILITY))p.removeStatusEffect(StatusEffects.INVISIBILITY);
        p.addStatusEffect(new StatusEffectInstance(SmoothEffects.REVEALED,180,5,false,false,true));p.setInvisible(false);
    }
    private static boolean targeted(ServerPlayerEntity p,int r) {
        Box b=p.getBoundingBox().expand(r);
        for(MobEntity m:p.getWorld().getEntitiesByClass(MobEntity.class,b,e->e.isAlive()))if(m.getTarget()==p)return true;
        for(PlayerEntity o:p.getWorld().getEntitiesByClass(PlayerEntity.class,b,e->e!=p&&e.isAlive())){Vec3d d=p.getPos().subtract(o.getPos()).normalize();if(o.getRotationVec(1F).normalize().dotProduct(d)>0)return true;}
        return false;
    }
    private static void inc(LivingEntity e,StatusEffect fx,int duration,int amount,int max){
        StatusEffectInstance c=e.getStatusEffect(fx);int a=c==null?amount:Math.min(max,c.getAmplifier()+amount);
        e.addStatusEffect(new StatusEffectInstance(fx,duration,a,false,false,true));
    }
    private static void dec(LivingEntity e,StatusEffect fx){
        StatusEffectInstance c=e.getStatusEffect(fx);if(c==null)return;
        if(c.getAmplifier()<=0)e.removeStatusEffect(fx);else e.addStatusEffect(new StatusEffectInstance(fx,c.getDuration(),c.getAmplifier()-1,c.isAmbient(),c.shouldShowParticles(),c.shouldShowIcon()));
    }
}
