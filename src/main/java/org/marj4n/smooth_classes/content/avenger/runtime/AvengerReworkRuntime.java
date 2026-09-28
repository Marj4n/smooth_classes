package org.marj4n.smooth_classes.content.avenger.runtime;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.Angerable;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.WardenEntity;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
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
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.village.VillagerProfession;
import org.marj4n.smooth_classes.content.avenger.AvengerClass;
import org.marj4n.smooth_classes.content.avenger.AvengerContent;
import org.marj4n.smooth_classes.integration.OptionalCompatRuntime;
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

    private AvengerReworkRuntime() {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                tickPlayer(player);
            }
            tickDevours(server);
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> DEVOURS.remove(handler.player.getUuid()));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            DEVOURS.clear();
            CURTAIN_BYPASS.clear();
            MANUAL_CAPTURED.clear();
        });
    }

    private static void tickPlayer(ServerPlayerEntity player) {
        MinecraftServer server = player.getServer();
        if (server == null) return;

        AvengerDeathListState state = AvengerDeathListState.get(server);
        boolean avenger = AbilityRuntime.isClass(player, AvengerClass.ID);
        if (avenger) state.awaken(player.getUuid());

        if (state.hasAwakened(player.getUuid())) {
            state.refreshCharges(player.getUuid(), System.currentTimeMillis());
            ensureDeathList(player, state);
        }

        if (!avenger) return;
        if ((player.age % 5) == 0) tickOwnedSummons(player);
    }

    // ---------------------------------------------------------------------
    // Persistent Death List / kill capture
    // ---------------------------------------------------------------------

    public static void onKilledOther(ServerPlayerEntity player, LivingEntity victim) {
        if (MANUAL_CAPTURED.remove(victim.getUuid())) return;
        if (!AbilityRuntime.isClass(player, AvengerClass.ID)) return;
        if (victim instanceof PlayerEntity) return;
        String entityId = Registries.ENTITY_TYPE.getId(victim.getType()).toString();
        if (!AvengerSummonRecipes.supported(entityId)) return;

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
        if (!AvengerSummonRecipes.supported(entityId)) return;
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
        if (!AvengerSummonRecipes.supported(entityId)) return;
        AvengerDeathListState state = AvengerDeathListState.get(player.getServer());
        state.recordKill(player.getUuid(), entityId);
        refreshDeathListNow(player, state);
    }

    public static boolean isDeathList(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.isOf(Items.WRITTEN_BOOK)
                && stack.hasNbt() && stack.getNbt().getBoolean(DEATH_LIST_MARKER);
    }

    private static void ensureDeathList(ServerPlayerEntity player, AvengerDeathListState state) {
        ItemStack book = null;
        int firstSlot = -1;
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (!isDeathList(stack)) continue;
            if (book == null) {
                book = stack;
                firstSlot = i;
            } else {
                player.getInventory().setStack(i, ItemStack.EMPTY);
            }
        }

        if (book == null) {
            book = createDeathListBook(player, state);
            if (!player.getInventory().insertStack(book)) return;
        } else if ((player.age % 20) == 0) {
            writeDeathListPages(book, player, state);
            if (firstSlot >= 0) player.getInventory().markDirty();
        }
        if ((player.age % 20) == 0) player.currentScreenHandler.sendContentUpdates();
    }

    private static void refreshDeathListNow(ServerPlayerEntity player, AvengerDeathListState state) {
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (isDeathList(stack)) {
                writeDeathListPages(stack, player, state);
                player.getInventory().markDirty();
                player.currentScreenHandler.sendContentUpdates();
                return;
            }
        }
    }

    private static ItemStack createDeathListBook(ServerPlayerEntity player, AvengerDeathListState state) {
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        book.setCustomName(Text.literal("Death List").formatted(Formatting.DARK_PURPLE, Formatting.BOLD));
        writeDeathListPages(book, player, state);
        return book;
    }

    private static void writeDeathListPages(ItemStack book, ServerPlayerEntity player, AvengerDeathListState state) {
        NbtCompound nbt = book.getOrCreateNbt();
        nbt.putBoolean(DEATH_LIST_MARKER, true);
        nbt.putString("title", "Death List");
        nbt.putString("author", "The Avenger");
        nbt.putBoolean("resolved", true);
        nbt.putInt("generation", 0);

        AvengerDeathListState.PlayerLedger ledger = state.ledger(player.getUuid());
        AvengerDeathListState.ChargeSnapshot charges = state.refreshCharges(player.getUuid(), System.currentTimeMillis());
        List<String> pages = new ArrayList<>();
        pages.add("DEATH LIST\n\nFrom corpses we arise.\n\nH: summon / recall aimed soul\nCharges: "
                + charges.charges() + "/" + AvengerDeathListState.MAX_SUMMON_CHARGES
                + (charges.remainingMillis() > 0 ? "\nNext: " + Math.max(1, (charges.remainingMillis() + 999) / 1000) + "s" : "")
                + "\n\nKill a creature first. Its soul and recipe will then appear here.");

        List<Map.Entry<String, Integer>> entries = new ArrayList<>(ledger.kills().entrySet());
        entries.removeIf(entry -> !AvengerSummonRecipes.supported(entry.getKey()));
        entries.sort(Comparator.comparing(entry -> AvengerSummonRecipes.friendlyEntityName(entry.getKey())));

        StringBuilder page = new StringBuilder();
        int onPage = 0;
        for (Map.Entry<String, Integer> entry : entries) {
            String id = entry.getKey();
            AvengerSummonRecipes.Recipe recipe = AvengerSummonRecipes.recipe(id);
            if (recipe == null) continue;
            String block = AvengerSummonRecipes.friendlyEntityName(id)
                    + "\nSouls: " + state.available(player.getUuid(), id)
                    + " | Slain: " + entry.getValue()
                    + "\n" + recipe.displayRecipe() + "\n\n";
            if (onPage >= 3 || page.length() + block.length() > 700) {
                pages.add(page.toString());
                page.setLength(0);
                onPage = 0;
            }
            page.append(block);
            onPage++;
        }
        if (!page.isEmpty()) pages.add(page.toString());
        if (entries.isEmpty()) pages.add("No names yet.\n\nThe Death List records creatures killed by its Avenger, devoured directly, or slain by bound summons.");

        NbtList list = new NbtList();
        for (String text : pages) list.add(NbtString.of(Text.Serializer.toJson(Text.literal(text))));
        nbt.put("pages", list);
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
        if (recalled != null) return recallSummon(player, state, recalled);

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

        LivingEntity summoned = spawnVanillaSoul(player, chosen);
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

    private static LivingEntity spawnVanillaSoul(ServerPlayerEntity player, AvengerSummonRecipes.Recipe recipe) {
        if (!Registries.ENTITY_TYPE.containsId(recipe.entityIdentifier())) return null;
        EntityType<?> type = Registries.ENTITY_TYPE.get(recipe.entityIdentifier());
        Vec3d look = player.getRotationVec(1F);
        Vec3d flat = new Vec3d(look.x, 0D, look.z);
        if (flat.lengthSquared() < 1.0E-6D) flat = new Vec3d(0D, 0D, 1D);
        flat = flat.normalize();
        Vec3d pos = player.getPos().add(flat.multiply(2.5D));
        Entity entity = type.spawn(player.getServerWorld(), net.minecraft.util.math.BlockPos.ofFloored(pos), SpawnReason.MOB_SUMMONED);
        if (!(entity instanceof LivingEntity living) || entity instanceof PlayerEntity) {
            if (entity != null) entity.discard();
            return null;
        }
        living.refreshPositionAndAngles(pos.x, living.getY(), pos.z, player.getYaw(), 0F);

        tagSummon(living, player.getUuid());
        configureOwnership(player, living);
        applySummonBuffs(player, living);
        return living;
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

        String entityId = Registries.ENTITY_TYPE.getId(summon.getType()).toString();
        AvengerSummonRecipes.Recipe recipe = AvengerSummonRecipes.recipe(entityId);
        if (recipe == null) {
            return ExecutionResult.failure("That summon has no Death List recall recipe.");
        }

        ServerWorld world = player.getServerWorld();
        Vec3d center = summon.getPos().add(0D, summon.getHeight() * 0.55D, 0D);

        // Return the bound soul without increasing lifetime kill statistics.
        state.restoreSoul(player.getUuid(), entityId);
        refundRecipeItem(player, recipe.mainItemId());
        if (recipe.offhandItemId() != null) refundRecipeItem(player, recipe.offhandItemId());

        // Recall is not a kill: no kill-credit callback is fired and no extra soul
        // is generated. Passengers are safely detached before the entity disappears.
        summon.removeAllPassengers();
        summon.stopRiding();
        world.spawnParticles(ParticleTypes.SOUL, center.x, center.y, center.z,
                24, 0.55D, 0.65D, 0.55D, 0.035D);
        world.spawnParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y, center.z,
                18, 0.45D, 0.55D, 0.45D, 0.05D);
        world.playSound(null, summon.getBlockPos(), SoundEvents.BLOCK_SOUL_SAND_BREAK,
                SoundCategory.PLAYERS, 0.9F, 0.55F);
        summon.discard();

        refreshDeathListNow(player, state);
        SmoothClassesNetworking.sendAbilityState(player);
        return ExecutionResult.success(1, "Recalled " + AvengerSummonRecipes.friendlyEntityName(entityId)
                + " to the Death List. Soul and summon ingredients restored.");
    }

    private static void refundRecipeItem(ServerPlayerEntity player, String itemId) {
        if (itemId == null || itemId.isBlank()) return;
        Identifier id = new Identifier(itemId);
        if (!Registries.ITEM.containsId(id)) return;
        Item item = Registries.ITEM.get(id);
        ItemStack refund = new ItemStack(item, 1);
        if (!player.getInventory().insertStack(refund)) player.dropItem(refund, false);
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
    }

    public static boolean isAvengerSummon(Entity entity) {
        return entity != null && entity.getCommandTags().contains(SUMMON_TAG) && summonOwnerUuid(entity) != null;
    }

    public static boolean isOwnedSummon(ServerPlayerEntity owner, Entity entity) {
        return owner != null && isOwnedSummon(owner.getUuid(), entity);
    }

    public static boolean isOwnedSummon(UUID owner, Entity entity) {
        UUID uuid = summonOwnerUuid(entity);
        return owner != null && owner.equals(uuid);
    }

    public static UUID summonOwnerUuid(Entity entity) {
        if (entity == null || !entity.getCommandTags().contains(SUMMON_TAG)) return null;
        for (String tag : entity.getCommandTags()) {
            if (!tag.startsWith(OWNER_TAG_PREFIX)) continue;
            try { return UUID.fromString(tag.substring(OWNER_TAG_PREFIX.length())); }
            catch (IllegalArgumentException ignored) { return null; }
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
        boolean hasOwnedWardenNearby = summons.stream().anyMatch(WardenEntity.class::isInstance);
        if (hasOwnedWardenNearby && owner.hasStatusEffect(StatusEffects.DARKNESS)) {
            owner.removeStatusEffect(StatusEffects.DARKNESS);
        }

        Set<UUID> alliedSummonIds = new HashSet<>();
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
                String id = Registries.ENTITY_TYPE.getId(summon.getType()).toString();
                boolean dangerousBoss = id.equals("minecraft:warden") || id.equals("minecraft:wither") || id.equals("minecraft:ender_dragon");
                if (dangerousBoss) mob.setTarget(null);
                if (summon.squaredDistanceTo(owner) > 64D * 64D) {
                    summon.requestTeleport(owner.getX() + 1.5D, owner.getY(), owner.getZ() + 1.5D);
                } else if (!dangerousBoss && summon.squaredDistanceTo(owner) > 8D * 8D && mob.getTarget() == null) {
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
        DEVOURS.put(player.getUuid(), new DevourChannel(player, target.getUuid(), target.getWorld().getRegistryKey().getValue(), channelTicks));
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
        final ServerPlayerEntity owner;
        final UUID targetId;
        final Identifier worldId;
        int ticksLeft;

        DevourChannel(ServerPlayerEntity owner, UUID targetId, Identifier worldId, int ticksLeft) {
            this.owner = owner;
            this.targetId = targetId;
            this.worldId = worldId;
            this.ticksLeft = ticksLeft;
        }
    }
}
