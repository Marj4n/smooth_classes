package org.marj4n.smooth_classes.origin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.GuardianEntity;
import net.minecraft.entity.mob.DrownedEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.marj4n.smooth_classes.network.SmoothClassesNetworking;

/** Server-authoritative active abilities for the four V1 Origins. */
public final class OriginAbilityDispatcher {
    public record Result(boolean success, String message) {
        public static Result fail(String message) { return new Result(false, message); }
        public static Result ok(String message) { return new Result(true, message); }
    }

    private OriginAbilityDispatcher() {}

    public static Result activate(ServerPlayerEntity player, int slot) {
        OriginState state = OriginRuntime.state(player);
        OriginType origin = state.origin();
        if (origin == null) return Result.fail("Choose an Origin first.");
        if (slot < 0 || slot > 2) return Result.fail("Unknown Origin ability slot.");

        return switch (origin) {
            case HUMAN -> activateHuman(player, state, slot);
            case VAMPIRE -> activateVampire(player, state, slot);
            case MERMAID -> activateMermaid(player, state, slot);
            case SLIME -> activateSlime(player, state, slot);
            default -> Result.fail("This Origin is archived for V2.");
        };
    }

    private static Result activateHuman(ServerPlayerEntity player, OriginState state, int slot) {
        if (slot == 0) {
            if (!OriginSkillRuntime.unlocked(player, OriginType.HUMAN, "survivor")) return Result.fail("Adaptation is still locked.");
            String[] known = {"nether", "ocean", "sky", "cold", "end"};
            int start = state.progress("human.adaptation_index");
            for (int i = 1; i <= known.length; i++) {
                int next = (start + i) % known.length;
                if (state.hasFlag("human.environment_mastered." + known[next])) {
                    state.progress("human.adaptation_index", next);
                    state.flag("human.adaptation_selected." + known[next]);
                    player.sendMessage(Text.literal("Adaptation: " + title(known[next])), true);
                    SmoothClassesNetworking.sendOriginState(player);
                    return Result.ok("Adaptation changed.");
                }
            }
            return Result.fail("Master a harsh environment first.");
        }
        if (slot == 1) {
            if (!OriginSkillRuntime.unlocked(player, OriginType.HUMAN, "second_wind")) return Result.fail("Second Wind is still locked.");
            long ready = state.longProgress("human.second_wind_manual_ready_at");
            if (player.getWorld().getTime() < ready) return Result.fail("Second Wind is recovering.");
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 20 * 8, 1, false, true, true));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 20 * 8, 0, false, true, true));
            player.heal(4.0F);
            state.longProgress("human.second_wind_manual_ready_at", player.getWorld().getTime() + 20L * 120L);
            return Result.ok("Second Wind.");
        }
        if (!OriginSkillRuntime.unlocked(player, OriginType.HUMAN, "final")) return Result.fail("Awakened Human is still locked.");
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 20 * 12, 0, false, true, true));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 20 * 12, 0, false, true, true));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 20 * 12, 0, false, true, true));
        return Result.ok("Human potential awakened.");
    }

    private static Result activateVampire(ServerPlayerEntity player, OriginState state, int slot) {
        if (slot == 0) {
            if (!OriginSkillRuntime.unlocked(player, OriginType.VAMPIRE, "bat_form")) return Result.fail("Bat Form is still locked.");
            long ready = state.longProgress("vampire.bat_form_ready_at");
            if (!state.hasFlag("vampire.form.bat") && player.getWorld().getTime() < ready) return Result.fail("Bat Form is recovering.");
            if (state.hasFlag("vampire.form.bat")) {
                state.unflag("vampire.form.bat");
                state.longProgress("vampire.bat_form_ready_at", player.getWorld().getTime() + 200L); // Nycto-like 10s cooldown
                if (!player.isCreative() && !player.isSpectator()) {
                    player.getAbilities().flying = false;
                    player.getAbilities().allowFlying = false;
                    player.sendAbilitiesUpdate();
                }
                player.calculateDimensions();
                vampireTransformFx(player, false);
                SmoothClassesNetworking.sendOriginState(player);
                return Result.ok("Bat Form ended.");
            }
            if (state.blood() < 3) return Result.fail("You need at least 3 Blood.");
            state.blood(state.blood() - 3);
            state.sunExposure(0);
            player.extinguish();
            if (state.hasFlag("vampire.form.man_bat")) {
                state.unflag("vampire.form.man_bat");
                VampireManBatRuntime.removeAttributes(player);
            }
            state.flag("vampire.form.bat");
            if (!player.isCreative() && !player.isSpectator()) {
                // Airborne transform should feel seamless: if the player jumps/falls and
                // transforms mid-air, enter Bat flight immediately instead of requiring
                // a second double-space input. Grounded transforms still begin grounded.
                boolean airborneTransform = !player.isOnGround();
                player.getAbilities().allowFlying = true;
                player.getAbilities().flying = airborneTransform;
                if (airborneTransform) player.fallDistance = 0.0F;
                player.sendAbilitiesUpdate();
            }
            player.calculateDimensions();
            vampireTransformFx(player, true);
            SmoothClassesNetworking.sendOriginState(player);
            return Result.ok("Bat Form.");
        }

        if (slot == 1) {
            if (!OriginSkillRuntime.unlocked(player, OriginType.VAMPIRE, "man_bat")) return Result.fail("Man-Bat is still locked.");

            if (state.hasFlag("vampire.form.man_bat")) {
                state.unflag("vampire.form.man_bat");
                VampireManBatRuntime.onExit(player, state);
                player.calculateDimensions();
                vampireTransformFx(player, false);
                SmoothClassesNetworking.sendOriginState(player);
                return Result.ok("Man-Bat dismissed.");
            }

            long ready = state.longProgress("vampire.man_bat_ready_at");
            if (player.getWorld().getTime() < ready) return Result.fail("Man-Bat is recovering.");
            if (state.blood() < 3) return Result.fail("You need at least 3 Blood.");

            state.blood(state.blood() - 3);
            // Forms are exclusive. If Bat Form was flying, strip that creative-flight permission
            // before entering Dark Form's air-jump/glide movement.
            state.unflag("vampire.form.bat");
            state.flag("vampire.form.man_bat");
            VampireManBatRuntime.onEnter(player, state);
            player.calculateDimensions();
            vampireTransformFx(player, true);
            SmoothClassesNetworking.sendOriginState(player);
            return Result.ok("Man-Bat unleashed.");
        }

        if (!OriginSkillRuntime.unlocked(player, OriginType.VAMPIRE, "blood_sense")) return Result.fail("Blood Sense is still locked.");
        for (LivingEntity living : player.getWorld().getEntitiesByClass(LivingEntity.class,
                player.getBoundingBox().expand(32.0D), e -> e != player && e.isAlive() && e.getHealth() < e.getMaxHealth())) {
            living.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 20 * 8, 0, false, false, false));
        }
        return Result.ok("Blood Sense.");
    }

    private static Result activateMermaid(ServerPlayerEntity player, OriginState state, int slot) {
        if (slot == 0) {
            if (!OriginSkillRuntime.unlocked(player, OriginType.MERMAID, "siren_voice")) return Result.fail("Siren Voice is still locked.");
            long ready = state.longProgress("mermaid.siren_ready_at");
            if (player.getWorld().getTime() < ready) return Result.fail("Siren Voice is recovering.");
            double radius = OriginSkillRuntime.unlocked(player, OriginType.MERMAID, "drowned_song") ? 18.0D : 12.0D;
            for (MobEntity mob : player.getWorld().getEntitiesByClass(MobEntity.class, player.getBoundingBox().expand(radius), MobEntity::isAlive)) {
                mob.setTarget(null);
                mob.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 20 * 6, 2, false, true, true));
                mob.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 20 * 6, 1, false, true, true));
                Vec3d toward = player.getPos().subtract(mob.getPos());
                if (toward.lengthSquared() > 0.001D) mob.addVelocity(toward.normalize().multiply(0.12D));
            }
            state.longProgress("mermaid.siren_ready_at", player.getWorld().getTime() + 20L * 30L);
            return Result.ok("Siren Voice.");
        }

        if (slot == 1) {
            if (!OriginSkillRuntime.unlocked(player, OriginType.MERMAID, "sea_kinship")) return Result.fail("Sea Kinship is still locked.");
            if (!player.isTouchingWaterOrRain()) return Result.fail("You need water for Tidal Rush.");
            Vec3d look = player.getRotationVec(1.0F).normalize();
            player.setVelocity(look.x * 1.35D, Math.max(0.15D, look.y * 0.8D), look.z * 1.35D);
            player.velocityModified = true;
            return Result.ok("Tidal Rush.");
        }

        if (!OriginSkillRuntime.unlocked(player, OriginType.MERMAID, "final")) return Result.fail("Mermaid Queen is still locked.");
        for (MobEntity mob : player.getWorld().getEntitiesByClass(MobEntity.class, player.getBoundingBox().expand(24.0D), MobEntity::isAlive)) {
            if (mob instanceof GuardianEntity || mob instanceof DrownedEntity) mob.setTarget(null);
        }
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 20 * 10, 1, false, true, true));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.DOLPHINS_GRACE, 20 * 15, 1, false, true, true));
        return Result.ok("Royal Tide.");
    }

    private static Result activateSlime(ServerPlayerEntity player, OriginState state, int slot) {
        if (slot == 0) {
            if (!OriginSkillRuntime.unlocked(player, OriginType.SLIME, "humanoid")) return Result.fail("Humanoid Form is still locked.");
            boolean humanoid = state.toggleFlag("slime.form.humanoid");
            player.calculateDimensions();
            SmoothClassesNetworking.sendOriginState(player);
            return Result.ok(humanoid ? "Humanoid Form." : "Slime Form.");
        }

        if (slot == 1) {
            if (!OriginSkillRuntime.unlocked(player, OriginType.SLIME, "size_control")) {
                if (!OriginSkillRuntime.unlocked(player, OriginType.SLIME, "squeeze")) return Result.fail("Squeeze is still locked.");
                boolean squeeze = state.toggleFlag("slime.squeeze");
                player.calculateDimensions();
                SmoothClassesNetworking.sendOriginState(player);
                return Result.ok(squeeze ? "Squeezed small." : "Normal size.");
            }
            if (state.hasFlag("slime.size.small")) {
                state.unflag("slime.size.small");
                state.flag("slime.size.large");
                state.unflag("slime.squeeze");
                player.calculateDimensions();
                return Result.ok("Large Slime.");
            }
            if (state.hasFlag("slime.size.large")) {
                state.unflag("slime.size.large");
                state.unflag("slime.squeeze");
                player.calculateDimensions();
                return Result.ok("Normal Slime.");
            }
            state.flag("slime.size.small");
            state.unflag("slime.size.large");
            state.flag("slime.squeeze");
            player.calculateDimensions();
            return Result.ok("Small Slime.");
        }

        if (!OriginSkillRuntime.unlocked(player, OriginType.SLIME, "fragment")) return Result.fail("Fragmentation is still locked.");
        long ready = state.longProgress("slime.fragment_manual_ready_at");
        if (player.getWorld().getTime() < ready) return Result.fail("Fragmentation is recovering.");
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 20 * 6, 2, false, true, true));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 20 * 6, 1, false, true, true));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 20 * 6, 1, false, true, true));
        state.longProgress("slime.fragment_manual_ready_at", player.getWorld().getTime() + 20L * 90L);
        return Result.ok("Fragmentation.");
    }

    private static void vampireTransformFx(ServerPlayerEntity player, boolean entering) {
        var world = player.getServerWorld();
        world.spawnParticles(ParticleTypes.LARGE_SMOKE, player.getX(), player.getBodyY(0.55D), player.getZ(),
                entering ? 18 : 10, 0.35D, 0.55D, 0.35D, 0.025D);
        world.spawnParticles(ParticleTypes.CLOUD, player.getX(), player.getBodyY(0.45D), player.getZ(),
                entering ? 12 : 7, 0.28D, 0.38D, 0.28D, 0.035D);
    }

    private static String title(String value) {
        if (value == null || value.isBlank()) return "Unknown";
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }
}
