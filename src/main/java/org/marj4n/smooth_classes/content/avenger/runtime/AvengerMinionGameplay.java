package org.marj4n.smooth_classes.content.avenger.runtime;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import org.marj4n.smooth_classes.content.avenger.AvengerContent;
import org.marj4n.smooth_classes.effects.SmoothEffects;
import org.marj4n.smooth_classes.effects.SourceStatusEffectInstance;
import org.marj4n.smooth_classes.entity.AvengerMinionEntity;
import org.marj4n.smooth_classes.entity.DreadglareEntity;
import org.marj4n.smooth_classes.entity.GreaterDreadglareEntity;
import org.marj4n.smooth_classes.entity.WraithEntity;
import org.marj4n.smooth_classes.registry.SmoothEntities;
import org.marj4n.smooth_classes.runtime.AbilityRuntime;
import org.marj4n.smooth_classes.runtime.SpellPowerRuntime;
import org.marj4n.smooth_classes.runtime.SkillFx;
import net.spell_power.api.SpellPower;
import net.spell_power.api.SpellSchools;

import java.util.ArrayList;
import java.util.List;

/**
 * Concrete Avenger summon gameplay.
 *
 * This is deliberately the integration boundary between physical Minecraft
 * entities and Puffish-owned talent state.
 */
public final class AvengerMinionGameplay {
    private AvengerMinionGameplay() {}

    public static int summon(ServerPlayerEntity owner, AvengerSummonPlan plan) {
        int spawned = 0;
        for (AvengerMinionType type : plan.minions()) {
            if (spawn(owner, type, plan) != null) {
                spawned++;
            }
        }
        if (spawned > 0) SkillFx.sound(owner, "magic_shamanic_voice_20", 0.3F, 1.0F);
        return spawned;
    }

    public static AvengerMinionEntity spawn(
            ServerPlayerEntity owner,
            AvengerMinionType type,
            AvengerSummonPlan plan
    ) {
        EntityType<? extends AvengerMinionEntity> entityType = switch (type) {
            case WRAITH -> SmoothEntities.WRAITH;
            case DREADGLARE -> SmoothEntities.DREADGLARE;
            case GREATER_DREADGLARE -> SmoothEntities.GREATER_DREADGLARE;
        };

        ServerWorld world = owner.getServerWorld();

        AvengerMinionEntity minion = entityType.spawn(
                world,
                owner.getBlockPos().up(2).offset(owner.getMovementDirection(), 3),
                SpawnReason.MOB_SUMMONED
        );

        if (minion == null) {
            return null;
        }

        minion.setOwner(owner);
        minion.setTamed(true);
        minion.setPositionTarget(owner.getBlockPos().up(2), 32);

        applyScaling(owner, minion, plan.necroticFortification());

        if (plan.shadowAura()) {
            minion.addStatusEffect(new StatusEffectInstance(
                    SmoothEffects.SHADOW_AURA,
                    AvengerMinionEntity.LIFESPAN_TICKS,
                    minion.isGreater() ? 3 : 0,
                    false,
                    false,
                    false
            ));
        }

        world.spawnParticles(
                ParticleTypes.SOUL,
                minion.getX(),
                minion.getBodyY(0.5),
                minion.getZ(),
                minion.isGreater() ? 30 : 15,
                0.5, 0.5, 0.5,
                0.03
        );

        return minion;
    }

    private static void applyScaling(
            ServerPlayerEntity owner,
            AvengerMinionEntity minion,
            boolean fortification
    ) {
        double soul = SpellPowerRuntime.soulBase(owner);

        set(
                minion,
                EntityAttributes.GENERIC_ATTACK_DAMAGE,
                3.0 + minion.attackMultiplier() * soul
        );

        double health =
                1.0
                + minion.healthMultiplier()
                * owner.getAttributeValue(EntityAttributes.GENERIC_MAX_HEALTH);

        set(minion, EntityAttributes.GENERIC_MAX_HEALTH, health);
        minion.setHealth((float) health);

        if (fortification) {
            double inherit = minion.isGreater() ? 1.0 : 0.5;

            set(
                    minion,
                    EntityAttributes.GENERIC_ARMOR,
                    1.0 + inherit * owner.getAttributeValue(EntityAttributes.GENERIC_ARMOR)
            );

            set(
                    minion,
                    EntityAttributes.GENERIC_ARMOR_TOUGHNESS,
                    1.0 + inherit * owner.getAttributeValue(EntityAttributes.GENERIC_ARMOR_TOUGHNESS)
            );
        }
    }

