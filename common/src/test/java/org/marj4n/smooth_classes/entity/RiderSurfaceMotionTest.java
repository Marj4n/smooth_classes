package org.marj4n.smooth_classes.entity;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import static org.marj4n.smooth_classes.entity.RiderSurfaceMotion.*;

class RiderSurfaceMotionTest {
    @Test void surfaceBoundaryUsesActualFlowHeight() {
        double top = top(20, 0.625D);
        double threshold = top - 0.42D;
        assertEquals(Mode.RISING, mode(true, false, Math.nextDown(threshold), top));
        assertEquals(Mode.SUPPORTED, mode(true, false, threshold, top));
        assertEquals(Mode.SUPPORTED, mode(true, false, Math.nextUp(threshold), top));
        assertEquals(Mode.RISING, mode(true, true, 200, top));
        assertEquals(Mode.UNCHANGED, mode(false, false, 200, top));
    }
    @Test void fallbackNeverInflatesPositiveFluidHeight() {
        assertEquals(21, top(20, 0));
        assertEquals(21, top(20, -1));
        assertEquals(20.125D, top(20, 0.125D));
    }
    @Test void supportCancelsOnlyFallingMotion() {
        for (double speed : new double[]{-3, -Double.MIN_VALUE, -0.0D, 0, Double.MIN_VALUE, 3}) {
            assertEquals(speed < 0, replacesVelocity(Mode.SUPPORTED, speed));
            assertEquals(Double.doubleToRawLongBits(speed < 0 ? 0.0D : speed),
                    Double.doubleToRawLongBits(vertical(Mode.SUPPORTED, speed)));
            assertEquals(speed, horizontal(Mode.SUPPORTED, speed));
        }
    }
    @Test void risingDampsHorizontalMotionAndAddsBuoyancyAfterDamping() {
        assertEquals(-1.5D, horizontal(Mode.RISING, -3));
        assertEquals(-0.45D, vertical(Mode.RISING, -1));
        double speed = -2;
        for (int i=0; i<100; i++) speed = vertical(Mode.RISING, speed);
        assertEquals(0.1D, speed, 1e-15D);
        assertTrue(replacesVelocity(Mode.RISING, 0.1D));
        assertFalse(replacesVelocity(Mode.UNCHANGED, -3));
    }
}
