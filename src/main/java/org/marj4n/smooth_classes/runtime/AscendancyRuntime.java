package org.marj4n.smooth_classes.runtime;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.marj4n.smooth_classes.effects.SmoothEffects;
import org.marj4n.smooth_classes.effects.SourceStatusEffectInstance;
import org.marj4n.smooth_classes.integration.PuffishSkillsIntegration;
import org.marj4n.smooth_classes.integration.SimplySkillsNodeIds;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Continued-style Ascendancy gameplay, isolated from optional compatibility mods. */
public final class AscendancyRuntime {
    private AscendancyRuntime() {}

    public static final String[] ABILITIES = {
            "righteous_hammers","bone_armor","cyclonic_cleave","magic_circle","arcane_slash",
            "agony","torment","rapidfire","cataclysm","ghostwalk","skyward_sunder",
            "righteous_shield","chainbreaker"
    };

    public static boolean unlocked(ServerPlayerEntity p, String ability) {
        String node = switch (ability) {
            case "righteous_hammers" -> SimplySkillsNodeIds.ascendancyRighteousHammers;
            case "bone_armor" -> SimplySkillsNodeIds.ascendancyBoneArmor;
            case "cyclonic_cleave" -> SimplySkillsNodeIds.ascendancyCyclonicCleave;
            case "magic_circle" -> SimplySkillsNodeIds.ascendancyMagicCircle;
            case "arcane_slash" -> SimplySkillsNodeIds.ascendancyArcaneSlash;
            case "agony" -> SimplySkillsNodeIds.ascendancyAgony;
            case "torment" -> SimplySkillsNodeIds.ascendancyTorment;
            case "rapidfire" -> SimplySkillsNodeIds.ascendancyRapidfire;
            case "cataclysm" -> SimplySkillsNodeIds.ascendancyCataclysm;
            case "ghostwalk" -> SimplySkillsNodeIds.ascendancyGhostwalk;
            case "skyward_sunder" -> SimplySkillsNodeIds.ascendancySkywardSunder;
            case "righteous_shield" -> SimplySkillsNodeIds.ascendancyRighteousShield;
            case "chainbreaker" -> SimplySkillsNodeIds.ascendancyChainbreaker;
            default -> null;
        };
        return node != null && PuffishSkillsIntegration.isSkillUnlocked(PuffishSkillsIntegration.ASCENDANCY,node,p);
    }

    public static int points(ServerPlayerEntity p) {
        return PuffishSkillsIntegration.countUnlockedSkills(PuffishSkillsIntegration.ASCENDANCY,p);
    }

    public static ExecutionResult cast(ServerPlayerEntity p, String ability) {
        if (!unlocked(p,ability)) return ExecutionResult.failure(ability+" is not unlocked in Puffish Ascendancy.");
        int pts=points(p);
        return switch (ability) {
            case "righteous_hammers" -> effect(p,SmoothEffects.RIGHTEOUS_HAMMERS,800,5,ability);
            case "bone_armor" -> effect(p,SmoothEffects.BONE_ARMOR,800,3+pts/10,ability);
            case "cyclonic_cleave" -> cyclonicCleave(p);
            case "magic_circle" -> magicCircle(p,pts);
            case "arcane_slash" -> arcaneSlash(p,pts);
            case "agony" -> curse(p,SmoothEffects.AGONY,200+pts,ability,pts);
            case "torment" -> curse(p,SmoothEffects.TORMENT,160+pts,ability,pts);
            case "rapidfire" -> effect(p,SmoothEffects.RAPIDFIRE,120+pts,0,ability);
            case "cataclysm" -> cataclysm(p);
            case "ghostwalk" -> ghostwalk(p);
            case "skyward_sunder" -> effect(p,SmoothEffects.SKYWARD_SUNDER,45,0,ability);
            case "righteous_shield" -> righteousShield(p);
            case "chainbreaker" -> chainbreaker(p,pts);
            default -> ExecutionResult.failure("Unknown ascendancy ability: "+ability);
        };
    }

    private static ExecutionResult effect(ServerPlayerEntity p, StatusEffect fx,int duration,int amp,String name){
        p.addStatusEffect(new StatusEffectInstance(fx,duration,amp,false,false,true));
        return ExecutionResult.success(1,name);
    }

