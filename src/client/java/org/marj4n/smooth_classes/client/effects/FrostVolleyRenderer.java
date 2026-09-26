package org.marj4n.smooth_classes.client.effects;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.util.Identifier;
import net.spell_engine.api.render.CustomLayers;
import net.spell_engine.api.render.LightEmission;
import org.marj4n.smooth_classes.SmoothClasses;

import java.util.List;

public class FrostVolleyRenderer extends OrbitingEffectRenderer {
    public static final Identifier modelId_base = new Identifier(SmoothClasses.MOD_ID, "spell_projectile/ice_projectile");
    public static final Identifier modelId_overlay = new Identifier(SmoothClasses.MOD_ID, "spell_projectile/ice_projectile");

    private static final RenderLayer BASE_RENDER_LAYER =
            RenderLayer.getEntityTranslucent(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE);
    private static final RenderLayer OVERLAY_RENDER_LAYER =
            CustomLayers.spellEffect(LightEmission.GLOW, true);


    @Override
    public void setSpeed(float newSpeed) {
        newSpeed = 7f;
        super.setSpeed(newSpeed);
    }

    public FrostVolleyRenderer() {
        super(List.of(
                new Model(OVERLAY_RENDER_LAYER, modelId_overlay)),
                0.4F,
                1.55F);
    }

}
