package org.marj4n.smooth_classes.content.archer.runtime;

import net.minecraft.item.*;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import org.marj4n.smooth_classes.SmoothClasses;
import org.marj4n.smooth_classes.content.archer.ArcherClass;
import org.marj4n.smooth_classes.entity.BladePortalEntity;
import org.marj4n.smooth_classes.network.SmoothClassesNetworking;
import org.marj4n.smooth_classes.integration.PuffishSkillsIntegration;
import org.marj4n.smooth_classes.integration.SkillNodeIds;
import org.marj4n.smooth_classes.effects.SmoothEffects;
import net.minecraft.entity.effect.StatusEffectInstance;
import org.marj4n.smooth_classes.registry.SmoothEntities;
import org.marj4n.smooth_classes.runtime.AbilityRuntime;
import org.marj4n.smooth_classes.runtime.ExecutionResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Archer H special: persistent weapon Treasury with talent-expandable capacity.
 *
 * - H while holding a supported weapon/arrow stack stores that exact stack.
 * - H on an empty selected hotbar slot opens the Treasury selector.
 * - Release H on a stored stack to physically draw it back through the portal.
 *
 * The Treasury itself is persistent via {@link ArcherTreasuryState}; this runtime only
 * owns short-lived portal / hand-animation transaction state.
 */
public final class ArcherSpecialRuntime {
    public static final Identifier ID = SmoothClasses.id("class_special_treasury_key");
    private static final int PORTAL_OPEN_TICKS = 7;
    private static final int PORTAL_CLOSE_TICKS = 9;
    private static final int DEPOSIT_COMMIT_TICKS = 10;
    private static final int WITHDRAW_COMMIT_TICKS = 7;
    private static final int DEPOSIT_CLOSE_DELAY_TICKS = 15;
    private static final int WITHDRAW_CLOSE_DELAY_TICKS = 13;
    private static final int SLOT_LOCK_TICKS = 18;

    private enum Action { NONE, DEPOSIT, WITHDRAW }

    private static final class State {
        boolean selectorHeld;
        long portalOpenedAt;
        long portalCloseAt;
        long portalDiscardAt;
        Action action = Action.NONE;
        long actionAt;
        long lockSlotUntil;
        int hotbarSlot = -1;
        int treasurySlot = -1;
        ItemStack pendingStack = ItemStack.EMPTY;
        final List<BladePortalEntity> portals = new ArrayList<>();
    }

    private static final Map<UUID, State> STATES = new HashMap<>();

    private ArcherSpecialRuntime() {}

    /** H with a non-empty selected hotbar stack means "store this in the Treasury". */
    public static ExecutionResult activate(ServerPlayerEntity player, boolean ignored) {
        if (!AbilityRuntime.isClass(player, ArcherClass.ID))
            return ExecutionResult.failure("Only Archer can use the Treasury.");

        State state = STATES.computeIfAbsent(player.getUuid(), ignoredState -> new State());
        if (busy(state)) return ExecutionResult.failure("The Treasury is already moving an item.");

        int hotbar = player.getInventory().selectedSlot;
        ItemStack held = player.getInventory().getStack(hotbar);
        if (held.isEmpty()) return ExecutionResult.failure("Hold H on an empty hotbar slot to draw from the Treasury.");
        if (!isAllowedTreasuryItem(held)) return ExecutionResult.failure("Only weapons and arrows can be stored in the Treasury.");

        ArcherTreasuryState treasury = ArcherTreasuryState.get(player.getServer());
        int capacity = treasuryCapacity(player);
        if (treasury.firstEmpty(player.getUuid(), capacity) < 0)
            return ExecutionResult.failure("The Treasury is full (" + capacity + "/" + capacity + ").");

        long now = player.getServer().getTicks();
        state.action = Action.DEPOSIT;
        state.actionAt = now + DEPOSIT_COMMIT_TICKS;
        state.portalCloseAt = now + DEPOSIT_CLOSE_DELAY_TICKS;
        state.portalDiscardAt = state.portalCloseAt + PORTAL_CLOSE_TICKS;
        state.portalOpenedAt = now;
        state.hotbarSlot = hotbar;
        state.treasurySlot = -1;
        state.pendingStack = held.copy();
        state.lockSlotUntil = now + SLOT_LOCK_TICKS;
        state.selectorHeld = false;

        discardPortals(state);
        spawnRightRearPortal(player, state);
        forceHotbarSlot(player, hotbar);
        playAnimation(player, "treasury_store");
        player.getServerWorld().playSound(null, player.getBlockPos(), SoundEvents.BLOCK_BEACON_ACTIVATE,
                SoundCategory.PLAYERS, 0.52F, 1.45F);
        return ExecutionResult.success(1, "Storing item in the Treasury.");
    }

