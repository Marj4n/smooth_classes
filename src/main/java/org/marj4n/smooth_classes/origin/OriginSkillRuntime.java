package org.marj4n.smooth_classes.origin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.GuardianEntity;
import net.minecraft.entity.mob.DrownedEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.puffish.skillsmod.api.Skill;
import org.marj4n.smooth_classes.integration.PuffishSkillsIntegration;

/**
 * Gameplay effects granted by unlocked Origin evolution nodes.
 * Kept server authoritative; render flags are mirrored through OriginState sync.
 */
public final class OriginSkillRuntime {
    private OriginSkillRuntime() {}

    public static boolean unlocked(ServerPlayerEntity player, OriginType origin, String skillId) {
        return PuffishSkillsIntegration.category(origin.categoryId())
                .flatMap(category -> category.getSkill(skillId))
                .map(skill -> skill.getState(player) == Skill.State.UNLOCKED)
                .orElse(false);
    }

    public static void tick(ServerPlayerEntity player, OriginState state) {
        OriginType origin = state.origin();
        if (origin == null) return;
        switch (origin) {
            case HUMAN -> tickHuman(player, state);
            case VAMPIRE -> tickVampire(player, state);
            case MERMAID -> tickMermaid(player, state);
            case SLIME -> tickSlime(player, state);
            default -> { }
        }
    }

