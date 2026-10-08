package org.marj4n.smooth_classes.origin;

import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameRules;
import org.marj4n.smooth_classes.network.SmoothClassesNetworking;

import java.util.UUID;

/**
 * 1.20.1 backport of the Nycto Dark Form rules used by Smooth Classes Man-Bat.
 * Armor is deliberately NOT removed; that is Smooth Classes' sole equipment exception.
 */
public final class VampireManBatRuntime {
    public static final float WIDTH = 0.80F;
    public static final float HEIGHT = 2.75F;
    public static final float CROUCH_HEIGHT = 1.80F;
    public static final float DAMAGE_MULTIPLIER = 0.68F;

    private static final UUID ARMOR = UUID.fromString("e8e554d2-8c5b-4e58-bd18-86c6f138b6be");
    private static final UUID TOUGHNESS = UUID.fromString("8f2ec85e-c2ac-4b43-b33b-7938fcfa9db2");
    private static final UUID DAMAGE = UUID.fromString("96dc8acc-d2f7-4df2-b8a7-e0cf70a7f094");
    private static final UUID ATTACK_SPEED = UUID.fromString("a0bd1fd8-807f-4f5e-a96c-41706ed377f9");
    private static final UUID KNOCKBACK = UUID.fromString("48e487c8-f1ec-4ef9-ad53-45041f12730d");

    private VampireManBatRuntime() {}

    public static boolean active(ServerPlayerEntity player) {
        OriginState state = OriginRuntime.state(player);
        return state.origin() == OriginType.VAMPIRE && state.hasFlag("vampire.form.man_bat");
    }

    public static void onEnter(ServerPlayerEntity player, OriginState state) {
        long now = player.getWorld().getTime();
        state.longProgress("vampire.man_bat_next_drain", now + 300L);
        state.longProgress("vampire.man_bat_jump_ready_at", now);
        if (!player.isCreative() && !player.isSpectator()) {
            // Dark Form is wing-assisted jumping/gliding, not creative flight.
            player.getAbilities().flying = false;
            player.getAbilities().allowFlying = false;
            player.sendAbilitiesUpdate();
        }
        applyAttributes(player);
    }

    public static void onExit(ServerPlayerEntity player, OriginState state) {
        state.longProgress("vampire.man_bat_ready_at", player.getWorld().getTime() + 200L);
        state.longProgress("vampire.man_bat_next_drain", 0L);
        removeAttributes(player);
        if (!player.isCreative() && !player.isSpectator()) {
            player.getAbilities().flying = false;
            player.getAbilities().allowFlying = false;
            player.sendAbilitiesUpdate();
        }
    }