    /** Called by the hold-H radial while the currently selected hotbar slot is empty. */
    public static void hold(ServerPlayerEntity player, boolean held) {
        if (!AbilityRuntime.isClass(player, ArcherClass.ID)) return;
        State state = STATES.computeIfAbsent(player.getUuid(), ignored -> new State());
        if (busy(state)) return;

        long now = player.getServer().getTicks();
        if (held) {
            if (!player.getMainHandStack().isEmpty()) return;
            if (!ArcherTreasuryState.get(player.getServer()).hasAny(player.getUuid(), treasuryCapacity(player))) return;
            if (!state.selectorHeld) {
                state.selectorHeld = true;
                state.portalOpenedAt = now;
                state.portalCloseAt = 0L;
                state.portalDiscardAt = 0L;
                discardPortals(state);
                spawnRightRearPortal(player, state);
            }
        } else if (state.selectorHeld) {
            state.selectorHeld = false;
            state.portalCloseAt = now;
            state.portalDiscardAt = now + PORTAL_CLOSE_TICKS;
        }
    }

    /** Release H on a usable Treasury slot to draw that real ItemStack. */
    public static ExecutionResult select(ServerPlayerEntity player, int index) {
        if (!AbilityRuntime.isClass(player, ArcherClass.ID))
            return ExecutionResult.failure("Only Archer can use the Treasury.");
        if (index < 0 || index >= treasuryCapacity(player))
            return ExecutionResult.failure("No Treasury slot selected.");

        State state = STATES.computeIfAbsent(player.getUuid(), ignored -> new State());
        if (busy(state)) return ExecutionResult.failure("The Treasury is already moving an item.");

        int hotbar = player.getInventory().selectedSlot;
        if (!player.getInventory().getStack(hotbar).isEmpty())
            return ExecutionResult.failure("Select an empty hotbar slot before drawing from the Treasury.");

        ArcherTreasuryState treasury = ArcherTreasuryState.get(player.getServer());
        ItemStack withdrawn = treasury.get(player.getUuid(), index);
        if (withdrawn.isEmpty()) return ExecutionResult.failure("That Treasury slot is empty.");

        long now = player.getServer().getTicks();
        state.selectorHeld = false;
        state.action = Action.WITHDRAW;
        state.actionAt = now + WITHDRAW_COMMIT_TICKS;
        state.portalCloseAt = now + WITHDRAW_CLOSE_DELAY_TICKS;
        state.portalDiscardAt = state.portalCloseAt + PORTAL_CLOSE_TICKS;
        state.hotbarSlot = hotbar;
        state.treasurySlot = index;
        state.pendingStack = withdrawn;
        state.lockSlotUntil = now + SLOT_LOCK_TICKS;

        // Reuse the already-open selector portal if it exists. If packet timing caused it
        // to disappear, recreate one and open it from its current state.
        if (state.portals.isEmpty()) {
            state.portalOpenedAt = now - PORTAL_OPEN_TICKS;
            spawnRightRearPortal(player, state);
        }
        for (BladePortalEntity portal : state.portals) {
            if (portal != null && !portal.isRemoved()) portal.setOpening(1F);
        }

        forceHotbarSlot(player, hotbar);
        playAnimation(player, "treasury_draw");
        player.getServerWorld().playSound(null, player.getBlockPos(), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME,
                SoundCategory.PLAYERS, 0.60F, 1.22F);
        return ExecutionResult.success(1, "Drawing item from the Treasury.");
    }

    public static void tick(ServerPlayerEntity player) {
        State state = STATES.get(player.getUuid());
        if (state == null) return;

        if (!AbilityRuntime.isClass(player, ArcherClass.ID) || !player.isAlive()) {
            discardPortals(state);
            STATES.remove(player.getUuid());
            return;
        }

        long now = player.getServer().getTicks();
        state.portals.removeIf(net.minecraft.entity.Entity::isRemoved);

        if (state.lockSlotUntil > now && state.hotbarSlot >= 0) forceHotbarSlot(player, state.hotbarSlot);

        updatePortalAnimation(player, state, now);

        if (state.action != Action.NONE && now >= state.actionAt) {
            if (state.action == Action.DEPOSIT) commitDeposit(player, state);
            else commitWithdraw(player, state);
            state.action = Action.NONE;
            state.actionAt = 0L;
        }

        if (!state.selectorHeld && state.action == Action.NONE && state.portals.isEmpty()) {
            state.pendingStack = ItemStack.EMPTY;
            state.hotbarSlot = -1;
            state.treasurySlot = -1;
            STATES.remove(player.getUuid());
        }
    }

