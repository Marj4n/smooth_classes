package org.marj4n.smooth_classes.client.origin.demon;
import net.minecraft.client.render.entity.*;
import net.minecraft.util.Identifier;
import org.marj4n.smooth_classes.entity.DemonSeatEntity;
public final class DemonSeatRenderer extends EntityRenderer<DemonSeatEntity>{
    public DemonSeatRenderer(EntityRendererFactory.Context ctx){super(ctx);}
    @Override public Identifier getTexture(DemonSeatEntity entity){return org.marj4n.smooth_classes.util.ResourceIds.parse("minecraft:textures/block/netherite_block.png");}
}