    private static ExecutionResult magicCircle(ServerPlayerEntity p,int pts){
        p.addStatusEffect(new StatusEffectInstance(SmoothEffects.MAGIC_CIRCLE,240+pts,0,false,false,true));
        p.addStatusEffect(new StatusEffectInstance(SmoothEffects.IMMOBILIZE,25,0,false,false,true));
        return ExecutionResult.success(1,"magic_circle");
    }

    private static ExecutionResult cyclonicCleave(ServerPlayerEntity p){
        boolean cast=InternalSpellRuntime.target(p,"smooth_classes:cyclonic_cleave",p,3F);
        return ExecutionResult.success(cast?1:0,"cyclonic_cleave");
    }

    private static ExecutionResult arcaneSlash(ServerPlayerEntity p,int pts){
        if (!ArcaneSlashVisuals.hasSword(p))
            return ExecutionResult.failure("Arcane Slash requires a sword in your main hand.");
        if (p.hasStatusEffect(SmoothEffects.ARCANE_SLASH))
            return ExecutionResult.failure("Arcane Slash is already charging.");
        if (!ArcaneSlashChargeRuntime.isHeld(p))
            return ExecutionResult.failure("Hold the Ascendancy key until Arcane Slash is released.");
        boolean cast=InternalSpellRuntime.target(p,"smooth_classes:arcane_slash",p,1F);
        if(cast){
            ArcaneSlashChargeRuntime.begin(p);
            if(pts>9) increment(p,SmoothEffects.ARCANE_ATTUNEMENT,60,1+pts/10,19);
        }
        return cast ? ExecutionResult.success(1,"arcane_slash")
                : ExecutionResult.failure("Arcane Slash spell unavailable.");
    }

    private static ExecutionResult curse(ServerPlayerEntity p,StatusEffect fx,int duration,String name,int pts){
        LivingEntity target=nearestEnemy(p,10);
        if(target==null)return ExecutionResult.failure("No valid target within 10 blocks.");
        target.addStatusEffect(new SourceStatusEffectInstance(fx,duration,0,false,false,true,p));
        ContinuedFx.sound(p,"magic_shamanic_spell_04",0.2F,1F);
        if(fx==SmoothEffects.TORMENT)
            ContinuedFx.beam(p,target,net.minecraft.particle.ParticleTypes.SMOKE,20);
        if(fx==SmoothEffects.TORMENT && pts>29)
            target.addStatusEffect(new SourceStatusEffectInstance(SmoothEffects.TAUNTED,duration,0,false,false,true,p));
        return ExecutionResult.success(1,name);
    }

    private static ExecutionResult cataclysm(ServerPlayerEntity p){
        boolean cast=InternalSpellRuntime.target(p,"smooth_classes:cataclysm",p,3F);
        if(cast)ContinuedFx.sound(p,"energy_charge",0.3F,1F);
        return cast?ExecutionResult.success(1,"cataclysm"):ExecutionResult.failure("Cataclysm spell unavailable.");
    }

    private static ExecutionResult ghostwalk(ServerPlayerEntity p){
        p.addStatusEffect(new StatusEffectInstance(SmoothEffects.GHOSTWALK,120,0,false,false,true));
        p.addStatusEffect(new StatusEffectInstance(net.minecraft.entity.effect.StatusEffects.SPEED,120,2,false,false,true));
        return ExecutionResult.success(1,"ghostwalk");
    }

    private static ExecutionResult righteousShield(ServerPlayerEntity p){
        if(!p.hasStatusEffect(SmoothEffects.GOLDEN_AEGIS))
            return ExecutionResult.failure("Righteous Shield requires Golden Aegis.");
        boolean cast=InternalSpellRuntime.target(p,"smooth_classes:righteous_shield",p,1F);
        return ExecutionResult.success(cast?1:0,"righteous_shield");
    }