    private static void commitDeposit(ServerPlayerEntity player, State state) {
        if (state.hotbarSlot < 0 || state.hotbarSlot >= 9 || state.pendingStack.isEmpty()) return;
        ItemStack current = player.getInventory().getStack(state.hotbarSlot);
        boolean same = !current.isEmpty()
                && current.getCount() == state.pendingStack.getCount()
                && ItemStack.canCombine(current, state.pendingStack);
        if (!same) return; // Item changed during the animation; never duplicate it.

        ArcherTreasuryState treasury = ArcherTreasuryState.get(player.getServer());
        int deposited = treasury.deposit(player.getUuid(), current.copy(), treasuryCapacity(player));
        if (deposited < 0) return;

        player.getInventory().setStack(state.hotbarSlot, ItemStack.EMPTY);
        player.getInventory().markDirty();
        player.swingHand(Hand.MAIN_HAND, true);
        state.pendingStack = ItemStack.EMPTY;
        state.treasurySlot = deposited;
        SmoothClassesNetworking.sendAbilityState(player);
    }

    private static void commitWithdraw(ServerPlayerEntity player, State state) {
        if (state.hotbarSlot < 0 || state.hotbarSlot >= 9 || state.pendingStack.isEmpty()) return;
        ItemStack current = player.getInventory().getStack(state.hotbarSlot);
        if (!current.isEmpty()) return;

        ArcherTreasuryState treasury = ArcherTreasuryState.get(player.getServer());
        ItemStack withdrawn = treasury.withdraw(player.getUuid(), state.treasurySlot);
        if (withdrawn.isEmpty()) return;
        if (!ItemStack.canCombine(withdrawn, state.pendingStack) || withdrawn.getCount() != state.pendingStack.getCount()) {
            // Unexpected mutation: put the real stack back rather than risking loss/duplication.
            treasury.putAt(player.getUuid(), state.treasurySlot, withdrawn);
            return;
        }

        player.getInventory().setStack(state.hotbarSlot, withdrawn);
        player.getInventory().markDirty();
        player.swingHand(Hand.MAIN_HAND, true);
        if (PuffishSkillsIntegration.isSkillUnlocked(PuffishSkillsIntegration.ARCHER,
                SkillNodeIds.rangerRoyalDraw, player)) {
            addMarksmanship(player, 100, 1, 3);
        }
        state.pendingStack = ItemStack.EMPTY;
        state.treasurySlot = -1;
        SmoothClassesNetworking.sendAbilityState(player);
    }

    private static void updatePortalAnimation(ServerPlayerEntity player, State state, long now) {
        if (state.portals.isEmpty()) return;

        float opening;
        if (state.selectorHeld || (state.action != Action.NONE && now < state.portalCloseAt)) {
            opening = Math.min(1F, Math.max(0F, (now - state.portalOpenedAt + 1L) / (float) PORTAL_OPEN_TICKS));
        } else if (state.portalDiscardAt > 0L) {
            opening = Math.max(0F, (state.portalDiscardAt - now) / (float) PORTAL_CLOSE_TICKS);
        } else {
            opening = 1F;
        }

        for (BladePortalEntity portal : state.portals) {
            if (portal == null || portal.isRemoved()) continue;
            portal.followCaster();
            portal.setOpening(opening);
        }

        if (!state.selectorHeld && state.portalDiscardAt > 0L && now >= state.portalDiscardAt) {
            discardPortals(state);
            state.portalCloseAt = 0L;
            state.portalDiscardAt = 0L;
        }
    }

    private static void spawnRightRearPortal(ServerPlayerEntity player, State state) {
        BladePortalEntity portal = new BladePortalEntity(SmoothEntities.BLADE_PORTAL, player.getServerWorld());
        portal.setCaster(player);
        portal.setSide(0);
        // Close enough for the right hand to visibly enter it, but still behind the shoulder.
        portal.setPlacement(0.90D, -1.46D, -0.02D);
        portal.setScale(0.84F);
        portal.setOpening(0F);
        portal.followCaster();
        state.portals.add(portal);
        if (!player.getServerWorld().spawnEntity(portal)) state.portals.remove(portal);
    }

    private static void forceHotbarSlot(ServerPlayerEntity player, int slot) {
        int safe = Math.max(0, Math.min(8, slot));
        if (player.getInventory().selectedSlot != safe) player.getInventory().selectedSlot = safe;
        SmoothClassesNetworking.forceHotbarSlot(player, safe);
    }

    private static void playAnimation(ServerPlayerEntity player, String animationId) {
        var entry = org.marj4n.smooth_classes.runtime.InternalSpellRuntime.entry(
                player, SmoothClasses.id(animationId));
        if (entry == null || entry.value().release == null || entry.value().release.animation == null) return;
        var viewers = new ArrayList<>(net.fabricmc.fabric.api.networking.v1.PlayerLookup.tracking(player));
        if (!viewers.contains(player)) viewers.add(player);
        net.spell_engine.utils.AnimationHelper.sendAnimation(player, viewers,
                net.spell_engine.internals.casting.SpellCast.Animation.RELEASE,
                entry.value().release.animation, 1F);
    }

