package org.marj4n.smooth_classes.registry;

import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import org.marj4n.smooth_classes.SmoothClasses;
import org.marj4n.smooth_classes.block.ArcaneFireBlock;
import org.marj4n.smooth_classes.block.VampireCoffinBlock;

public final class SmoothBlocks {
    public static final Block VAMPIRE_COFFIN = new VampireCoffinBlock(
            FabricBlockSettings.copyOf(Blocks.DARK_OAK_PLANKS).strength(2.0F).nonOpaque());
    public static final Block ARCANE_FIRE = new ArcaneFireBlock(
            FabricBlockSettings.copyOf(Blocks.FIRE).noCollision().nonOpaque()
                    .luminance(15).dropsNothing());
    private SmoothBlocks() {}
    public static void register() {
        Registry.register(Registries.BLOCK, SmoothClasses.id("arcane_fire"), ARCANE_FIRE);
        Registry.register(Registries.BLOCK, SmoothClasses.id("vampire_coffin"), VAMPIRE_COFFIN);
    }
}
