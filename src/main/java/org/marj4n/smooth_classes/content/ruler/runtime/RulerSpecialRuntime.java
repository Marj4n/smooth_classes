package org.marj4n.smooth_classes.content.ruler.runtime;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.spell_power.api.SpellDamageSource;
import net.spell_power.api.SpellSchools;
import org.marj4n.smooth_classes.SmoothClasses;
import org.marj4n.smooth_classes.content.ruler.RulerClass;
import org.marj4n.smooth_classes.runtime.AbilityCooldowns;
import org.marj4n.smooth_classes.runtime.AbilityRuntime;
import org.marj4n.smooth_classes.runtime.ExecutionResult;
import org.marj4n.smooth_classes.runtime.SpellPowerRuntime;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Ruler H special: one battlefield edict, either Protect or Condemn. */
public final class RulerSpecialRuntime {
    public static final Identifier ID = SmoothClasses.id("class_special_divine_edict");
    public static final int ACTIVE_TICKS = 30 * 20;
    public static final int COOLDOWN_TICKS = 12 * 20;
    private static final double RANGE = 24D;

    public enum Mode { PROTECT, CONDEMN }
    private static final class State {
        UUID target;
        Mode mode;
        long expiresAt;
    }
    private static final Map<UUID, State> STATES = new HashMap<>();

    private RulerSpecialRuntime() {}

    public static ExecutionResult activate(ServerPlayerEntity player) {
        if (!AbilityRuntime.isClass(player, RulerClass.ID))
            return ExecutionResult.failure("Only Ruler can use Divine Edict.");
        if (!AbilityCooldowns.ready(player, ID))
            return ExecutionResult.failure("Divine Edict can be issued again in " + seconds(AbilityCooldowns.remainingTicks(player, ID)) + "s.");

        LivingEntity target = aimedLiving(player, RANGE);
        if (target == null) target = player;
        Mode mode = isProtectedTarget(player, target) ? Mode.PROTECT : Mode.CONDEMN;

        State state = new State();
        state.target = target.getUuid();
        state.mode = mode;
        state.expiresAt = player.getServer().getTicks() + ACTIVE_TICKS;
        STATES.put(player.getUuid(), state);
        AbilityCooldowns.start(player, ID, COOLDOWN_TICKS);
        applyEdict(target, mode);
        edictFx(player, target, mode);

        String name = target == player ? "yourself" : target.getName().getString();
        player.sendMessage(Text.literal("Edict: " + (mode == Mode.PROTECT ? "Protect " : "Condemn ") + name)
                .formatted(mode == Mode.PROTECT ? Formatting.GOLD : Formatting.RED), true);
        return ExecutionResult.success(1, "Divine Edict: " + mode.name().toLowerCase(java.util.Locale.ROOT));
    }

    public static void tick(ServerPlayerEntity player) {
        State state = STATES.get(player.getUuid());
        if (state == null) return;
        if (!AbilityRuntime.isClass(player, RulerClass.ID) || !player.isAlive()
                || player.getServer().getTicks() >= state.expiresAt) {
            STATES.remove(player.getUuid());
            return;
        }
        LivingEntity target = resolve(player, state.target);
        if (target == null || !target.isAlive() || target.getWorld() != player.getWorld()) {
            STATES.remove(player.getUuid());
            return;
        }
        applyEdict(target, state.mode);
        if ((player.age % 20) == 0) {
            player.getServerWorld().spawnParticles(state.mode == Mode.PROTECT ? ParticleTypes.END_ROD : ParticleTypes.SOUL_FIRE_FLAME,
                    target.getX(), target.getBodyY(0.8D), target.getZ(), 4, 0.25D, 0.25D, 0.25D, 0.02D);
        }
    }

    /** Ruler's own attacks gain a small judgement follow-up against the condemned target. */
    public static void onMeleeHit(ServerPlayerEntity player, LivingEntity target) {
        State state = STATES.get(player.getUuid());
        if (state == null || state.mode != Mode.CONDEMN || !active(player) || !target.getUuid().equals(state.target) || !target.isAlive()) return;
        float damage = Math.max(1.0F, SpellPowerRuntime.healing(player, 0.30D));
        target.timeUntilRegen = 0;
        target.damage(SpellDamageSource.create(SpellSchools.HEALING, player), damage);
        player.getServerWorld().spawnParticles(ParticleTypes.END_ROD,
                target.getX(), target.getBodyY(0.65D), target.getZ(), 7, 0.3D, 0.35D, 0.3D, 0.04D);
    }

    private static void applyEdict(LivingEntity target, Mode mode) {
        if (mode == Mode.PROTECT) {
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 30, 0, false, false, true));
        } else {
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 30, 0, false, false, true));
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 30, 0, false, false, false));
        }
    }

    private static boolean isProtectedTarget(ServerPlayerEntity caster, LivingEntity target) {
        if (target == caster) return true;
        if (target instanceof HostileEntity) return false;
        if (target instanceof PlayerEntity player) return caster.isTeammate(player);
        return true;
    }

    private static LivingEntity aimedLiving(ServerPlayerEntity player, double range) {
        Vec3d eye = player.getEyePos();
        Vec3d end = eye.add(player.getRotationVec(1.0F).normalize().multiply(range));
        Box scan = player.getBoundingBox().stretch(end.subtract(eye)).expand(1.0D);
        LivingEntity selected = null;
        double best = Double.MAX_VALUE;
        for (LivingEntity candidate : player.getServerWorld().getEntitiesByClass(LivingEntity.class, scan,
                e -> e != player && e.isAlive())) {
            var hit = candidate.getBoundingBox().expand(0.30D).raycast(eye, end);
            if (hit.isEmpty()) continue;
            double dist = eye.squaredDistanceTo(hit.get());
            if (dist < best) { best = dist; selected = candidate; }
        }
        return selected;
    }

    private static LivingEntity resolve(ServerPlayerEntity player, UUID uuid) {
        Entity entity = player.getServerWorld().getEntity(uuid);
        return entity instanceof LivingEntity living ? living : null;
    }

    private static void edictFx(ServerPlayerEntity caster, LivingEntity target, Mode mode) {
        ServerWorld world = caster.getServerWorld();
        var particle = mode == Mode.PROTECT ? ParticleTypes.END_ROD : ParticleTypes.SOUL_FIRE_FLAME;
        world.spawnParticles(particle, target.getX(), target.getBodyY(0.65D), target.getZ(),
                26, 0.5D, 0.75D, 0.5D, 0.08D);
        world.playSound(null, caster.getBlockPos(), SoundEvents.BLOCK_BEACON_ACTIVATE,
                SoundCategory.PLAYERS, 0.65F, mode == Mode.PROTECT ? 1.45F : 0.75F);
    }

    public static boolean active(ServerPlayerEntity player) {
        State state = STATES.get(player.getUuid());
        return state != null && state.expiresAt > player.getServer().getTicks();
    }
    public static int variant(ServerPlayerEntity player) {
        State state = STATES.get(player.getUuid());
        return state == null ? 0 : state.mode == Mode.PROTECT ? 1 : 2;
    }
    public static long remainingTicks(ServerPlayerEntity player) { return AbilityCooldowns.remainingTicks(player, ID); }
    public static int cooldownTicks() { return COOLDOWN_TICKS; }
    public static void cleanup(ServerPlayerEntity player) { STATES.remove(player.getUuid()); }
    public static void clear() { STATES.clear(); }
    private static int seconds(long ticks) { return (int) Math.ceil(Math.max(0L, ticks) / 20.0D); }
}