    public static boolean isAllowedTreasuryItem(ItemStack stack) {
        if (stack == null || stack.isEmpty() || stack.getItem() instanceof BlockItem) return false;
        Item item = stack.getItem();
        if (item instanceof ArrowItem || item instanceof SwordItem || item instanceof AxeItem
                || item instanceof TridentItem || item instanceof BowItem || item instanceof CrossbowItem) return true;

        // Compatibility fallback for modded weapon classes that don't subclass vanilla weapon items.
        String path = Registries.ITEM.getId(item).getPath().toLowerCase(Locale.ROOT);
        return containsWeaponWord(path, "sword") || containsWeaponWord(path, "blade")
                || containsWeaponWord(path, "dagger") || containsWeaponWord(path, "spear")
                || containsWeaponWord(path, "lance") || containsWeaponWord(path, "katana")
                || containsWeaponWord(path, "cutlass") || containsWeaponWord(path, "rapier")
                || containsWeaponWord(path, "glaive") || containsWeaponWord(path, "scythe")
                || containsWeaponWord(path, "halberd") || containsWeaponWord(path, "mace")
                || containsWeaponWord(path, "hammer") || containsWeaponWord(path, "sai")
                || containsWeaponWord(path, "twinblade") || containsWeaponWord(path, "warglaive")
                || containsWeaponWord(path, "chakram") || containsWeaponWord(path, "claymore")
                || containsWeaponWord(path, "greataxe") || containsWeaponWord(path, "greathammer")
                || containsWeaponWord(path, "bow") || containsWeaponWord(path, "arrow");
    }

    private static boolean containsWeaponWord(String path, String word) {
        return path.equals(word) || path.startsWith(word + "_") || path.endsWith("_" + word)
                || path.contains("_" + word + "_");
    }

    private static boolean busy(State state) { return state.action != Action.NONE; }

    public static int treasuryCapacity(ServerPlayerEntity player) {
        int capacity = ArcherTreasuryState.BASE_SLOT_COUNT;
        if (PuffishSkillsIntegration.isSkillUnlocked(PuffishSkillsIntegration.ARCHER, SkillNodeIds.rangerTreasuryExpansionI, player)) capacity++;
        if (PuffishSkillsIntegration.isSkillUnlocked(PuffishSkillsIntegration.ARCHER, SkillNodeIds.rangerTreasuryExpansionII, player)) capacity++;
        if (PuffishSkillsIntegration.isSkillUnlocked(PuffishSkillsIntegration.ARCHER, SkillNodeIds.rangerTreasuryExpansionIII, player)) capacity++;
        if (PuffishSkillsIntegration.isSkillUnlocked(PuffishSkillsIntegration.ARCHER, SkillNodeIds.rangerTreasuryExpansionIV, player)) capacity++;
        return Math.min(ArcherTreasuryState.MAX_SLOT_COUNT, capacity);
    }

    public static ItemStack[] treasurySlots(ServerPlayerEntity player) {
        return ArcherTreasuryState.get(player.getServer()).snapshot(player.getUuid());
    }

    private static void addMarksmanship(ServerPlayerEntity player, int duration, int stacks, int maxStacks) {
        StatusEffectInstance current = player.getStatusEffect(SmoothEffects.MARKSMANSHIP);
        int amplifier = current == null ? Math.max(0, stacks - 1)
                : Math.min(maxStacks - 1, current.getAmplifier() + stacks);
        player.addStatusEffect(new StatusEffectInstance(SmoothEffects.MARKSMANSHIP,
                duration, amplifier, false, false, true));
    }

    public static boolean drawing(ServerPlayerEntity player) {
        State state = STATES.get(player.getUuid());
        return state != null && state.action != Action.NONE;
    }
    public static void cleanup(ServerPlayerEntity player) {
        State state = STATES.remove(player.getUuid());
        if (state != null) {
            discardPortals(state);
        }
    }

    public static void clear() {
        for (State state : STATES.values()) discardPortals(state);
        STATES.clear();
    }

    public static boolean ownsPortal(BladePortalEntity portal) {
        if (portal == null) return false;
        for (State state : STATES.values()) if (state.portals.contains(portal)) return true;
        return false;
    }

    private static void discardPortals(State state) {
        for (BladePortalEntity portal : state.portals) {
            if (portal != null && !portal.isRemoved()) portal.discard();
        }
        state.portals.clear();
    }
}
