package org.marj4n.smooth_classes.origin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.BlazeEntity;
import net.minecraft.entity.mob.DrownedEntity;
import net.minecraft.entity.mob.ElderGuardianEntity;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MagmaCubeEntity;
import net.minecraft.entity.passive.BatEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

/**
 * Lightweight milestone tracker used by the Origin page.  Counters live in
 * {@link OriginState}, so adding later evolution requirements does not require
 * another player-data format change.
 */
public final class OriginProgressTracker {
    private OriginProgressTracker() {}

    public static void onKill(ServerPlayerEntity player, LivingEntity victim) {
        OriginState state = OriginRuntime.state(player);
        OriginType origin = state.origin();
        if (origin == null || victim == null) return;

        Identifier typeId = Registries.ENTITY_TYPE.getId(victim.getType());
        String species = typeId.toString();
        switch (origin) {
            case VAMPIRE -> {
                if (victim instanceof BatEntity) state.addProgress("vampire.bats_killed", 1);
                if (player.getWorld().isNight()) state.addProgress("vampire.night_kills", 1);
                VampireLordTrial.onKill(player, state, victim);
            }
            case WEREWOLF -> {
                state.addProgress("werewolf.prey_kills", 1);
                state.flag("werewolf.prey_species." + species);
                if (player.getWorld().isNight()) state.addProgress("werewolf.night_prey", 1);
            }
            case MERMAID -> {
                if (victim instanceof DrownedEntity drowned) {
                    state.addProgress("mermaid.drowned_kills", 1);
                    if (drowned.getMainHandStack().isOf(Items.TRIDENT) || drowned.getOffHandStack().isOf(Items.TRIDENT)) {
                        state.addProgress("mermaid.trident_drowned_kills", 1);
                    }
                }
                if (victim instanceof ElderGuardianEntity) state.addProgress("mermaid.elder_guardian_kills", 1);
                if (victim instanceof net.minecraft.entity.mob.GuardianEntity) state.addProgress("mermaid.guardian_kills", 1);
            }
            case DEMON -> {
                if (victim instanceof BlazeEntity) state.addProgress("demon.blaze_kills", 1);
                if (victim instanceof MagmaCubeEntity) state.addProgress("demon.magma_kills", 1);
                if (player.getWorld().getRegistryKey() == World.NETHER) state.addProgress("demon.nether_hostile_kills", 1);
            }
            case ANGEL -> {
                if (victim instanceof HostileEntity) {
                    state.addProgress("angel.hostile_kills", 1);
                    state.flag("angel.hostile_species." + species);
                }
            }
            case SLIME -> {
                if (isHumanoid(victim)) {
                    state.addProgress("slime.humanoid_residue", 1);
                    state.flag("slime.humanoid_species." + species);
                }
                if (victim instanceof SlimeEntity slime) {
                    state.addProgress("slime.slimes_defeated", 1);
                    if (slime.getSize() >= 4) state.addProgress("slime.large_slimes_defeated", 1);
                }
            }
            case UNDEAD -> {
                String path = typeId.getPath();
                if (path.contains("skeleton") || path.contains("stray")) {
                    state.addProgress("undead.skeletal_kills", 1);
                    state.flag("undead.skeletal_species." + species);
                }
            }
            case HUMAN -> {
                if (victim.getMaxHealth() >= 80.0F) {
                    state.addProgress("human.major_kills", 1);
                    state.flag("human.major_species." + species);
                }
            }
            default -> { }
        }
    }

    public static void onVampireFeed(ServerPlayerEntity player, LivingEntity victim, int bloodGain) {
        OriginState state = OriginRuntime.state(player);
        if (state.origin() != OriginType.VAMPIRE) return;
        state.addProgress("vampire.lifetime_blood", Math.max(0, bloodGain));
        if (isHumanoid(victim)) state.addProgress("vampire.humanoid_blood", Math.max(0, bloodGain));
        VampireBloodDiet.migrateLegacy(state);
        state.flag(VampireBloodDiet.DISCOVERY_PREFIX + VampireBloodDiet.type(victim).key());
        if (state.blood() >= 100) state.flag("vampire.filled_blood_once");
    }

    public static void onFoodUse(ServerPlayerEntity player, ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;
        OriginState state = OriginRuntime.state(player);
        OriginType origin = state.origin();
        if (origin == null) return;

        if (origin == OriginType.WEREWOLF && stack.getItem().getFoodComponent() != null
                && stack.getItem().getFoodComponent().isMeat()) {
            state.addProgress("werewolf.meat_eaten", 1);
            Identifier itemId = Registries.ITEM.getId(stack.getItem());
            state.flag("werewolf.meat_species." + itemId);
        }
        if (origin == OriginType.VOID && stack.isOf(Items.CHORUS_FRUIT)) {
            state.addProgress("void.chorus_uses", 1);
        }
    }

