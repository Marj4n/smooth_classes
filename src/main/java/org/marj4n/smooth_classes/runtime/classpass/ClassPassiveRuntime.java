package org.marj4n.smooth_classes.runtime.classpass;

import org.marj4n.smooth_classes.config.SmoothBalance;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.SwordItem;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.spell_power.api.SpellSchool;
import net.spell_power.api.SpellSchools;
import org.marj4n.smooth_classes.effects.SmoothEffects;
import org.marj4n.smooth_classes.integration.PuffishSkillsIntegration;
import org.marj4n.smooth_classes.integration.SkillNodeIds;
import org.marj4n.smooth_classes.integration.OptionalCompatRuntime;
import org.marj4n.smooth_classes.runtime.AscendancyRuntime;
import org.marj4n.smooth_classes.content.saber.runtime.SaberRuntime;
import org.marj4n.smooth_classes.runtime.SkillFx;
import org.marj4n.smooth_classes.runtime.InternalSpellRuntime;
import org.marj4n.smooth_classes.runtime.AbilityRuntime;
import org.marj4n.smooth_classes.content.berserker.BerserkerClass;
import org.marj4n.smooth_classes.content.assassin.AssassinClass;
import org.marj4n.smooth_classes.content.archer.ArcherClass;
import org.marj4n.smooth_classes.content.saber.SaberClass;
import org.marj4n.smooth_classes.content.ruler.RulerClass;
import org.marj4n.smooth_classes.content.caster.CasterClass;
import org.marj4n.smooth_classes.content.foreigner.ForeignerContent;

import java.util.List;

/**
 * Chapter 2: class passive parity.
 * Values and triggers mirror Smooth Classes's non-signature class passives.
 * Puffish Skills remains authoritative for every unlock.
 */
public final class ClassPassiveRuntime {
    private static final String[] SPELLWEAVING_BASIC = {
            "frost_arrow", "fire_arrow", "lightning_arrow", "arcane_bolt",
            "arcane_bolt_lesser", "ice_comet", "fire_meteor_small", "static_discharge"
    };
    private static final String[] SPELLWEAVING_ENHANCED = {
            "frost_arrow", "fire_arrow", "lightning_arrow", "arcane_bolt",
            "arcane_bolt_lesser", "ice_comet", "fire_meteor_small", "static_discharge",
            "physical_swordrain", "arcane_slash_projectile", "righteous_hammer_projectile",
            "lightning_ball_homing", "fire_meteor_large"
    };

