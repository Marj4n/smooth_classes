package org.marj4n.smooth_classes.client.renderer;

import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.util.Identifier;
import org.marj4n.smooth_classes.entity.TormentFieldEntity;
import org.marj4n.smooth_classes.SmoothClasses;

public final class TormentFieldRenderer extends EntityRenderer<TormentFieldEntity> {
    public TormentFieldRenderer(EntityRendererFactory.Context context) { super(context); }
    // This invisible marker renders no geometry; keep its required texture identifier valid.
    @Override public Identifier getTexture(TormentFieldEntity entity) { return Identifier.of("minecraft", "textures/block/white_concrete.png"); }
}
