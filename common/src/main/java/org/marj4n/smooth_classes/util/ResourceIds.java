package org.marj4n.smooth_classes.util;
import net.minecraft.util.Identifier;
/** Strict resource-id parsing shared by Fabric and Forge 1.20.1. */
public final class ResourceIds {
    public static Identifier parse(String value) {
        int separator = value.indexOf(':');
        String namespace = separator <= 0 ? "minecraft" : value.substring(0, separator);
        String path = separator < 0 ? value : value.substring(separator + 1);
        return Identifier.of(namespace, path);
    }
    private ResourceIds() {}
}
