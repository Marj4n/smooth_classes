package org.marj4n.smooth_classes.origin;

import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.GameRules;
import org.marj4n.smooth_classes.network.SmoothClassesNetworking;

import java.util.UUID;

/**
 * 1.20.1 backport of the Nycto Dark Form rules used by Smooth Classes Man-Bat.
 * Armor is deliberately NOT removed; that is Smooth Classes' sole equipment exception.
 */
public final class VampireManBatRuntime {
    public static final long MAX_DURATION_TICKS = 60L * 20L;
    public static final long COOLDOWN_TICKS = 30L * 20L;
    public static final long LORD_COOLDOWN_TICKS = 15L * 20L;
    private static final float HEAVY_FLY_SPEED = 0.022F;
    private static final float LORD_FLY_SPEED = HEAVY_FLY_SPEED * 2.0F;
    public static final float WIDTH = 0.80F;
    // R14 VSB mesh reaches Y=34/16 blocks from its feet (head at 34px).
    // One extra pixel of clearance keeps rotating head/ears inside the hitbox.
    // Large animated wings remain cosmetic and are not part of collision.
    public static final float HEIGHT = 2.1875F;         // 35/16 blocks
    public static final float CROUCH_HEIGHT = 1.9375F;  // 31/16; head shifts -4px
    public static final float EYE_HEIGHT = 1.875F;      // head/eye at approx 30px
    public static final float CROUCH_EYE_HEIGHT = 1.625F;
    public static final float DAMAGE_MULTIPLIER = 0.68F;

    private static final UUID ARMOR = UUID.fromString("e8e554d2-8c5b-4e58-bd18-86c6f138b6be");
    private static final UUID TOUGHNESS = UUID.fromString("8f2ec85e-c2ac-4b43-b33b-7938fcfa9db2");
    private static final UUID DAMAGE = UUID.fromString("96dc8acc-d2f7-4df2-b8a7-e0cf70a7f094");
    private static final UUID ATTACK_SPEED = UUID.fromString("a0bd1fd8-807f-4f5e-a96c-41706ed377f9");
    private static final UUID KNOCKBACK = UUID.fromString("48e487c8-f1ec-4ef9-ad53-45041f12730d");
    private static final UUID LORD_ATTACK = UUID.fromString("d6d4e960-0361-4300-a4bb-6c3896432d24");
    private static final UUID LORD_MOVEMENT = UUID.fromString("2dbed909-aea4-4964-9e82-117b97b69be9");

    /** Lord upgrades apply exclusively to Man-Bat, never the tiny Bat or human vampire. */
    public static boolean lord(ServerPlayerEntity player) {
        return OriginSkillRuntime.unlocked(player, OriginType.VAMPIRE, "final");
    }

    private static float flySpeed(ServerPlayerEntity player) {
        return lord(player) ? LORD_FLY_SPEED : HEAVY_FLY_SPEED;
    }

    private VampireManBatRuntime() {}

    public static boolean active(ServerPlayerEntity player) {
        OriginState state = OriginRuntime.state(player);
        return state.origin() == OriginType.VAMPIRE && state.hasFlag("vampire.form.man_bat");
    }

    public static void onEnter(ServerPlayerEntity player, OriginState state) {
        // Only Lord is immune to sunlight burning. Regular Man-Bat inherits
        // the ordinary Vampire exposure and fire instead of resetting it here.
        if (lord(player)) {
            if (state.sunExposure() > 0) player.extinguish();
            state.sunExposure(0);
        }
        long now = player.getWorld().getTime();
        state.longProgress("vampire.man_bat_next_drain", now + 300L);
        state.longProgress("vampire.man_bat_end_at", now + MAX_DURATION_TICKS);
        state.longProgress("vampire.man_bat_jump_ready_at", now);
        if (!player.isSpectator()) {
            // Apply heavy flight speed in Creative too; test worlds must match Survival.
            // Preserve Creative's existing flight state and restore the prior speed on exit.
            state.longProgress("vampire.man_bat_previous_fly_speed", Math.round(player.getAbilities().getFlySpeed() * 100000F));
            player.getAbilities().allowFlying = true;
            if (!player.isCreative()) player.getAbilities().flying = false;
            player.getAbilities().setFlySpeed(flySpeed(player));
            player.fallDistance = 0F;
            player.sendAbilitiesUpdate();
        }
        applyAttributes(player);
    }

