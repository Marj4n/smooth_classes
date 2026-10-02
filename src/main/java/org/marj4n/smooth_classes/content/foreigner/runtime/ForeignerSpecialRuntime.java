package org.marj4n.smooth_classes.content.foreigner.runtime;

import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.spell_power.api.SpellDamageSource;
import net.spell_power.api.SpellSchool;
import org.marj4n.smooth_classes.SmoothClasses;
import org.marj4n.smooth_classes.content.foreigner.ForeignerClass;
import org.marj4n.smooth_classes.runtime.AbilityCooldowns;
import org.marj4n.smooth_classes.runtime.AbilityRuntime;
import org.marj4n.smooth_classes.runtime.ExecutionResult;
import org.marj4n.smooth_classes.runtime.SpellPowerRuntime;
import org.marj4n.smooth_classes.runtime.SpellSchoolSelection;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Foreigner H special: hold H and choose the magic directly for the weapon imprint. */
public final class ForeignerSpecialRuntime {
    public static final Identifier ID = SmoothClasses.id("class_special_spell_imprint");
    public static final int ACTIVE_TICKS = 15 * 20;
    public static final int COOLDOWN_TICKS = 25 * 20;

    private static final Map<UUID, Integer> ACTIVE_SCHOOL = new HashMap<>();
    private static final Map<UUID, Integer> LAST_SELECTION = new HashMap<>();
    private static final Map<UUID, Long> ACTIVE_UNTIL = new HashMap<>();

    private ForeignerSpecialRuntime() {}

    public static ExecutionResult activate(ServerPlayerEntity player) {
        if (!AbilityRuntime.isClass(player, ForeignerClass.ID))
            return ExecutionResult.failure("Only Foreigner can use Spell Imprint.");
        return ExecutionResult.failure("Hold H, choose a spell school, then release H.");
    }

    public static ExecutionResult select(ServerPlayerEntity player, int index) {
        if (!AbilityRuntime.isClass(player, ForeignerClass.ID))
            return ExecutionResult.failure("Only Foreigner can use Spell Imprint.");
        if (index < 0 || index >= SpellSchoolSelection.SIZE)
            return ExecutionResult.failure("No spell school selected.");
        if (player.getMainHandStack().isEmpty())
            return ExecutionResult.failure("Spell Imprint requires a weapon in your main hand.");
        if (active(player))
            return ExecutionResult.failure("Spell Imprint is already active.");
        if (!AbilityCooldowns.ready(player, ID))
            return ExecutionResult.failure("Spell Imprint can be used again in " + seconds(AbilityCooldowns.remainingTicks(player, ID)) + "s.");

        SpellSchool school = SpellSchoolSelection.school(index);
        if (school == null)
            return ExecutionResult.failure(SpellSchoolSelection.name(index) + " magic is not registered in this modpack.");

        LAST_SELECTION.put(player.getUuid(), index);
        ACTIVE_SCHOOL.put(player.getUuid(), index);
        ACTIVE_UNTIL.put(player.getUuid(), (long) player.getServer().getTicks() + ACTIVE_TICKS);
        AbilityCooldowns.start(player, ID, COOLDOWN_TICKS);
        imprintFx(player, index);
        player.sendMessage(Text.literal("Spell Imprint: " + SpellSchoolSelection.name(index))
                .formatted(SpellSchoolSelection.color(index)), true);
        return ExecutionResult.success(1, "Imprinted " + SpellSchoolSelection.name(index) + " into your weapon for 15s.");
    }

    public static void onMeleeHit(ServerPlayerEntity player, LivingEntity target) {
        if (!active(player) || target == null || !target.isAlive() || player.getMainHandStack().isEmpty()) return;
        int index = ACTIVE_SCHOOL.getOrDefault(player.getUuid(), -1);
        SpellSchool school = SpellSchoolSelection.school(index);
        if (school == null) return;
        float damage = Math.max(1.0F, SpellPowerRuntime.scaled(school, player, 0.45D));
        target.timeUntilRegen = 0;
        target.damage(SpellDamageSource.create(school, player), damage);
        player.getServerWorld().spawnParticles(SpellSchoolSelection.particle(index),
                target.getX(), target.getBodyY(0.55D), target.getZ(), 10, 0.35D, 0.45D, 0.35D, 0.04D);
        player.getServerWorld().playSound(null, target.getBlockPos(), SoundEvents.BLOCK_AMETHYST_BLOCK_HIT,
                SoundCategory.PLAYERS, 0.35F, 1.35F);
    }