    public static void tick(ServerPlayerEntity player, OriginState state) {
        if (!state.hasFlag("vampire.form.man_bat")) {
            removeAttributes(player);
            return;
        }

        applyAttributes(player);
        long now = player.getWorld().getTime();

        if (!player.isCreative() && !player.isSpectator()) {
            long nextDrain = state.longProgress("vampire.man_bat_next_drain");
            if (nextDrain <= 0L) {
                state.longProgress("vampire.man_bat_next_drain", now + 300L);
            } else if (now >= nextDrain) {
                state.blood(state.blood() - 1);
                state.longProgress("vampire.man_bat_next_drain", now + 300L);
                SmoothClassesNetworking.sendOriginState(player);
            }
            if (state.blood() <= 0) {
                state.unflag("vampire.form.man_bat");
                onExit(player, state);
                player.calculateDimensions();
                SmoothClassesNetworking.sendOriginState(player);
                return;
            }
        }

        // Dark Form regenerates very aggressively while it still has blood.
        if (state.blood() > 0 && player.getHealth() < player.getMaxHealth()
                && player.getWorld().getGameRules().getBoolean(GameRules.NATURAL_REGENERATION)
                && player.age % 10 == 0) {
            player.heal(1.0F);
        }

        // Wing-assisted glide: forward momentum like a restrained Elytra glide, but slower
        // and still compatible with the Man-Bat air-jump. Sneak deliberately cancels lift.
        if (!player.isOnGround() && !player.isSneaking()) {
            Vec3d velocity = player.getVelocity();
            Vec3d look = player.getRotationVec(1.0F);
            double horizontalLook = Math.sqrt(look.x * look.x + look.z * look.z);
            double horizontalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);

            double vx = velocity.x;
            double vz = velocity.z;
            if (horizontalLook > 1.0E-4D) {
                // Target ~0.42 b/t: clearly faster than falling, intentionally below Elytra travel.
                double target = 0.42D;
                double desiredX = look.x / horizontalLook * target;
                double desiredZ = look.z / horizontalLook * target;
                double steer = horizontalSpeed < target ? 0.075D : 0.035D;
                vx += (desiredX - vx) * steer;
                vz += (desiredZ - vz) * steer;
            }

            double vy = velocity.y;
            if (vy < 0.0D) vy = Math.max(-0.13D, vy * 0.72D);
            player.setVelocity(vx, vy, vz);
            player.velocityModified = true;
            player.fallDistance = Math.min(player.fallDistance, 1.0F);
        }
    }

    /** Called from the client jump packet while airborne. One boosted flap per second. */
    public static void tryAirJump(ServerPlayerEntity player) {
        if (!active(player) || player.isOnGround() || player.isTouchingWater()) return;
        OriginState state = OriginRuntime.state(player);
        long now = player.getWorld().getTime();
        if (now < state.longProgress("vampire.man_bat_jump_ready_at")) return;

        Vec3d v = player.getVelocity();
        // Equivalent feel to Nycto's jump followed by x/z *1.3 and y *1.1.
        double y = Math.max(v.y, 0.42D) * 1.10D;
        player.setVelocity(v.x * 1.30D, y, v.z * 1.30D);
        player.velocityModified = true;
        player.fallDistance = 0.0F;
        state.longProgress("vampire.man_bat_jump_ready_at", now + 20L);
        player.getWorld().playSound(null, player.getBlockPos(), net.minecraft.sound.SoundEvents.ENTITY_PHANTOM_FLAP,
                net.minecraft.sound.SoundCategory.PLAYERS, 0.65F, 0.92F);
    }

    public static void applyAttributes(ServerPlayerEntity player) {
        set(player, EntityAttributes.GENERIC_ARMOR, ARMOR, "Man-Bat Armor", 20.0D, EntityAttributeModifier.Operation.ADDITION);
        set(player, EntityAttributes.GENERIC_ARMOR_TOUGHNESS, TOUGHNESS, "Man-Bat Toughness", 8.0D, EntityAttributeModifier.Operation.ADDITION);
        set(player, EntityAttributes.GENERIC_ATTACK_DAMAGE, DAMAGE, "Man-Bat Damage", 10.0D, EntityAttributeModifier.Operation.ADDITION);
        set(player, EntityAttributes.GENERIC_ATTACK_SPEED, ATTACK_SPEED, "Man-Bat Attack Speed", -0.50D, EntityAttributeModifier.Operation.MULTIPLY_TOTAL);
        set(player, EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, KNOCKBACK, "Man-Bat Knockback Resistance", 0.70D, EntityAttributeModifier.Operation.ADDITION);
    }

    public static void removeAttributes(ServerPlayerEntity player) {
        remove(player, EntityAttributes.GENERIC_ARMOR, ARMOR);
        remove(player, EntityAttributes.GENERIC_ARMOR_TOUGHNESS, TOUGHNESS);
        remove(player, EntityAttributes.GENERIC_ATTACK_DAMAGE, DAMAGE);
        remove(player, EntityAttributes.GENERIC_ATTACK_SPEED, ATTACK_SPEED);
        remove(player, EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, KNOCKBACK);
    }

    private static void set(ServerPlayerEntity player, net.minecraft.entity.attribute.EntityAttribute attribute,
                            UUID id, String name, double amount, EntityAttributeModifier.Operation op) {
        EntityAttributeInstance instance = player.getAttributeInstance(attribute);
        if (instance == null) return;
        instance.removeModifier(id);
        instance.addTemporaryModifier(new EntityAttributeModifier(id, name, amount, op));
    }

    private static void remove(ServerPlayerEntity player, net.minecraft.entity.attribute.EntityAttribute attribute, UUID id) {
        EntityAttributeInstance instance = player.getAttributeInstance(attribute);
        if (instance != null) instance.removeModifier(id);
    }
}
