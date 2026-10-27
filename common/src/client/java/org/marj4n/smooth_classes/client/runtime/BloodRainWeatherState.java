package org.marj4n.smooth_classes.client.runtime;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.world.ClientWorld;
import org.marj4n.smooth_classes.entity.BloodRainEntity;

/**
 * Client-only lookup for local blood-rain storms.
 *
 * Vanilla precipitation is rendered around the camera rather than as world entities,
 * so the renderer needs a cheap way to know whether the current camera is inside one
 * of our local 64-block storms. The loaded entity list is scanned at most once per
 * client world tick; render frames reuse the tiny cached storm list.
 */
public final class BloodRainWeatherState {
    private static final List<BloodRainEntity> ACTIVE_STORMS = new ArrayList<>(2);

    private static ClientWorld cachedWorld;
    private static long cachedWorldTime = Long.MIN_VALUE;

    private BloodRainWeatherState() {
    }

    public static boolean isInsideStorm(ClientWorld world, double x, double z) {
        refresh(world);

        for (BloodRainEntity storm : ACTIVE_STORMS) {
            if (!storm.isRemoved() && storm.inside(x - storm.getX(), z - storm.getZ())) {
                return true;
            }
        }
        return false;
    }

    private static void refresh(ClientWorld world) {
        long worldTime = world.getTime();
        if (cachedWorld == world && cachedWorldTime == worldTime) {
            return;
        }

        cachedWorld = world;
        cachedWorldTime = worldTime;
        ACTIVE_STORMS.clear();

        for (var entity : world.getEntities()) {
            if (entity instanceof BloodRainEntity storm && !storm.isRemoved()) {
                ACTIVE_STORMS.add(storm);
            }
        }
    }
}
