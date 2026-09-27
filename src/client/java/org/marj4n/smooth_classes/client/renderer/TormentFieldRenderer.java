package org.marj4n.smooth_classes.client.renderer;

import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.util.Identifier;
import org.marj4n.smooth_classes.entity.TormentFieldEntity;
import org.marj4n.smooth_classes.SmoothClasses;

public final class TormentFieldRenderer extends EntityRenderer<TormentFieldEntity> {
    public TormentFieldRenderer(EntityRendererFactory.Context context) { super(context); }
    @Override public Identifier getTexture(TormentFieldEntity entity) { return SmoothClasses.id("textures/entity/spell_target.png"); }
}