    private ClassPassiveRuntime() {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                // Every periodic class passive is 5 ticks or slower. Routing at 4 Hz
                // avoids probing every tree for every player on all 20 server ticks.
                if (p.isAlive() && (p.age % 5) == 0) tick(p);
            }
        });
    }

    private static boolean has(ServerPlayerEntity p, Identifier category, String node) {
        return PuffishSkillsIntegration.isSkillUnlocked(category, node, p);
    }

    private static void tick(ServerPlayerEntity p) {
        OptionalCompatRuntime.tick(p);

        // Only enter the active class runtime. Before this pass each player probed
        // all class categories even though Puffish only selects one class path.
        if (AbilityRuntime.isClass(p, BerserkerClass.ID)) berserkerTick(p);
        else if (AbilityRuntime.isClass(p, AssassinClass.ID)) assassinTick(p);
        else if (AbilityRuntime.isClass(p, ArcherClass.ID)) archerTick(p);
        else if (AbilityRuntime.isClass(p, SaberClass.ID)) {
            saberTick(p);
            SaberRuntime.tick(p);
        }
        else if (AbilityRuntime.isClass(p, RulerClass.ID)) rulerTick(p);
        else if (AbilityRuntime.isClass(p, CasterClass.ID)) casterTick(p);

        AscendancyRuntime.serverTick(p);
    }

    // ------------------------------------------------------------
    // CASTER — Lightning Orb lifecycle
    // ------------------------------------------------------------
    private static void casterTick(ServerPlayerEntity p) {
        if(p.age%40!=0)return;
        if(!has(p,PuffishSkillsIntegration.CASTER,SkillNodeIds.wizardSpecialisationStaticDischargeLightningOrb))return;
        int count=0;
        net.minecraft.util.math.Box box=new net.minecraft.util.math.Box(p.getX()+15,p.getY()+45,p.getZ()+15,p.getX()-15,p.getY()-45,p.getZ()-15);
        for(net.spell_engine.entity.SpellProjectile projectile:p.getWorld().getEntitiesByClass(net.spell_engine.entity.SpellProjectile.class,box,q->q.getOwner()==p))
            if(p.getRandom().nextInt(100)<35)count++;
        if(count>0)inc(p,SmoothEffects.SOULSHOCK,45,count,count);
    }

    // ------------------------------------------------------------
    // BERSERKER
    // ------------------------------------------------------------
    private static void berserkerTick(ServerPlayerEntity p) {
        if (p.age % 20 != 0) return;

        if (has(p,PuffishSkillsIntegration.BERSERKER,SkillNodeIds.berserkerSwordMastery)
                && p.getMainHandStack().getItem() instanceof SwordItem) {
            int mastery = has(p,PuffishSkillsIntegration.BERSERKER,SkillNodeIds.berserkerSwordMasteryProficient) ? 1 : 0;
            p.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED,25,mastery,false,false,true));
            if (has(p,PuffishSkillsIntegration.BERSERKER,SkillNodeIds.berserkerSwordMasterySkilled) && p.getOffHandStack().isEmpty())
                inc(p,SmoothEffects.MIGHT,25,1,3);
        }

        if (has(p,PuffishSkillsIntegration.BERSERKER,SkillNodeIds.berserkerAxeMastery)
                && p.getMainHandStack().getItem() instanceof AxeItem) {
            int mastery = 0;
            if (has(p,PuffishSkillsIntegration.BERSERKER,SkillNodeIds.berserkerAxeMasterySkilled)) mastery += 2;
            else if (has(p,PuffishSkillsIntegration.BERSERKER,SkillNodeIds.berserkerAxeMasteryProficient)) mastery += 1;
            p.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH,25,mastery,false,false,true));
        }

        if (has(p,PuffishSkillsIntegration.BERSERKER,SkillNodeIds.berserkerIgnorePain)
                && p.getHealth() <= p.getMaxHealth()*0.40F) {
            int mastery = 0;
            if (has(p,PuffishSkillsIntegration.BERSERKER,SkillNodeIds.berserkerIgnorePainSkilled)) mastery += 2;
            else if (has(p,PuffishSkillsIntegration.BERSERKER,SkillNodeIds.berserkerIgnorePainProficient)) mastery += 1;
            p.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE,25,mastery,false,false,true));
        }

        // contains the Recklessness method even though its current call site is absent.
        // Smooth Classes wires the intended passive so the unlocked node is not dead.
        if (has(p,PuffishSkillsIntegration.BERSERKER,SkillNodeIds.berserkerRecklessness)
                && p.getHealth() >= p.getMaxHealth()*0.70F)
            p.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS,25,0,false,false,true));

        if (has(p,PuffishSkillsIntegration.BERSERKER,SkillNodeIds.berserkerChallenge)) {
            int count=Math.min(5,enemies(p,2).size());
            if(count>1)p.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE,25,count-1,false,false,true));
        }
    }

    // ------------------------------------------------------------
    // ASSASSIN / ROGUE
    // ------------------------------------------------------------
    private static void assassinTick(ServerPlayerEntity p) {
        if (p.age % 20 != 0) return;
        // Backstab's secondary passive: nearby weakened enemies can return the rogue to stealth.
        if (has(p,PuffishSkillsIntegration.ASSASSIN,SkillNodeIds.rogueBackstab)) {
            for (LivingEntity e:enemies(p,8)) {
                if (e.hasStatusEffect(StatusEffects.WEAKNESS) && p.getRandom().nextInt(100)<3) {
                    p.addStatusEffect(new StatusEffectInstance(SmoothEffects.STEALTH,200,0,false,false,true));
                    SkillFx.sound(p, "soundeffect_39", 0.6F, 1.6F);
                }
            }
        }

        // Stealth upkeep and Bladestorm hits run once in EffectBehaviorRuntime.
    }

    // ------------------------------------------------------------
    // ARCHER / RANGER
    // ------------------------------------------------------------
    private static void archerTick(ServerPlayerEntity p) {
        int age = p.age;
        if (age % 10 != 0) return;

        boolean every80 = age % 80 == 0;
        boolean every20 = age % 20 == 0;

        if (every80 && has(p,PuffishSkillsIntegration.ARCHER,SkillNodeIds.rangerReveal)) {
            for(LivingEntity e:enemies(p,12)) if(e.hasStatusEffect(SmoothEffects.STEALTH)) {
                e.removeStatusEffect(SmoothEffects.STEALTH);
                e.addStatusEffect(new StatusEffectInstance(SmoothEffects.REVEALED,180,1,false,false,true));
            }
        }

        boolean tamer = every80 && has(p,PuffishSkillsIntegration.ARCHER,SkillNodeIds.rangerTamer);
        boolean bonded = has(p,PuffishSkillsIntegration.ARCHER,SkillNodeIds.rangerBonded);
        boolean trained = every80 && has(p,PuffishSkillsIntegration.ARCHER,SkillNodeIds.rangerTrained);
        boolean incognito = every20 && p.hasStatusEffect(SmoothEffects.STEALTH)
                && has(p,PuffishSkillsIntegration.ARCHER,SkillNodeIds.rangerIncognito);
        if (!tamer && !bonded && !trained && !incognito) return;

        // One nearby-pet query serves every Archer passive due on this tick.
        List<LivingEntity> pets = pets(p,12);
        if (tamer) pets.forEach(e -> {
            e.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION,85,1,false,false,true));
            e.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE,85,2,false,false,true));
        });

        if (bonded) {
            for(LivingEntity e:pets) {
                float petPct=e.getHealth()/e.getMaxHealth()*100F, playerPct=p.getHealth()/p.getMaxHealth()*100F;
                if(petPct>playerPct && petPct>30F) { e.setHealth(Math.max(1F,e.getHealth()-1F)); p.heal(1F); }
            }
        }

        if (trained) pets.forEach(e -> {
            if(e.getHealth()/e.getMaxHealth()*100F>70F) {
                e.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH,85,1,false,false,true));
                e.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED,85,1,false,false,true));
            }
        });

        if (incognito)
            pets.forEach(e -> e.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY,25,0,false,false,true)));
    }

    // ------------------------------------------------------------
    // SABER / CRUSADER
    // ------------------------------------------------------------
    private static void saberTick(ServerPlayerEntity p) {
        if(p.age%25==0 && p.hasStatusEffect(SmoothEffects.EXHAUSTION)
                && has(p,PuffishSkillsIntegration.SABER,SkillNodeIds.crusaderAegis)) {
            StatusEffectInstance exhaustion=p.getStatusEffect(SmoothEffects.EXHAUSTION);
            if(exhaustion!=null && exhaustion.getAmplifier()>35) {
                // Paladins' Divine Protection is optional in this runtime. Core Smooth Classes
                // uses its own defensive effect surface so Saber remains functional without Paladins.
                inc(p,SmoothEffects.GOLDEN_AEGIS,200,1,5);
                dec(p,SmoothEffects.EXHAUSTION,35);
            }
        }
    }

    // ------------------------------------------------------------
    // RULER / CLERIC
    // ------------------------------------------------------------
    private static void rulerTick(ServerPlayerEntity p) {
        if(p.age%600==0 && p.getArmor()<=10
                && has(p,PuffishSkillsIntegration.RULER,SkillNodeIds.clericAltruism))
            inc(p,SmoothEffects.SPELLFORGED,605,1,2);
    }

    // ------------------------------------------------------------
    // SHARED COMBAT TRIGGERS
    // ------------------------------------------------------------
    public static void onMeleeHit(ServerPlayerEntity p, LivingEntity target) {
        if (p.hasStatusEffect(SmoothEffects.BERSERKING)) {
            inc(p,StatusEffects.HASTE,200,1,3);
            inc(p,StatusEffects.STRENGTH,200,1,3);
            inc(p,StatusEffects.SPEED,200,1,3);
        }
        if(has(p,PuffishSkillsIntegration.BERSERKER,SkillNodeIds.berserkerExploit)
                && target.hasStatusEffect(SmoothEffects.IMMOBILIZE))
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS,120,1,false,false,true));

        // applies Backstab + Opportunistic Mastery on every melee hit;
        // only Exploitation is specifically gated behind a back/stealth break.
        if(has(p,PuffishSkillsIntegration.ASSASSIN,SkillNodeIds.rogueBackstab) && behind(p,target))
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS,60,0,false,false,true));

        if(has(p,PuffishSkillsIntegration.ASSASSIN,SkillNodeIds.rogueOpportunisticMastery)) {
            int duration=80;
            if(has(p,PuffishSkillsIntegration.ASSASSIN,SkillNodeIds.rogueOpportunisticMasterySkilled))duration+=160;
            else if(has(p,PuffishSkillsIntegration.ASSASSIN,SkillNodeIds.rogueOpportunisticMasteryProficient))duration+=80;
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON,duration,2,false,false,true));
        }

        if(p.hasStatusEffect(SmoothEffects.STEALTH) && behind(p,target)) {
            if(has(p,PuffishSkillsIntegration.ASSASSIN,SkillNodeIds.rogueExploitation))
                inc(target,SmoothEffects.DEATH_MARK,80,1,3);
        }

        if(p.hasStatusEffect(SmoothEffects.SIPHONING_STRIKES)) {
            p.heal((float)(p.getAttributeValue(net.minecraft.entity.attribute.EntityAttributes.GENERIC_ATTACK_DAMAGE)*0.15));
            dec(p,SmoothEffects.SIPHONING_STRIKES,1);
            for(StatusEffectInstance effect:new java.util.ArrayList<>(target.getStatusEffects())) {
                if(effect.getEffectType().isBeneficial()) {
                    target.removeStatusEffect(effect.getEffectType());
                    break;
                }
            }
        }
        if(has(p,PuffishSkillsIntegration.ASSASSIN,SkillNodeIds.rogueSpecialisationSiphoningStrikesVanish)
                && p.hasStatusEffect(SmoothEffects.SIPHONING_STRIKES))
            p.removeStatusEffect(SmoothEffects.REVEALED);

        if(has(p,PuffishSkillsIntegration.FOREIGNER,SkillNodeIds.spellbladeSpellweaving)) {
            boolean enhanced=p.hasStatusEffect(SmoothEffects.SPELLWEAVER);
            if(p.getRandom().nextInt(100)<(enhanced?30:15)) {
                String[] spells = enhanced ? SPELLWEAVING_ENHANCED : SPELLWEAVING_BASIC;
                InternalSpellRuntime.target(p,"smooth_classes:"+spells[p.getRandom().nextInt(spells.length)],target,1F);
                if(AbilityRuntime.hasTalent(p,ForeignerContent.SPELLWEAVER_HASTE.id()))
                    inc(p,StatusEffects.HASTE,100,1,5);
                if(AbilityRuntime.hasTalent(p,ForeignerContent.SPELLWEAVER_REGENERATION.id())
                        && p.getRandom().nextInt(100)<50)
                    inc(p,StatusEffects.REGENERATION,140,1,2);
            }
        }
    }

    /** Spell Engine cast hook. */
    public static void onSpellCast(ServerPlayerEntity p, List<Entity> targets, SpellSchool school) {
        // Wizard Spell Echo: uses a 15% random echo when a cast has targets.
        if(has(p,PuffishSkillsIntegration.CASTER,SkillNodeIds.wizardSpellEcho)
                && targets!=null && !targets.isEmpty() && p.getRandom().nextInt(100)<15)
            inc(p,SmoothEffects.ARCANE_VOLLEY,80,1,3);

        // Weapon Expert is actually a spell-cast passive in this runtime.
        if(has(p,PuffishSkillsIntegration.FOREIGNER,SkillNodeIds.spellbladeWeaponExpert)) {
            inc(p,SmoothEffects.MIGHT,60,1,3);
            if(p.getRandom().nextInt(100)<SmoothBalance.Foreigner.weaponExpertSpellforgedChance)inc(p,SmoothEffects.SPELLFORGED,80,1,3);
        }

        if(school==SpellSchools.HEALING) {
            if(has(p,PuffishSkillsIntegration.RULER,SkillNodeIds.clericHealingWard)
                    && targets!=null && p.getRandom().nextInt(100)<10)
                for(Entity e:targets)if(e instanceof LivingEntity l)inc(l,SmoothEffects.BARRIER,100,1,20);

            // re-casts the same healing spell on self. Smooth Classes uses the
            // healing result directly here so core Ruler remains independent of Paladins IDs.
            if(has(p,PuffishSkillsIntegration.RULER,SkillNodeIds.clericMutualMending)
                    && targets!=null && !targets.contains(p) && p.getRandom().nextInt(100)<20)
                p.heal(Math.max(1F,p.getMaxHealth()*0.08F));
        }
    }

    /**
     * Incoming damage pre-hook. Return false to cancel damage.
     * This is the class-passive equivalent of ServerPlayerEntityMixin.
     */
    public static boolean allowDamage(ServerPlayerEntity p) {
        if(has(p,PuffishSkillsIntegration.ASSASSIN,SkillNodeIds.rogueSmokeBomb)
                && p.getRandom().nextInt(100)<10) {
            for(LivingEntity e:enemies(p,6))
                e.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS,40,0,false,false,true));
            p.addStatusEffect(new StatusEffectInstance(SmoothEffects.IMMOBILIZING_AURA,40,0,false,false,true));
            SkillFx.sound(p, "soundeffect_32", 0.4F, 1.2F);
        }

        if(has(p,PuffishSkillsIntegration.ASSASSIN,SkillNodeIds.rogueEvasionMastery)) {
            int mastery=15;
            if(has(p,PuffishSkillsIntegration.ASSASSIN,SkillNodeIds.rogueDeflection)
                    && p.getMainHandStack().getItem() instanceof SwordItem
                    && p.getOffHandStack().getItem() instanceof SwordItem) mastery+=10;
            if(has(p,PuffishSkillsIntegration.ASSASSIN,SkillNodeIds.rogueEvasionMasterySkilled))mastery+=10;
            else if(has(p,PuffishSkillsIntegration.ASSASSIN,SkillNodeIds.rogueEvasionMasteryProficient))mastery+=5;
            StatusEffectInstance blade=p.getStatusEffect(SmoothEffects.BLADESTORM);
            if(blade!=null)mastery+=blade.getAmplifier()+1;
            if(p.hasStatusEffect(SmoothEffects.EVASION))mastery*=2;
            if(p.hasStatusEffect(SmoothEffects.AGILE) && p.getRandom().nextInt(100)<mastery){
                OptionalCompatRuntime.onEvasion(p);
                SkillFx.sound(p, "fx_skill_backstab", 1.0F, 1.0F);
                return false;
            }
        }
        return true;
    }

    /** Damage that was not evaded. Attacker may be null for environmental damage. */
    /** Shield-block event. triggers Saber Retribution and Exhaustive Recovery here too. */
    public static void onShieldHit(ServerPlayerEntity p, LivingEntity attacker) {
        if(has(p,PuffishSkillsIntegration.SABER,SkillNodeIds.crusaderRetribution)
                && attacker!=null && p.getRandom().nextInt(100)<15) {
            attacker.timeUntilRegen=0;
            attacker.damage(p.getDamageSources().playerAttack(p),
                    (float)Math.max(1D,p.getAttributeValue(net.minecraft.entity.attribute.EntityAttributes.GENERIC_ATTACK_DAMAGE)));
            attacker.timeUntilRegen=0;
        }
        if(has(p,PuffishSkillsIntegration.SABER,SkillNodeIds.crusaderExhaustiveRecovery)
                && p.getRandom().nextInt(100)<15) {
            p.heal(Math.max(1F,p.getMaxHealth()*0.10F));
            inc(p,SmoothEffects.EXHAUSTION,300,9,99);
        }
    }

    public static void onDamaged(ServerPlayerEntity p, LivingEntity attacker) {
        if(p.hasStatusEffect(SmoothEffects.STEALTH)
                && has(p,PuffishSkillsIntegration.ASSASSIN,SkillNodeIds.rogueFleetfooted))
            inc(p,StatusEffects.SPEED,40,1,6);

        if(has(p,PuffishSkillsIntegration.SABER,SkillNodeIds.crusaderRetribution)
                && attacker!=null && p.getRandom().nextInt(100)<15) {
            attacker.timeUntilRegen=0;
            attacker.damage(p.getDamageSources().playerAttack(p),
                    (float)Math.max(1D,p.getAttributeValue(net.minecraft.entity.attribute.EntityAttributes.GENERIC_ATTACK_DAMAGE)));
            attacker.timeUntilRegen=0;
        }

        if(has(p,PuffishSkillsIntegration.SABER,SkillNodeIds.crusaderExhaustiveRecovery)
                && p.getRandom().nextInt(100)<15) {
            p.heal(Math.max(1F,p.getMaxHealth()*0.10F));
            inc(p,SmoothEffects.EXHAUSTION,300,9,99);
        }
    }

    private static List<LivingEntity> enemies(ServerPlayerEntity p,double radius) {
        return p.getWorld().getEntitiesByClass(LivingEntity.class,p.getBoundingBox().expand(radius),
                e->e!=p && e.isAlive() && !e.isTeammate(p)
                        && (!(e instanceof PlayerEntity other)||p.shouldDamagePlayer(other)));
    }

    private static List<LivingEntity> pets(ServerPlayerEntity p,double radius) {
        return p.getWorld().getEntitiesByClass(LivingEntity.class,p.getBoundingBox().expand(radius),
                e->e instanceof TameableEntity tame && tame.isOwner(p));
    }

    private static boolean behind(ServerPlayerEntity p,LivingEntity target) {
        Vec3d toPlayer=p.getPos().subtract(target.getPos()).normalize();
        return target.getRotationVec(1F).normalize().dotProduct(toPlayer)<-0.45D;
    }

    private static void inc(LivingEntity e,StatusEffect effect,int duration,int stacks,int maxStacks) {
        StatusEffectInstance current=e.getStatusEffect(effect);
        int amplifier=current==null?Math.max(0,stacks-1):Math.min(Math.max(0,maxStacks-1),current.getAmplifier()+stacks);
        e.addStatusEffect(new StatusEffectInstance(effect,duration,amplifier,false,false,true));
    }

    private static void dec(LivingEntity e,StatusEffect effect,int stacks) {
        StatusEffectInstance current=e.getStatusEffect(effect);
        if(current==null)return;
        int amplifier=current.getAmplifier()-stacks;
        if(amplifier<0)e.removeStatusEffect(effect);
        else e.addStatusEffect(new StatusEffectInstance(effect,current.getDuration(),amplifier,false,false,true));
    }
}
