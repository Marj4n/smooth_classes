package org.marj4n.smooth_classes.entity;

import static org.junit.jupiter.api.Assertions.*;
import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.Test;

class RiderFlightMotionTest {
    private static RiderFlightMotion.Input input(double forward, float pitch, boolean up, boolean down) {
        return new RiderFlightMotion.Input(forward, 0, pitch, 0, .35D, up, down, false, Vec3d.ZERO);
    }

    @Test void simultaneousVerticalKeysCancelEachOther() {
        var memory = new RiderFlightMotion.Memory(0, 0);
        assertEquals(RiderFlightMotion.step(input(1, 0, false, false), memory),
                RiderFlightMotion.step(input(1, 0, true, true), memory));
    }

    @Test void hoveringRetainsDistinctAscendAndDescendThrust() {
        var memory = new RiderFlightMotion.Memory(0, 0);
        assertEquals(.090D, RiderFlightMotion.step(input(0, 0, true, false), memory).velocity().y);
        assertEquals(-.125D, RiderFlightMotion.step(input(0, 0, false, true), memory).velocity().y);
        assertEquals(Vec3d.ZERO, RiderFlightMotion.step(input(0, 0, false, false), memory).velocity());
    }

    @Test void diveBuildsFiftyTickMomentumAndForwardPullUpConsumesOne() {
        var dive = RiderFlightMotion.step(input(1, 70, false, false), new RiderFlightMotion.Memory(0, 0));
        assertEquals(50, dive.memory().momentumTicks());
        var pullUp = new RiderFlightMotion.Input(1, 0, -45, 0, .35D, false, false, false, dive.velocity());
        assertEquals(49, RiderFlightMotion.step(pullUp, dive.memory()).memory().momentumTicks());
    }

    @Test void exactForwardDeadzoneDoesNotStartDiveMomentum() {
        assertEquals(0, RiderFlightMotion.step(input(.01D, 70, false, false),
                new RiderFlightMotion.Memory(0, 0)).memory().momentumTicks());
        assertEquals(50, RiderFlightMotion.step(input(Math.nextUp(.01D), 70, false, false),
                new RiderFlightMotion.Memory(0, 0)).memory().momentumTicks());
    }
}
