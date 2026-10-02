package org.marj4n.smooth_classes.registry;

import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import org.marj4n.smooth_classes.SmoothClasses;

public final class SmoothItems {
    public static final Item SACRED_BANNER_ICON = new Item(new Item.Settings().maxCount(1));

    private SmoothItems() {}

    public static void register() {
        Registry.register(Registries.ITEM, SmoothClasses.id("sacred_banner_icon"), SACRED_BANNER_ICON);
    }
}
