package org.marj4n.smooth_classes.content.avenger.runtime;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.Angerable;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.VexEntity;
import net.minecraft.entity.mob.WardenEntity;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.village.VillagerProfession;
import org.marj4n.smooth_classes.content.avenger.AvengerClass;
import org.marj4n.smooth_classes.content.avenger.AvengerContent;
import org.marj4n.smooth_classes.integration.OptionalCompatRuntime;
import org.marj4n.smooth_classes.mixin.AvengerWitherBossBarAccessor;
import org.marj4n.smooth_classes.network.SmoothClassesNetworking;
import org.marj4n.smooth_classes.runtime.AbilityRuntime;
import org.marj4n.smooth_classes.runtime.ExecutionResult;
import org.marj4n.smooth_classes.runtime.SpellPowerRuntime;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Reworked Avenger gameplay: persistent Death List, vanilla corpse summons,
 * Curtain Call and Endless Devour.
 */
public final class AvengerReworkRuntime {
    private static final String DEATH_LIST_MARKER = "SmoothClassesDeathList";
    private static final String SUMMON_TAG = "smooth_classes_avenger_summon";
    private static final String OWNER_TAG_PREFIX = "smooth_classes_avenger_owner_";

    private static final Map<UUID, DevourChannel> DEVOURS = new HashMap<>();
    private static final Set<UUID> CURTAIN_BYPASS = new HashSet<>();
    private static final Set<UUID> MANUAL_CAPTURED = new HashSet<>();
    private static final Map<Entity, UUID> SUMMON_OWNER_CACHE = new WeakHashMap<>();

    private AvengerReworkRuntime() {}

