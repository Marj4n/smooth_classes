package org.marj4n.smooth_classes.util;
public final class LoadedChunks {
    public static boolean contains(net.minecraft.server.world.ServerWorld world, net.minecraft.util.math.BlockPos pos) {
        return world.getChunkManager().isChunkLoaded(pos.getX() >> 4, pos.getZ() >> 4);
    }
    private LoadedChunks() {}
}