    /** Called once per second by OriginRuntime. */
    public static void tickSecond(ServerPlayerEntity player, OriginState state) {
        OriginType origin = state.origin();
        if (origin == null) return;

        switch (origin) {
            case HUMAN -> tickHuman(player, state);
            case VAMPIRE -> {
                if (player.getWorld().isNight()) state.addProgress("vampire.night_ticks", 20);
                if (state.progress("vampire.night_ticks") >= 10_000) state.flag("vampire.full_night_survived");
                VampireLordTrial.onVisit(player, state);
            }
            case MERMAID -> {
                if (player.isSubmergedInWater()) state.addProgress("mermaid.submerged_ticks", 20);
                else state.addProgress("mermaid.dry_ticks", 20);
                if (player.getInventory().contains(new ItemStack(Items.NAUTILUS_SHELL))) state.flag("mermaid.has_nautilus");
            }
            case DEMON -> {
                if (player.isInLava()) state.addProgress("demon.lava_ticks", 20);
            }
            case ANGEL -> {
                if (!player.isOnGround()) state.addProgress("angel.airborne_ticks", 20);
            }
            case SLIME -> {
                observeHumanoids(player, state, "slime.observe_ticks", "slime.observed_species.");
                if (!player.isOnGround()) state.addProgress("slime.airborne_ticks", 20);
                if (player.isSneaking()) state.addProgress("slime.squeeze_ticks", 20);
            }
            case VOID -> {
                if (player.getWorld().getRegistryKey() == World.END) state.addProgress("void.end_ticks", 20);
            }
            case UNDEAD -> state.progress("undead.soul_peak", Math.max(state.progress("undead.soul_peak"), state.soul()));
            case SPRIGGAN -> {
                var below = player.getWorld().getBlockState(player.getBlockPos().down());
                if (below.isIn(BlockTags.DIRT) || below.isIn(BlockTags.LEAVES)) {
                    state.addProgress("spriggan.nature_ticks", 20);
                }
            }
            case DOPPELGANGER -> observeForms(player, state);
            default -> { }
        }
    }

    private static void tickHuman(ServerPlayerEntity player, OriginState state) {
        if (player.getWorld().getRegistryKey() == World.NETHER) state.flag("human.dimension_visited.nether");
        else if (player.getWorld().getRegistryKey() == World.END) state.flag("human.dimension_visited.end");
        else state.flag("human.dimension_visited.overworld");

        String environment = null;
        if (player.getWorld().getRegistryKey() == World.NETHER) environment = "nether";
        else if (player.getWorld().getRegistryKey() == World.END) environment = "end";
        else if (player.isSubmergedInWater()) environment = "ocean";
        else if (player.getY() >= 160) environment = "sky";
        else if (player.isFrozen()) environment = "cold";

        if (environment == null) return;
        String key = "human.environment_ticks." + environment;
        int ticks = state.addProgress(key, 20);
        if (ticks >= 12_000) state.flag("human.environment_mastered." + environment); // 10 minutes
    }

    private static void observeHumanoids(ServerPlayerEntity player, OriginState state, String counter, String prefix) {
        var nearby = player.getWorld().getEntitiesByClass(LivingEntity.class, player.getBoundingBox().expand(8.0D),
                entity -> entity != player && entity.isAlive() && isHumanoid(entity));
        if (nearby.isEmpty()) return;
        state.addProgress(counter, 20);
        for (LivingEntity entity : nearby) {
            state.flag(prefix + Registries.ENTITY_TYPE.getId(entity.getType()));
        }
    }

    private static void observeForms(ServerPlayerEntity player, OriginState state) {
        var nearby = player.getWorld().getEntitiesByClass(LivingEntity.class, player.getBoundingBox().expand(8.0D),
                entity -> entity != player && entity.isAlive());
        for (LivingEntity entity : nearby) {
            Identifier id = Registries.ENTITY_TYPE.getId(entity.getType());
            String tickKey = "doppelganger.observe_ticks." + id;
            int ticks = state.addProgress(tickKey, 20);
            if (ticks >= 200) state.flag("doppelganger.form_studied." + id); // 10 cumulative seconds
        }
    }

    public static boolean isHumanoid(LivingEntity entity) {
        if (entity == null) return false;
        String path = Registries.ENTITY_TYPE.getId(entity.getType()).getPath();
        return path.contains("villager") || path.contains("illager") || path.contains("pillager")
                || path.contains("vindicator") || path.contains("evoker") || path.contains("witch")
                || path.contains("zombie") || path.contains("skeleton") || path.contains("piglin")
                || path.contains("player");
    }

}
