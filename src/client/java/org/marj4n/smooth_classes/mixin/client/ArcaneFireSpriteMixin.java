package org.marj4n.smooth_classes.mixin.client;

import net.minecraft.client.resource.metadata.AnimationResourceMetadata;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.SpriteContents;
import net.minecraft.client.texture.SpriteDimensions;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Recolor only private sprite aliases; vanilla fire and its animation stay intact. */
@Mixin(SpriteContents.class)
public abstract class ArcaneFireSpriteMixin {
    @Inject(method = "<init>", at = @At("RETURN"))
    private void smooth$violetFire(Identifier id, SpriteDimensions dimensions,
            NativeImage image, AnimationResourceMetadata metadata, CallbackInfo ci) {
        if (!id.getNamespace().equals("smooth_classes")) return;
        String path = id.getPath();
        if (!path.equals("block/arcane_fire_0") && !path.equals("block/arcane_fire_1")
                && !path.equals("arcane_flame")) return;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int abgr = image.getColor(x, y);
                int alpha = abgr >>> 24;
                if (alpha == 0) continue;
                int red = abgr & 255;
                int green = (abgr >>> 8) & 255;
                int blue = (abgr >>> 16) & 255;
                float heat = (red + green + blue) / 765F;
                int r = (int) (100 + 145 * heat);
                int g = (int) (12 + 168 * heat * heat);
                int b = (int) (170 + 85 * heat);
                image.setColor(x, y, (alpha << 24) | (b << 16) | (g << 8) | r);
            }
        }
    }
}
