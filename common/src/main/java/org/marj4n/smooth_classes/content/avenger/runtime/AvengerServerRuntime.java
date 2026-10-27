package org.marj4n.smooth_classes.content.avenger.runtime;

/** Server lifecycle entry point for the reworked Avenger. */
public final class AvengerServerRuntime {
    private AvengerServerRuntime() {}
    public static void register() { AvengerReworkRuntime.register(); }
}