    private static void tickHuman(ServerPlayerEntity player, OriginState state) {
        if (unlocked(player, OriginType.HUMAN, "survivor")) {
            String current = currentHumanEnvironment(player);
            if (current != null && state.hasFlag("human.environment_mastered." + current)) {
                applyHumanAdaptation(player, current);
            }

            String[] known = {"nether", "ocean", "sky", "cold", "end"};
            int selected = Math.floorMod(state.progress("human.adaptation_index"), known.length);
            if (unlocked(player, OriginType.HUMAN, "memory")
                    && state.hasFlag("human.environment_mastered." + known[selected])) {
                applyHumanAdaptation(player, known[selected]);
            }

            if (unlocked(player, OriginType.HUMAN, "wayfarer")) {
                for (int i = 1; i <= known.length; i++) {
                    String extra = known[(selected + i) % known.length];
                    if (state.hasFlag("human.environment_mastered." + extra)) {
                        applyHumanAdaptation(player, extra);
                        break;
                    }
                }
            }

            if (unlocked(player, OriginType.HUMAN, "final")) {
                int applied = 0;
                for (String extra : known) {
                    if (!state.hasFlag("human.environment_mastered." + extra)) continue;
                    applyHumanAdaptation(player, extra);
                    if (++applied >= 3) break;
                }
            }
        }

        if (unlocked(player, OriginType.HUMAN, "second_wind")) {
            long readyAt = state.longProgress("human.second_wind_ready_at");
            if (player.getHealth() <= player.getMaxHealth() * 0.30F && player.getWorld().getTime() >= readyAt) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 20 * 6, 1, false, true, true));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 20 * 6, 0, false, true, true));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 20 * 4, 0, false, true, true));
                state.longProgress("human.second_wind_ready_at", player.getWorld().getTime() + 20L * 120L);
            }
        }
    }

    private static String currentHumanEnvironment(ServerPlayerEntity player) {
        if (player.getWorld().getRegistryKey() == net.minecraft.world.World.NETHER) return "nether";
        if (player.getWorld().getRegistryKey() == net.minecraft.world.World.END) return "end";
        if (player.isSubmergedInWater()) return "ocean";
        if (player.getY() >= 160) return "sky";
        if (player.isFrozen()) return "cold";
        return null;
    }

    private static void applyHumanAdaptation(ServerPlayerEntity player, String adaptation) {
        switch (adaptation) {
            case "nether" -> player.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 50, 0, false, false, true));
            case "ocean" -> player.addStatusEffect(new StatusEffectInstance(StatusEffects.WATER_BREATHING, 50, 0, false, false, true));
            case "sky" -> player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 50, 0, false, false, true));
            case "cold" -> player.setFrozenTicks(Math.max(0, player.getFrozenTicks() - 8));
            case "end" -> player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 50, 0, false, false, true));
            default -> { }
        }
    }

    private static void tickVampire(ServerPlayerEntity player, OriginState state) {
        boolean lord = unlocked(player, OriginType.VAMPIRE, "final");
        if (lord) state.flag("vampire.evolution.lord");
        else state.unflag("vampire.evolution.lord");
        boolean bat = state.hasFlag("vampire.form.bat");
        boolean manBat = state.hasFlag("vampire.form.man_bat");

        if (bat) {
            // Nycto-style utility form: tiny body, free aerial movement, no combat role.
            if (!player.isCreative() && !player.isSpectator()) {
                // Creative-like flight: entering Bat Form grants flight permission, but does not
                // auto-launch. Vanilla double-space toggles flying on/off for the player.
                boolean changed = !player.getAbilities().allowFlying;
                player.getAbilities().allowFlying = true;
                if (changed) player.sendAbilitiesUpdate();
            }
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 30, 1, false, false, false));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 30, 4, false, false, false));
            if (player.age % 40 == 0) state.blood(state.blood() - 1);
            if (state.blood() <= 0) exitVampireForms(player, state);
        } else if (manBat) {
            // Man-Bat is the Nycto-style Dark Form combat body. It uses air-jumps and
            // glide drag rather than Bat Form's creative flight permission.
            VampireManBatRuntime.tick(player, state);
        }

        if (lord && player.getWorld().isNight()) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 30, 0, false, false, false));
            if (!bat) player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 30, 0, false, false, false));
        }

        if (unlocked(player, OriginType.VAMPIRE, "blood_sense") && player.age % 10 == 0) {
            for (LivingEntity living : player.getWorld().getEntitiesByClass(LivingEntity.class,
                    player.getBoundingBox().expand(24.0D), e -> e != player && e.isAlive() && e.getHealth() < e.getMaxHealth())) {
                living.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 16, 0, false, false, false));
            }
        }
    }

    private static void tickMermaid(ServerPlayerEntity player, OriginState state) {
        boolean submerged = player.isSubmergedInWater();
        if (submerged && unlocked(player, OriginType.MERMAID, "sea_kinship")) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.DOLPHINS_GRACE, 40, 0, false, false, false));
            if (player.age % 60 == 0 && player.getHealth() < player.getMaxHealth()) player.heal(1.0F);
        }

        if (unlocked(player, OriginType.MERMAID, "oceans_favor") && player.age % 10 == 0) {
            for (MobEntity mob : player.getWorld().getEntitiesByClass(MobEntity.class,
                    player.getBoundingBox().expand(18.0D), MobEntity::isAlive)) {
                if (mob.getTarget() == player && (mob instanceof GuardianEntity || mob instanceof DrownedEntity)) {
                    mob.setTarget(null);
                }
            }
        }
    }

    private static void tickSlime(ServerPlayerEntity player, OriginState state) {
        if (unlocked(player, OriginType.SLIME, "fragment")) {
            long readyAt = state.longProgress("slime.fragment_ready_at");
            if (player.getHealth() <= player.getMaxHealth() * 0.25F && player.getWorld().getTime() >= readyAt) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 20 * 5, 2, false, true, true));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 20 * 5, 1, false, true, true));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 20 * 5, 1, false, true, true));
                state.longProgress("slime.fragment_ready_at", player.getWorld().getTime() + 20L * 90L);
            }
        }
    }

    public static boolean blockVampireCombat(PlayerEntity player) {
        OriginState state = OriginRuntime.state(player);
        return state.origin() == OriginType.VAMPIRE && state.hasFlag("vampire.form.bat");
    }

    public static void exitVampireForms(ServerPlayerEntity player, OriginState state) {
        boolean wasManBat = state.hasFlag("vampire.form.man_bat");
        state.unflag("vampire.form.bat");
        state.unflag("vampire.form.man_bat");
        if (wasManBat) VampireManBatRuntime.onExit(player, state);
        if (!player.isCreative() && !player.isSpectator()) {
            player.getAbilities().flying = false;
            player.getAbilities().allowFlying = false;
            player.sendAbilitiesUpdate();
        }
        player.calculateDimensions();
    }
}