    private static void set(
            AvengerMinionEntity minion,
            EntityAttribute attribute,
            double value
    ) {
        EntityAttributeInstance instance = minion.getAttributeInstance(attribute);
        if (instance != null) {
            instance.setBaseValue(value);
        }
    }

    /**
     * ranged pulse for Dreadglares.
     * Wraiths keep melee combat and receive their elemental/wither rider on hit.
     */
    public static boolean trySpecialAttack(
            ServerPlayerEntity owner,
            AvengerMinionEntity minion
    ) {
        LivingEntity target = minion.getTarget();

        // intentionally rolls a fresh divisor every tick rather than using a fixed cooldown.
        int cadence = minion instanceof WraithEntity ? 20 + minion.getRandom().nextInt(30)
                : minion instanceof GreaterDreadglareEntity ? 10 + minion.getRandom().nextInt(10)
                : 15 + minion.getRandom().nextInt(15);
        if (minion.age % cadence != 0) return false;

        if (minion instanceof WraithEntity) {
            // Wraith does not use melee target goals. Every pulse it independently
            // finds the nearest non-passive valid living target in a 16 block box.
            Entity nearest=null;
            double nearestDistance=Double.MAX_VALUE;
            for(Entity candidate:minion.getWorld().getOtherEntities(
                    minion,
                    minion.getBoundingBox().expand(16),
                    e -> e instanceof LivingEntity living
                            && living.isAlive()
                            && e != owner
                            && !(e instanceof PassiveEntity)
                            && !(e instanceof TameableEntity tame && tame.isTamed()
                            && owner.getUuid().equals(tame.getOwnerUuid())))) {
                double distance=candidate.squaredDistanceTo(minion);
                if(distance<nearestDistance){nearestDistance=distance;nearest=candidate;}
            }
            if (!(nearest instanceof LivingEntity found) || !org.marj4n.smooth_classes.integration.OptionalCompatRuntime.canHarm(owner,found)) return false;
            target=found;
            // Wraiths are ranged casters rather than ordinary melee summons.
            float damage = AbilityRuntime.hasTalent(owner, AvengerContent.WITHER_WRAITHS.id())
                    ? SpellPowerRuntime.soul(owner, 0.5)
                    : AbilityRuntime.hasTalent(owner, AvengerContent.FROST_WRAITHS.id())
                    ? SpellPowerRuntime.frost(owner, 0.5)
                    : SpellPowerRuntime.soul(owner, 0.5);
            target.timeUntilRegen = 0;
            target.damage(owner.getDamageSources().indirectMagic(minion, owner), Math.max(1F, damage));
            if (AbilityRuntime.hasTalent(owner, AvengerContent.WITHER_WRAITHS.id()))
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 100, 0), minion);
            else if (AbilityRuntime.hasTalent(owner, AvengerContent.FROST_WRAITHS.id()))
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 160, 0), minion);
            applyAttackTalents(owner,minion,target);
            // Wraith Legion: proc chance is 5% per harmful effect currently
            // carried by this wraith; Agony lasts 200 + Ascendancy points and owns its caster.
            if (AbilityRuntime.hasTalent(owner, AvengerContent.WRAITH_LEGION.id())) {
                int chanceCheck=countHarmfulEffects(minion)*5;
                if(owner.getRandom().nextInt(100)<chanceCheck) {
                    int pts=org.marj4n.smooth_classes.integration.PuffishSkillsIntegration.countUnlockedSkills(
                            org.marj4n.smooth_classes.integration.PuffishSkillsIntegration.ASCENDANCY,owner);
                    target.addStatusEffect(new SourceStatusEffectInstance(
                            SmoothEffects.AGONY,200+pts,0,false,false,true,owner));
                }
            }
            return true;
        }

        if (target == null || !target.isAlive() || target == owner || minion.squaredDistanceTo(target) > 20.0 * 20.0) return false;

        // fires only when the Dreadglare is facing the target and is not point blank.
        net.minecraft.util.math.Vec3d look=minion.getRotationVec(1F);
        net.minecraft.util.math.Vec3d to=target.getPos().subtract(minion.getPos()).normalize();
        double threshold=Math.cos(Math.toRadians(minion instanceof GreaterDreadglareEntity?20:15));
        if(look.dotProduct(to)<=threshold || minion.distanceTo(target)<=(minion instanceof GreaterDreadglareEntity?1.5:2.0)) return false;

        float baseDamage =
                (float) minion.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE);

        float multiplier =
                2.0F;

        target.timeUntilRegen = 0;
        target.damage(
                owner.getDamageSources().indirectMagic(minion, owner),
                baseDamage * multiplier
        );

        ServerWorld world = owner.getServerWorld();

        double dx = target.getX() - minion.getX();
        double dy = target.getBodyY(0.5) - minion.getBodyY(0.5);
        double dz = target.getZ() - minion.getZ();

        int steps = minion instanceof GreaterDreadglareEntity ? 12 : 8;

        for (int i = 1; i <= steps; i++) {
            double progress = i / (double) steps;
            world.spawnParticles(
                    ParticleTypes.SOUL,
                    minion.getX() + dx * progress,
                    minion.getBodyY(0.5) + dy * progress,
                    minion.getZ() + dz * progress,
                    1,
                    0.02, 0.02, 0.02,
                    0.0
            );
        }

        world.spawnParticles(
                ParticleTypes.SONIC_BOOM,
                target.getX(),
                target.getBodyY(0.5),
                target.getZ(),
                1,
                0, 0, 0,
                0
        );

        world.playSound(
                null,
                minion.getBlockPos(),
                SoundEvents.ENTITY_WARDEN_SONIC_BOOM,
                minion.getSoundCategory(),
                0.45F,
                minion.isGreater() ? 0.75F : 1.35F
        );

        applyAttackTalents(owner, minion, target);
        return true;
    }

    public static void onMinionAttack(
            ServerPlayerEntity owner,
            AvengerMinionEntity minion,
            Entity attacked
    ) {
        if (!(attacked instanceof LivingEntity target)) {
            return;
        }

        applyAttackTalents(owner, minion, target);
    }

    private static void applyAttackTalents(
            ServerPlayerEntity owner,
            AvengerMinionEntity minion,
            LivingEntity target
    ) {
        // Blood Harvest: minion heals for 60% of attack damage; owner gets half.
        if (AbilityRuntime.hasTalent(owner, AvengerContent.BLOOD_HARVEST.id())) {
            float siphonMultiplier=minion instanceof GreaterDreadglareEntity?0.60F:0.30F;
            float siphon=(float)minion.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE)*siphonMultiplier;
            minion.heal(siphon);
            owner.heal(siphon*0.5F);
        }

        // Wraith specialisations.
        if (minion instanceof WraithEntity) {
            if (AbilityRuntime.hasTalent(owner, AvengerContent.WITHER_WRAITHS.id())) {
                target.addStatusEffect(
                        new StatusEffectInstance(StatusEffects.WITHER, 100, 0),
                        minion
                );
            }

            if (AbilityRuntime.hasTalent(owner, AvengerContent.FROST_WRAITHS.id())) {
                target.addStatusEffect(
                        new StatusEffectInstance(StatusEffects.SLOWNESS, 100, 1),
                        minion
                );
            }
        }

        // Pestilence moves one harmful stack from the minion to its victim.
        if (AbilityRuntime.hasTalent(owner, AvengerContent.PESTILENCE.id())) {
            transferHarmfulStack(minion, target);
        }

        // applies Taunted + Might here only for Greater Dreadglare melee hits.
        if (minion instanceof GreaterDreadglareEntity) {
            target.addStatusEffect(new SourceStatusEffectInstance(
                    SmoothEffects.TAUNTED,100,0,false,false,true,minion), minion);
            int harmful=countHarmfulEffects(minion);
            minion.addStatusEffect(new StatusEffectInstance(
                    SmoothEffects.MIGHT,220,harmful,false,false,false));
        }
    }

    private static void transferHarmfulStack(LivingEntity from, LivingEntity to) {
        StatusEffectInstance selected=null;
        for(StatusEffectInstance effect:from.getStatusEffects()) {
            if(!effect.getEffectType().isBeneficial()){selected=effect;break;}
        }
        if(selected==null)return;
        StatusEffectInstance existing=to.getStatusEffect(selected.getEffectType());
        int amplifier=existing==null?0:Math.min(selected.getAmplifier(),existing.getAmplifier()+1);
        to.addStatusEffect(new StatusEffectInstance(selected.getEffectType(),selected.getDuration(),amplifier,false,false,true));
        if(selected.getAmplifier()==0)from.removeStatusEffect(selected.getEffectType());
        else from.addStatusEffect(new StatusEffectInstance(selected.getEffectType(),selected.getDuration(),selected.getAmplifier()-1,false,false,true));
    }

    public static void tickMinion(
            ServerPlayerEntity owner,
            AvengerMinionEntity minion
    ) {
        // Keep a summon engaged with the owner's current combat target.
        LivingEntity ownerTarget = owner.getAttacking();

        if (ownerTarget != null
                && ownerTarget.isAlive()
                && ownerTarget != minion
                && minion.squaredDistanceTo(ownerTarget) <= 48.0 * 48.0) {
            minion.setTarget(ownerTarget);
        }

        // Greater Dreadglare slowly regenerates, matching its boss-like role.
        if (minion instanceof GreaterDreadglareEntity
                && minion.age % 40 == 0
                && minion.getHealth() < minion.getMaxHealth()) {
            minion.heal(1.0F);
        }
    }

    public static void tickPlayer(ServerPlayerEntity owner) {
        if (owner.age % 200 == 0 && AbilityRuntime.hasTalent(owner, AvengerContent.WINTERBORN.id())) {
            int frost = Math.max(0, (int) SpellPower.getSpellPower(SpellSchools.FROST, owner).baseValue());
            owner.addStatusEffect(new StatusEffectInstance(SmoothEffects.SOULSHOCK, 220, frost, false, false, false));
        }
        if (owner.age % 20 != 0) return;
        List<AvengerMinionEntity> minions = ownedMinions(owner, 15.0);

        // Plague: periodically transfer one harmful effect from the Avenger
        // to a random owned summon.
        if (AbilityRuntime.hasTalent(owner, AvengerContent.PLAGUE.id())
                && hasHarmfulEffect(owner)
                && !minions.isEmpty()) {
            AvengerMinionEntity recipient = minions.get(owner.getRandom().nextInt(minions.size()));
            transferHarmfulStack(owner, recipient);
            SkillFx.beam(owner, recipient, ParticleTypes.EFFECT, 12);
            SkillFx.sound(owner, "magic_shamanic_spell_03", 0.1F, 1.5F);
        }
    }

    public static void onSpellCast(ServerPlayerEntity owner) {
        if (!AbilityRuntime.hasTalent(owner, AvengerContent.DELIGHTFUL_SUFFERING.id())) return;
        var effect = switch (owner.getRandom().nextInt(4)) {
            case 0 -> StatusEffects.HUNGER;
            case 1 -> StatusEffects.SLOWNESS;
            case 2 -> StatusEffects.WITHER;
            default -> StatusEffects.MINING_FATIGUE;
        };
        StatusEffectInstance old = owner.getStatusEffect(effect);
        int amplifier = old == null ? 0 : Math.min(1, old.getAmplifier() + 1);
        owner.addStatusEffect(new StatusEffectInstance(effect, 800, amplifier, false, false, true));
    }

    public static void onIncomingDamage(ServerPlayerEntity owner) {
        if (!AbilityRuntime.hasTalent(owner, AvengerContent.DEATH_WARDEN.id())
                || owner.getHealth() >= owner.getMaxHealth() * 0.5F) return;
        float heal = owner.getMaxHealth() * 0.15F;
        List<AvengerMinionEntity> sacrifices = ownedMinions(owner, 15.0);
        for (AvengerMinionEntity sacrifice : sacrifices) {
            owner.heal(heal);
            sacrifice.damage(owner.getDamageSources().generic(), heal);
            SkillFx.beam(owner, sacrifice, ParticleTypes.POOF, 20);
        }
        if (!sacrifices.isEmpty()) SkillFx.sound(owner, "magic_shamanic_voice_20", 0.2F, 1.3F);
    }

    private static List<AvengerMinionEntity> ownedMinions(
            ServerPlayerEntity owner,
            double radius
    ) {
        return owner.getServerWorld().getEntitiesByClass(
                AvengerMinionEntity.class,
                owner.getBoundingBox().expand(radius),
                minion -> minion.isAlive() && owner.getUuid().equals(minion.getOwnerUuid())
        );
    }

    public static void onMinionDeath(
            ServerPlayerEntity owner,
            AvengerMinionEntity minion
    ) {
        if (AbilityRuntime.hasTalent(owner, AvengerContent.ENRAGE.id())) {
            enrageNearbyMinions(owner, minion);
        }

        if (AbilityRuntime.hasTalent(owner, AvengerContent.DEATH_ESSENCE.id())) {
            incrementBoneArmor(owner);
        }

        if (AbilityRuntime.hasTalent(owner, AvengerContent.SHADOW_COMBUST.id())) {
            shadowCombust(owner, minion);
        }

        if (AbilityRuntime.hasTalent(owner, AvengerContent.ENDLESS_SERVITUDE.id())) {
            int harmful = countHarmfulEffects(minion);

            if (owner.getRandom().nextInt(100)
                    < AvengerScaling.endlessServitudeChance(harmful)) {

                AvengerMinionType type =
                        minion instanceof WraithEntity
                                ? AvengerMinionType.WRAITH
                                : minion.isGreater()
                                ? AvengerMinionType.GREATER_DREADGLARE
                                : AvengerMinionType.DREADGLARE;

                spawn(
                        owner,
                        type,
                        new AvengerSummonPlan(
                                List.of(type),
                                AbilityRuntime.hasTalent(
                                        owner,
                                        AvengerContent.NECROTIC_FORTIFICATION.id()
                                ),
                                AbilityRuntime.hasTalent(
                                        owner,
                                        AvengerContent.SHADOW_AURA.id()
                                )
                        )
                );
                SkillFx.sound(owner, "magic_shamanic_spell_02", 0.2F, 1.0F);
            }
        }
    }

    private static void enrageNearbyMinions(
            ServerPlayerEntity owner,
            AvengerMinionEntity deadMinion
    ) {
        List<AvengerMinionEntity> minions =
                owner.getServerWorld().getEntitiesByClass(
                        AvengerMinionEntity.class,
                        deadMinion.getBoundingBox().expand(15.0),
                        minion ->
                                minion.isAlive()
                                && owner.getUuid().equals(minion.getOwnerUuid())
                );

        for (AvengerMinionEntity minion : minions) {
            incrementEffect(minion, StatusEffects.STRENGTH, 200, 3);
            incrementEffect(minion, StatusEffects.RESISTANCE, 200, 3);
        }
        SkillFx.sound(owner, "magic_shamanic_voice_20", 0.2F, 1.2F);
    }

    private static void incrementBoneArmor(ServerPlayerEntity owner) {
        StatusEffectInstance existing =
                owner.getStatusEffect(SmoothEffects.BONE_ARMOR);

        int amplifier = existing == null ? 0 : Math.min(24, existing.getAmplifier() + 1);

        owner.addStatusEffect(
                new StatusEffectInstance(
                        SmoothEffects.BONE_ARMOR,
                        400,
                        amplifier,
                        false,
                        false,
                        true
                )
        );
    }

    private static void incrementEffect(LivingEntity entity, net.minecraft.entity.effect.StatusEffect effect, int duration, int maxStacks) {
        StatusEffectInstance old = entity.getStatusEffect(effect);
        int amplifier = old == null ? 0 : Math.min(maxStacks - 1, old.getAmplifier() + 1);
        entity.addStatusEffect(new StatusEffectInstance(effect, duration, amplifier, false, false, true));
    }

    private static boolean hasHarmfulEffect(LivingEntity entity) {
        for(StatusEffectInstance effect:entity.getStatusEffects())
            if(!effect.getEffectType().isBeneficial())return true;
        return false;
    }

    private static int countHarmfulEffects(LivingEntity entity) {
        int count=0;
        for(StatusEffectInstance effect:entity.getStatusEffects())
            if(!effect.getEffectType().isBeneficial())count++;
        return count;
    }

    private static void shadowCombust(
            ServerPlayerEntity owner,
            AvengerMinionEntity minion
    ) {
        double radius = minion.isGreater() ? 7.0 : 4.0;

        float damage = (float) (SpellPowerRuntime.soulBase(owner) * (minion.isGreater() ? 6.4 : 3.2));

        Box box = minion.getBoundingBox().expand(radius);

        minion.getWorld()
                .getEntitiesByClass(
                        LivingEntity.class,
                        box,
                        entity ->
                                entity.isAlive()
                                && entity != owner
                                && entity != minion
                                && org.marj4n.smooth_classes.integration.OptionalCompatRuntime.canHarm(owner, entity)
                                && !(entity instanceof TameableEntity tame
                                && owner.getUuid().equals(tame.getOwnerUuid()))
                )
                .forEach(entity -> {
                    entity.timeUntilRegen = 0;
                    entity.damage(
                            owner.getDamageSources().indirectMagic(owner, owner),
                            damage
                    );
                    SkillFx.beam(minion, entity, ParticleTypes.SMOKE, 8);
                });
        SkillFx.sound(owner, "magic_shamanic_spell_03", 0.1F, 1.0F);
        owner.getServerWorld().playSound(null, minion.getBlockPos(), SoundEvents.ENTITY_GENERIC_EXPLODE,
                minion.getSoundCategory(), 0.1F, 1.0F);
        SkillFx.orbit(minion, ParticleTypes.EXPLOSION, 1, 2);
        SkillFx.orbit(minion, ParticleTypes.SOUL, 2, 20);
        SkillFx.orbit(minion, ParticleTypes.SMOKE, radius, 20);
    }
}