    public static void register() {
        AvengerSoulAnimationRuntime.register();
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            // Avenger maintenance has no behavior faster than five ticks. Keep the
            // player's own age phase so scheduled 20-tick work still lands correctly.
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                if ((player.age % 5) == 0) tickPlayer(player);
            }
            if (!DEVOURS.isEmpty()) tickDevours(server);
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID playerId = handler.player.getUuid();
            DEVOURS.remove(playerId);
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            DEVOURS.clear();
            CURTAIN_BYPASS.clear();
            MANUAL_CAPTURED.clear();
            SUMMON_OWNER_CACHE.clear();
        });
    }

    private static void tickPlayer(ServerPlayerEntity player) {
        MinecraftServer server = player.getServer();
        if (server == null) return;

        UUID playerId = player.getUuid();
        boolean avenger = AbilityRuntime.isClass(player, AvengerClass.ID);
        boolean cleanupTick = (player.age % 20) == 0;

        // Non-Avengers only need a once-per-second legacy-book cleanup check.
        // Current Avengers still run summon ownership/AI at 4 Hz.
        if (!avenger && !cleanupTick) return;

        AvengerDeathListState state = AvengerDeathListState.get(server);
        if (avenger) state.awaken(playerId);

        // Death List is virtual now. Old physical/Patchouli copies are quietly removed
        // so upgraded worlds do not keep duplicate permanent books in inventories.
        if (cleanupTick && state.hasAwakened(playerId)) purgeLegacyDeathLists(player);

        // The 96-block summon ownership/AI sweep runs at 4 Hz.
        if (avenger) tickOwnedSummons(player);
    }

    // ---------------------------------------------------------------------
    // Persistent Death List / kill capture
    // ---------------------------------------------------------------------

    public static void onKilledOther(ServerPlayerEntity player, LivingEntity victim) {
        if (MANUAL_CAPTURED.remove(victim.getUuid())) return;
        if (!AbilityRuntime.isClass(player, AvengerClass.ID)) return;
        if (victim instanceof PlayerEntity) return;
        String entityId = Registries.ENTITY_TYPE.getId(victim.getType()).toString();
        if (!AvengerSummonRecipes.supported(entityId) || !AvengerSummonSafety.allowed(victim)) return;

        AvengerDeathListState state = AvengerDeathListState.get(player.getServer());
        state.recordKill(player.getUuid(), entityId);
        refreshDeathListNow(player, state);
        player.sendMessage(Text.literal("Death List +1: " + AvengerSummonRecipes.friendlyEntityName(entityId))
                .formatted(Formatting.DARK_PURPLE), true);
    }

    /** Credits kills made by bound summons to their Avenger owner. */
    public static void onSummonKilledOther(LivingEntity victim, DamageSource source) {
        if (victim == null || source == null || victim instanceof PlayerEntity || victim.getWorld().isClient) return;
        Entity killer = source.getAttacker();
        if (killer == null) killer = source.getSource();
        UUID owner = summonOwnerUuid(killer);
        if (owner == null && victim.getAttacker() != null) owner = summonOwnerUuid(victim.getAttacker());
        if (owner == null) return;

        String entityId = Registries.ENTITY_TYPE.getId(victim.getType()).toString();
        if (!AvengerSummonRecipes.supported(entityId) || !AvengerSummonSafety.allowed(victim)) return;
        MinecraftServer server = victim.getServer();
        if (server == null) return;

        AvengerDeathListState state = AvengerDeathListState.get(server);
        if (!state.hasAwakened(owner)) return;
        state.recordKill(owner, entityId);

        ServerPlayerEntity player = server.getPlayerManager().getPlayer(owner);
        if (player != null) {
            refreshDeathListNow(player, state);
            player.sendMessage(Text.literal("Death List +1 (summon): "
                    + AvengerSummonRecipes.friendlyEntityName(entityId)).formatted(Formatting.DARK_PURPLE), true);
        }
    }

    private static void captureDevoured(ServerPlayerEntity player, LivingEntity victim) {
        String entityId = Registries.ENTITY_TYPE.getId(victim.getType()).toString();
        if (!AvengerSummonRecipes.supported(entityId) || !AvengerSummonSafety.allowed(victim)) return;
        AvengerDeathListState state = AvengerDeathListState.get(player.getServer());
        state.recordKill(player.getUuid(), entityId);
        refreshDeathListNow(player, state);
    }

    /** Recognizes old physical Death Lists so they can be removed after upgrading. */
    public static boolean isDeathList(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.hasNbt()) return false;
        NbtCompound nbt = stack.getNbt();
        return nbt.getBoolean(DEATH_LIST_MARKER)
                || "smooth_classes:death_list".equals(nbt.getString("patchouli:book"));
    }

    private static void purgeLegacyDeathLists(ServerPlayerEntity player) {
        boolean changed = false;
        for (int slot = 0; slot < player.getInventory().size(); slot++) {
            if (isDeathList(player.getInventory().getStack(slot))) {
                player.getInventory().setStack(slot, ItemStack.EMPTY);
                changed = true;
            }
        }
        if (changed) {
            player.getInventory().markDirty();
            player.currentScreenHandler.sendContentUpdates();
        }
    }

    /**
     * The ledger itself lives in PersistentState, so gameplay events no longer rewrite
     * an inventory book. Ability state is still refreshed immediately for the HUD.
     */
    private static void refreshDeathListNow(ServerPlayerEntity player, AvengerDeathListState state) {
        if (player != null) SmoothClassesNetworking.sendAbilityState(player);
    }

    /** Builds the Ctrl+H virtual Death List. Nothing is inserted into the inventory. */
    public static NbtCompound deathListBookNbt(ServerPlayerEntity player) {
        AvengerDeathListState state = AvengerDeathListState.get(player.getServer());
        state.awaken(player.getUuid());

        NbtCompound nbt = new NbtCompound();
        nbt.putString("title", "Death List");
        nbt.putString("author", "The Avenger");
        nbt.putBoolean("resolved", true);
        nbt.putInt("generation", 0);
        writeDeathListPages(nbt, player, state);
        return nbt;
    }

    private static void writeDeathListPages(NbtCompound nbt, ServerPlayerEntity player, AvengerDeathListState state) {
        UUID playerId = player.getUuid();
        AvengerDeathListState.PlayerLedger ledger = state.ledger(playerId);
        AvengerDeathListState.ChargeSnapshot charges = state.refreshCharges(playerId, System.currentTimeMillis());

        List<AvengerSummonRecipes.Recipe> recipes = new ArrayList<>(AvengerSummonRecipes.allRecipes());
        recipes.sort(Comparator.comparing(recipe -> AvengerSummonRecipes.friendlyEntityName(recipe.entityId())));

        int totalSouls = ledger.souls().values().stream().mapToInt(Integer::intValue).sum();
        int discovered = 0;
        NbtCompound soulCounts = new NbtCompound();
        NbtCompound killCounts = new NbtCompound();
        for (AvengerSummonRecipes.Recipe recipe : recipes) {
            int kills = ledger.kills().getOrDefault(recipe.entityId(), 0);
            int souls = state.available(playerId, recipe.entityId());
            if (kills > 0) discovered++;
            soulCounts.putInt(recipe.entityId(), souls);
            killCounts.putInt(recipe.entityId(), kills);
        }

        // Raw ledger snapshot for the virtual Patchouli UI. The written-book pages
        // below remain as a dependency-free fallback if Patchouli is unavailable.
        nbt.putInt("SmoothDeathTotalSouls", totalSouls);
        nbt.putInt("SmoothDeathDiscovered", discovered);
        nbt.putInt("SmoothDeathRecipeCount", recipes.size());
        nbt.putInt("SmoothDeathCharges", charges.charges());
        nbt.putInt("SmoothDeathMaxCharges", AvengerDeathListState.MAX_SUMMON_CHARGES);
        nbt.putLong("SmoothDeathRechargeMillis", Math.max(0L, charges.remainingMillis()));
        nbt.put("SmoothDeathSoulCounts", soulCounts);
        nbt.put("SmoothDeathKillCounts", killCounts);

        List<Text> pages = new ArrayList<>();
        pages.add(deathListCover(charges, totalSouls, discovered, recipes.size()));
        appendSoulStockPages(pages, playerId, state, ledger, recipes);
        appendRecipePages(pages, playerId, state, ledger, recipes);

        NbtList list = new NbtList();
        for (Text page : pages) list.add(NbtString.of(Text.Serializer.toJson(page)));
        nbt.put("pages", list);
    }

    private static Text deathListCover(AvengerDeathListState.ChargeSnapshot charges, int totalSouls,
                                       int discovered, int recipeCount) {
        var page = Text.empty();
        page.append(Text.literal("DEATH LIST\n").formatted(Formatting.DARK_PURPLE, Formatting.BOLD));
        page.append(Text.literal("From Corpses We Arise\n\n").formatted(Formatting.GRAY, Formatting.ITALIC));

        page.append(Text.literal("TOTAL SOULS  ").formatted(Formatting.GOLD, Formatting.BOLD));
        page.append(Text.literal(totalSouls + "\n").formatted(Formatting.AQUA, Formatting.BOLD));
        page.append(Text.literal("DISCOVERED    ").formatted(Formatting.DARK_GRAY));
        page.append(Text.literal(discovered + "/" + recipeCount + "\n\n").formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD));

        page.append(Text.literal("[H] ").formatted(Formatting.GOLD, Formatting.BOLD));
        page.append(Text.literal("Summon / Recall\n").formatted(Formatting.GRAY));
        page.append(Text.literal("[Ctrl+H] ").formatted(Formatting.AQUA, Formatting.BOLD));
        page.append(Text.literal("Open Death List\n").formatted(Formatting.GRAY));
        page.append(Text.literal("[Shift+H] ").formatted(Formatting.DARK_PURPLE, Formatting.BOLD));
        page.append(Text.literal("Unbind aimed soul\n\n").formatted(Formatting.GRAY));

        page.append(Text.literal("CHARGES  ").formatted(Formatting.GOLD, Formatting.BOLD));
        page.append(Text.literal(charges.charges() + "/" + AvengerDeathListState.MAX_SUMMON_CHARGES + "\n")
                .formatted(Formatting.GREEN, Formatting.BOLD));
        if (charges.remainingMillis() > 0) {
            long seconds = Math.max(1L, (charges.remainingMillis() + 999L) / 1000L);
            page.append(Text.literal("Next charge  ").formatted(Formatting.DARK_GRAY));
            page.append(Text.literal(seconds + "s\n").formatted(Formatting.AQUA));
        }
        return page;
    }

    private static void appendSoulStockPages(List<Text> pages, UUID playerId, AvengerDeathListState state,
                                             AvengerDeathListState.PlayerLedger ledger,
                                             List<AvengerSummonRecipes.Recipe> recipes) {
        var page = Text.empty();
        int onPage = 0;
        int pageIndex = 1;
        boolean any = false;
        for (AvengerSummonRecipes.Recipe recipe : recipes) {
            int slain = ledger.kills().getOrDefault(recipe.entityId(), 0);
            if (slain <= 0) continue;
            if (onPage == 0) {
                page.append(Text.literal("SOUL STOCK " + pageIndex + "\n")
                        .formatted(Formatting.DARK_PURPLE, Formatting.BOLD));
                page.append(Text.literal("----------------\n").formatted(Formatting.DARK_GRAY));
            }
            String name = AvengerSummonRecipes.friendlyEntityName(recipe.entityId());
            int souls = state.available(playerId, recipe.entityId());
            page.append(Text.literal(name + "  ").formatted(Formatting.WHITE));
            page.append(Text.literal("x" + souls + "\n")
                    .formatted(souls > 0 ? Formatting.AQUA : Formatting.DARK_GRAY, Formatting.BOLD));
            any = true;
            if (++onPage >= 8) {
                pages.add(page);
                page = Text.empty();
                onPage = 0;
                pageIndex++;
            }
        }
        if (onPage > 0) pages.add(page);
        if (!any) {
            pages.add(Text.empty()
                    .append(Text.literal("SOUL STOCK\n\n").formatted(Formatting.DARK_PURPLE, Formatting.BOLD))
                    .append(Text.literal("No souls discovered yet.\n\n").formatted(Formatting.GRAY))
                    .append(Text.literal("Kill a supported creature to reveal its soul and recipe.")
                            .formatted(Formatting.DARK_GRAY, Formatting.ITALIC)));
        }
    }

    private static void appendRecipePages(List<Text> pages, UUID playerId, AvengerDeathListState state,
                                          AvengerDeathListState.PlayerLedger ledger,
                                          List<AvengerSummonRecipes.Recipe> recipes) {
        // Avoid 100+ oversized vanilla book pages now that ~150 optional mobs exist.
        // Discovered recipes get full ingredient listings; undiscovered souls are
        // compact, since their recipes must remain secret until the first kill.
        List<AvengerSummonRecipes.Recipe> known = new ArrayList<>();
        List<AvengerSummonRecipes.Recipe> unknown = new ArrayList<>();
        for (AvengerSummonRecipes.Recipe recipe : recipes) {
            if (ledger.kills().getOrDefault(recipe.entityId(), 0) > 0) known.add(recipe);
            else unknown.add(recipe);
        }

        int pageIndex = 1;
        for (int i = 0; i < known.size() && pages.size() < 98; i += 2) {
            var page = Text.empty();
            page.append(Text.literal("SOUL RECIPES " + pageIndex++ + "\n")
                    .formatted(Formatting.DARK_PURPLE, Formatting.BOLD));
            page.append(Text.literal("----------------\n").formatted(Formatting.DARK_GRAY));
            for (int j = i; j < Math.min(i + 2, known.size()); j++) {
                AvengerSummonRecipes.Recipe recipe = known.get(j);
                appendSoulRecord(page, playerId, state, recipe,
                        ledger.kills().getOrDefault(recipe.entityId(), 0));
            }
            pages.add(page);
        }

        for (int i = 0; i < unknown.size() && pages.size() < 98; i += 8) {
            var page = Text.empty();
            page.append(Text.literal("UNDISCOVERED " + (1 + i / 8) + "\n")
                    .formatted(Formatting.DARK_PURPLE, Formatting.BOLD));
            page.append(Text.literal("----------------\n").formatted(Formatting.DARK_GRAY));
            for (int j = i; j < Math.min(i + 8, unknown.size()); j++) {
                page.append(Text.literal(AvengerSummonRecipes.friendlyEntityName(unknown.get(j).entityId()) + "  ???\n")
                        .formatted(Formatting.DARK_GRAY));
            }
            pages.add(page);
        }
        if (pages.size() >= 98) {
            pages.add(Text.literal("Large Soul Ledger\n\nFor all collected souls and recipes, use the Patchouli Death List interface.")
                    .formatted(Formatting.DARK_PURPLE));
        }
    }

    private static void appendSoulRecord(net.minecraft.text.MutableText page, UUID playerId,
                                         AvengerDeathListState state, AvengerSummonRecipes.Recipe recipe, int slain) {
        String entityId = recipe.entityId();
        page.append(Text.literal("\n" + AvengerSummonRecipes.friendlyEntityName(entityId) + "\n")
                .formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD));
        page.append(Text.literal("SOULS ").formatted(Formatting.GOLD, Formatting.BOLD));
        page.append(Text.literal(String.valueOf(state.available(playerId, entityId))).formatted(Formatting.AQUA, Formatting.BOLD));
        page.append(Text.literal("   SLAIN ").formatted(Formatting.DARK_GRAY));
        page.append(Text.literal(String.valueOf(slain) + "\n").formatted(Formatting.YELLOW, Formatting.BOLD));

        page.append(Text.literal("MAIN  ").formatted(Formatting.GREEN, Formatting.BOLD));
        page.append(Text.literal(AvengerSummonRecipes.friendlyItemName(recipe.mainItemId()) + "\n").formatted(Formatting.WHITE));
        page.append(Text.literal("OFF   ").formatted(Formatting.BLUE, Formatting.BOLD));
        page.append(Text.literal(recipe.offhandItemId() == null
                        ? "Not required\n" : AvengerSummonRecipes.friendlyItemName(recipe.offhandItemId()) + "\n")
                .formatted(recipe.offhandItemId() == null ? Formatting.DARK_GRAY : Formatting.WHITE));
    }

    private static void appendUndiscoveredRecord(net.minecraft.text.MutableText page, AvengerSummonRecipes.Recipe recipe) {
        page.append(Text.literal("\n" + AvengerSummonRecipes.friendlyEntityName(recipe.entityId()) + "\n")
                .formatted(Formatting.DARK_GRAY, Formatting.BOLD));
        page.append(Text.literal("UNDISCOVERED\n").formatted(Formatting.RED, Formatting.BOLD));
        page.append(Text.literal("Recipe hidden until first kill.\n").formatted(Formatting.GRAY, Formatting.ITALIC));
    }

    // ---------------------------------------------------------------------
    // H special cast: 3-charge corpse summoning
    // ---------------------------------------------------------------------

    public static ExecutionResult summonFromHands(ServerPlayerEntity player) {
        if (!AbilityRuntime.isClass(player, AvengerClass.ID)) return ExecutionResult.failure("You are not an Avenger.");
        MinecraftServer server = player.getServer();
        if (server == null) return ExecutionResult.failure("Server state unavailable.");

        AvengerDeathListState state = AvengerDeathListState.get(server);

        // H is context-sensitive for Avenger. If the crosshair is on one of the
        // player's own bound summons, return that summon to the Death List first.
        // Recall never consumes a summon charge and does not require ingredients.
        LivingEntity recalled = aimedLiving(player, 32D, entity -> isOwnedSummon(player, entity));
        if (recalled != null) return player.isSneaking()
                ? unbindSummon(player, state, recalled) : recallSummon(player, state, recalled);
        if (player.isSneaking()) return ExecutionResult.failure("Aim at your own bound soul to unbind it.");

        AvengerDeathListState.ChargeSnapshot charge = state.refreshCharges(player.getUuid(), System.currentTimeMillis());
        if (charge.charges() <= 0) {
            return ExecutionResult.failure("From Corpses We Arise has no charges. Next charge in "
                    + String.format(java.util.Locale.ROOT, "%.1f", charge.remainingMillis() / 1000D) + "s");
        }

        ItemStack main = player.getMainHandStack();
        ItemStack off = player.getOffHandStack();
        if (main.isEmpty()) return ExecutionResult.failure("Hold a Death List summon ingredient in your main hand.");

        List<AvengerSummonRecipes.Recipe> recipes = AvengerSummonRecipes.matching(main, off);
        if (recipes.isEmpty()) {
            return ExecutionResult.failure("No Death List recipe matches your main/off-hand items.");
        }

        AvengerSummonRecipes.Recipe chosen = null;
        for (AvengerSummonRecipes.Recipe recipe : recipes) {
            if (state.available(player.getUuid(), recipe.entityId()) > 0) {
                chosen = recipe;
                break;
            }
        }
        if (chosen == null) {
            String first = recipes.get(0).entityId();
            return ExecutionResult.failure("That soul is not in your Death List. Kill "
                    + AvengerSummonRecipes.friendlyEntityName(first) + " first.");
        }

        LivingEntity summoned = spawnRegisteredSoul(player, chosen);
        if (summoned == null) return ExecutionResult.failure("That soul could not manifest here.");

        // Resource spending only happens after a successful spawn.
        if (!state.consumeSoul(player.getUuid(), chosen.entityId())) {
            summoned.discard();
            return ExecutionResult.failure("The soul vanished before it could be bound.");
        }
        if (!state.consumeCharge(player.getUuid(), System.currentTimeMillis())) {
            summoned.discard();
            state.recordKill(player.getUuid(), chosen.entityId());
            return ExecutionResult.failure("No summon charge remained.");
        }
        consumeHandIngredient(player, Hand.MAIN_HAND);
        if (chosen.offhandItemId() != null) consumeHandIngredient(player, Hand.OFF_HAND);

        refreshDeathListNow(player, state);
        AvengerSoulAnimationRuntime.emerge(player, summoned);
        ServerWorld world = player.getServerWorld();
        world.spawnParticles(ParticleTypes.SOUL, summoned.getX(), summoned.getBodyY(0.55D), summoned.getZ(),
                28, 0.65D, 0.75D, 0.65D, 0.04D);
        world.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, summoned.getX(), summoned.getBodyY(0.55D), summoned.getZ(),
                12, 0.45D, 0.55D, 0.45D, 0.02D);
        world.playSound(null, summoned.getBlockPos(), SoundEvents.BLOCK_SOUL_SAND_BREAK, SoundCategory.PLAYERS, 1.0F, 0.65F);
        SmoothClassesNetworking.sendAbilityState(player);
        return ExecutionResult.success(1, "Summoned " + AvengerSummonRecipes.friendlyEntityName(chosen.entityId())
                + " (" + summonCharges(player) + "/3 charges)");
    }

    private static LivingEntity spawnRegisteredSoul(ServerPlayerEntity player, AvengerSummonRecipes.Recipe recipe) {
        // Do not create an unexpected boss or non-living entity, even from a forged old ledger.
        if (!AvengerSummonRecipes.supported(recipe.entityId()) || AvengerSummonSafety.bannedId(recipe.entityId())) return null;
        EntityType<?> type = Registries.ENTITY_TYPE.get(recipe.entityIdentifier());
        Vec3d look = player.getRotationVec(1F);
        Vec3d flat = new Vec3d(look.x, 0D, look.z);
        if (flat.lengthSquared() < 1.0E-6D) flat = new Vec3d(0D, 0D, 1D);
        flat = flat.normalize();
        Vec3d pos = player.getPos().add(flat.multiply(2.5D));
        Entity entity;
        try {
            entity = type.spawn(player.getServerWorld(), net.minecraft.util.math.BlockPos.ofFloored(pos), SpawnReason.MOB_SUMMONED);
        } catch (RuntimeException spawnFailure) {
            // Optional modded entities can reject invalid spawn contexts. No item,
            // soul, or charge has been consumed yet; don't crash the server.
            return null;
        }
        if (!(entity instanceof LivingEntity living) || entity instanceof PlayerEntity) {
            if (entity != null) entity.discard();
            return null;
        }
        if (!AvengerSummonSafety.allowed(living)) {
            living.discard();
            return null;
        }
        living.refreshPositionAndAngles(pos.x, living.getY(), pos.z, player.getYaw(), 0F);

        tagSummon(living, player.getUuid());
        configureOwnership(player, living);
        hideBoundWitherBossBar(living);
        applySummonBuffs(player, living);
        return living;
    }

    private static void hideBoundWitherBossBar(LivingEntity entity) {
        if (entity instanceof WitherEntity wither && isAvengerSummon(wither)) {
            ((AvengerWitherBossBarAccessor) wither).smoothClasses$getBossBar().setVisible(false);
        }
    }

    private static void configureOwnership(ServerPlayerEntity owner, LivingEntity living) {
        if (living instanceof TameableEntity tameable) {
            tameable.setOwner(owner);
            tameable.setTamed(true);
        }
        if (living instanceof AbstractHorseEntity horse) {
            horse.setOwnerUuid(owner.getUuid());
            horse.setTame(true);
        }
        // Death List villagers start genuinely unemployed. They may acquire a
        // profession later through normal workstation AI, but summoning never
        // grants a profession or pre-baked trades.
        if (living instanceof VillagerEntity villager) {
            villager.setVillagerData(villager.getVillagerData()
                    .withProfession(VillagerProfession.NONE)
                    .withLevel(1));
            villager.setExperience(0);
        }
        if (living instanceof MobEntity mob) mob.setPersistent();
    }

    private static void applySummonBuffs(ServerPlayerEntity owner, LivingEntity summon) {
        int damageRank = talentRank(owner, AvengerContent.SUMMON_DAMAGE_I, AvengerContent.SUMMON_DAMAGE_II);
        int healthRank = talentRank(owner, AvengerContent.SUMMON_HEALTH_I, AvengerContent.SUMMON_HEALTH_II);
        int armorRank = talentRank(owner, AvengerContent.SUMMON_ARMOR_I, AvengerContent.SUMMON_ARMOR_II);

        EntityAttributeInstance damage = summon.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE);
        if (damage != null && damageRank > 0) damage.setBaseValue(damage.getBaseValue() * (1D + damageRank * 0.30D));

        EntityAttributeInstance maxHealth = summon.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (maxHealth != null) {
            double multiplier = 1D + healthRank * 0.30D;
            if (AbilityRuntime.hasTalent(owner, AvengerContent.SOUL_PACT.id())) multiplier += 0.25D;
            maxHealth.setBaseValue(Math.max(1D, maxHealth.getBaseValue() * multiplier));
            summon.setHealth(summon.getMaxHealth());
        }

        EntityAttributeInstance armor = summon.getAttributeInstance(EntityAttributes.GENERIC_ARMOR);
        if (armor != null) {
            double add = armorRank * 4D;
            if (AbilityRuntime.hasTalent(owner, AvengerContent.SOUL_PACT.id())) {
                add += owner.getAttributeValue(EntityAttributes.GENERIC_ARMOR) * 0.35D;
            }
            armor.setBaseValue(armor.getBaseValue() + add);
        }

        if (AbilityRuntime.hasTalent(owner, AvengerContent.DEATH_MARCH.id())) {
            EntityAttributeInstance speed = summon.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
            if (speed != null) speed.setBaseValue(speed.getBaseValue() * 1.15D);
        }
    }

    private static int talentRank(ServerPlayerEntity player, org.marj4n.smooth_classes.api.talent.Talent... talents) {
        int count = 0;
        for (var talent : talents) if (AbilityRuntime.hasTalent(player, talent.id())) count++;
        return count;
    }

    private static void consumeHandIngredient(ServerPlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (stack.isEmpty()) return;

        // Summoning temporarily binds the entire ingredient to the soul. We do not
        // create vanilla crafting remainders here (for example an empty bucket),
        // because recalling the summon restores the original recipe ingredient.
        stack.decrement(1);
        if (stack.isEmpty()) player.setStackInHand(hand, ItemStack.EMPTY);
    }

    private static ExecutionResult recallSummon(ServerPlayerEntity player, AvengerDeathListState state, LivingEntity summon) {
        if (!isOwnedSummon(player, summon)) {
            return ExecutionResult.failure("That summon does not belong to your Death List.");
        }
        if (AvengerSoulAnimationRuntime.busy(summon)) {
            return ExecutionResult.failure("That soul is still materializing or being absorbed.");
        }

        String entityId = Registries.ENTITY_TYPE.getId(summon.getType()).toString();
        AvengerSummonRecipes.Recipe recipe = AvengerSummonRecipes.recipe(entityId);
        if (recipe == null) {
            return ExecutionResult.failure("That summon has no Death List recall recipe.");
        }

        if (!AvengerSoulAnimationRuntime.recall(player, summon, recipe)) {
            return ExecutionResult.failure("That soul cannot be recalled right now.");
        }
        player.getServerWorld().playSound(null, summon.getBlockPos(), SoundEvents.BLOCK_SOUL_SAND_BREAK,
                SoundCategory.PLAYERS, 0.9F, 0.55F);
        return ExecutionResult.success(1, "Absorbing " + AvengerSummonRecipes.friendlyEntityName(entityId)
                + " into the Death List. Soul and ingredients return after the ritual.");
    }

    private static ExecutionResult unbindSummon(ServerPlayerEntity player, AvengerDeathListState state, LivingEntity summon) {
        if (AvengerSoulAnimationRuntime.busy(summon)) return ExecutionResult.failure("That soul is still in a ritual.");
        // A manifested soul was already consumed from the ledger. Unbinding never
        // refunds it or erases other captured souls of the same species.
        summon.removeScoreboardTag(SUMMON_TAG);
        for (String tag : new HashSet<>(summon.getCommandTags())) {
            if (tag.startsWith(OWNER_TAG_PREFIX)) summon.removeScoreboardTag(tag);
        }
        SUMMON_OWNER_CACHE.remove(summon);
        if (summon instanceof TameableEntity tameable) {
            tameable.setOwnerUuid(null);
            tameable.setTamed(false);
            tameable.setSitting(false);
            tameable.setInSittingPose(false);
        }
        if (summon instanceof AbstractHorseEntity horse) {
            horse.setOwnerUuid(null);
            horse.setTame(false);
        }
        if (summon instanceof MobEntity mob) {
            mob.setTarget(null);
            mob.getNavigation().stop();
        }
        summon.setAttacker(null);
        if (summon instanceof Angerable angerable) angerable.stopAnger();
        player.getServerWorld().spawnParticles(ParticleTypes.SOUL, summon.getX(), summon.getBodyY(.5), summon.getZ(),
                20, .4, .5, .4, .02);
        refreshDeathListNow(player, state);
        return ExecutionResult.success(1, "Soul unbound. Its normal AI is restored.");
    }

    static void refundRecipeItem(ServerPlayerEntity player, String itemId) {
        if (itemId == null || itemId.isBlank()) return;
        Identifier id = new Identifier(itemId);
        if (!Registries.ITEM.containsId(id)) return;

        ItemStack refund = new ItemStack(Registries.ITEM.get(id), 1);

        // Inventory#insertStack may choose an empty slot before coalescing a stack in
        // some modded inventory setups. Recall should feel exactly like returning the
        // ingredient the player just spent, so merge compatible stacks ourselves first.
        ItemStack mainHand = player.getMainHandStack();
        mergeRefundStack(mainHand, refund);
        for (int slot = 0; slot < player.getInventory().size() && !refund.isEmpty(); slot++) {
            ItemStack existing = player.getInventory().getStack(slot);
            if (existing == mainHand) continue;
            mergeRefundStack(existing, refund);
        }

        if (!refund.isEmpty() && !player.getInventory().insertStack(refund)) {
            player.dropItem(refund, false);
        }
        player.getInventory().markDirty();
        player.currentScreenHandler.sendContentUpdates();
    }

    private static void mergeRefundStack(ItemStack existing, ItemStack refund) {
        if (existing == null || existing.isEmpty() || refund.isEmpty() || !ItemStack.canCombine(existing, refund)) return;
        int room = existing.getMaxCount() - existing.getCount();
        if (room <= 0) return;
        int moved = Math.min(room, refund.getCount());
        existing.increment(moved);
        refund.decrement(moved);
    }

    public static int summonCharges(ServerPlayerEntity player) {
        if (player.getServer() == null) return 0;
        return AvengerDeathListState.get(player.getServer())
                .refreshCharges(player.getUuid(), System.currentTimeMillis()).charges();
    }

    public static long summonRechargeRemainingTicks(ServerPlayerEntity player) {
        if (player.getServer() == null) return 0L;
        long ms = AvengerDeathListState.get(player.getServer())
                .refreshCharges(player.getUuid(), System.currentTimeMillis()).remainingMillis();
        return (ms + 49L) / 50L;
    }

    public static int summonRechargeTotalTicks() { return (int) (AvengerDeathListState.RECHARGE_MILLIS / 50L); }
    public static int maxSummonCharges() { return AvengerDeathListState.MAX_SUMMON_CHARGES; }

    // ---------------------------------------------------------------------
    // Summon ownership / allied behavior
    // ---------------------------------------------------------------------

    private static void tagSummon(LivingEntity entity, UUID owner) {
        entity.addCommandTag(SUMMON_TAG);
        entity.addCommandTag(OWNER_TAG_PREFIX + owner);
        SUMMON_OWNER_CACHE.put(entity, owner);
    }

    public static boolean isAvengerSummon(Entity entity) {
        return entity != null && summonOwnerUuid(entity) != null;
    }

    public static boolean isOwnedSummon(ServerPlayerEntity owner, Entity entity) {
        return owner != null && isOwnedSummon(owner.getUuid(), entity);
    }

    public static boolean isOwnedSummon(UUID owner, Entity entity) {
        UUID uuid = summonOwnerUuid(entity);
        return owner != null && owner.equals(uuid);
    }

    public static UUID summonOwnerUuid(Entity entity) {
        if (entity == null) return null;

        if (entity instanceof VexEntity vex && vex.getOwner() != null) {
            UUID indirectOwner = summonOwnerUuid(vex.getOwner());
            if (indirectOwner != null) {
                SUMMON_OWNER_CACHE.put(entity, indirectOwner);
                return indirectOwner;
            }
        }

        if (!entity.getCommandTags().contains(SUMMON_TAG)) return null;
        UUID cached = SUMMON_OWNER_CACHE.get(entity);
        if (cached != null) return cached;

        for (String tag : entity.getCommandTags()) {
            if (!tag.startsWith(OWNER_TAG_PREFIX)) continue;
            try {
                UUID owner = UUID.fromString(tag.substring(OWNER_TAG_PREFIX.length()));
                SUMMON_OWNER_CACHE.put(entity, owner);
                return owner;
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
        return null;
    }

    /**
     * One faction rule for every vanilla AI family. A bound summon can never
     * consider its owner, another summon owned by that player, or any villager
     * a combat target. This is intentionally checked both by canTarget and by
     * setTarget mixins so goal-based and brain-based mobs agree.
     */
    public static boolean shouldIgnoreSummonTarget(LivingEntity actor, LivingEntity target) {
        if (actor == null || target == null) return false;
        UUID actorOwner = summonOwnerUuid(actor);
        if (actorOwner == null) return false;
        if (target instanceof ServerPlayerEntity player && actorOwner.equals(player.getUuid())) return true;
        UUID targetOwner = summonOwnerUuid(target);
        if (actorOwner.equals(targetOwner)) return true;
        return target instanceof VillagerEntity;
    }

    public static boolean shouldWardenIgnore(Entity warden, Entity target) {
        if (!(warden instanceof LivingEntity livingWarden) || !(target instanceof LivingEntity livingTarget)) return false;
        return shouldIgnoreSummonTarget(livingWarden, livingTarget);
    }

    public static boolean shouldBlockSummonedWardenDarkness(LivingEntity entity) {
        if (entity == null || entity.getWorld().isClient) return false;
        UUID protectedOwner = entity instanceof ServerPlayerEntity player
                ? player.getUuid() : summonOwnerUuid(entity);
        if (protectedOwner == null || !(entity.getWorld() instanceof ServerWorld world)) return false;
        return !world.getEntitiesByClass(WardenEntity.class, entity.getBoundingBox().expand(32D),
                warden -> warden.isAlive() && isOwnedSummon(protectedOwner, warden)).isEmpty();
    }

    public static boolean shouldBlockSummonedWardenDarknessFrom(LivingEntity entity, Entity source) {
        if (entity == null || source == null || entity.getWorld().isClient) return false;
        UUID protectedOwner = entity instanceof ServerPlayerEntity player
                ? player.getUuid() : summonOwnerUuid(entity);
        if (protectedOwner == null) return false;
        return source instanceof WardenEntity && isOwnedSummon(protectedOwner, source);
    }

    public static boolean shouldCancelFriendlyDamage(LivingEntity victim, DamageSource source) {
        if (victim == null || source == null || CURTAIN_BYPASS.contains(victim.getUuid())) return false;
        Entity attacker = source.getAttacker();
        if (attacker == null) attacker = source.getSource();
        if (attacker == null) return false;

        UUID victimOwner = summonOwnerUuid(victim);
        UUID attackerOwner = summonOwnerUuid(attacker);

        if (victimOwner != null) {
            if (attacker instanceof ServerPlayerEntity player && victimOwner.equals(player.getUuid())) return true;
            if (attackerOwner != null && victimOwner.equals(attackerOwner)) return true;
        }
        if (attackerOwner != null && victim instanceof ServerPlayerEntity player && attackerOwner.equals(player.getUuid())) return true;
        if (attackerOwner != null && victim instanceof VillagerEntity) return true;
        return false;
    }

    private static void tickOwnedSummons(ServerPlayerEntity owner) {
        Box area = owner.getBoundingBox().expand(96D);
        List<LivingEntity> summons = owner.getServerWorld().getEntitiesByClass(
                LivingEntity.class, area, entity -> entity.isAlive() && isOwnedSummon(owner, entity));

        // Vanilla Warden darkness is an ambient pulse, not a normal combat target action.
        // Scrub it from the owner as a second line of defence in case vanilla/modded code
        // reaches a status-effect application path other than the sourced overload mixin.
        boolean hasOwnedWardenNearby = false;
        for (LivingEntity summon : summons) {
            hideBoundWitherBossBar(summon);
            if (summon instanceof WardenEntity) {
                hasOwnedWardenNearby = true;
                break;
            }
        }
        if (hasOwnedWardenNearby && owner.hasStatusEffect(StatusEffects.DARKNESS)) {
            owner.removeStatusEffect(StatusEffects.DARKNESS);
        }

        Set<UUID> alliedSummonIds = new HashSet<>(Math.max(16, summons.size() * 2));
        for (LivingEntity summon : summons) alliedSummonIds.add(summon.getUuid());

        LivingEntity ownerTarget = owner.getAttacking();
        if (ownerTarget == null || !validEnemy(owner, ownerTarget)) ownerTarget = owner.getLastAttacker();
        if (ownerTarget != null && !validEnemy(owner, ownerTarget)) ownerTarget = null;

        for (LivingEntity summon : summons) {
            // Clear stale retaliation data left by vanilla before our target filters run.
            LivingEntity attacker = summon.getAttacker();
            if (attacker != null && shouldIgnoreSummonTarget(summon, attacker)) summon.setAttacker(null);

            if (summon instanceof Angerable angerable) {
                UUID angryAt = angerable.getAngryAt();
                if (angryAt != null && (angryAt.equals(owner.getUuid()) || alliedSummonIds.contains(angryAt))) {
                    angerable.setAngryAt(null);
                    angerable.setAngerTime(0);
                }
            }

            if (summon instanceof WardenEntity warden) {
                warden.removeSuspect(owner);
                for (LivingEntity ally : summons) if (ally != summon) warden.removeSuspect(ally);
            }

            if (!(summon instanceof MobEntity mob)) continue;
            LivingEntity current = mob.getTarget();
            if (current != null && (!validEnemy(owner, current) || shouldIgnoreSummonTarget(summon, current))) {
                mob.setTarget(null);
                mob.getNavigation().stop();
            }

            // Brain-driven mobs (piglins, hoglins, wardens, etc.) can keep an
            // ATTACK_TARGET memory even after MobEntity#target is cleared.
            var brain = mob.getBrain();
            if (brain.hasMemoryModule(net.minecraft.entity.ai.brain.MemoryModuleType.ATTACK_TARGET)) {
                var rememberedTarget = brain.getOptionalMemory(net.minecraft.entity.ai.brain.MemoryModuleType.ATTACK_TARGET);
                if (rememberedTarget != null && rememberedTarget.isPresent()
                        && shouldIgnoreSummonTarget(summon, rememberedTarget.get())) {
                    brain.forget(net.minecraft.entity.ai.brain.MemoryModuleType.ATTACK_TARGET);
                    if (brain.hasMemoryModule(net.minecraft.entity.ai.brain.MemoryModuleType.WALK_TARGET)) {
                        brain.forget(net.minecraft.entity.ai.brain.MemoryModuleType.WALK_TARGET);
                    }
                }
            }
            if (brain.hasMemoryModule(net.minecraft.entity.ai.brain.MemoryModuleType.ANGRY_AT)) {
                var angryMemory = brain.getOptionalMemory(net.minecraft.entity.ai.brain.MemoryModuleType.ANGRY_AT);
                if (angryMemory != null && angryMemory.isPresent()) {
                    UUID angryAt = angryMemory.get();
                    if (angryAt.equals(owner.getUuid()) || alliedSummonIds.contains(angryAt)) {
                        brain.forget(net.minecraft.entity.ai.brain.MemoryModuleType.ANGRY_AT);
                        if (brain.hasMemoryModule(net.minecraft.entity.ai.brain.MemoryModuleType.ATTACK_TARGET)) {
                            brain.forget(net.minecraft.entity.ai.brain.MemoryModuleType.ATTACK_TARGET);
                        }
                    }
                }
            }

            if (ownerTarget != null && summon.squaredDistanceTo(ownerTarget) <= 64D * 64D
                    && !shouldIgnoreSummonTarget(summon, ownerTarget)) {
                mob.setAiDisabled(false);
                if (summon instanceof WardenEntity warden && warden.isValidTarget(ownerTarget)) {
                    warden.increaseAngerAt(ownerTarget, 150, false);
                    warden.updateAttackTarget(ownerTarget);
                } else if (mob.canTarget(ownerTarget)) {
                    mob.setTarget(ownerTarget);
                }
            } else {
                boolean dangerousBoss = summon instanceof WardenEntity
                        || summon.getType() == EntityType.WITHER
                        || summon.getType() == EntityType.ENDER_DRAGON;
                if (dangerousBoss) mob.setTarget(null);
                double ownerDistanceSq = summon.squaredDistanceTo(owner);
                if (ownerDistanceSq > 64D * 64D) {
                    summon.requestTeleport(owner.getX() + 1.5D, owner.getY(), owner.getZ() + 1.5D);
                } else if (!dangerousBoss && ownerDistanceSq > 8D * 8D && mob.getTarget() == null) {
                    mob.getNavigation().startMovingTo(owner, 1.15D);
                }
            }
        }
    }

    private static boolean validEnemy(ServerPlayerEntity owner, LivingEntity target) {
        return target != null && target.isAlive() && target != owner && !(target instanceof VillagerEntity)
                && !owner.isTeammate(target) && !isOwnedSummon(owner, target)
                && OptionalCompatRuntime.canHarm(owner, target);
    }

    // ---------------------------------------------------------------------
    // Signature: Curtain Call
    // ---------------------------------------------------------------------

    public static ExecutionResult executeCurtainCall(ServerPlayerEntity player) {
        if (!AbilityRuntime.isClass(player, AvengerClass.ID)) return ExecutionResult.failure("You are not an Avenger.");
        LivingEntity target = aimedLiving(player, 36D, entity -> isOwnedSummon(player, entity));
        if (target == null) return ExecutionResult.failure("Curtain Call requires one of your summons under the crosshair.");
        if (AvengerSoulAnimationRuntime.busy(target)) return ExecutionResult.failure("Wait for the soul ritual to finish.");

        int rank = talentRank(player, AvengerContent.CURTAIN_CALL_I, AvengerContent.CURTAIN_CALL_II);
        float explosionPower = rank >= 2 ? 8F : rank == 1 ? 6F : 4F;
        double radius = rank >= 2 ? 9D : rank == 1 ? 7D : 5D;
        double soulCoefficient = rank >= 2 ? 2.5D : rank == 1 ? 1.75D : 1.0D;
        float soulDamage = Math.max(1F, SpellPowerRuntime.soul(player, soulCoefficient));
        ServerWorld world = player.getServerWorld();
        Vec3d center = target.getPos().add(0D, target.getHeight() * 0.45D, 0D);

        // Return the sacrificed soul deterministically before the blast. The kill hook is suppressed
        // for this one entity so Curtain Call never duplicates the Death List entry.
        AvengerDeathListState.get(player.getServer()).recordKill(player.getUuid(),
                Registries.ENTITY_TYPE.getId(target.getType()).toString());
        refreshDeathListNow(player, AvengerDeathListState.get(player.getServer()));
        MANUAL_CAPTURED.add(target.getUuid());
        CURTAIN_BYPASS.add(target.getUuid());
        try {
            world.createExplosion(player, center.x, center.y, center.z, explosionPower, false, World.ExplosionSourceType.TNT);
            for (LivingEntity enemy : world.getEntitiesByClass(LivingEntity.class,
                    target.getBoundingBox().expand(radius), entity -> validEnemy(player, entity))) {
                if (enemy == target) continue;
                enemy.timeUntilRegen = 0;
                enemy.damage(player.getDamageSources().indirectMagic(player, player), soulDamage);
            }
            if (target.isAlive()) {
                target.timeUntilRegen = 0;
                target.damage(player.getDamageSources().playerAttack(player), Math.max(1000F, target.getMaxHealth() * 10F));
            }
        } finally {
            CURTAIN_BYPASS.remove(target.getUuid());
            if (target.isAlive()) MANUAL_CAPTURED.remove(target.getUuid());
        }

        world.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, center.x, center.y, center.z,
                rank >= 2 ? 70 : 40, radius * 0.35D, 1.2D, radius * 0.35D, 0.06D);
        return ExecutionResult.success(1, "Curtain Call detonated " + AvengerSummonRecipes.friendlyEntityName(
                Registries.ENTITY_TYPE.getId(target.getType()).toString()));
    }

    // ---------------------------------------------------------------------
    // Signature: Endless Devour
    // ---------------------------------------------------------------------

    public static ExecutionResult executeEndlessDevour(ServerPlayerEntity player) {
        if (!AbilityRuntime.isClass(player, AvengerClass.ID)) return ExecutionResult.failure("You are not an Avenger.");
        if (DEVOURS.containsKey(player.getUuid())) return ExecutionResult.failure("Endless Devour is already channeling.");

        LivingEntity target = aimedLiving(player, 28D, entity -> entity != player
                && !(entity instanceof PlayerEntity) && !isOwnedSummon(player, entity));
        if (target == null) return ExecutionResult.failure("Endless Devour requires a mob under the crosshair.");

        double thresholdMultiplier = AbilityRuntime.hasTalent(player, AvengerContent.DEVOUR_200.id()) ? 2.0D
                : AbilityRuntime.hasTalent(player, AvengerContent.DEVOUR_150.id()) ? 1.5D : 1.0D;
        float allowed = Math.max(1F, player.getHealth()) * (float) thresholdMultiplier;
        if (target.getHealth() > allowed) {
            return ExecutionResult.failure("Too much blood: target has " + String.format(java.util.Locale.ROOT, "%.1f", target.getHealth())
                    + " HP, Devour limit is " + String.format(java.util.Locale.ROOT, "%.1f", allowed) + " HP.");
        }

        int channelTicks = AbilityRuntime.hasTalent(player, AvengerContent.DEVOUR_FAST.id()) ? 14 : 24;
        DEVOURS.put(player.getUuid(), new DevourChannel(target.getUuid(), target.getWorld().getRegistryKey().getValue(), channelTicks));
        player.getServerWorld().playSound(null, player.getBlockPos(), SoundEvents.ENTITY_WARDEN_HEARTBEAT,
                SoundCategory.PLAYERS, 0.65F, 0.65F);
        return ExecutionResult.success(1, "Endless Devour channeling " + target.getName().getString());
    }

    private static void tickDevours(MinecraftServer server) {
        if (DEVOURS.isEmpty()) return;
        List<UUID> remove = new ArrayList<>();
        for (Map.Entry<UUID, DevourChannel> entry : DEVOURS.entrySet()) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());
            DevourChannel channel = entry.getValue();
            if (player == null || !player.isAlive() || !AbilityRuntime.isClass(player, AvengerClass.ID)) {
                remove.add(entry.getKey());
                continue;
            }
            ServerWorld world = null;
            for (ServerWorld candidate : server.getWorlds()) {
                if (candidate.getRegistryKey().getValue().equals(channel.worldId)) { world = candidate; break; }
            }
            if (world == null) { remove.add(entry.getKey()); continue; }
            Entity entity = world.getEntity(channel.targetId);
            if (!(entity instanceof LivingEntity target) || !target.isAlive() || target instanceof PlayerEntity
                    || player.squaredDistanceTo(target) > 32D * 32D) {
                player.sendMessage(Text.literal("Endless Devour lost its prey."), true);
                remove.add(entry.getKey());
                continue;
            }

            channel.ticksLeft--;
            Vec3d center = target.getPos().add(0D, target.getHeight() * 0.55D, 0D);
            world.spawnParticles(ParticleTypes.SCULK_SOUL, center.x, center.y, center.z, 4,
                    0.35D, 0.45D, 0.35D, 0.02D);
            world.spawnParticles(ParticleTypes.SOUL, player.getX(), player.getBodyY(0.55D), player.getZ(), 3,
                    0.25D, 0.35D, 0.25D, 0.015D);
            if ((channel.ticksLeft % 5) == 0) {
                // Repeated arm motion + eating audio gives the channel a visible chewing cadence
                // without swapping or consuming whatever item the player is actually holding.
                player.swingHand(Hand.MAIN_HAND, true);
                world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_GENERIC_EAT,
                        SoundCategory.PLAYERS, 0.8F, 0.75F + player.getRandom().nextFloat() * 0.2F);
            }
            if (channel.ticksLeft > 0) continue;

            captureDevoured(player, target);
            world.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, center.x, center.y, center.z, 32,
                    0.6D, 0.8D, 0.6D, 0.06D);
            target.discard();
            world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_PLAYER_BURP,
                    SoundCategory.PLAYERS, 1.0F, 0.7F);
            player.sendMessage(Text.literal("Devoured: " + target.getName().getString()).formatted(Formatting.DARK_PURPLE), true);
            remove.add(entry.getKey());
        }
        remove.forEach(DEVOURS::remove);
    }

    private static LivingEntity aimedLiving(ServerPlayerEntity player, double range,
                                             java.util.function.Predicate<LivingEntity> predicate) {
        Vec3d eye = player.getEyePos();
        Vec3d direction = player.getRotationVec(1F).normalize();
        Vec3d end = eye.add(direction.multiply(range));
        Box scan = player.getBoundingBox().stretch(direction.multiply(range)).expand(1.5D);
        LivingEntity selected = null;
        double best = Double.MAX_VALUE;
        for (LivingEntity target : player.getServerWorld().getEntitiesByClass(LivingEntity.class, scan,
                entity -> entity.isAlive() && predicate.test(entity))) {
            var hit = target.getBoundingBox().expand(0.30D).raycast(eye, end);
            if (hit.isEmpty()) continue;
            double distance = eye.squaredDistanceTo(hit.get());
            if (distance < best) {
                best = distance;
                selected = target;
            }
        }
        return selected;
    }

    private static final class DevourChannel {
        final UUID targetId;
        final Identifier worldId;
        int ticksLeft;

        DevourChannel(UUID targetId, Identifier worldId, int ticksLeft) {
            this.targetId = targetId;
            this.worldId = worldId;
            this.ticksLeft = ticksLeft;
        }
    }
}
