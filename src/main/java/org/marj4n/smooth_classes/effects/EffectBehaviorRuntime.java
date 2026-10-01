package org.marj4n.smooth_classes.effects;

import org.marj4n.smooth_classes.integration.PuffishSkillsIntegration;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.spell_engine.fx.SpellEngineParticles;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.spell_engine.entity.SpellProjectile;
import org.marj4n.smooth_classes.integration.OptionalCompatRuntime;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;
import org.marj4n.smooth_classes.content.assassin.AssassinContent;
import org.marj4n.smooth_classes.content.foreigner.ForeignerContent;
import org.marj4n.smooth_classes.content.saber.SaberContent;
import org.marj4n.smooth_classes.content.ruler.RulerContent;
import org.marj4n.smooth_classes.content.caster.runtime.CasterRuntime;
import org.marj4n.smooth_classes.entity.AvengerMinionEntity;
import org.marj4n.smooth_classes.runtime.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.HashMap;

/**
 * Server-side gameplay for the registered Smooth Classes status-effect surface.
 *
 * intentionally has many marker/state effects whose update method is
 * empty; those remain marker effects here. Effects with real update behavior
 * are implemented here without introducing optional-mod hard dependencies.
 */
public final class EffectBehaviorRuntime {
    private EffectBehaviorRuntime() {}
    private static final Map<UUID,Integer> RAPIDFIRE_ARROW_COUNT=new HashMap<>();

    static boolean hasTickBehavior(String id) {
        return switch (id) {
            case "rage", "overload", "immobilize", "immobilizing_aura", "exhaustion",
                 "stealth", "bladestorm", "elemental_surge", "elemental_impact",
                 "consecration", "sacred_onslaught", "focus", "melody_of_safety",
                 "bullrush", "leapslam", "earthshaker", "disenchantment", "magic_circle",
                 "righteous_hammers", "cyclonic_cleave", "arcane_slash", "rapidfire",
                 "cataclysm", "ghostwalk", "skyward_sunder", "righteous_shield",
                 "spellbreaking", "raging_javelin", "agony", "torment", "taunted",
                 "vitality_bond", "anointed", "shadow_aura", "static_charge",
                 "fanofblades", "frost_volley", "arcane_volley", "meteoric_wrath",
                 "barrier", "bone_armor", "undying", "crimson_revenant_charge", "crimson_revenant" -> true;
            default -> false;
        };
    }

    public static void applied(String id, LivingEntity entity, int amplifier) {
        if (!entity.getWorld().isClient()) {
            switch (id) {
                case "barrier", "golden_aegis" -> SkillFx.sound(entity, "spell_gain_barrier", 0.4F, 1F + amplifier / 10F);
                case "marksmanship" -> SkillFx.sound(entity, "activate_tower_beacon", 0.1F, 1F + amplifier / 10F);
                case "bone_armor" -> SkillFx.sound(entity, "magic_shamanic_spell_01", 0.2F, 1F + amplifier / 10F);
                case "anointed" -> SkillFx.sound(entity, "spell_celestial_hit", 0.1F, 1.4F);
                case "undying" -> SkillFx.sound(entity, "spell_celestial_hit", 0.1F, 1.4F);
                case "vitality_bond" -> SkillFx.sound(entity, "spell_radiant_hit", 0.1F, 1.5F);
                default -> { }
            }
        }
        if (!(entity instanceof ServerPlayerEntity p)) return;
        if ("ghostwalk".equals(id)) {
            int pts = PuffishSkillsIntegration.countUnlockedSkills(PuffishSkillsIntegration.ASCENDANCY,p);
            increment(p,SmoothEffects.SOULSHOCK,60,1+(pts/10),9);
        } else if ("skyward_sunder".equals(id) && p.hasStatusEffect(SmoothEffects.MIGHT)) {
            StatusEffectInstance might=p.getStatusEffect(SmoothEffects.MIGHT);
            if(might!=null) increment(p,SmoothEffects.BARRIER,might.getDuration(),might.getAmplifier()+1,9);
        }
    }

    public static void removed(String id, LivingEntity entity, int amplifier) {
        if ("arcane_slash".equals(id) && entity instanceof ServerPlayerEntity player)
            ArcaneSlashChargeRuntime.removed(player);
        if ("crimson_revenant_charge".equals(id) && entity instanceof ServerPlayerEntity player)
            org.marj4n.smooth_classes.content.berserker.runtime.BerserkerSpecialRuntime.chargeEffectRemoved(player);
        if ("crimson_revenant".equals(id) && entity instanceof ServerPlayerEntity player)
            org.marj4n.smooth_classes.content.berserker.runtime.BerserkerSpecialRuntime.activeEffectRemoved(player);
        if (entity instanceof ServerPlayerEntity player && switch (id) {
            case "sacred_onslaught", "arcane_slash", "rapidfire", "cataclysm",
                 "ghostwalk", "bullrush", "cyclonic_cleave", "skyward_sunder" -> true;
            default -> false;
        }) OptionalCompatRuntime.onChannelEnd(player);
        if ("anointed".equals(id) && !entity.getWorld().isClient())
            SkillFx.sound(entity, "spell_radiant_expire", 0.4F, 1F);
        if ("vitality_bond".equals(id) && !entity.getWorld().isClient())
            SkillFx.sound(entity, "spell_radiant_expire", 0.4F, 1F);
        if ("undying".equals(id) && !entity.getWorld().isClient()) {
            if (entity.getHealth() / entity.getMaxHealth() < 0.60F) {
                SkillFx.sound(entity, "soundeffect_36", 0.4F, 1.3F);
                SkillFx.plane(entity, ParticleTypes.SOUL, entity.getBlockPos(), 2, 0, 0.4, 0);
                SkillFx.plane(entity, ParticleTypes.SCULK_SOUL, entity.getBlockPos(), 2, 0, 0.6, 0);
            } else SkillFx.sound(entity, "spell_radiant_expire", 0.4F, 1F);
        }
        if ("ghostwalk".equals(id)) {
            entity.setNoGravity(false);
            entity.setInvisible(false);
        } else if ("undying".equals(id) && entity.getHealth()/entity.getMaxHealth() < 0.60F) {
            entity.damage(entity.getDamageSources().magic(),entity.getMaxHealth());
        } else if ("rapidfire".equals(id) && entity instanceof ServerPlayerEntity p) {
            RAPIDFIRE_ARROW_COUNT.remove(p.getUuid());

        }
    }

