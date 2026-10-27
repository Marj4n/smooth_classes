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

/** Masks the supplied screenshot's neutral backdrop only in our private atlas sprite. */
@Mixin(SpriteContents.class)
public abstract class SacredBannerSpriteMixin {
    @Inject(method="<init>",at=@At("RETURN"))
    private void smooth$bannerMask(Identifier id, SpriteDimensions dimensions, NativeImage image,
                                   AnimationResourceMetadata metadata, CallbackInfo ci) {
        if (id.equals(new Identifier("smooth_classes","item/sacred_banner_white"))) {
            maskOuterBackground(image);
            return;
        }
        boolean goldenCloth = id.equals(new Identifier("smooth_classes","item/sacred_banner_gold"));
        if (!goldenCloth && !id.equals(new Identifier("smooth_classes","item/sacred_banner_source"))) return;
        for (int y=0;y<image.getHeight();y++) {
            for (int x=0;x<image.getWidth();x++) {
                int color=image.getColor(x,y);
                int r=color&255,g=(color>>>8)&255,b=(color>>>16)&255;
                if (r>=225 && g>=225 && b>=225) image.setColor(x,y,0);
                else if (goldenCloth && r<110 && g<110 && b<110) {
                    // Replace the black outline with cloth gold, preserving alpha.
                    image.setColor(x,y,(color & 0xFF000000) | (20 << 16) | (190 << 8) | 240);
                }
            }
        }
    }
    /** Remove only pale background connected to the image edges, preserving white fabric. */
    private static void maskOuterBackground(NativeImage image) {
        int width=image.getWidth(), height=image.getHeight();
        boolean[] visited=new boolean[width*height];
        java.util.ArrayDeque<Integer> queue=new java.util.ArrayDeque<>();
        for(int x=0;x<width;x++){queue.add(x);queue.add((height-1)*width+x);}
        for(int y=0;y<height;y++){queue.add(y*width);queue.add(y*width+width-1);}
        while(!queue.isEmpty()){
            int index=queue.removeFirst();
            if(visited[index])continue;
            visited[index]=true;
            int x=index%width,y=index/width,color=image.getColor(x,y);
            int red=color&255,green=(color>>>8)&255,blue=(color>>>16)&255;
            if((color>>>24)!=0 && !(red>=225 && green>=225 && blue>=225))continue;
            image.setColor(x,y,0);
            if(x>0)queue.add(index-1);
            if(x+1<width)queue.add(index+1);
            if(y>0)queue.add(index-width);
            if(y+1<height)queue.add(index+width);
        }
    }

}
