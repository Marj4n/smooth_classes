package org.marj4n.smooth_classes.runtime;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.server.network.ServerPlayerEntity;
import org.marj4n.smooth_classes.effects.SmoothEffects;
import org.marj4n.smooth_classes.effects.SourceStatusEffectInstance;
import org.marj4n.smooth_classes.integration.PuffishSkillsIntegration;
import org.marj4n.smooth_classes.integration.SkillNodeIds;

import java.util.ArrayList;
import java.util.List;

/** Ascendancy gameplay, isolated from optional compatibility mods. */
public final class AscendancyRuntime {
    private AscendancyRuntime() {}

    public static final String[] ABILITIES = {
            "righteous_hammers","bone_armor","cyclonic_cleave","magic_circle","arcane_slash",
            "agony","torment","rapidfire","cataclysm","ghostwalk","skyward_sunder",
            "righteous_shield","chainbreaker"
    };

    public static boolean unlocked(ServerPlayerEntity p, String ability) {
        String node = switch (ability) {
            case "righteous_hammers" -> SkillNodeIds.ascendancyRighteousHammers;
            case "bone_armor" -> SkillNodeIds.ascendancyBoneArmor;
            case "cyclonic_cleave" -> SkillNodeIds.ascendancyCyclonicCleave;
            case "magic_circle" -> SkillNodeIds.ascendancyMagicCircle;
            case "arcane_slash" -> SkillNodeIds.ascendancyArcaneSlash;
            case "agony" -> SkillNodeIds.ascendancyAgony;
            case "torment" -> SkillNodeIds.ascendancyTorment;
            case "rapidfire" -> SkillNodeIds.ascendancyRapidfire;
            case "cataclysm" -> SkillNodeIds.ascendancyCataclysm;
            case "ghostwalk" -> SkillNodeIds.ascendancyGhostwalk;
            case "skyward_sunder" -> SkillNodeIds.ascendancySkywardSunder;
            case "righteous_shield" -> SkillNodeIds.ascendancyRighteousShield;
            case "chainbreaker" -> SkillNodeIds.ascendancyChainbreaker;
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
            case "righteous_hammers" -> righteousHammers(p,pts);
            case "bone_armor" -> effect(p,SmoothEffects.BONE_ARMOR,pts>=60?600:400,AscendancyBalance.boneCharges(pts)-1,ability);
            case "cyclonic_cleave" -> cyclonicCleave(p);
            case "magic_circle" -> BloodRainRuntime.cast(p,pts);
            case "arcane_slash" -> arcaneSlash(p,pts);
            case "agony" -> WhenOnHighRuntime.cast(p,pts);
            case "torment" -> TormentRuntime.cast(p,pts);
            case "rapidfire" -> rapidfire(p,pts);
            case "cataclysm" -> cataclysm(p);
            case "ghostwalk" -> ghostwalk(p,pts);
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

    private static ExecutionResult righteousHammers(ServerPlayerEntity p,int pts){
        int count=pts>=60?12:pts>=30?8:6;
        int duration=pts>=60?700:pts>=30?600:500;
        return effect(p,SmoothEffects.RIGHTEOUS_HAMMERS,duration,count-1,"righteous_hammers");
    }

    private static ExecutionResult rapidfire(ServerPlayerEntity p,int pts){
        var item=p.getMainHandStack().getItem();
        if(!(item instanceof net.minecraft.item.BowItem) && !(item instanceof net.minecraft.item.CrossbowItem))
            return ExecutionResult.failure("Rapidfire requires a Bow or Crossbow in your main hand.");
        return effect(p,SmoothEffects.RAPIDFIRE,AscendancyBalance.rapidfireDuration(pts),0,"rapidfire");
    }
    public static void boneArmorHit(ServerPlayerEntity p){
        var fx=p.getStatusEffect(SmoothEffects.BONE_ARMOR);
        if(fx==null)return;
        int pts=points(p);
        boolean last=fx.getAmplifier()==0;
        decrement(p,SmoothEffects.BONE_ARMOR,1);
        if(pts>=60)p.heal(p.getMaxHealth()*.04F);
        if(last&&pts>=60){
            p.addStatusEffect(new StatusEffectInstance(SmoothEffects.BARRIER,100,2,false,false,true));
            p.addStatusEffect(new StatusEffectInstance(net.minecraft.entity.effect.StatusEffects.REGENERATION,120,3,false,false,true));
        }else if(last&&pts>=30){
            p.addStatusEffect(new StatusEffectInstance(SmoothEffects.BARRIER,60,0,false,false,true));
            p.addStatusEffect(new StatusEffectInstance(net.minecraft.entity.effect.StatusEffects.REGENERATION,80,1,false,false,true));
        }
    }
    private static ExecutionResult cyclonicCleave(ServerPlayerEntity p){
        boolean cast=InternalSpellRuntime.targetUniversal(p,"smooth_classes:cyclonic_cleave",p,1F);
        return cast?ExecutionResult.success(1,"cyclonic_cleave"):ExecutionResult.failure("Cyclonic Cleave spell unavailable.");
    }

    private static ExecutionResult arcaneSlash(ServerPlayerEntity p,int pts){
        if (p.hasStatusEffect(SmoothEffects.ARCANE_SLASH))
            return ExecutionResult.failure("Arcane Slash is already charging.");
        if (!ArcaneSlashChargeRuntime.isHeld(p))
            return ExecutionResult.failure("Hold the Ascendancy key until Arcane Slash is released.");
        boolean cast=InternalSpellRuntime.targetUniversal(p,"smooth_classes:arcane_slash",p,1F);
        if(cast){
            ArcaneSlashChargeRuntime.begin(p);
            if(pts>9) increment(p,SmoothEffects.ARCANE_ATTUNEMENT,60,1+pts/10,19);
        }
        return cast ? ExecutionResult.success(1,"arcane_slash")
                : ExecutionResult.failure("Arcane Slash spell unavailable.");
    }

    private static ExecutionResult cataclysm(ServerPlayerEntity p){
        boolean cast=InternalSpellRuntime.targetUniversal(p,"smooth_classes:cataclysm",p,1F);
        if(cast)SkillFx.sound(p,"energy_charge",0.3F,1F);
        return cast?ExecutionResult.success(1,"cataclysm"):ExecutionResult.failure("Cataclysm spell unavailable.");
    }

    private static ExecutionResult ghostwalk(ServerPlayerEntity p,int pts){
        int duration=pts>=60?200:120;
        p.addStatusEffect(new StatusEffectInstance(SmoothEffects.GHOSTWALK,duration,0,false,false,true));
        p.addStatusEffect(new StatusEffectInstance(net.minecraft.entity.effect.StatusEffects.SPEED,duration,pts>=60?3:2,false,false,true));
        return ExecutionResult.success(1,"ghostwalk");
    }

    private static ExecutionResult righteousShield(ServerPlayerEntity p){
        if(!p.hasStatusEffect(SmoothEffects.GOLDEN_AEGIS))
            return ExecutionResult.failure("Righteous Shield requires Golden Aegis.");
        boolean cast=InternalSpellRuntime.targetUniversal(p,"smooth_classes:righteous_shield",p,1F);
        return cast?ExecutionResult.success(1,"righteous_shield"):ExecutionResult.failure("Righteous Shield spell unavailable.");
    }

    private static ExecutionResult chainbreaker(ServerPlayerEntity p,int pts){
        List<StatusEffect> remove=new ArrayList<>();
        for(StatusEffectInstance e:p.getStatusEffects())
            if(e.getEffectType().getCategory()==StatusEffectCategory.HARMFUL)remove.add(e.getEffectType());
        remove.forEach(p::removeStatusEffect);
        int stacks=pts>=60?10:1+AscendancyBalance.points(pts)/10;
        increment(p,SmoothEffects.MIGHT,160,stacks,19);
        increment(p,SmoothEffects.MARKSMANSHIP,160,stacks,19);
        increment(p,SmoothEffects.SPELLFORGED,160,stacks,19);
        if(pts>=60){
            p.addStatusEffect(new StatusEffectInstance(net.minecraft.entity.effect.StatusEffects.RESISTANCE,160,2,false,false,true));
            p.addStatusEffect(new StatusEffectInstance(net.minecraft.entity.effect.StatusEffects.STRENGTH,160,2,false,false,true));
            p.addStatusEffect(new StatusEffectInstance(net.minecraft.entity.effect.StatusEffects.HASTE,160,2,false,false,true));
            p.addStatusEffect(new StatusEffectInstance(net.minecraft.entity.effect.StatusEffects.SPEED,160,2,false,false,true));
            p.addStatusEffect(new StatusEffectInstance(net.minecraft.entity.effect.StatusEffects.ABSORPTION,160,3,false,false,true));
            p.addStatusEffect(new StatusEffectInstance(SmoothEffects.BARRIER,100,2,false,false,true));
        }else if(pts>=30){
            p.addStatusEffect(new StatusEffectInstance(net.minecraft.entity.effect.StatusEffects.RESISTANCE,80,1,false,false,true));
            p.addStatusEffect(new StatusEffectInstance(SmoothEffects.BARRIER,60,0,false,false,true));
        }
        SkillFx.orbit(p,net.minecraft.particle.ParticleTypes.END_ROD,1.5,32);
        SkillFx.sound(p,"spell_arcane_cast",0.3F,1.1F);
        SkillFx.plane(p,net.minecraft.particle.ParticleTypes.POOF,p.getBlockPos(),1,0,0.1,0);
        return ExecutionResult.success(1,"chainbreaker");
    }

    public static boolean incomingDamage(ServerPlayerEntity p, net.minecraft.entity.damage.DamageSource source,float amount){
        LivingEntity attacker=source.getAttacker() instanceof LivingEntity l?l:null;
        if(attacker!=null && attacker.getStatusEffect(SmoothEffects.TORMENT) instanceof SourceStatusEffectInstance torment
                && torment.getSourceEntity()==p){
            attacker.timeUntilRegen=0;
            attacker.damage(p.getDamageSources().indirectMagic(p,p),amount);
            attacker.timeUntilRegen=0;
            SkillFx.beam(p,attacker,net.minecraft.particle.ParticleTypes.SMOKE,12);
            return false;
        }
        // Bone charges are consumed only after a successful incoming hit.
        return true;
    }

    public static void spellProjectileHit(ServerPlayerEntity owner, LivingEntity target){
        if(target.getStatusEffect(SmoothEffects.AGONY) instanceof SourceStatusEffectInstance agony){
            target.timeUntilRegen=0;
            target.damage(owner.getDamageSources().indirectMagic(owner,owner),
                    Math.max(1F,SpellPowerRuntime.strongest(owner,0.1)));
            target.timeUntilRegen=0;
            if(agony.getSourceEntity() instanceof ServerPlayerEntity sourcePlayer && points(sourcePlayer)>29)
                owner.heal(Math.max(1F,SpellPowerRuntime.healing(sourcePlayer,0.1)));
        }
    }

    /** Righteous Shield is class-agnostic: blocking accelerates Aegis, but is never mandatory. */
    public static void serverTick(ServerPlayerEntity p){
        if(!unlocked(p,"righteous_shield"))return;
        int pts=points(p);
        int interval=pts>=60?40:pts>=30?200:300;
        if((p.age % interval)!=0)return;
        int cap=pts>=60?30:pts>=30?15+pts/10:5+AscendancyBalance.points(pts)/10;
        increment(p,SmoothEffects.GOLDEN_AEGIS,2400,1,cap);
    }

    public static void shieldHit(ServerPlayerEntity p){
        if (!unlocked(p,"righteous_shield")) return;
        int pts=points(p);
        increment(p,SmoothEffects.GOLDEN_AEGIS,2400,1,pts>=60?30:15+pts/10);
    }

    private static void increment(LivingEntity e,StatusEffect fx,int duration,int amount,int max){
        StatusEffectInstance old=e.getStatusEffect(fx);
        int amp=old==null?Math.max(0,amount-1):Math.min(max-1,old.getAmplifier()+amount);
        e.addStatusEffect(new StatusEffectInstance(fx,duration,amp,false,false,true));
    }
    private static void decrement(LivingEntity e,StatusEffect fx,int amount){
        StatusEffectInstance old=e.getStatusEffect(fx);if(old==null)return;
        int amp=old.getAmplifier()-amount;
        e.removeStatusEffect(fx);
        if(amp>=0)e.addStatusEffect(new StatusEffectInstance(fx,old.getDuration(),amp,false,false,true));
    }
}