    public static void tick(String id, LivingEntity entity, int amplifier) {
        if (entity.getWorld().isClient()) return;
        switch (id) {
            case "rage" -> rage(entity, amplifier);
            case "overload" -> overload(entity, amplifier);
            case "immobilize" -> immobilize(entity);
            case "immobilizing_aura" -> immobilizingAura(entity);
            case "exhaustion" -> exhaustion(entity);
            case "stealth" -> stealth(entity);
            case "bladestorm" -> bladestorm(entity, amplifier);
            case "elemental_surge" -> elementalSurge(entity);
            case "elemental_impact" -> elementalImpact(entity);
            case "consecration" -> consecration(entity);
            case "sacred_onslaught" -> sacredOnslaught(entity);
            case "focus" -> focus(entity);
            case "melody_of_safety" -> melodyOfSafety(entity);
            case "bullrush" -> bullrush(entity);
            case "leapslam" -> leapSlam(entity);
            case "earthshaker" -> earthshaker(entity);
            case "disenchantment" -> disenchantment(entity);
            case "righteous_hammers" -> righteousHammers(entity, amplifier);
            case "cyclonic_cleave" -> cyclonicCleave(entity);
            case "arcane_slash" -> arcaneSlash(entity);
            case "rapidfire" -> rapidfire(entity);
            case "cataclysm" -> cataclysm(entity);
            case "ghostwalk" -> ghostwalk(entity);
            case "skyward_sunder" -> skywardSunder(entity);
            case "righteous_shield" -> righteousShield(entity);
            case "spellbreaking" -> spellbreaking(entity);
            case "raging_javelin" -> ragingJavelin(entity);
            case "agony", "torment", "taunted" -> curseTarget(entity);
            case "vitality_bond" -> vitalityBond(entity);
            case "anointed" -> anointed(entity);
            case "shadow_aura" -> shadowAura(entity, amplifier);
            case "static_charge" -> staticCharge(entity);
            case "fanofblades" -> fanOfBlades(entity);
            case "frost_volley" -> casterVolley(entity, "frost_arrow", 8);
            case "arcane_volley" -> casterVolley(entity, "arcane_bolt_lesser", 3);
            case "meteoric_wrath" -> meteoricWrath(entity);
            case "barrier" -> statusAura(entity, ParticleTypes.REVERSE_PORTAL, 0.85);
            case "bone_armor" -> boneArmorParticles(entity, amplifier);
            case "undying" -> { statusAura(entity, ParticleTypes.SOUL, 0.9); undyingWarning(entity); }
            case "crimson_revenant_charge" -> {
                crimsonRevenantChargeAura(entity);
                if (entity instanceof ServerPlayerEntity p) {
                    org.marj4n.smooth_classes.content.berserker.runtime.BerserkerSpecialRuntime.tickCharge(p);
                }
            }
            case "crimson_revenant" -> {
                crimsonRevenantAura(entity, amplifier);
                if (entity instanceof ServerPlayerEntity p) {
                    org.marj4n.smooth_classes.content.berserker.runtime.BerserkerSpecialRuntime.tickFrenzy(p);
                }
            }
            // Marker/state effects are consumed by combat, signature, projectile
            // and ascendancy hooks exactly where consumes them.
            default -> { }
        }
    }


    private static final DustParticleEffect CRIMSON_DUST = new DustParticleEffect(new Vector3f(1.0F, 0.15F, 0.15F), 1.45F);
    private static final DustParticleEffect CRIMSON_DUST_LARGE = new DustParticleEffect(new Vector3f(0.88F, 0.03F, 0.03F), 2.1F);
    private static final DustParticleEffect CRIMSON_MIST = new DustParticleEffect(new Vector3f(0.62F, 0.01F, 0.01F), 2.65F);
    private static final DustParticleEffect CRIMSON_MIST_SOFT = new DustParticleEffect(new Vector3f(0.78F, 0.04F, 0.04F), 2.15F);

    private static void crimsonRevenantChargeAura(LivingEntity entity) {
        if (!(entity.getWorld() instanceof net.minecraft.server.world.ServerWorld world)) return;
        if (entity.age % 2 != 0) return;

        // Keep body particles very light so third-person view stays readable.
        if (entity.age % 6 == 0) {
            double emberRadius = 0.22D;
            for (int i = 0; i < 2; i++) {
                double angle = entity.age * 0.18D + i * Math.PI;
                double x = entity.getX() + Math.cos(angle) * emberRadius;
                double z = entity.getZ() + Math.sin(angle) * emberRadius;
                double y = entity.getBodyY(0.42D + i * 0.16D);
                world.spawnParticles(CRIMSON_DUST, x, y, z, 1, 0.008D, 0.012D, 0.008D, 0.0D);
            }
        }

        // Red foot mist / dust like a crimson Earthshaker.
        crimsonRevenantFootMist(entity, world, true);
    }

    private static void crimsonRevenantAura(LivingEntity entity, int amplifier) {
        if (!(entity.getWorld() instanceof net.minecraft.server.world.ServerWorld world)) return;
        if (entity.age % 2 != 0) return;

        // Subtle body embers only, to avoid covering the player model in third person.
        if (entity.age % 6 == 0) {
            for (int i = 0; i < 3; i++) {
                double angle = entity.age * 0.11D + i * (Math.PI * 2.0D / 3.0D);
                double x = entity.getX() + Math.cos(angle) * 0.26D;
                double z = entity.getZ() + Math.sin(angle) * 0.26D;
                double y = entity.getBodyY(0.20D + i * 0.12D);
                world.spawnParticles((i == 1) ? CRIMSON_DUST_LARGE : CRIMSON_DUST, x, y, z,
                        1, 0.008D, 0.015D, 0.008D, 0.0D);
            }
        }

        // Main visual presence is low ground fog around the feet.
        crimsonRevenantFootMist(entity, world, false);

        if (entity.age % 10 == 0) {
            world.spawnParticles(CRIMSON_DUST, entity.getX(), entity.getBodyY(0.58D), entity.getZ(),
                    2, 0.10D, 0.18D, 0.10D, 0.0D);
        }
    }

    private static void crimsonRevenantFootMist(LivingEntity entity, net.minecraft.server.world.ServerWorld world, boolean charging) {
        double radius = charging ? 1.05D : 1.28D;
        int count = charging ? 8 : 12;

        // Full-red ground mist: no vanilla grey smoke particles here.
        for (int i = 0; i < count; i++) {
            double angle = entity.age * 0.10D + i * (Math.PI * 2.0D / count);
            double wave = 0.10D * Math.sin(entity.age * 0.18D + i * 0.8D);
            double ringRadius = radius + wave;
            double x = entity.getX() + Math.cos(angle) * ringRadius;
            double z = entity.getZ() + Math.sin(angle) * ringRadius;
            double y = entity.getY() + 0.035D + (i % 3) * 0.018D;

            world.spawnParticles((i & 1) == 0 ? CRIMSON_MIST : CRIMSON_MIST_SOFT,
                    x, y, z, 1, 0.055D, 0.018D, 0.055D, 0.0D);

            if (i % 3 == 0) {
                world.spawnParticles(CRIMSON_DUST, x, y + 0.03D, z,
                        1, 0.025D, 0.012D, 0.025D, 0.0D);
            }
        }

        // Dense red dust burst hugs the floor, similar to Tremor/Earthshaker dust.
        if (entity.age % 4 == 0) {
            world.spawnParticles(CRIMSON_MIST, entity.getX(), entity.getY() + 0.035D, entity.getZ(),
                    charging ? 4 : 7,
                    radius * 0.48D, 0.025D, radius * 0.48D, 0.0D);
            world.spawnParticles(CRIMSON_DUST_LARGE, entity.getX(), entity.getY() + 0.065D, entity.getZ(),
                    charging ? 3 : 5,
                    radius * 0.38D, 0.045D, radius * 0.38D, 0.0D);
        }
    }

    private static void rage(LivingEntity e, int amp) {
        if (amp > 25 && e.age % 10 == 0) decrement(e, SmoothEffects.EXHAUSTION, 1);
    }

    /** Bone fragments provide the Bone Armor visual without orbiting models. */
    private static void boneArmorParticles(LivingEntity entity, int amplifier) {
        if (entity.age % 8 != 0
                || !(entity.getWorld() instanceof net.minecraft.server.world.ServerWorld world)) return;
        int count = Math.min(amplifier + 1, 12);
        var particle = new ItemStackParticleEffect(ParticleTypes.ITEM, new ItemStack(Items.BONE));
        double radius = 1.2 * entity.getScaleFactor();
        for (int i = 0; i < count; i++) {
            double angle = Math.toRadians(entity.getWorld().getTime() * 6.0 - 45.0)
                    + i * Math.PI * 2.0 / count;
            world.spawnParticles(particle,
                    entity.getX() + Math.sin(angle) * radius,
                    entity.getY() + entity.getHeight() * 0.5,
                    entity.getZ() - Math.cos(angle) * radius,
                    1, 0.025, 0.04, 0.025, 0.015);
        }
    }

