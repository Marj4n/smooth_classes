package org.marj4n.smooth_classes.client.effects;

import java.util.EnumMap;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.Identifier;
import net.spell_engine.api.effect.CustomModelStatusEffect;
import net.spell_engine.api.render.CustomLayers;
import net.spell_engine.api.render.CustomModels;
import net.spell_engine.api.render.LightEmission;
import org.marj4n.smooth_classes.SmoothClasses;

public final class SkillEffectRenderer implements CustomModelStatusEffect.Renderer {
    private static final EnumMap<SkillEffectVisual.Material, RenderLayer> LAYERS = layers();
    private final SkillEffectVisual visual;
    private final List<Identifier> models;

    public SkillEffectRenderer(SkillEffectVisual visual) {
        this.visual = visual;
        models = visual.passes().stream().map(pass -> SmoothClasses.id(pass.modelPath())).toList();
    }

    @Override
    public void renderEffect(long appliedAt, int amplifier, LivingEntity entity, float delta,
                             MatrixStack matrices, VertexConsumerProvider consumers, int light) {
        MinecraftClient client = MinecraftClient.getInstance();
        var frame = new SkillEffectPose.Frame(
                entity.getWorld().getTime() + delta, entity.age + client.getTickDelta(),
                entity.getWidth(), entity.getHeight(), entity.getScaleFactor(), entity.bodyYaw,
                entity.getPitch(), entity.isCrawling() || entity.isFallFlying()
                        || entity.isInSwimmingPose(), amplifier);
        SkillEffectPose.draw(visual, frame, matrices, (pass, pose) ->
                CustomModels.render(LAYERS.get(pass.material()), client.getItemRenderer(),
                        models.get(visual.passes().indexOf(pass)), pose, consumers, light, entity.getId()));
    }

    private static EnumMap<SkillEffectVisual.Material, RenderLayer> layers() {
        var result = new EnumMap<SkillEffectVisual.Material, RenderLayer>(SkillEffectVisual.Material.class);
        result.put(SkillEffectVisual.Material.TRANSLUCENT,
                RenderLayer.getEntityTranslucent(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE));
        result.put(SkillEffectVisual.Material.GLOW, CustomLayers.spellEffect(LightEmission.GLOW, false));
        result.put(SkillEffectVisual.Material.GLOW_CULL, CustomLayers.spellEffect(LightEmission.GLOW, true));
        result.put(SkillEffectVisual.Material.RADIATE, CustomLayers.spellEffect(LightEmission.RADIATE, false));
        return result;
    }
}
