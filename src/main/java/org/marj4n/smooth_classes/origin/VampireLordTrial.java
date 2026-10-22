package org.marj4n.smooth_classes.origin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

/**
 * Final Vampire trial tied to Alex's Caves, without linking its Java classes.
 * Cave discovery and monster kills persist with the other Origin milestones.
 */
public final class VampireLordTrial {
    public static final String DISCOVERED_KEY = "vampire.found_forlorn_hollows";
    public static final String VESPER_KILLS_KEY = "vampire.forlorn_vesper_kills";
    public static final int REQUIRED_VESPERS = 5;
    public static final String FORSAKEN_KILLS_KEY = "vampire.forlorn_forsaken_kills";
    public static final int REQUIRED_FORSAKEN = 2;

    private static final Identifier VESPER_ID = new Identifier("alexscaves", "vesper");
    private static final Identifier FORSAKEN_ID = new Identifier("alexscaves", "forsaken");
    private static final RegistryKey<Biome> FORLORN_HOLLOWS = RegistryKey.of(
            RegistryKeys.BIOME, new Identifier("alexscaves", "forlorn_hollows"));

    private VampireLordTrial() {}

    public static boolean isForlornHollows(World world, BlockPos pos) {
        return world.getBiome(pos).matchesKey(FORLORN_HOLLOWS);
    }

    /** Visit the actual 3-D cave biome, not a structure/item or a spawned bat. */
    public static void onVisit(ServerPlayerEntity player, OriginState state) {
        if (isForlornHollows(player.getWorld(), player.getBlockPos()) && state.flag(DISCOVERED_KEY)) {
            player.sendMessage(Text.literal("Vampire Lord Trial: Forlorn Hollows discovered!")
                    .formatted(Formatting.LIGHT_PURPLE), true);
        }
    }

    /** Only actual Vesper and Forsaken kills INSIDE Forlorn Hollows count.
     *  The existing player-kill callback supplies the credited killer, so nearby
     *  kills or despawns cannot advance someone else's Vampire Lord trial.
     */
    public static void onKill(ServerPlayerEntity player, OriginState state, LivingEntity victim) {
        Identifier id = Registries.ENTITY_TYPE.getId(victim.getType());
        if (!VESPER_ID.equals(id) && !FORSAKEN_ID.equals(id)) return;
        if (!isForlornHollows(victim.getWorld(), victim.getBlockPos())) return;
        // If the very first tracked kill happens as the player enters the cave,
        // register discovery on this tick as well.
        onVisit(player, state);
        if (!state.hasFlag(DISCOVERED_KEY)) return;

        boolean vesper = VESPER_ID.equals(id);
        String key = vesper ? VESPER_KILLS_KEY : FORSAKEN_KILLS_KEY;
        int required = vesper ? REQUIRED_VESPERS : REQUIRED_FORSAKEN;
        int previous = state.progress(key);
        if (previous >= required) return; // Avoid replaying maxed-out progress messages.
        int kills = Math.min(required, previous + 1);
        state.progress(key, kills);
        player.sendMessage(Text.literal("Vampire Lord Trial: "
                + (vesper ? "Vespers" : "Forsaken") + " defeated "
                + kills + "/" + required).formatted(
                        kills == required ? Formatting.GREEN : Formatting.LIGHT_PURPLE), true);
    }

    public static boolean completed(OriginState state) {
        return state.hasFlag(DISCOVERED_KEY)
                && state.progress(VESPER_KILLS_KEY) >= REQUIRED_VESPERS
                && state.progress(FORSAKEN_KILLS_KEY) >= REQUIRED_FORSAKEN;
    }
}
