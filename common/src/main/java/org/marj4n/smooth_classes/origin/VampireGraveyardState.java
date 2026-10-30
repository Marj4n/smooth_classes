package org.marj4n.smooth_classes.origin;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;
import java.util.*;

/** Overworld-owned ledger, independent of player Origin resets and respawns. */
public final class VampireGraveyardState extends PersistentState {
    private static final String SAVE_ID = "smooth_classes_vampire_graveyard";
    private BlockPos church;
    private int nextCandidate;
    private final List<Plot> plots = new ArrayList<>();
    private final Map<UUID, Plot> owners = new HashMap<>();

    public static final class Plot {
        private final BlockPos center;
        private final long fingerprint;
        private final boolean original;
        private UUID owner;
        private boolean built;
        private boolean blocked;
        private Plot(BlockPos center, long fingerprint, boolean original) {
            this.center = center.toImmutable(); this.fingerprint = fingerprint; this.original = original;
        }
        public BlockPos center() { return center; }
        public long fingerprint() { return fingerprint; }
        public boolean original() { return original; }
        public UUID owner() { return owner; }
        public boolean built() { return built; }
        public boolean available() { return owner == null && !blocked; }
    }

    public static VampireGraveyardState get(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager().getOrCreate(
                VampireGraveyardState::fromNbt, VampireGraveyardState::new, SAVE_ID);
    }
    public BlockPos church() { return church; }
    public boolean hasChurch() { return church != null; }
    public List<Plot> plots() { return Collections.unmodifiableList(plots); }
    public Plot ownedBy(UUID player) { return owners.get(player); }
    public int claimedCount() { return owners.size(); }
    public int availableCount() { return (int) plots.stream().filter(Plot::available).count(); }
    public int nextCandidate() { return nextCandidate; }
    public int takeCandidate() {
        if (nextCandidate >= VampireGraveyardLayout.MAX_CANDIDATES) return -1;
        markDirty(); return nextCandidate++;
    }
    public void establishChurch(BlockPos origin) {
        if (church != null && !church.equals(origin)) throw new IllegalStateException("This world already has a Vampire church");
        church = origin.toImmutable(); markDirty();
    }
    public Plot reserve(BlockPos center, long fingerprint, boolean original) {
        for (Plot existing : plots) {
            if (Math.abs((long) center.getX() - existing.center.getX()) <= 8
                    && Math.abs((long) center.getZ() - existing.center.getZ()) <= 8)
                throw new IllegalArgumentException("Grave footprints overlap");
        }
        Plot plot = new Plot(center, fingerprint, original);
        plots.add(plot); markDirty(); return plot;
    }
    public void block(Plot plot) { requireOwnedPlot(plot); plot.blocked = true; markDirty(); }
    public Plot claim(UUID owner, Plot plot) {
        Plot previous = owners.get(owner);
        if (previous != null) return previous;
        requireOwnedPlot(plot);
        if (!plot.available()) throw new IllegalStateException("Grave already claimed or disturbed");
        plot.owner = owner; owners.put(owner, plot); markDirty(); return plot;
    }
    public void finish(Plot plot) {
        requireOwnedPlot(plot);
        if (plot.owner == null) throw new IllegalStateException("Unclaimed grave");
        plot.built = true; markDirty();
    }
    private void requireOwnedPlot(Plot plot) {
        if (!plots.contains(plot)) throw new IllegalArgumentException("Foreign grave record");
    }

    public static VampireGraveyardState fromNbt(NbtCompound nbt) {
        VampireGraveyardState state = new VampireGraveyardState();
        if (nbt.contains("Church", NbtElement.LONG_TYPE)) state.church = BlockPos.fromLong(nbt.getLong("Church"));
        state.nextCandidate = Math.max(0, Math.min(VampireGraveyardLayout.MAX_CANDIDATES, nbt.getInt("NextCandidate")));
        NbtList list = nbt.getList("Plots", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < list.size(); i++) {
            NbtCompound entry = list.getCompound(i);
            if (!entry.contains("Center", NbtElement.LONG_TYPE)) continue;
            Plot plot = new Plot(BlockPos.fromLong(entry.getLong("Center")), entry.getLong("Fingerprint"), entry.getBoolean("Original"));
            plot.blocked = entry.getBoolean("Blocked");
            plot.built = entry.getBoolean("Built");
            if (entry.containsUuid("Owner")) {
                UUID owner = entry.getUuid("Owner");
                if (state.owners.containsKey(owner)) { plot.blocked = true; plot.built = false; }
                else { plot.owner = owner; state.owners.put(owner, plot); }
            }
            state.plots.add(plot);
        }
        return state;
    }
    @Override public NbtCompound writeNbt(NbtCompound nbt) {
        nbt.putInt("Version", 1);
        if (church != null) nbt.putLong("Church", church.asLong());
        nbt.putInt("NextCandidate", nextCandidate);
        NbtList list = new NbtList();
        for (Plot plot : plots) {
            NbtCompound entry = new NbtCompound();
            entry.putLong("Center", plot.center.asLong()); entry.putLong("Fingerprint", plot.fingerprint);
            entry.putBoolean("Original", plot.original); entry.putBoolean("Blocked", plot.blocked); entry.putBoolean("Built", plot.built);
            if (plot.owner != null) entry.putUuid("Owner", plot.owner);
            list.add(entry);
        }
        nbt.put("Plots", list); return nbt;
    }
}
