package org.marj4n.smooth_classes.entity;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/** Smooth Classes flight solver. Inputs and memory are explicit; world mutation stays in the entity. */
public final class RiderFlightMotion {
    private static final float RAD = (float) (Math.PI / 180.0D);
    private static final float QUARTER_TURN = (float) (Math.PI / 2.0D);
    private RiderFlightMotion() {}

    public record Input(double forward, double strafe, float pitch, float yaw,
                        double movementSpeed, boolean up, boolean down, boolean boost, Vec3d velocity) {}
    public record Memory(double throttle, int momentumTicks) {}
    public record Step(Vec3d velocity, Memory memory) {}

    private record Regime(boolean active, boolean forward, double base, double sprint,
                          double target, double ceiling, double dive) {}

    public static Step step(Input input, Memory previous) {
        float pitch = pitch(input);
        double dive = MathHelper.clamp((Math.toDegrees(pitch) - 30.0D) / 40.0D, 0.0D, 1.0D);
        double speed = Math.max(0.20D, input.movementSpeed());
        double base = speed * 1.65D;
        double sprint = speed * 2.65D;
        double target = input.boost() ? sprint : base;
        Regime regime = new Regime(Math.abs(input.forward()) > 0.01D || Math.abs(input.strafe()) > 0.01D,
                input.forward() > 0.01D, base, sprint, target, Math.max(target, sprint) * 2.05D, dive);
        Memory next = advanceThrottle(regime, previous, input.boost());
        float yaw = input.yaw() * RAD;
        double horizontal = Math.cos(pitch);
        Vec3d requested = new Vec3d(
                -Math.sin(yaw) * horizontal * input.forward() + Math.cos(yaw) * input.strafe() * 0.45D,
                -Math.sin(pitch) * input.forward() * 1.35D,
                Math.cos(yaw) * horizontal * input.forward() + Math.sin(yaw) * input.strafe() * 0.45D);
        double length = requested.length();
        boolean diving = regime.forward() && dive > 0.0D;
        boolean pullUp = false;
        Vec3d motion;
        if (regime.active() && length > 0.01D) {
            Vec3d direction = new Vec3d(requested.x / length, requested.y / length, requested.z / length);
            pullUp = !diving && regime.forward() && direction.y > 0.01D && next.momentumTicks() > 0;
            if (pullUp) {
                double preserved = MathHelper.clamp(Math.max(input.velocity().length(), next.throttle()),
                        next.throttle(), Math.max(base, sprint) * 2.05D);
                motion = rotate(input.velocity(), direction, preserved, yaw);
            } else {
                Vec3d desired = direction.multiply(next.throttle());
                double response = diving ? 0.27D : 0.24D;
                motion = new Vec3d(approach(input.velocity().x, desired.x, response),
                        approach(input.velocity().y, desired.y, response),
                        approach(input.velocity().z, desired.z, response));
            }
        } else {
            motion = input.velocity().multiply(1.0D - 0.055D);
            if (motion.lengthSquared() < 0.0001D) motion = Vec3d.ZERO;
        }
        double vertical = motion.y;
        if (!diving && input.up() != input.down()) vertical += input.up() ? 0.090D : -0.125D;
        double upper = pullUp ? Math.max(0.85D, next.throttle()) : 0.85D;
        double lower = -Math.max(0.85D, diving ? next.throttle() : 0.0D);
        return new Step(new Vec3d(motion.x, MathHelper.clamp(vertical, lower, upper), motion.z), next);
    }

    private static float pitch(Input input) {
        float result = MathHelper.clamp(input.pitch(), -75.0F, 75.0F) * RAD;
        if (input.up() != input.down()) {
            float bias = (input.forward() > 0.01D ? 45.0F : 80.0F) * RAD * (input.up() ? -1.0F : 1.0F);
            result = Math.signum(result) != Math.signum(bias) && Math.abs(result) >= Math.abs(bias)
                    ? 0.0F : result + bias;
        }
        return MathHelper.clamp(result, -QUARTER_TURN, QUARTER_TURN);
    }

    private enum ThrottlePhase { COAST, DIVE, HOLD, BLEED, GLIDE, CRUISE }

    private static Memory advanceThrottle(Regime regime, Memory old, boolean boost) {
        double speed = MathHelper.clamp(old.throttle(), 0.0D, regime.ceiling());
        if (regime.active()) {
            speed = speed <= 0.0D ? regime.target()
                    : speed < regime.target() ? approach(speed, regime.target(), 0.24D) : speed;
        }
        ThrottlePhase phase = !regime.active() ? ThrottlePhase.COAST
                : regime.forward() && regime.dive() > 0.0D ? ThrottlePhase.DIVE
                : speed > regime.target() ? (regime.forward() && old.momentumTicks() > 0
                    ? ThrottlePhase.HOLD : ThrottlePhase.BLEED)
                : !boost && speed > 0.0D ? ThrottlePhase.GLIDE : ThrottlePhase.CRUISE;
        int ticks = old.momentumTicks();
        switch (phase) {
            case COAST -> { return new Memory(Math.max(0.0D, speed - 0.035D), ticks); }
            case DIVE -> {
                ticks = 50;
                double cap = approach(regime.target(), regime.ceiling(), regime.dive());
                if (speed < cap) speed = Math.min(cap, speed + regime.target() * 0.27D * 0.16D * regime.dive());
            }
            case HOLD -> ticks--;
            case BLEED -> speed = Math.max(regime.target(), speed - regime.target() * 0.035D);
            case GLIDE -> speed = Math.max(0.0D, speed - regime.target() * 0.025D);
            case CRUISE -> {}
        }
        return new Memory(MathHelper.clamp(speed, 0.0D, regime.ceiling()), ticks);
    }

    private static double approach(double start, double target, double fraction) {
        return MathHelper.lerp(fraction, start, target);
    }

    /** Rodrigues rotation with an explicit antiparallel fallback; retain the established turn rate. */
    private static Vec3d rotate(Vec3d current, Vec3d desired, double speed, float yaw) {
        Vec3d end = desired.normalize();
        if (current.lengthSquared() < 1.0E-8D) return end.multiply(speed);
        Vec3d start = current.normalize();
        double cosine = MathHelper.clamp(start.dotProduct(end), -1.0D, 1.0D);
        if (cosine > 0.9999D) return end.multiply(speed);
        Vec3d axis = start.crossProduct(end);
        if (axis.lengthSquared() < 1.0E-8D) {
            axis = start.crossProduct(new Vec3d(-Math.sin(yaw), 0.0D, Math.cos(yaw)));
            if (axis.lengthSquared() < 1.0E-8D) axis = new Vec3d(1.0D, 0.0D, 0.0D);
        }
        axis = axis.normalize();
        double angle = Math.acos(cosine) * MathHelper.clamp(0.24D, 0.0D, 1.0D);
        double c = Math.cos(angle);
        double s = Math.sin(angle);
        Vec3d rotated = start.multiply(c).add(axis.crossProduct(start).multiply(s))
                .add(axis.multiply(axis.dotProduct(start) * (1.0D - c)));
        return rotated.normalize().multiply(speed);
    }
}
