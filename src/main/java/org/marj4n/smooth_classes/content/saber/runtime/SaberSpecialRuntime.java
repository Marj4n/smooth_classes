package org.marj4n.smooth_classes.content.saber.runtime;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.spell_power.api.SpellDamageSource;
import net.spell_power.api.SpellSchools;
import org.marj4n.smooth_classes.SmoothClasses;
import org.marj4n.smooth_classes.content.saber.SaberClass;
import org.marj4n.smooth_classes.runtime.AbilityCooldowns;
import org.marj4n.smooth_classes.runtime.AbilityRuntime;
import org.marj4n.smooth_classes.runtime.ExecutionResult;
import org.marj4n.smooth_classes.runtime.SpellPowerRuntime;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Saber H special: short holy melee overdrive. */
public final class SaberSpecialRuntime {
    public static final Identifier ID = SmoothClasses.id("class_special_radiant_burst");
    public static final int ACTIVE_TICKS = 10 * 20;
    public static final int COOLDOWN_TICKS = 35 * 20;

    private static final UUID DAMAGE_MOD = UUID.fromString("66dad93f-2b71-4372-8e7a-4ee0bb8b9278");
    private static final UUID SPEED_MOD = UUID.fromString("57ab041f-1722-4a93-b684-c4588415d677");
    private static final UUID MOVE_MOD = UUID.fromString("252494c1-d027-41e9-af34-f76e03dc798f");
    private static final Map<UUID, Long> ACTIVE_UNTIL = new HashMap<>();

    private SaberSpecialRuntime() {}

    public static ExecutionResult activate(ServerPlayerEntity player) {
        if (!AbilityRuntime.isClass(player, SaberClass.ID))
            return ExecutionResult.failure("Only Saber can use Radiant Burst.");
        if (!isSword(player.getMainHandStack()))
            return ExecutionResult.failure("Radiant Burst requires a sword in your main hand.");
        if (active(player)) return ExecutionResult.failure("Radiant Burst is already active.");
        if (!AbilityCooldowns.ready(player, ID))
            return ExecutionResult.failure("Radiant Burst can be used again in " + seconds(AbilityCooldowns.remainingTicks(player, ID)) + "s.");

        long now = player.getServer().getTicks();
        ACTIVE_UNTIL.put(player.getUuid(), now + ACTIVE_TICKS);
        AbilityCooldowns.start(player, ID, COOLDOWN_TICKS);
        applyModifiers(player);
        burstFx(player);
        player.sendMessage(Text.literal("Radiant Burst").formatted(Formatting.GOLD), true);
        return ExecutionResult.success(1, "Radiant Burst empowered your sword for 10s.");
    }

    public static void tick(ServerPlayerEntity player) {
        Long until = ACTIVE_UNTIL.get(player.getUuid());
        if (until == null) {
            if (!AbilityRuntime.isClass(player, SaberClass.ID)) removeModifiers(player);
            return;
        }
        if (!AbilityRuntime.isClass(player, SaberClass.ID) || !player.isAlive() || player.getServer().getTicks() >= until) {
            ACTIVE_UNTIL.remove(player.getUuid());
            removeModifiers(player);
            return;
        }
        applyModifiers(player);
        if ((player.age % 4) == 0) {
            player.getServerWorld().spawnParticles(ParticleTypes.END_ROD,
                    player.getX(), player.getBodyY(0.55D), player.getZ(), 3, 0.35D, 0.6D, 0.35D, 0.015D);
        }
    }

    /** Called after the vanilla melee hit lands so the holy follow-up cannot steal the base hit via i-frames. */
    public static void onMeleeHit(ServerPlayerEntity player, LivingEntity target) {
        if (!active(player) || !isSword(player.getMainHandStack()) || !target.isAlive()) return;
        float attack = (float) Math.max(1.0D, player.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE) * 0.35D);
        float holy = Math.max(attack, SpellPowerRuntime.healing(player, 0.55D));
        target.timeUntilRegen = 0;
        target.damage(SpellDamageSource.create(SpellSchools.HEALING, player), holy);
        player.getServerWorld().spawnParticles(ParticleTypes.END_ROD,
                target.getX(), target.getBodyY(0.55D), target.getZ(), 9, 0.35D, 0.45D, 0.35D, 0.04D);
        player.getServerWorld().playSound(null, target.getBlockPos(), SoundEvents.BLOCK_AMETHYST_CLUSTER_HIT,
                SoundCategory.PLAYERS, 0.45F, 1.6F);
    }

    public static boolean active(ServerPlayerEntity player) {
        return ACTIVE_UNTIL.getOrDefault(player.getUuid(), 0L) > player.getServer().getTicks();
    }

    public static long remainingTicks(ServerPlayerEntity player) { return AbilityCooldowns.remainingTicks(player, ID); }
    public static int cooldownTicks() { return COOLDOWN_TICKS; }

    public static void cleanup(ServerPlayerEntity player) {
        ACTIVE_UNTIL.remove(player.getUuid());
        removeModifiers(player);
    }
    public static void clear() { ACTIVE_UNTIL.clear(); }

    private static boolean isSword(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (stack.getItem() instanceof SwordItem) return true;
        String path = Registries.ITEM.getId(stack.getItem()).getPath().toLowerCase(java.util.Locale.ROOT);
        return path.contains("sword") || path.contains("blade") || path.contains("katana") || path.contains("saber");
    }

    private static void applyModifiers(ServerPlayerEntity player) {
        set(player.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE), DAMAGE_MOD,
                "Smooth Classes Radiant Burst Damage", 0.20D);
        set(player.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_SPEED), SPEED_MOD,
                "Smooth Classes Radiant Burst Speed", 0.15D);
        set(player.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED), MOVE_MOD,
                "Smooth Classes Radiant Burst Movement", 0.10D);
    }

    private static void removeModifiers(ServerPlayerEntity player) {
        remove(player.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE), DAMAGE_MOD);
        remove(player.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_SPEED), SPEED_MOD);
        remove(player.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED), MOVE_MOD);
    }

    private static void set(EntityAttributeInstance attribute, UUID id, String name, double value) {
        if (attribute == null) return;
        EntityAttributeModifier existing = attribute.getModifier(id);
        if (existing != null && Math.abs(existing.getValue() - value) < 1.0E-6D) return;
        if (existing != null) attribute.removeModifier(id);
        attribute.addTemporaryModifier(new EntityAttributeModifier(id, name, value, EntityAttributeModifier.Operation.MULTIPLY_TOTAL));
    }

    private static void remove(EntityAttributeInstance attribute, UUID id) {
        if (attribute != null) attribute.removeModifier(id);
    }

    private static void burstFx(ServerPlayerEntity player) {
        var world = player.getServerWorld();
        world.spawnParticles(ParticleTypes.FLASH, player.getX(), player.getBodyY(0.55D), player.getZ(), 1, 0, 0, 0, 0);
        world.spawnParticles(ParticleTypes.END_ROD, player.getX(), player.getBodyY(0.55D), player.getZ(),
                34, 0.75D, 0.95D, 0.75D, 0.12D);
        world.playSound(null, player.getBlockPos(), SoundEvents.ITEM_TOTEM_USE, SoundCategory.PLAYERS, 0.65F, 1.45F);
    }

    private static int seconds(long ticks) { return (int) Math.ceil(Math.max(0L, ticks) / 20.0D); }
}