    private static void statusAura(LivingEntity entity, net.minecraft.particle.ParticleEffect effect, double radius) {
        if (entity.age%5!=0 || !(entity.getWorld() instanceof net.minecraft.server.world.ServerWorld world)) return;
        for (int i=0;i<6;i++) {
            double angle=(entity.age*0.12)+i*Math.PI/3;
            world.spawnParticles(effect,entity.getX()+Math.cos(angle)*radius,
                    entity.getY()+0.25+(i%3)*0.55,entity.getZ()+Math.sin(angle)*radius,
                    1,0,0,0,0);
        }
    }


    private static void shadowAura(LivingEntity bearer, int amplifier) {
        if (!(bearer.getWorld() instanceof net.minecraft.server.world.ServerWorld world)) return;
        SkillFx.orbit(bearer, ParticleTypes.SMOKE, 0.5, 3);
        if (bearer.age % Math.max(22 - amplifier * 2, 1) != 0) return;
        ServerPlayerEntity owner = null;
        if (bearer instanceof ServerPlayerEntity player) owner = player;
        else if (bearer instanceof AvengerMinionEntity minion
                && minion.getOwner() instanceof ServerPlayerEntity player) owner = player;
        if (owner == null) return;
        final ServerPlayerEntity caster = owner;
        for (LivingEntity target : world.getEntitiesByClass(LivingEntity.class,
                bearer.getBoundingBox().expand(2), target -> target != bearer && target.isAlive()
                        && OptionalCompatRuntime.canHarm(caster, target))) {
            target.timeUntilRegen = 0;
            target.damage(owner.getDamageSources().indirectMagic(owner, owner),
                    SpellPowerRuntime.soul(owner, amplifier / 5.0));
            SkillFx.beam(bearer, target, ParticleTypes.SMOKE, 5);
            target.timeUntilRegen = 0;
        }
        float drain = 1F + SpellPowerRuntime.soul(owner, amplifier / 10.0);
        if (bearer instanceof AvengerMinionEntity && bearer.getHealth() - 2F * drain < 0F) {
            bearer.removeStatusEffect(SmoothEffects.SHADOW_AURA);
            bearer.damage(bearer.getDamageSources().generic(), bearer.getMaxHealth());
        } else {
            bearer.setHealth(bearer.getHealth() - drain);
        }
    }

