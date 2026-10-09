package org.marj4n.smooth_classes.origin;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.marj4n.smooth_classes.integration.PuffishSkillsIntegration;
import org.marj4n.smooth_classes.network.SmoothClassesNetworking;
import org.marj4n.smooth_classes.registry.SmoothItems;

import java.util.UUID;

/**
 * First playable Origin runtime.
 *
 * This V1 deliberately focuses on selection, persistent biology and the visible
 * body-stat differences.  Large transformation/model systems are represented by
 * their Origin tree nodes and can be layered onto this runtime without changing
 * the save format.
 */
public final class OriginRuntime {
    private static final UUID HEALTH_MODIFIER = UUID.fromString("b2e7342c-3c2b-4c72-8f8a-3706118b0a11");
    private static final UUID SPEED_MODIFIER = UUID.fromString("54c3f7bd-c230-4e62-9b3e-6f2a85220631");
    private static final UUID ARMOR_MODIFIER = UUID.fromString("7f8d2218-2d6d-4a76-8e68-bd01c4da6c81");
    private static final UUID ATTACK_MODIFIER = UUID.fromString("de04d8a8-6348-43e7-bc6e-9decb459df81");
    private static final UUID KNOCKBACK_MODIFIER = UUID.fromString("b0c239e2-7df8-478b-aa2f-99308345ef51");