    private static ExecutionResult chainbreaker(ServerPlayerEntity p,int pts){
        List<StatusEffect> remove=new ArrayList<>();
        for(StatusEffectInstance e:p.getStatusEffects())
            if(e.getEffectType().getCategory()==StatusEffectCategory.HARMFUL)remove.add(e.getEffectType());
        remove.forEach(p::removeStatusEffect);
        increment(p,SmoothEffects.MIGHT,120,1+pts/10,19);
        increment(p,SmoothEffects.MARKSMANSHIP,120,1+pts/10,19);
        if(pts>29)p.addStatusEffect(new StatusEffectInstance(SmoothEffects.UNDYING,120,0,false,false,true));
        ContinuedFx.sound(p,"spell_arcane_cast",0.3F,1.1F);
        ContinuedFx.plane(p,net.minecraft.particle.ParticleTypes.POOF,p.getBlockPos(),1,0,0.1,0);
        return ExecutionResult.success(1,"chainbreaker");
    }

    public static boolean incomingDamage(ServerPlayerEntity p, net.minecraft.entity.damage.DamageSource source,float amount){
        LivingEntity attacker=source.getAttacker() instanceof LivingEntity l?l:null;
        if(attacker!=null && attacker.getStatusEffect(SmoothEffects.TORMENT) instanceof SourceStatusEffectInstance torment
                && torment.getSourceEntity()==p){
            attacker.timeUntilRegen=0;
            attacker.damage(p.getDamageSources().indirectMagic(p,p),amount);
            attacker.timeUntilRegen=0;
            ContinuedFx.beam(p,attacker,net.minecraft.particle.ParticleTypes.SMOKE,12);
            return false;
        }
        if(p.hasStatusEffect(SmoothEffects.BONE_ARMOR)){
            decrement(p,SmoothEffects.BONE_ARMOR,1);
        }
        return true;
    }

    public static void spellProjectileHit(ServerPlayerEntity owner, LivingEntity target){
        if(target.getStatusEffect(SmoothEffects.AGONY) instanceof SourceStatusEffectInstance agony){
            target.timeUntilRegen=0;
            target.damage(owner.getDamageSources().indirectMagic(owner,owner),
                    Math.max(1F,highestSpellPower(owner)*0.1F));
            target.timeUntilRegen=0;
            if(agony.getSourceEntity() instanceof ServerPlayerEntity sourcePlayer && points(sourcePlayer)>29)
                owner.heal(Math.max(1F,SpellPowerRuntime.healing(sourcePlayer,0.1)));
        }
    }

    /** Continued: >29 ascendancy points grants one Golden Aegis stack every 400 ticks. */
    public static void serverTick(ServerPlayerEntity p){
        if(unlocked(p,"righteous_shield")&&points(p)>29&&p.age%400==0)
            increment(p,SmoothEffects.GOLDEN_AEGIS,2400,1,15+points(p)/10);
    }

    public static void shieldHit(ServerPlayerEntity p){
        if(unlocked(p,"righteous_shield"))
            increment(p,SmoothEffects.GOLDEN_AEGIS,2400,1,15+points(p)/10);
    }

    private static float highestSpellPower(ServerPlayerEntity p){
        return Math.max(SpellPowerRuntime.arcane(p,1),Math.max(SpellPowerRuntime.fire(p,1),
                Math.max(SpellPowerRuntime.frost(p,1),Math.max(SpellPowerRuntime.lightning(p,1),
                        Math.max(SpellPowerRuntime.soul(p,1),SpellPowerRuntime.healing(p,1))))));
    }

    private static LivingEntity nearestEnemy(ServerPlayerEntity p,double radius){
        return CombatRuntime.nearbyEnemies(p,radius).stream()
                .min(Comparator.comparingDouble(p::squaredDistanceTo)).orElse(null);
    }

    private static void increment(LivingEntity e,StatusEffect fx,int duration,int amount,int max){
        StatusEffectInstance old=e.getStatusEffect(fx);
        int amp=old==null?Math.max(0,amount-1):Math.min(max-1,old.getAmplifier()+amount);
        e.addStatusEffect(new StatusEffectInstance(fx,duration,amp,false,false,true));
    }
    private static void decrement(LivingEntity e,StatusEffect fx,int amount){
        StatusEffectInstance old=e.getStatusEffect(fx);if(old==null)return;
        int amp=old.getAmplifier()-amount;
        if(amp<0)e.removeStatusEffect(fx);
        else e.addStatusEffect(new StatusEffectInstance(fx,old.getDuration(),amp,false,false,true));
    }
}