    private static void staticCharge(LivingEntity charged) {
        if (charged.age % 5 != 0 || !(charged.getWorld() instanceof net.minecraft.server.world.ServerWorld world)) return;
        StatusEffectInstance effect = charged.getStatusEffect(SmoothEffects.STATIC_CHARGE);
        if (!(effect instanceof SourceStatusEffectInstance sourced)
                || !(sourced.getSourceEntity() instanceof ServerPlayerEntity owner) || !owner.isAlive()) return;
        for (LivingEntity target : world.getEntitiesByClass(LivingEntity.class,
                charged.getBoundingBox().expand(9), e -> e != charged && e.isAlive()
                        && OptionalCompatRuntime.canHarm(owner, e))) {
            if (target.getRandom().nextInt(100) >= 30) continue;
            InternalSpellRuntime.target(owner, "smooth_classes:static_charge", target, 3F);
            SkillFx.beam(charged, target, SpellEngineParticles.lightning_arc_A.type(), 6);
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 80, 0, false, false, true));
            int remaining = effect.getAmplifier() - 1;
            if (remaining < 0) charged.removeStatusEffect(SmoothEffects.STATIC_CHARGE);
            else {
                target.addStatusEffect(new SourceStatusEffectInstance(SmoothEffects.STATIC_CHARGE,
                        effect.getDuration(), remaining, false, false, true, owner));
                charged.removeStatusEffect(SmoothEffects.STATIC_CHARGE);
            }
            CasterRuntime.onStaticChargeHit(owner, target);
            break;
        }
    }

    private static void fanOfBlades(LivingEntity bearer) {
        if (!(bearer instanceof ServerPlayerEntity player)
                || !AbilityRuntime.hasTalent(player, AssassinContent.EVASION_FAN_OF_BLADES.id())) return;
        boolean assault = AbilityRuntime.hasTalent(player, AssassinContent.EVASION_FAN_OF_BLADES_ASSAULT.id());
        int frequency = assault ? 5 : 20;
        if (player.age % frequency != 0) return;
        int range = 8;
        net.minecraft.util.math.BlockPos endpoint = player.getBlockPos().offset(player.getMovementDirection(), range);
        for (int i = range; i > 0; i--) {
            if (player.getWorld().getBlockState(endpoint).isAir()
                    && player.getWorld().getBlockState(endpoint.up()).isAir()) break;
            endpoint = player.getBlockPos().offset(player.getMovementDirection(), i);
        }
        Vec3d end = Vec3d.ofCenter(endpoint);
        net.minecraft.util.math.Box corridor = new net.minecraft.util.math.Box(player.getPos(), end).expand(6);
        for (LivingEntity target : player.getWorld().getEntitiesByClass(LivingEntity.class, corridor,
                e -> e != player && e.isAlive() && OptionalCompatRuntime.canHarm(player, e))) {
            InternalSpellRuntime.target(player,
                    assault ? "smooth_classes:fan_of_blades_assault" : "smooth_classes:fan_of_blades", target, 1F);
            if (AbilityRuntime.hasTalent(player, AssassinContent.EVASION_FAN_OF_BLADES_DISENCHANTMENT.id()))
                target.addStatusEffect(new StatusEffectInstance(SmoothEffects.DISENCHANTMENT, 160, 0, false, false, true));
        }
        if (AbilityRuntime.hasTalent(player, AssassinContent.EVASION_BLADESTORM.id())
                && player.getRandom().nextInt(100) < 35 + frequency)
            increment(player, SmoothEffects.BLADESTORM, 400, 1, 20);
        decrement(player, SmoothEffects.FANOFBLADES, 1);
    }

    private static void casterVolley(LivingEntity bearer, String spell, int frequency) {
        if (!(bearer instanceof ServerPlayerEntity player) || player.age % frequency != 0) return;
        var talent = spell.equals("frost_arrow")
                ? org.marj4n.smooth_classes.content.caster.CasterContent.ICE_COMET_VOLLEY.id()
                : org.marj4n.smooth_classes.content.caster.CasterContent.ARCANE_BOLT_VOLLEY.id();
        if (!AbilityRuntime.hasTalent(player, talent)) return;
        LivingEntity target = lookTarget(player, 120);
        if (target == null) InternalSpellRuntime.dumbFire(player, "smooth_classes:" + spell, 1F);
        else InternalSpellRuntime.target(player, "smooth_classes:" + spell, target, 1F);
        decrement(player, spell.equals("frost_arrow") ? SmoothEffects.FROST_VOLLEY : SmoothEffects.ARCANE_VOLLEY, 1);
    }

    private static void meteoricWrath(LivingEntity bearer) {
        if (!(bearer instanceof ServerPlayerEntity player) || player.age % 15 != 0
                || !AbilityRuntime.hasTalent(player,
                org.marj4n.smooth_classes.content.caster.CasterContent.METEOR_SHOWER_WRATH.id())) return;
        if (!InternalSpellRuntime.aoe(player, "smooth_classes:fire_meteor_small", 12, 35, true, false, 1F)) return;
        int renewal = 0;
        if (PuffishSkillsIntegration.isSkillUnlocked(PuffishSkillsIntegration.CASTER,
                org.marj4n.smooth_classes.integration.SkillNodeIds.wizardSpecialisationMeteorShowerRenewingWrathThree, player)) renewal = 40;
        else if (PuffishSkillsIntegration.isSkillUnlocked(PuffishSkillsIntegration.CASTER,
                org.marj4n.smooth_classes.integration.SkillNodeIds.wizardSpecialisationMeteorShowerRenewingWrathTwo, player)) renewal = 25;
        else if (AbilityRuntime.hasTalent(player,
                org.marj4n.smooth_classes.content.caster.CasterContent.METEOR_SHOWER_RENEWING_WRATH.id())) renewal = 10;
        if (player.getRandom().nextInt(100) >= renewal) decrement(player, SmoothEffects.METEORIC_WRATH, 1);
    }

    private static void undyingWarning(LivingEntity bearer) {
        StatusEffectInstance effect = bearer.getStatusEffect(SmoothEffects.UNDYING);
        if (effect != null && effect.getDuration() == 35
                && bearer.getHealth() / bearer.getMaxHealth() < 0.60F)
            SkillFx.sound(bearer, "soundeffect_11", 0.3F, 1F);
    }

    private static void exhaustion(LivingEntity e) {
        if (e.age % 20 == 0) decrement(e, SmoothEffects.EXHAUSTION, 1);
    }

    private static void overload(LivingEntity e, int amp) {
        if (amp < 5) return;
        float damage = (float)Math.min((e.getMaxHealth() / 6F) * 2F, e.getMaxHealth());
        for (LivingEntity target : nearbyHostiles(e, 3)) {
            pushAway(e, target, 4);
            target.timeUntilRegen = 0;
            target.damage(e.getDamageSources().indirectMagic(e, e), damage);
            target.timeUntilRegen = 0;
        }
        e.damage(e.getDamageSources().indirectMagic(e, e), Math.max(0F, e.getMaxHealth() - 2F));
        SkillFx.sound(e, "soundeffect_14", 0.8F, 0.9F);
        SkillFx.plane(e, ParticleTypes.CAMPFIRE_COSY_SMOKE, e.getBlockPos(), 3, 0, 0.3, 0);
        e.removeStatusEffect(SmoothEffects.OVERLOAD);
    }

    private static void immobilize(LivingEntity e) {
        if (e.age % 5 != 0) return;
        Vec3d v=e.getVelocity();
        if (Math.abs(v.x)+Math.abs(v.z) > 0.08) {
            e.damage(e.getDamageSources().generic(), Math.min(e.getMaxHealth()*0.10F,10F));
            increment(e, StatusEffects.SLOWNESS,80,1,9);
        }
    }

    private static void immobilizingAura(LivingEntity e) {
        if (e.age % 20 != 0) return;
        for (LivingEntity target: nearbyHostiles(e,2))
            target.addStatusEffect(new StatusEffectInstance(SmoothEffects.IMMOBILIZE,25,0,false,false,true));
    }

    private static void stealth(LivingEntity e) {
        if (!(e instanceof ServerPlayerEntity p)) return;
        if (p.hasStatusEffect(SmoothEffects.REVEALED)) {
            p.removeStatusEffect(SmoothEffects.STEALTH);
            return;
        }
        StatusEffectInstance stealth=p.getStatusEffect(SmoothEffects.STEALTH);
        if (stealth != null && stealth.getDuration() < 10)
            p.addStatusEffect(new StatusEffectInstance(SmoothEffects.REVEALED,180,2,false,false,true));
        if (AbilityRuntime.hasTalent(p, AssassinContent.RECOVERY.id()) && p.age % 20 == 0)
            p.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION,25,0,false,false,true));
        if (AbilityRuntime.hasTalent(p, AssassinContent.SHADOW_VEIL.id()) && p.age % 20 == 0)
            increment(p,StatusEffects.RESISTANCE,25,1,3);
    }

    private static void bladestorm(LivingEntity e, int amp) {
        if (!(e instanceof ServerPlayerEntity p) || e.age % Math.max(22-amp,1) != 0) return;
        for (LivingEntity target:CombatRuntime.nearbyEnemies(p,2)) {
            target.timeUntilRegen=0;
            target.damage(p.getDamageSources().playerAttack(p),
                    (float)p.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE)*0.3F);
            target.timeUntilRegen=0;
            if (AbilityRuntime.hasTalent(p, AssassinContent.EVASION_BLADESTORM_SIPHON.id())
                    && p.getRandom().nextInt(100)<3) p.heal(1);
        }
    }

    private static void elementalSurge(LivingEntity e) {
        if (!(e instanceof ServerPlayerEntity p) || p.age % 20 != 0) return;
        boolean frost=!AbilityRuntime.hasTalent(p,ForeignerContent.ELEMENTAL_SURGE_NO_FROST.id());
        boolean fire=!AbilityRuntime.hasTalent(p,ForeignerContent.ELEMENTAL_SURGE_NO_FIRE.id());
        boolean lightning=!AbilityRuntime.hasTalent(p,ForeignerContent.ELEMENTAL_SURGE_NO_LIGHTNING.id());
        int enabled=(frost?1:0)+(fire?1:0)+(lightning?1:0);
        float damage;
        if(enabled==0) damage=SpellPowerRuntime.arcane(p,1.0);
        else {
            int pick=p.getRandom().nextInt(enabled);
            if(frost && pick--==0) damage=SpellPowerRuntime.frost(p,1.0);
            else if(fire && pick--==0) damage=SpellPowerRuntime.fire(p,1.0);
            else damage=SpellPowerRuntime.lightning(p,1.0);
        }
        CombatRuntime.damageNearby(p,3,damage);
    }

    private static void elementalImpact(LivingEntity e) {
        if (!(e instanceof ServerPlayerEntity p) || !p.isOnGround()) return;
        Vec3d look=p.getRotationVec(1).normalize();
        p.setVelocity(look.x*2,0,look.z*2);
        p.velocityModified=true;
        float damage=Math.max(SpellPowerRuntime.fire(p,1.0),Math.max(SpellPowerRuntime.frost(p,1.0),SpellPowerRuntime.lightning(p,1.0)));
        CombatRuntime.damageNearby(p,3,damage);
        if (AbilityRuntime.hasTalent(p,ForeignerContent.ELEMENTAL_IMPACT_MAGNET.id())) {
            for(LivingEntity target:CombatRuntime.nearbyEnemies(p,6)) {
                Vec3d d=p.getPos().subtract(target.getPos()).multiply(0.25);
                target.setVelocity(d.x,d.y,d.z);
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS,60,2,false,false,true));
            }
        }
    }

    private static void consecration(LivingEntity e) {
        if (!(e instanceof ServerPlayerEntity p) || !p.isOnGround() || p.age%18!=0) return;
        float power=Math.max(1F,SpellPowerRuntime.healing(p,1.9));
        boolean taunt=AbilityRuntime.hasTalent(p,SaberContent.CONSECRATION_TAUNT.id());
        boolean mighty=AbilityRuntime.hasTalent(p,SaberContent.CONSECRATION_MIGHTY.id());
        boolean spellforged=AbilityRuntime.hasTalent(p,SaberContent.CONSECRATION_SPELLFORGED.id());
        p.heal(power/5F);
        SkillFx.plane(p, SpellEngineParticles.magic_holy.type(), p.getBlockPos(), 6, 0, 0.4, 0);
        SkillFx.plane(p, SpellEngineParticles.magic_holy.type(), p.getBlockPos(), 6, 0, 0.2, 0);
        SkillFx.sound(p, "soundeffect_25", 0.05F, 0.8F);
        for(LivingEntity target:CombatRuntime.nearbyEnemies(p,6)) {
            target.timeUntilRegen=0;
            target.damage(p.getDamageSources().indirectMagic(p,p),power);
            target.timeUntilRegen=1;
            if(taunt && target instanceof MobEntity mob) mob.setTarget(p);
        }
        for(LivingEntity ally:p.getWorld().getEntitiesByClass(LivingEntity.class,p.getBoundingBox().expand(6),
                x->x!=p&&x.isAlive()&&p.isTeammate(x))) {
            ally.heal(power/4F);
            if(mighty) increment(ally,SmoothEffects.MIGHT,19,1,5);
            if(spellforged) increment(ally,SmoothEffects.SPELLFORGED,19,1,3);
        }
    }

    private static void sacredOnslaught(LivingEntity e) {
        if (!(e instanceof ServerPlayerEntity p) || !p.isOnGround()) return;
        Vec3d look=p.getRotationVec(1).normalize();
        p.setVelocity(look.x,0,look.z); p.velocityModified=true;
        if(p.age%10!=0)return;
        SkillFx.plane(p, ParticleTypes.CLOUD, p.getBlockPos(), 2, 0, 0.2, 0);
        SkillFx.sound(p, "soundeffect_32", 0.6F, 1.0F);
        float damage=(float)(p.getArmor()*0.60);
        for(LivingEntity target:CombatRuntime.nearbyEnemies(p,6)) {
            pushAway(p,target,4);
            target.damage(p.getDamageSources().playerAttack(p),damage);
            if(AbilityRuntime.hasTalent(p,SaberContent.SACRED_ONSLAUGHT_STUN.id()))
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS,50,4,false,false,true));
        }
        if(AbilityRuntime.hasTalent(p,SaberContent.SACRED_ONSLAUGHT_HEAL.id())) p.heal(Math.max(1F,SpellPowerRuntime.healing(p,0.60)));
    }

    private static void focus(LivingEntity e) {
        if (!(e instanceof ServerPlayerEntity p)) return;
        if (p.getMainHandStack().getItem() instanceof BowItem && p.isUsingItem()) {
            p.setVelocity(Vec3d.ZERO); p.velocityModified=true;
            if(p.age%10==0) {
                increment(p,SmoothEffects.MARKSMANSHIP,16,1,15);
                if (p.getWorld() instanceof net.minecraft.server.world.ServerWorld world) {
                    Vec3d look=p.getRotationVec(1F).normalize();
                    Vec3d c=p.getEyePos().add(look.multiply(2));
                    for(int i=0;i<8;i++) world.spawnParticles(SpellEngineParticles.magic_holy.type(),c.x,c.y,c.z,0,look.x*0.1,look.y*0.1,look.z*0.1,1);
                }
                SkillFx.sound(p,"soundeffect_31",1.4F,1.0F);
            }
        }
    }

    private static void melodyOfSafety(LivingEntity e) {
        if(e.age%20==0)e.heal(e.getMaxHealth()/10F);
    }

    private static void bullrush(LivingEntity e) {
        if (!(e instanceof ServerPlayerEntity p) || !p.isOnGround()) return;
        Vec3d look=p.getRotationVec(1).normalize(); p.setVelocity(look.x*2,0,look.z*2);p.velocityModified=true;
        for(LivingEntity target:CombatRuntime.nearbyEnemies(p,3)) {
            pullToward(p,target,4);
            SkillFx.plane(p, ParticleTypes.CLOUD, p.getBlockPos(), 2, 0, 0.2, 0);
            SkillFx.sound(p,"soundeffect_32",0.6F,1.0F);
            target.damage(p.getDamageSources().playerAttack(p),
                    (float)p.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE)*1.8F);
            target.addStatusEffect(new StatusEffectInstance(SmoothEffects.IMMOBILIZE,80,0,false,false,true));
        }
    }

    private static void leapSlam(LivingEntity e) {
        if (!(e instanceof ServerPlayerEntity p)) return;
        if(!p.isOnGround()){ if(p.getVelocity().y>-0.9)p.setVelocity(p.getVelocity().x,-1.0,p.getVelocity().z); return; }
        SkillFx.plane(p, ParticleTypes.CAMPFIRE_COSY_SMOKE, p.getBlockPos(), 3, 0, 0.3, 0);
        SkillFx.sound(p,"soundeffect_14",0.5F,0.9F);
        for(LivingEntity target:CombatRuntime.nearbyEnemies(p,3)) {
            pushAway(p,target,4);
            target.damage(p.getDamageSources().playerAttack(p),
                    (float)p.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE)*2.8F);
            target.addStatusEffect(new StatusEffectInstance(SmoothEffects.IMMOBILIZE,80,0,false,false,true));
        }
        p.removeStatusEffect(SmoothEffects.LEAPSLAM);
    }

    private static void earthshaker(LivingEntity e) {
        if (!(e instanceof ServerPlayerEntity p) || !p.isOnGround()) return;
        SkillFx.sound(p,"soundeffect_14",0.3F,1.1F);
        SkillFx.plane(p,ParticleTypes.CAMPFIRE_COSY_SMOKE,p.getBlockPos(),3,0,0.3,0);
        float damage=1F+p.getArmor()*0.5F;
        for(LivingEntity target:CombatRuntime.nearbyEnemies(p,3)) {
            pushAway(p,target,4); target.timeUntilRegen=0;
            target.damage(p.getDamageSources().playerAttack(p),damage); target.timeUntilRegen=0;
        }
        p.removeStatusEffect(SmoothEffects.EARTHSHAKER);
    }

    private static void disenchantment(LivingEntity e) {
        if(e.age%20!=0)return;
        List<StatusEffect> remove=new ArrayList<>();
        for(StatusEffectInstance x:e.getStatusEffects()) if(x.getEffectType().isBeneficial()) remove.add(x.getEffectType());
        remove.forEach(e::removeStatusEffect);
    }

    private static void righteousHammers(LivingEntity e,int amp){
        if (!(e instanceof ServerPlayerEntity p) || p.age % 10 != 0) return;
        int count=amp+1;
        double angleBase=Math.toRadians(p.getWorld().getTime()*9.0-45.0);
        double hammerY=p.getY()+p.getHeight()*0.5;
        double[] hammerX=new double[count];
        double[] hammerZ=new double[count];
        for(int i=0;i<count;i++){
            double a=angleBase+(Math.PI*2*i/count);
            hammerX[i]=p.getX()-Math.sin(a)*3.0;
            hammerZ[i]=p.getZ()-Math.cos(a)*3.0;
        }
        int pts=PuffishSkillsIntegration.countUnlockedSkills(PuffishSkillsIntegration.ASCENDANCY,p);
        float coefficient=pts>=60?1.00F:pts>=30?.75F:.55F;
        float damage=SpellPowerRuntime.strongest(p,coefficient);
        for (LivingEntity target:nearbyHostiles(p,4.5)) {
            if (target instanceof net.minecraft.entity.passive.TameableEntity tame && tame.isOwner(p)) continue;
            var box=target.getBoundingBox();
            if (box.maxY < hammerY-0.7 || box.minY > hammerY+0.7) continue;
            for (int i=0;i<count;i++) {
                double hx=hammerX[i],hz=hammerZ[i];
                double dx=Math.max(box.minX-hx,Math.max(0,hx-box.maxX));
                double dz=Math.max(box.minZ-hz,Math.max(0,hz-box.maxZ));
                if (dx*dx+dz*dz > 0.75*0.75) continue;
                org.marj4n.smooth_classes.runtime.RighteousHammerChargeRuntime.passiveHit(p,target,damage);
                break;
            }
        }
    }
    private static void cyclonicCleave(LivingEntity e){
        if(!(e instanceof ServerPlayerEntity p))return;
        StatusEffectInstance fx=p.getStatusEffect(SmoothEffects.CYCLONIC_CLEAVE); if(fx==null)return;
        int dur=fx.getDuration(), pts=PuffishSkillsIntegration.countUnlockedSkills(PuffishSkillsIntegration.ASCENDANCY,p);
        if(dur>10){
            double velocity=Math.min(pts>=60?.85:.65,Math.max(0,0.05D*(39-dur)));
            Vec3d v=p.getRotationVector().multiply(velocity);
            p.setVelocity(v.x,0,v.z); p.velocityModified=true;
        }
        if(dur<30 && dur%5==0){
            SkillFx.sound(p,"spell_slash",1.0F,pts>=60?1.35F:1.1F);
            SkillFx.plane(p, pts>=30?ParticleTypes.PORTAL:ParticleTypes.CLOUD, p.getBlockPos(), pts>=60?4:2, 0, 0.2, 0);
            double base=(0.75D+0.015D*AscendancyBalance.points(pts));
            if(pts>=60)base*=1.75D;
            float damage=SpellPowerRuntime.strongest(p,base);
            double radius=pts>=60?4.5:2.5;
            for(LivingEntity target:nearbyHostiles(p,radius)){
                if(pts>=30) pullToward(p,target,pts>=60?10:6);
                if(pts>=60)target.addStatusEffect(new StatusEffectInstance(SmoothEffects.IMMOBILIZE,15,0,false,false,true));
                target.timeUntilRegen=0;target.damage(p.getDamageSources().playerAttack(p),damage);target.timeUntilRegen=0;
            }
            if(pts>=60&&dur==5){
                float shock=SpellPowerRuntime.strongest(p,2.5D);
                for(LivingEntity target:nearbyHostiles(p,7)){
                    target.timeUntilRegen=0;target.damage(p.getDamageSources().playerAttack(p),shock);target.timeUntilRegen=0;
                    Vec3d away=target.getPos().subtract(p.getPos());
                    if(away.lengthSquared()>0.01){Vec3d n=away.normalize().multiply(.8);target.addVelocity(n.x,.65,n.z);}
                }
                SkillFx.plane(p,ParticleTypes.EXPLOSION,p.getBlockPos(),5,0,0.35,0);
            }
        }
    }

    private static void arcaneSlash(LivingEntity e) {
        if (!(e instanceof ServerPlayerEntity p)) return;
        StatusEffectInstance fx = p.getStatusEffect(SmoothEffects.ARCANE_SLASH);
        if (fx == null) return;
        if (!ArcaneSlashChargeRuntime.canContinue(p)) {
            p.removeStatusEffect(SmoothEffects.ARCANE_SLASH);
            return;
        }
        p.setSprinting(false);
        ArcaneSlashVisuals.charge(p, fx.getDuration());
        // The status is a single 16-tick windup. No recast on removal.
        if (fx.getDuration() == 1) {
            int pts = PuffishSkillsIntegration.countUnlockedSkills(PuffishSkillsIntegration.ASCENDANCY, p);
            String spell = pts >= 60 ? "smooth_classes:arcane_slash_projectile_3"
                    : pts >= 30 ? "smooth_classes:arcane_slash_projectile_2"
                    : "smooth_classes:arcane_slash_projectile";
            p.swingHand(net.minecraft.util.Hand.MAIN_HAND);
            if (InternalSpellRuntime.dumbFireUniversal(p, spell, 3F))
                ArcaneSlashChargeRuntime.finish(p);
            // Natural status removal cancels if projectile delivery failed.
        }
    }
    private static void rapidfire(LivingEntity e){
        if(!(e instanceof ServerPlayerEntity p))return;
        var fx=p.getStatusEffect(SmoothEffects.RAPIDFIRE);if(fx==null)return;
        var weapon=p.getMainHandStack().getItem();
        if(!(weapon instanceof BowItem) && !(weapon instanceof CrossbowItem)){
            p.removeStatusEffect(SmoothEffects.RAPIDFIRE);
            RAPIDFIRE_ARROW_COUNT.remove(p.getUuid());
            return;
        }
        int dur=fx.getDuration(),pts=PuffishSkillsIntegration.countUnlockedSkills(PuffishSkillsIntegration.ASCENDANCY,p);
        if(dur%20==0)InternalSpellRuntime.targetUniversal(p,
                weapon instanceof CrossbowItem ? "smooth_classes:rapidfire_crossbow" : "smooth_classes:rapidfire",p,1F);
        int interval=AscendancyBalance.rapidfireInterval(pts);
        if(dur%interval!=0)return;
        float multiplier=AscendancyBalance.rapidfireCastMultiplier(pts);
        boolean fired=InternalSpellRuntime.dumbFireUniversal(p,"smooth_classes:rapidfire_projectile",multiplier);
        if(fired){
            int count=RAPIDFIRE_ARROW_COUNT.merge(p.getUuid(),1,Integer::sum);
            if(pts>=60&&count%2==0)increment(p,SmoothEffects.MARKSMANSHIP,80,1,12);
            else if(pts>=30&&count%4==0)increment(p,SmoothEffects.MARKSMANSHIP,60,1,8);
        }
    }

    private static void cataclysm(LivingEntity e){
        if(!(e instanceof ServerPlayerEntity p))return;
        var fx=p.getStatusEffect(SmoothEffects.CATACLYSM);if(fx==null)return;
        int pts=PuffishSkillsIntegration.countUnlockedSkills(PuffishSkillsIntegration.ASCENDANCY,p);
        int elapsed=70-fx.getDuration(),frequency=pts>=60?10:pts>=30?14:20;
        if(elapsed<0||elapsed%frequency!=0)return;
        int distance=6+(elapsed/frequency)*4;
        Vec3d look=p.getRotationVec(1F);
        Vec3d center=p.getPos().add(look.x*distance,0,look.z*distance);
        double fire=net.spell_power.api.SpellPower.getSpellPower(net.spell_power.api.SpellSchools.FIRE,p).baseValue();
        double frost=net.spell_power.api.SpellPower.getSpellPower(net.spell_power.api.SpellSchools.FROST,p).baseValue();
        SkillFx.sound(p,"spell_energy",.5F,pts>=60?1.35F:1.1F);
        boolean launched;
        if(pts>=60){
            float multiplier=AscendancyBalance.cataclysmCastMultiplier(pts);
            boolean a=InternalSpellRuntime.atPositionUniversal(p,"smooth_classes:cataclysm_meteor",center,multiplier);
            boolean b=InternalSpellRuntime.atPositionUniversal(p,"smooth_classes:cataclysm_comet",center,multiplier);
            launched=a||b;
        }else{
            String spell=fire>=frost?"smooth_classes:cataclysm_meteor":"smooth_classes:cataclysm_comet";
            launched=InternalSpellRuntime.atPositionUniversal(p,spell,center,1F);
        }
        if(launched&&pts>=60)increment(p,SmoothEffects.SPELLFORGED,100,2,10);
        else if(launched&&pts>=30)increment(p,SmoothEffects.SPELLFORGED,60,1,5);
    }

    private static void ghostwalk(LivingEntity e){
        if(!(e instanceof ServerPlayerEntity p))return;
        StatusEffectInstance fx=p.getStatusEffect(SmoothEffects.GHOSTWALK);if(fx==null)return;
        int dur=fx.getDuration(),pts=PuffishSkillsIntegration.countUnlockedSkills(PuffishSkillsIntegration.ASCENDANCY,p);
        SkillFx.orbit(p,ParticleTypes.SMOKE,pts>=60?1.6:1,pts>=60?36:20);
        if(dur>10){
            double groundY=p.getY()-3;
            for(int i=0;i<8;i++) if(!p.getWorld().getBlockState(p.getBlockPos().down(i)).isAir()) {
                groundY=p.getBlockPos().getY()-i+1; break;
            }
            double lift=Math.min(dur>(pts>=60?188:108)?0.42:0.18,
                    Math.max(-0.18,(groundY+3-p.getY())*0.35));
            double forward=dur<=(pts>=60?188:108)?Math.min(pts>=60?.9:.6,0.02D*((pts>=60?191:111)-dur)):0;
            Vec3d look=p.getRotationVector();
            p.setVelocity(look.x*forward,lift,look.z*forward);
            p.setNoGravity(true);p.velocityModified=true;
        }
        int interval=AscendancyBalance.ghostwalkInterval(pts);
        if(dur%interval==0){
            double radius=pts>=60?18:10;
            int limit=pts>=60?2:1;
            LivingEntity first=null,second=null;
            double firstDistance=Double.MAX_VALUE,secondDistance=Double.MAX_VALUE;
            for(LivingEntity candidate:nearbyHostiles(p,radius)){
                double distance=p.squaredDistanceTo(candidate);
                if(distance<firstDistance){
                    second=first;secondDistance=firstDistance;
                    first=candidate;firstDistance=distance;
                } else if(distance<secondDistance){
                    second=candidate;secondDistance=distance;
                }
            }
            LivingEntity[] targets=limit>1?new LivingEntity[]{first,second}:new LivingEntity[]{first};
            for(LivingEntity target:targets){
                if(target==null)continue;
                double coefficient=AscendancyBalance.ghostwalkCoefficient(pts);
                float damage=SpellPowerRuntime.strongest(p,coefficient);
                int previous=target.timeUntilRegen;boolean hit;
                try{target.timeUntilRegen=0;hit=target.damage(p.getDamageSources().playerAttack(p),damage);}finally{target.timeUntilRegen=previous;}
                SkillFx.beam(p,target,ParticleTypes.SOUL,pts>=60?36:24);
                if(hit&&pts>=60)p.heal(Math.min(p.getMaxHealth()*.10F,damage*.50F));
                else if(hit&&pts>=30)p.heal(Math.min(p.getMaxHealth()*.05F,damage*.25F));
            }
        }
    }

    private static void skywardSunder(LivingEntity e){
        if(!(e instanceof ServerPlayerEntity p))return;
        StatusEffectInstance fx=p.getStatusEffect(SmoothEffects.SKYWARD_SUNDER);if(fx==null)return;
        int dur=fx.getDuration(),pts=PuffishSkillsIntegration.countUnlockedSkills(PuffishSkillsIntegration.ASCENDANCY,p);
        double damageModifier=pts>=60?3D:1D+0.02D*AscendancyBalance.points(pts);
        if(dur==42){
            org.marj4n.smooth_classes.runtime.InternalSpellRuntime.dumbFire(p,"smooth_classes:skyward_sunder",1F);
            SkillFx.sound(p,"object_impact_thud_repeat",0.6F,1.0F);
        }
        if(dur==20)org.marj4n.smooth_classes.runtime.InternalSpellRuntime.dumbFire(p,"smooth_classes:skyward_sunder_slam",1F);
        if(dur==13) SkillFx.sound(p,"damage_03",0.8F,1.0F);
        if(dur==12) SkillFx.sound(p,"spell_earth_punch",0.6F,1.0F);
        if(dur==2){
            SkillFx.plane(p,ParticleTypes.CAMPFIRE_SIGNAL_SMOKE,p.getBlockPos(),2,0,1,0);
            SkillFx.plane(p,ParticleTypes.POOF,p.getBlockPos(),2,0,1,0);
        }
        if(dur>30){
            double velocity=0.1D*(46-dur);Vec3d v=p.getRotationVector().multiply(velocity);
            p.setVelocity(v.x,0,v.z);p.velocityModified=true;
        }
        if(dur==30){p.setVelocity(0,1.2,0);p.velocityModified=true;}
        if(dur==15){p.setVelocity(0,-1.2,0);p.velocityModified=true;}
        boolean slash=dur==1||dur==15||dur==30;
        if(slash){
            float damage=SpellPowerRuntime.strongest(p,damageModifier);
            for(LivingEntity target:nearbyHostiles(p,pts>=60?5:2)){
                if(pts>=60)target.addStatusEffect(new StatusEffectInstance(SmoothEffects.DEATH_MARK,120,0,false,false,true));
                target.timeUntilRegen=0;target.damage(p.getDamageSources().playerAttack(p),damage);target.timeUntilRegen=0;
                target.setVelocity(p.getVelocity());target.velocityModified=true;
                if(pts>=60&&dur==1){target.addStatusEffect(new StatusEffectInstance(SmoothEffects.IMMOBILIZE,40,0,false,false,true));target.addVelocity(0,.8,0);}
                SkillFx.plane(p,ParticleTypes.CLOUD,p.getBlockPos(),pts>=60?3:1,0,1,0);
            }
        }
        if(dur>30&&dur%2==0)for(LivingEntity target:nearbyHostiles(p,pts>=60?3.5:2)){
            if(pts>=30)target.addStatusEffect(new StatusEffectInstance(SmoothEffects.DEATH_MARK,pts>=60?120:60,0,false,false,true));
            target.timeUntilRegen=0;target.damage(p.getDamageSources().playerAttack(p),pts>=60?2.0F:0.5F);target.timeUntilRegen=0;
            target.setVelocity(p.getVelocity());target.velocityModified=true;
        }
    }
    private static void vitalityBond(LivingEntity e){
        if(e.age%10!=0)return;
        StatusEffectInstance fx=e.getStatusEffect(SmoothEffects.VITALITY_BOND);
        if(!(fx instanceof SourceStatusEffectInstance sourced))return;
        LivingEntity source=sourced.getSourceEntity();
        if(!(source instanceof ServerPlayerEntity owner)||source==e||!source.isAlive())return;
        if(AbilityRuntime.hasTalent(owner,RulerContent.SACRED_ORB_SPEED.id())){
            increment(e,StatusEffects.HASTE,15,1,6); increment(e,StatusEffects.SPEED,15,1,2);
            increment(owner,StatusEffects.MINING_FATIGUE,15,1,3);
        }
        if(AbilityRuntime.hasTalent(owner,RulerContent.SACRED_ORB_DEBUFFS.id())){
            StatusEffectInstance transfer=null;
            for(StatusEffectInstance x:e.getStatusEffects()) if(!x.getEffectType().isBeneficial()){transfer=x;break;}
            if(transfer!=null){
                owner.addStatusEffect(new StatusEffectInstance(transfer));
                e.removeStatusEffect(transfer.getEffectType());
            }
        }
        if(AbilityRuntime.hasTalent(owner,RulerContent.SACRED_ORB_BUFFS.id())){
            // Owner effects are only read in this loop, so no defensive copy is needed.
            for(StatusEffectInstance x:owner.getStatusEffects())if(x.getEffectType().isBeneficial()&&x.getEffectType()!=SmoothEffects.VITALITY_BOND)
                e.addStatusEffect(new StatusEffectInstance(x));
        }
        float ep=e.getHealth()/e.getMaxHealth()*100F,op=owner.getHealth()/owner.getMaxHealth()*100F;
        if(Math.abs(ep-op)>15F&&(ep<85F||op<85F)){
            LivingEntity heal=ep<op?e:owner, sacrifice=ep<op?owner:e;
            if(sacrifice.getHealth()>5F){
                sacrifice.setHealth(sacrifice.getHealth()-1F);heal.heal(1F);
                SkillFx.plane(heal,SpellEngineParticles.magic_heal.type(),heal.getBlockPos(),1,0,0.2,0);
                SkillFx.plane(heal,SpellEngineParticles.magic_holy.type(),heal.getBlockPos(),1,0,0.2,0);
                SkillFx.sound(sacrifice,"soundeffect_28",0.1F,1.1F);
                SkillFx.sound(heal,"soundeffect_25",0.1F,1.0F);
            }
        }
    }

    private static void anointed(LivingEntity e){
        if(!(e instanceof ServerPlayerEntity p)||p.age%20!=0||!AbilityRuntime.hasTalent(p,RulerContent.ANOINT_WEAPON_CLEANSE.id()))return;
        List<StatusEffect> remove=new ArrayList<>();
        for(StatusEffectInstance x:p.getStatusEffects())if(!x.getEffectType().isBeneficial())remove.add(x.getEffectType());
        remove.forEach(p::removeStatusEffect);
    }

    private static void curseTarget(LivingEntity e){
        if(!(e instanceof MobEntity mob))return;
        LivingEntity source=null;
        StatusEffectInstance taunted=e.getStatusEffect(SmoothEffects.TAUNTED);
        if(taunted instanceof SourceStatusEffectInstance sourced)source=sourced.getSourceEntity();
        if(source==null){
            StatusEffectInstance agony=e.getStatusEffect(SmoothEffects.AGONY);
            if(agony instanceof SourceStatusEffectInstance sourced)source=sourced.getSourceEntity();
        }
        if(source==null){
            StatusEffectInstance torment=e.getStatusEffect(SmoothEffects.TORMENT);
            if(torment instanceof SourceStatusEffectInstance sourced)source=sourced.getSourceEntity();
        }
        if(source!=null&&source.isAlive()&&mob.getTarget()!=source)mob.setTarget(source);
    }

    private static void ragingJavelin(LivingEntity e){
        if (!(e instanceof ServerPlayerEntity p) || p.age % 8 != 0 || p.getMainHandStack().isEmpty()) return;
        for (LivingEntity target : CombatRuntime.nearbyEnemies(p,10)) {
            if (p.getRandom().nextInt(100) < 80) {
                org.marj4n.smooth_classes.runtime.InternalSpellRuntime.target(
                        p,"smooth_classes:passive_throw",target,1F);
                break;
            }
        }
    }

    private static void spellbreaking(LivingEntity e){
        if(!(e instanceof ServerPlayerEntity p)||p.age%5!=0)return;
        for(SpellProjectile projectile:p.getWorld().getEntitiesByClass(SpellProjectile.class,p.getBoundingBox().expand(4),
                x->x.isAlive()&&x.getOwner()!=p)){
            if(projectile.getOwner() instanceof LivingEntity owner && !OptionalCompatRuntime.canHarm(p,owner))continue;
            projectile.discard();
        }
    }

    private static void righteousShield(LivingEntity e){
        if(!(e instanceof ServerPlayerEntity p))return;
        StatusEffectInstance shield=p.getStatusEffect(SmoothEffects.RIGHTEOUS_SHIELD);
        if(shield==null||shield.getDuration()!=10)return;
        SkillFx.sound(p,"hit_03",1F,1.1F);
        StatusEffectInstance aegis=p.getStatusEffect(SmoothEffects.GOLDEN_AEGIS);
        if(aegis==null)return;
        int pts=PuffishSkillsIntegration.countUnlockedSkills(PuffishSkillsIntegration.ASCENDANCY,p);
        int stacks=aegis.getAmplifier();
        String spell;
        int consume;
        int tier=AscendancyBalance.shieldTier(stacks+1);
        if(tier==4){spell="righteous_shield_projectile_4";consume=15;}
        else if(tier==3){spell="righteous_shield_projectile_3";consume=10;}
        else if(tier==2){spell="righteous_shield_projectile_2";consume=5;}
        else {spell="righteous_shield_projectile";consume=stacks+1;}
        float multiplier=pts>=60?1.25F:1F;
        if(!InternalSpellRuntime.targetUniversal(p,"smooth_classes:"+spell,p,multiplier))return;
        if(pts>=60)InternalSpellRuntime.targetUniversal(p,"smooth_classes:"+spell,p,multiplier);
        if(pts>=60)consume=Math.max(1,(consume+1)/2);
        int remaining=(stacks+1)-consume;
        p.removeStatusEffect(SmoothEffects.GOLDEN_AEGIS);
        if(remaining>0)p.addStatusEffect(new StatusEffectInstance(SmoothEffects.GOLDEN_AEGIS,
                aegis.getDuration(),remaining-1,false,false,true));
    }



    private static LivingEntity lookTarget(ServerPlayerEntity p,double radius){
        Vec3d eye=p.getEyePos(),look=p.getRotationVec(1F).normalize();
        LivingEntity best=null;double bestScore=Double.MAX_VALUE;
        for(LivingEntity x:nearbyHostiles(p,radius)){
            Vec3d to=x.getEyePos().subtract(eye);double along=to.dotProduct(look);
            if(along<=0||along>radius)continue;
            double off=to.subtract(look.multiply(along)).lengthSquared();
            if(off<2.25&&off<bestScore){best=x;bestScore=off;}
        }
        return best;
    }

    private static List<LivingEntity> nearbyHostiles(LivingEntity source,double radius){
        return source.getWorld().getEntitiesByClass(LivingEntity.class,source.getBoundingBox().expand(radius),
                x->x!=source&&x.isAlive()&&(!(source instanceof PlayerEntity p)||!p.isTeammate(x)));
    }
    private static void pushAway(LivingEntity source,LivingEntity target,double divisor){
        Vec3d d=target.getPos().subtract(source.getPos()).multiply(1D/divisor);target.setVelocity(d.x,d.y,d.z);
    }
    private static void pullToward(LivingEntity source,LivingEntity target,double divisor){
        Vec3d d=source.getPos().subtract(target.getPos()).multiply(1D/divisor);target.setVelocity(d.x,d.y,d.z);
    }
    private static void increment(LivingEntity e, StatusEffect effect,int duration,int amount,int max){
        StatusEffectInstance old=e.getStatusEffect(effect);
        int amp=old==null?Math.max(0,amount-1):Math.min(max-1,old.getAmplifier()+amount);
        e.addStatusEffect(new StatusEffectInstance(effect,duration,amp,false,false,true));
    }
    private static void decrement(LivingEntity e,StatusEffect effect,int amount){
        StatusEffectInstance old=e.getStatusEffect(effect); if(old==null)return;
        int amp=old.getAmplifier()-amount;
        if(amp<0)e.removeStatusEffect(effect);
        else e.addStatusEffect(new StatusEffectInstance(effect,old.getDuration(),amp,false,false,true));
    }
}