    public static void tick(ServerPlayerEntity player) {
        Long until = ACTIVE_UNTIL.get(player.getUuid());
        if (until == null) return;
        if (!AbilityRuntime.isClass(player, ForeignerClass.ID) || !player.isAlive()
                || player.getServer().getTicks() >= until) {
            ACTIVE_UNTIL.remove(player.getUuid());
            ACTIVE_SCHOOL.remove(player.getUuid());
            return;
        }

        // Keep the selected school visibly wrapped around the held weapon. This is
        // deliberately a lightweight particle sheath rather than copying another mod's
        // sword textures: the aura works on any weapon the Spellblade decides to use.
        if (player.age % 4 == 0 && !player.getMainHandStack().isEmpty()) {
            weaponAura(player, ACTIVE_SCHOOL.getOrDefault(player.getUuid(), -1));
        }
    }

    public static boolean active(ServerPlayerEntity player) {
        return ACTIVE_UNTIL.getOrDefault(player.getUuid(), 0L) > player.getServer().getTicks();
    }

    public static SpellSchool activeSchool(ServerPlayerEntity player) {
        return SpellSchoolSelection.school(ACTIVE_SCHOOL.getOrDefault(player.getUuid(), -1));
    }

    public static int variant(ServerPlayerEntity player) {
        return active(player)
                ? ACTIVE_SCHOOL.getOrDefault(player.getUuid(), -1)
                : LAST_SELECTION.getOrDefault(player.getUuid(), -1);
    }

    public static long remainingTicks(ServerPlayerEntity player) { return AbilityCooldowns.remainingTicks(player, ID); }
    public static long activeRemainingTicks(ServerPlayerEntity player) {
        return Math.max(0L, ACTIVE_UNTIL.getOrDefault(player.getUuid(), 0L) - player.getServer().getTicks());
    }
    public static int cooldownTicks() { return COOLDOWN_TICKS; }
    public static void cleanup(ServerPlayerEntity player) {
        ACTIVE_UNTIL.remove(player.getUuid());
        ACTIVE_SCHOOL.remove(player.getUuid());
        LAST_SELECTION.remove(player.getUuid());
    }
    public static void clear() { LAST_SELECTION.clear(); ACTIVE_SCHOOL.clear(); ACTIVE_UNTIL.clear(); }


    private static void weaponAura(ServerPlayerEntity player, int index) {
        if (index < 0) return;
        net.minecraft.util.math.Vec3d look = player.getRotationVec(1F).normalize();
        net.minecraft.util.math.Vec3d right = new net.minecraft.util.math.Vec3d(-look.z, 0D, look.x);
        if (right.lengthSquared() > 0.0001D) right = right.normalize();
        net.minecraft.util.math.Vec3d base = player.getEyePos()
                .add(0D, -0.58D, 0D)
                .add(right.multiply(0.34D))
                .add(look.multiply(0.20D));

        for (int i = 0; i < 3; i++) {
            double along = 0.18D + i * 0.24D;
            net.minecraft.util.math.Vec3d point = base.add(look.multiply(along));
            player.getServerWorld().spawnParticles(SpellSchoolSelection.particle(index),
                    point.x, point.y, point.z, 1, 0.025D, 0.035D, 0.025D, 0.005D);
        }

        // A second school-specific accent makes the aura read immediately even on fast swings.
        net.minecraft.particle.ParticleEffect accent = switch (Math.floorMod(index, SpellSchoolSelection.SIZE)) {
            case SpellSchoolSelection.FIRE -> net.minecraft.particle.ParticleTypes.SMALL_FLAME;
            case SpellSchoolSelection.FROST -> net.minecraft.particle.ParticleTypes.SNOWFLAKE;
            case SpellSchoolSelection.WIND -> net.minecraft.particle.ParticleTypes.POOF;
            case SpellSchoolSelection.WATER -> net.minecraft.particle.ParticleTypes.SPLASH;
            case SpellSchoolSelection.EARTH -> net.minecraft.particle.ParticleTypes.ASH;
            default -> net.minecraft.particle.ParticleTypes.END_ROD;
        };
        net.minecraft.util.math.Vec3d tip = base.add(look.multiply(0.70D));
        player.getServerWorld().spawnParticles(accent, tip.x, tip.y, tip.z, 1,
                0.02D, 0.02D, 0.02D, 0.002D);
    }

    private static void imprintFx(ServerPlayerEntity player, int index) {
        player.getServerWorld().spawnParticles(SpellSchoolSelection.particle(index),
                player.getX(), player.getBodyY(0.55D), player.getZ(), 32, 0.65D, 0.85D, 0.65D, 0.08D);
        player.getServerWorld().playSound(null, player.getBlockPos(), SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE,
                SoundCategory.PLAYERS, 0.7F, 1.2F);
    }

    private static int seconds(long ticks) { return (int) Math.ceil(Math.max(0L, ticks) / 20.0D); }
}