    private OriginRuntime() {}

    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> server.execute(() -> {
            ServerPlayerEntity player = handler.player;
            ensureSelectedCategory(player);
            OriginProgressUnlocks.sync(player, state(player));
            SmoothClassesNetworking.sendOriginState(player);
            if (!state(player).hasOrigin()) {
                if (!player.isCreative()) player.setInvulnerable(true);
                SmoothClassesNetworking.openOriginSelection(player);
            }
        }));

        // Respawn creates a new PlayerEntity with the same UUID. Re-apply Origin
        // biology on the new entity and broadcast its cleared form to all clients.
        ServerPlayerEvents.AFTER_RESPAWN.register((previous, respawned, alive) -> {
            OriginState restored = state(respawned);
            if (!alive) VampireFormRespawnRuntime.clear(restored);
            if (restored.origin() != null) {
                HomunculusAccessories.updateAttributes(respawned);
                applyAttributes(respawned, restored.origin());
                respawned.calculateDimensions();
                ensureSelectedCategory(respawned);
            }
            SmoothClassesNetworking.sendOriginState(respawned);
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                tick(player);
            }
        });

        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient || !(player instanceof ServerPlayerEntity serverPlayer)) return ActionResult.PASS;
            OriginState state = state(serverPlayer);
            if (state.origin() != OriginType.VAMPIRE || !(entity instanceof LivingEntity living)) return ActionResult.PASS;
            if (!living.isAlive() || living == serverPlayer) return ActionResult.PASS;
            boolean transformed = state.hasFlag("vampire.form.bat") || state.hasFlag("vampire.form.man_bat");
            boolean biteable = VampireBloodReserve.isFeedable(living);
            if (transformed || !biteable) return ActionResult.PASS;

            // Direct bottle collection stays on sneak-right-click. Normal feeding is hold-to-feed
            // and is handled by VampireFeedingRuntime, matching the Vampirism interaction loop.
            ItemStack held = serverPlayer.getStackInHand(hand);
            boolean bottleDirect = serverPlayer.isSneaking() && held.isOf(Items.GLASS_BOTTLE)
                    && OriginSkillRuntime.unlocked(serverPlayer, OriginType.VAMPIRE, "blood_flask");
            if (!bottleDirect) {
                // Empty-hand use is reserved for hold-to-feed so villagers/animals do not
                // open their normal interaction UI while the fang reticle is active.
                if (held.isEmpty()) return ActionResult.SUCCESS;
                return ActionResult.PASS;
            }

            if (!VampireBloodReserve.consume(living, serverPlayer)) {
                serverPlayer.sendMessage(Text.literal("This creature needs time to replenish its blood."), true);
                return ActionResult.SUCCESS;
            }
            if (!serverPlayer.isCreative()) held.decrement(1);
            ItemStack bottle = createBloodFlask();
            if (!serverPlayer.getInventory().insertStack(bottle)) serverPlayer.dropItem(bottle, false);
            state.lastFeedTick(serverPlayer.age);
            OriginProgressTracker.onVampireFeed(serverPlayer, living, 30);
            serverPlayer.getWorld().playSound(null, serverPlayer.getBlockPos(),
                    org.marj4n.smooth_classes.registry.SmoothSounds.get("origin_vampire_bite"),
                    net.minecraft.sound.SoundCategory.PLAYERS, 0.8F, 1.0F);
            SmoothClassesNetworking.sendOriginState(serverPlayer);
            return ActionResult.SUCCESS;
        });

        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getStackInHand(hand);
            if (world.isClient || !(player instanceof ServerPlayerEntity serverPlayer)) {
                return TypedActionResult.pass(stack);
            }

            OriginState originState = state(serverPlayer);
            OriginType origin = originState.origin();

            if (origin == OriginType.VAMPIRE
                    && originState.hasFlag("vampire.form.bat")
                    && !stack.isEmpty()) {
                // The tiny Bat still has no usable hands. Man-Bat uses the
                // source avatar's actual claw/item pivots and may use items.
                return TypedActionResult.fail(stack);
            }

            if (origin == OriginType.VAMPIRE && OriginSkillRuntime.unlocked(serverPlayer, OriginType.VAMPIRE, "blood_flask")) {
                if (isBloodFlask(stack)) {
                    originState.blood(Math.min(vampireBloodCapacity(serverPlayer), originState.blood() + 30));
                    if (!serverPlayer.isCreative()) {
                        stack.decrement(1);
                        ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
                        if (!serverPlayer.getInventory().insertStack(bottle)) serverPlayer.dropItem(bottle, false);
                    }
                    serverPlayer.heal(2.0F);
                    SmoothClassesNetworking.sendOriginState(serverPlayer);
                    serverPlayer.sendMessage(Text.literal("Blood " + originState.blood() + "/" + vampireBloodCapacity(serverPlayer)), true);
                    return TypedActionResult.success(stack, false);
                }
                if (serverPlayer.isSneaking() && stack.isOf(Items.GLASS_BOTTLE) && originState.blood() >= 30) {
                    originState.blood(originState.blood() - 30);
                    if (!serverPlayer.isCreative()) stack.decrement(1);
                    ItemStack flask = createBloodFlask();
                    if (!serverPlayer.getInventory().insertStack(flask)) serverPlayer.dropItem(flask, false);
                    SmoothClassesNetworking.sendOriginState(serverPlayer);
                    return TypedActionResult.success(stack, false);
                }
            }

            if (!stack.isFood()) return TypedActionResult.pass(stack);

            OriginProgressTracker.onFoodUse(serverPlayer, stack);
            if (origin == OriginType.VAMPIRE) {
                if (!serverPlayer.isCreative()) stack.decrement(1);
                originState.blood(originState.blood() - 5);
                serverPlayer.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 20 * 6, 0, false, false, true));
                serverPlayer.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 20 * 3, 0, false, false, true));
                serverPlayer.sendMessage(Text.literal("Your body rejects mortal food."), true);
                return TypedActionResult.success(stack, false);
            }
            return TypedActionResult.pass(stack);
        });
    }

    public static OriginState state(ServerPlayerEntity player) {
        return ((OriginDataHolder) player).smooth_classes$getOriginState();
    }

    public static OriginState state(net.minecraft.entity.player.PlayerEntity player) {
        return ((OriginDataHolder) player).smooth_classes$getOriginState();
    }

    public static void setOrigin(ServerPlayerEntity player, OriginType origin) {
        if (origin != OriginType.VAMPIRE) VampireGraveSpawn.cancelIntro(player, state(player));
        if (state(player).hasFlag("vampire.form.bat")) {
            state(player).unflag("vampire.form.bat");
            VampireBatHealthRuntime.exit(player, state(player));
        }
        // The inner equipment belongs only to Homunculus; return it safely if changing Origins.
        if (origin != OriginType.HOMUNCULUS) HomunculusAccessories.returnItems(player);
        lockOriginCategories(player);
        state(player).setOrigin(origin);
        if (!player.isCreative()) player.setInvulnerable(false);
        if (origin != OriginType.ANGEL) setAngelFlight(player, false);
        unlockOriginCategory(player, origin);
        OriginAdvancementNotices.onOriginSelected(player, origin);
        applyAttributes(player, origin);
        player.calculateDimensions();
        if (origin == OriginType.VAMPIRE) VampireGraveSpawn.awakenOnce(player, state(player));
        SmoothClassesNetworking.sendOriginState(player);
        player.sendMessage(Text.literal("Origin Chosen: " + origin.displayName()), false);
        player.sendMessage(Text.literal(origin.subtitle()), true);
    }

    public static void clearOrigin(ServerPlayerEntity player) {
        VampireGraveSpawn.cancelIntro(player, state(player));
        if (state(player).hasFlag("vampire.form.bat")) {
            state(player).unflag("vampire.form.bat");
            VampireBatHealthRuntime.exit(player, state(player));
        }
        HomunculusAccessories.returnItems(player);
        lockOriginCategories(player);
        state(player).clear();
        if (!player.isCreative()) player.setInvulnerable(true);
        removeModifiers(player);
        setAngelFlight(player, false);
        player.calculateDimensions();
        SmoothClassesNetworking.sendOriginState(player);
    }

    public static void openSelection(ServerPlayerEntity player) {
        SmoothClassesNetworking.openOriginSelection(player);
    }

    public static void onKilledOther(ServerPlayerEntity player, LivingEntity victim) {
        OriginState state = state(player);
        OriginProgressTracker.onKill(player, victim);
        if (state.origin() == OriginType.UNDEAD) {
            int gain = victim.getMaxHealth() >= 100 ? 30 : victim.getMaxHealth() >= 40 ? 15 : victim.getMaxHealth() >= 20 ? 8 : 5;
            state.soul(state.soul() + gain);
            state.addProgress("undead.soul_harvested", gain);
            if (player.age % 5 == 0) player.sendMessage(Text.literal("Soul Hunger " + state.soul() + "%"), true);
        }
        SmoothClassesNetworking.sendOriginState(player);
    }

    /** Damage gates that are genuinely biological rather than class defenses. */
    public static boolean allowIncomingDamage(ServerPlayerEntity player, DamageSource source, float amount) {
        OriginState state = state(player);
        OriginType origin = state.origin();
        if (origin == null) return true;

        // Backup protection for newly connected/respawned Vampires whose hidden
        // vanilla food value may still be 0 before the first Origin tick.
        // Blood low/empty penalties are handled separately by tickVampire().
        if (origin == OriginType.VAMPIRE
                && source.isOf(net.minecraft.entity.damage.DamageTypes.STARVE)) {
            // Vanilla hunger never drives Vampire starvation. Only an empty
            // Blood reservoir allows a starvation damage source to apply.
            return state.blood() <= 0;
        }

        if (origin == OriginType.DEMON && source.isIn(DamageTypeTags.IS_FIRE)) {
            if (player.isInLava() && state.lavaGraceTicks() > 0) return false;
        }
        if (origin == OriginType.SLIME && source.isIn(DamageTypeTags.IS_FALL)) {
            return false;
        }
        return true;
    }

    private static void tick(ServerPlayerEntity player) {
        OriginState state = state(player);
        OriginType origin = state.origin();
        if (origin == null) return;

        if (player.age % 10 == 0) {
            HomunculusAccessories.updateAttributes(player);
            applyAttributes(player, origin);
        }

        OriginSkillRuntime.tick(player, state);
        if (origin == OriginType.VAMPIRE) VampireFeedingRuntime.tick(player);

        switch (origin) {
            case VAMPIRE -> tickVampire(player, state);
            case MERMAID -> tickMermaid(player, state);
            case DEMON -> tickDemon(player, state);
            case ANGEL -> tickAngel(player, state);
            case SLIME -> tickSlime(player, state);
            case VOID -> tickVoid(player, state);
            case UNDEAD -> tickUndead(player, state);
            case SPRIGGAN -> tickSpriggan(player, state);
            default -> { }
        }

        if (player.age % 20 == 0) {
            OriginProgressTracker.tickSecond(player, state);
            OriginProgressUnlocks.sync(player, state);
            SmoothClassesNetworking.sendOriginState(player);
        }
    }

    private static void tickVampire(ServerPlayerEntity player, OriginState state) {
        // Complete the one-time coffin awakening BEFORE normal sunlight checks.
        VampireGraveSpawn.tickIntro(player, state);
        // Blood is the Vampire's nutrition; vanilla HungerManager.update is
        // suppressed by VampireHungerManagerMixin, including vanilla passive
        // food/saturation healing and vanilla starvation. Keep the hidden value
        // full only for save/packet compatibility when returning to other Origins.
        if (player.getHungerManager().getFoodLevel() != 20) {
            player.getHungerManager().setFoodLevel(20);
        }
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 240, 0, false, false, false));

        long coffinUntil = state.longProgress("vampire.coffin_rest_until");
        if (coffinUntil > 0L) {
            long now = player.getWorld().getTime();
            if (now < coffinUntil) {
                player.setVelocity(0.0D, 0.0D, 0.0D);
                player.velocityModified = true;
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 10, 0, false, false, false));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 10, 10, false, false, false));
            } else {
                state.longProgress("vampire.coffin_rest_until", 0L);
                long day = player.getWorld().getTimeOfDay();
                long nextNight = day - (day % 24000L) + 13000L;
                if (nextNight <= day) nextNight += 24000L;
                player.getServerWorld().setTimeOfDay(nextNight);
                player.heal(4.0F);
                state.sunExposure(0);
                player.sendMessage(Text.literal("Night falls. You awaken refreshed."), true);
            }
        }
        boolean batForm = state.hasFlag("vampire.form.bat");
        boolean manBatForm = state.hasFlag("vampire.form.man_bat");

        // Keep the authoritative bounding box compact for the entire Bat Form.
        // A recalculation every few ticks prevents stale humanoid dimensions after
        // pose/flight changes or client/server state resyncs.
        if (batForm && player.age % 5 == 0) {
            player.calculateDimensions();
        }

        // Creative/spectator are debug/build modes: never run Vampire sunlight punishment,
        // never accumulate exposure, and never leave fire behind when switching modes.
        if (player.isCreative() || player.isSpectator()) {
            state.sunExposure(0);
            player.extinguish();
            return;
        }

        boolean exposed = !batForm && player.getWorld().isDay() && player.getWorld().isSkyVisible(player.getBlockPos());
        boolean lord = OriginSkillRuntime.unlocked(player, OriginType.VAMPIRE, "final");
        if (batForm) {
            // Travel-form protection: daylight does not burn the vampire while transformed.
            state.sunExposure(0);
            player.extinguish();
        } else if (lord) {
            // Clear existing sunlight fire once when evolution takes effect.
            // Do not extinguish ordinary lava/fire repeatedly: Lord is not fireproof.
            if (state.sunExposure() > 0) player.extinguish();
            state.sunExposure(0);
        } else if (exposed) {
            if (player.age % 5 == 0) state.sunExposure(state.sunExposure() + 1);
        } else if (player.age % 4 == 0) {
            state.sunExposure(state.sunExposure() - 1);
        }

        if (lord) {
            // Only Vampire Lord has royal sunlight tolerance in humanoid/Man-Bat.
            // Ordinary Man-Bat follows the same accumulating burn as normal Vampire.
            // Tiny Bat retains its existing travel-form protection.
            if (exposed) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 30, 0, false, false, true));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 30, 0, false, false, true));
            }
        } else {
            int sun = state.sunExposure();
            boolean tolerant = OriginSkillRuntime.unlocked(player, OriginType.VAMPIRE, "sun_tolerance");
            int slowAt = tolerant ? 70 : 50;
            int weakAt = tolerant ? 90 : 75;
            if (sun >= slowAt) player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 30, 0, false, false, true));
            if (sun >= weakAt) player.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 30, 0, false, false, true));
            if (sun >= 100 && player.age % (tolerant ? 40 : 20) == 0) player.setOnFireFor(1);
        }

        if (player.age % 200 == 0) {
            state.blood(state.blood() - 1);
            if (state.blood() == 0) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 220, 0, false, false, true));
            }
        }

        // Blood is the starvation source as well as the recovery source.
        // No hidden vanilla hunger timer may drain HP. Apply only when Blood
        // is truly empty, at vanilla's 80-tick starvation cadence. Like vanilla
        // survival starvation, Easy won't reduce health below 5 hearts and
        // Normal won't reduce it below half a heart; Hard can be fatal.
        if (state.blood() == 0 && player.age % 80 == 0
                && VampireBloodMetabolism.canStarve(state.blood(),
                        player.getWorld().getDifficulty(), player.getHealth())) {
            player.damage(player.getDamageSources().starve(), 1.0F);
        }

        // Base Vampire recovery uses Blood rather than the invisible vanilla
        // food/saturation bar. Man-Bat has its own faster Blood-powered recovery;
        // don't add another heal on top of its existing regeneration.
        if (!manBatForm && state.blood() > 0
                && player.getWorld().getGameRules().getBoolean(net.minecraft.world.GameRules.NATURAL_REGENERATION)
                && player.getHealth() < player.getMaxHealth() && player.age % 80 == 0) {
            player.heal(1.0F);
        }
    }

    private static void tickMermaid(ServerPlayerEntity player, OriginState state) {
        boolean wet = player.isTouchingWaterOrRain() || player.isSubmergedInWater();
        if (wet) {
            state.wetnessTicks(90 * 20);
            player.setAir(player.getMaxAir());
        } else if (!player.isCreative()) {
            state.wetnessTicks(state.wetnessTicks() - 1);
            int wetness = state.wetnessTicks();
            if (wetness <= 60 * 20) {
                // An unevolved Mermaid cannot regenerate normally while drying out.
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 30, 0, false, false, true));
            }
            if (wetness <= 30 * 20) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 30, 1, false, false, true));
            }
            if (wetness == 0 && player.age % 20 == 0
                    && !OriginSkillRuntime.unlocked(player, OriginType.MERMAID, "landwalker")) {
                player.damage(player.getDamageSources().generic(), 1.0F);
            }
            if (player.age % 40 == 0 && wetness <= 60 * 20) {
                player.sendMessage(Text.literal("Wetness: " + (wetness / 20) + "s"), true);
            }
        }
    }

    private static void tickDemon(ServerPlayerEntity player, OriginState state) {
        if (player.isInLava()) {
            state.lavaGraceTicks(state.lavaGraceTicks() - 1);
        } else {
            state.lavaGraceTicks(5 * 20);
        }
        if (player.isTouchingWaterOrRain()) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 30, 0, false, false, true));
        }
    }

    private static void tickSlime(ServerPlayerEntity player, OriginState state) {
        // True slime body: no drowning, no fall damage, and movement resolves as hops/bounces.
        player.setAir(player.getMaxAir());
        if (state.hasFlag("slime.form.humanoid")) {
            state.unflag("slime.airborne");
            state.progress("slime.fall_strength", 0);
            return;
        }

        var velocity = player.getVelocity();
        double horizontal = velocity.x * velocity.x + velocity.z * velocity.z;

        if (!player.isOnGround()) {
            state.flag("slime.airborne");
            if (velocity.y < -0.18D) {
                int strength = Math.min(100, (int) Math.round(Math.abs(velocity.y) * 100.0D));
                if (strength > state.progress("slime.fall_strength")) state.progress("slime.fall_strength", strength);
            }
        } else if (state.hasFlag("slime.airborne")) {
            int strength = state.progress("slime.fall_strength");
            state.unflag("slime.airborne");
            state.progress("slime.fall_strength", 0);
            if (strength >= 24 && !player.isSneaking()) {
                double rebound = Math.min(0.95D, 0.34D + strength * 0.0055D);
                player.setVelocity(velocity.x * 0.88D, rebound, velocity.z * 0.88D);
                player.velocityModified = true;
                state.longProgress("slime.last_bounce_tick", player.getWorld().getTime());
                return;
            }
        }

        // Small automatic hop cadence while moving on land, close to a vanilla Slime's rhythm.
        if (player.isOnGround() && !player.isSneaking() && horizontal > 0.0008D && (player.age % 12) == 0) {
            double hop = state.hasFlag("slime.size.large") ? 0.38D : state.hasFlag("slime.size.small") ? 0.48D : 0.42D;
            player.setVelocity(velocity.x, hop, velocity.z);
            player.velocityModified = true;
        }
    }

    private static void tickAngel(ServerPlayerEntity player, OriginState state) {
        AngelIcarusRuntime.tick(player, state);
        if (player.isCreative() || player.isSpectator()) return;

        int stamina = state.wingStaminaTicks();
        if (player.getAbilities().flying) {
            stamina = Math.max(0, stamina - 1);
            if (stamina == 0) {
                player.getAbilities().flying = false;
                player.getAbilities().allowFlying = false;
                player.sendAbilitiesUpdate();
                player.sendMessage(Text.literal("Your wings are exhausted."), true);
            }
        } else if (player.isOnGround() && stamina < 6 * 20) {
            stamina = Math.min(6 * 20, stamina + 1);
        }
        state.wingStaminaTicks(stamina);

        boolean shouldAllow = stamina > 0;
        if (player.getAbilities().allowFlying != shouldAllow) {
            player.getAbilities().allowFlying = shouldAllow;
            player.sendAbilitiesUpdate();
        }
    }

    private static void tickVoid(ServerPlayerEntity player, OriginState state) {
        if (player.isTouchingWaterOrRain()) {
            if (player.age % 10 == 0) state.instability(state.instability() + (player.isSubmergedInWater() ? 3 : 1));
        } else if (player.age % 10 == 0) {
            state.instability(state.instability() - 1);
        }
        if (state.instability() >= 100) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 30, 0, false, false, true));
        }
    }

    private static void tickUndead(ServerPlayerEntity player, OriginState state) {
        player.setAir(player.getMaxAir());
        if (player.age % 200 == 0) {
            state.soul(state.soul() - 1);
            if (state.soul() == 0) player.damage(player.getDamageSources().generic(), 1.0F);
        }
    }

    private static void tickSpriggan(ServerPlayerEntity player, OriginState state) {
        if (player.isOnFire()) {
            // V1 preview: the full permanent branch-burning loop is represented by Living Mass;
            // actual tree absorption comes with the Worldroot interaction pass.
            if (player.age % (20 * 8) == 0 && state.livingMass() > 0) {
                state.livingMass(state.livingMass() - 1);
                player.sendMessage(Text.literal("A burning branch was lost. Living Mass: " + state.livingMass()), true);
                applyAttributes(player, OriginType.SPRIGGAN);
            }
        }
    }

    /** Apply the current vampire form health cap immediately during morph transitions. */
    static void refreshVampireAttributes(ServerPlayerEntity player) {
        if (state(player).origin() == OriginType.VAMPIRE) applyAttributes(player, OriginType.VAMPIRE);
    }

    private static void applyAttributes(ServerPlayerEntity player, OriginType origin) {
        OriginState state = state(player);
        World world = player.getWorld();
        boolean night = world.isNight();
        boolean fullMoon = night && world.getMoonPhase() == 0;
        boolean nether = world.getRegistryKey() == World.NETHER;
        boolean wet = player.isSubmergedInWater() || player.isTouchingWaterOrRain();

        double health = 0.0;
        double speed = 0.0;
        double armor = 0.0;
        double attack = 0.0;
        double knockback = 0.0;

        switch (origin) {
            case HUMAN -> { }
            case VAMPIRE -> {
                boolean lord = OriginSkillRuntime.unlocked(player, OriginType.VAMPIRE, "final");
                boolean bat = state.hasFlag("vampire.form.bat");
                boolean manBat = state.hasFlag("vampire.form.man_bat");
                health = lord ? -2.0 : -5.0;
                if (night) { speed = 0.12; attack = 0.08; }
                if (bat) {
                    health -= 8.0;
                    speed += 0.35;
                    attack -= 0.95;
                    armor -= 4.0;
                } else if (manBat) {
                    // Dark Form's exact combat modifiers live in VampireManBatRuntime.
                    // Do not stack the old percentage Man-Bat bonuses on top.
                }
            }
            case WEREWOLF -> {
                if (night) {
                    health = fullMoon ? 4.0 : 2.0;
                    speed = fullMoon ? 0.15 : 0.10;
                    attack = fullMoon ? 0.10 : 0.06;
                    armor = fullMoon ? 4.0 : 2.0;
                }
            }
            case MERMAID -> {
                boolean queen = OriginSkillRuntime.unlocked(player, OriginType.MERMAID, "final");
                health = queen ? -2.0 : -4.0;
                if (player.isSubmergedInWater()) {
                    speed = OriginSkillRuntime.unlocked(player, OriginType.MERMAID, "sea_kinship") ? 0.40 : 0.25;
                    armor = queen ? 6.0 : 4.0;
                }
            }
            case DEMON -> {
                if (nether) { speed = 0.08; attack = 0.08; armor = 1.5; }
                if (wet) speed -= 0.05;
            }
            case ANGEL -> { health = -4.0; speed = 0.05; knockback = -0.10; }
            case SLIME -> {
                boolean sovereign = OriginSkillRuntime.unlocked(player, OriginType.SLIME, "final");
                health = sovereign ? 0.0 : -4.0;
                speed = state.hasFlag("slime.size.small") ? 0.22 : state.hasFlag("slime.size.large") ? -0.12 : 0.05;
                armor = state.hasFlag("slime.size.large") ? 4.0 : 0.0;
                knockback = state.hasFlag("slime.size.large") ? 0.35 : -0.20;
            }
            case HOMUNCULUS -> health = -10.0;
            case VOID -> {
                health = -6.0;
                speed = state.instability() >= 75 ? 0.0 : 0.08;
            }
            case UNDEAD -> {
                int mass = state.boneMass();
                if (mass < 25) { health = -8.0; speed = 0.05; }
                else if (mass < 50) { health = -4.0; speed = 0.02; armor = 2; }
                else if (mass < 75) { health = 0.0; speed = -0.03; armor = 4; }
                else if (mass < 100) { health = 4.0; speed = -0.08; armor = 6; }
                else if (mass < 150) { health = 8.0; speed = -0.15; armor = 8; }
                else { health = 12.0; speed = -0.25; armor = 10; knockback = 0.40; }
            }
            case SPRIGGAN -> {
                int mass = state.livingMass();
                health = -4.0 + (mass * 2.0);
                armor = 2.0 + Math.min(8.0, mass * 0.8);
                speed = -0.05 - (mass * 0.023);
                knockback = Math.min(0.50, mass * 0.05);
            }
            case DOPPELGANGER -> { health = -2.0; speed = 0.02; }
        }

        setModifier(player, EntityAttributes.GENERIC_MAX_HEALTH, HEALTH_MODIFIER, "Smooth Classes Origin Health", health, EntityAttributeModifier.Operation.ADDITION);
        setModifier(player, EntityAttributes.GENERIC_MOVEMENT_SPEED, SPEED_MODIFIER, "Smooth Classes Origin Speed", speed, EntityAttributeModifier.Operation.MULTIPLY_TOTAL);
        setModifier(player, EntityAttributes.GENERIC_ARMOR, ARMOR_MODIFIER, "Smooth Classes Origin Armor", armor, EntityAttributeModifier.Operation.ADDITION);
        setModifier(player, EntityAttributes.GENERIC_ATTACK_DAMAGE, ATTACK_MODIFIER, "Smooth Classes Origin Attack", attack, EntityAttributeModifier.Operation.MULTIPLY_TOTAL);
        setModifier(player, EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, KNOCKBACK_MODIFIER, "Smooth Classes Origin Knockback", knockback, EntityAttributeModifier.Operation.ADDITION);

        if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
    }

    private static void setModifier(ServerPlayerEntity player, net.minecraft.entity.attribute.EntityAttribute attribute,
                                    UUID id, String name, double amount, EntityAttributeModifier.Operation operation) {
        EntityAttributeInstance instance = player.getAttributeInstance(attribute);
        if (instance == null) return;
        instance.removeModifier(id);
        if (Math.abs(amount) > 0.00001) {
            instance.addTemporaryModifier(new EntityAttributeModifier(id, name, amount, operation));
        }
    }

    private static void removeModifiers(ServerPlayerEntity player) {
        VampireManBatRuntime.removeAttributes(player);
        remove(player, EntityAttributes.GENERIC_MAX_HEALTH, HEALTH_MODIFIER);
        remove(player, EntityAttributes.GENERIC_MOVEMENT_SPEED, SPEED_MODIFIER);
        remove(player, EntityAttributes.GENERIC_ARMOR, ARMOR_MODIFIER);
        remove(player, EntityAttributes.GENERIC_ATTACK_DAMAGE, ATTACK_MODIFIER);
        remove(player, EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, KNOCKBACK_MODIFIER);
    }

    private static void remove(ServerPlayerEntity player, net.minecraft.entity.attribute.EntityAttribute attr, UUID id) {
        EntityAttributeInstance instance = player.getAttributeInstance(attr);
        if (instance != null) instance.removeModifier(id);
    }

    public static int vampireBloodCapacity(ServerPlayerEntity player) {
        if (OriginSkillRuntime.unlocked(player, OriginType.VAMPIRE, "final")) return 200;
        if (OriginSkillRuntime.unlocked(player, OriginType.VAMPIRE, "nobility")) return 150;
        return 100;
    }

    private static ItemStack createBloodFlask() {
        ItemStack stack = new ItemStack(SmoothItems.BLOOD_BOTTLE);
        stack.setCustomName(Text.literal("Blood Bottle"));
        return stack;
    }

    private static boolean isBloodFlask(ItemStack stack) {
        return stack.isOf(SmoothItems.BLOOD_BOTTLE);
    }

    private static void setAngelFlight(ServerPlayerEntity player, boolean allow) {
        if (player.isCreative() || player.isSpectator()) return;
        player.getAbilities().flying = false;
        player.getAbilities().allowFlying = allow;
        player.sendAbilitiesUpdate();
    }

    private static void ensureSelectedCategory(ServerPlayerEntity player) {
        OriginType origin = state(player).origin();
        if (origin != null) unlockOriginCategory(player, origin);
    }

    private static void unlockOriginCategory(ServerPlayerEntity player, OriginType origin) {
        PuffishSkillsIntegration.category(origin.categoryId()).ifPresent(category -> {
            if (!category.isUnlocked(player)) category.unlock(player);
            category.getSkill("origin_base").ifPresent(skill -> {
                if (skill.getState(player) != net.puffish.skillsmod.api.Skill.State.UNLOCKED) skill.unlock(player);
            });
        });
        PuffishSkillsIntegration.invalidateRuntimeCache(player);
    }

    private static void lockOriginCategories(ServerPlayerEntity player) {
        for (OriginType type : OriginType.values()) {
            PuffishSkillsIntegration.category(type.categoryId()).ifPresent(category -> {
                if (category.isUnlocked(player)) category.lock(player);
            });
        }
        PuffishSkillsIntegration.invalidateRuntimeCache(player);
    }
}