    public static void onExit(ServerPlayerEntity player, OriginState state) {
        state.longProgress("vampire.man_bat_ready_at", player.getWorld().getTime()
                + (lord(player) ? LORD_COOLDOWN_TICKS : COOLDOWN_TICKS));
        state.longProgress("vampire.man_bat_end_at", 0L);
        state.longProgress("vampire.man_bat_next_drain", 0L);
        removeAttributes(player);
        if (!player.isOnGround() && !player.isCreative() && !player.isSpectator()) {
            // Auto timeout must not kill the player by dropping them out of flight.
            player.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                    net.minecraft.entity.effect.StatusEffects.SLOW_FALLING, 20 * 10, 0, false, false, false));
        }
        if (!player.isSpectator()) {
            if (!player.isCreative()) {
                player.getAbilities().flying = false;
                player.getAbilities().allowFlying = false;
            }
            long savedSpeed = state.longProgress("vampire.man_bat_previous_fly_speed");
            player.getAbilities().setFlySpeed(savedSpeed > 0L ? savedSpeed / 100000F : 0.05F);
            state.longProgress("vampire.man_bat_previous_fly_speed", 0L);
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
        // Timer applies in every game mode, preventing permanently stuck transformations.
        long endsAt = state.longProgress("vampire.man_bat_end_at");
        if (endsAt <= 0L) {
            state.longProgress("vampire.man_bat_end_at", now + MAX_DURATION_TICKS);
        } else if (now >= endsAt) {
            endForm(player, state);
            return;
        }

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
                endForm(player, state);
                return;
            }
        }

        // Dark Form regenerates very aggressively while it still has blood.
        if (state.blood() > 0 && player.getHealth() < player.getMaxHealth()
                && player.getWorld().getGameRules().getBoolean(GameRules.NATURAL_REGENERATION)
                && player.age % 10 == 0) {
            player.heal(1.0F);
        }

        // Flight itself is owned by vanilla PlayerAbilities, matching the tiny Bat's
        // double-space controls. No glide/push physics: movement stays player-controlled.
        player.fallDistance = 0.0F;
        if (!player.isSpectator()) {
            float targetSpeed = flySpeed(player);
            boolean update = !player.getAbilities().allowFlying
                    || Math.abs(player.getAbilities().getFlySpeed() - targetSpeed) > 0.0001F;
            player.getAbilities().allowFlying = true;
            player.getAbilities().setFlySpeed(targetSpeed);
            if (update) player.sendAbilitiesUpdate();
        }
    }

    private static void endForm(ServerPlayerEntity player, OriginState state) {
        state.unflag("vampire.form.man_bat");
        onExit(player, state);
        player.calculateDimensions();
        VampireTransformationEffects.play(player, false);
        SmoothClassesNetworking.sendOriginState(player);
    }

    /** Legacy packet compatibility; normal double-space flight is handled by Minecraft. */
    public static void tryAirJump(ServerPlayerEntity player) {
        // Intentionally do nothing: a single held jump must not auto-toggle flight.
    }

    public static void applyAttributes(ServerPlayerEntity player) {
        set(player, EntityAttributes.GENERIC_ARMOR, ARMOR, "Man-Bat Armor", 20.0D, EntityAttributeModifier.Operation.ADDITION);
        set(player, EntityAttributes.GENERIC_ARMOR_TOUGHNESS, TOUGHNESS, "Man-Bat Toughness", 8.0D, EntityAttributeModifier.Operation.ADDITION);
        set(player, EntityAttributes.GENERIC_ATTACK_DAMAGE, DAMAGE, "Man-Bat Damage", 10.0D, EntityAttributeModifier.Operation.ADDITION);
        // Only regular Man-Bat attacks feel heavy. Vampire Lord restores normal
        // weapon-based attack speed (including heavy-weapon penalties, if any).
        // Remove the exact same UUID so a newly earned Lord evolution also
        // clears an active Man-Bat penalty on the next server tick.
        if (lord(player)) {
            remove(player, EntityAttributes.GENERIC_ATTACK_SPEED, ATTACK_SPEED);
        } else {
            set(player, EntityAttributes.GENERIC_ATTACK_SPEED, ATTACK_SPEED, "Man-Bat Attack Speed", -0.50D, EntityAttributeModifier.Operation.MULTIPLY_TOTAL);
        }
        set(player, EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, KNOCKBACK, "Man-Bat Knockback Resistance", 0.70D, EntityAttributeModifier.Operation.ADDITION);
        // 2x final damage AFTER weapon/base/normal Man-Bat modifiers, including heavy swords.
        // 2x ground movement speed; flying speed is synchronized separately above.
        if (lord(player)) {
            set(player, EntityAttributes.GENERIC_ATTACK_DAMAGE, LORD_ATTACK, "Vampire Lord Man-Bat Damage x2",
                    1.0D, EntityAttributeModifier.Operation.MULTIPLY_TOTAL);
            set(player, EntityAttributes.GENERIC_MOVEMENT_SPEED, LORD_MOVEMENT, "Vampire Lord Man-Bat Speed x2",
                    1.0D, EntityAttributeModifier.Operation.MULTIPLY_TOTAL);
        } else {
            remove(player, EntityAttributes.GENERIC_ATTACK_DAMAGE, LORD_ATTACK);
            remove(player, EntityAttributes.GENERIC_MOVEMENT_SPEED, LORD_MOVEMENT);
        }
    }

    public static void removeAttributes(ServerPlayerEntity player) {
        remove(player, EntityAttributes.GENERIC_ARMOR, ARMOR);
        remove(player, EntityAttributes.GENERIC_ARMOR_TOUGHNESS, TOUGHNESS);
        remove(player, EntityAttributes.GENERIC_ATTACK_DAMAGE, DAMAGE);
        remove(player, EntityAttributes.GENERIC_ATTACK_SPEED, ATTACK_SPEED);
        remove(player, EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, KNOCKBACK);
        remove(player, EntityAttributes.GENERIC_ATTACK_DAMAGE, LORD_ATTACK);
        remove(player, EntityAttributes.GENERIC_MOVEMENT_SPEED, LORD_MOVEMENT);
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
