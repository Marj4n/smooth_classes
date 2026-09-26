package org.marj4n.smooth_classes.client.renderer;

import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.util.Identifier;
import org.marj4n.smooth_classes.entity.SpellTargetEntity;
import org.marj4n.smooth_classes.SmoothClasses;

public final class SpellTargetRenderer extends EntityRenderer<SpellTargetEntity> {
    public SpellTargetRenderer(EntityRendererFactory.Context context) { super(context); }
    @Override public Identifier getTexture(SpellTargetEntity entity) { return SmoothClasses.id("textures/entity/spell_target.png"); }
}
