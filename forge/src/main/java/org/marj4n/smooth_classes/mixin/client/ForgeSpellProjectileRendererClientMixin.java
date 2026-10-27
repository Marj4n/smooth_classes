package org.marj4n.smooth_classes.mixin.client;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.client.render.SpellProjectileRenderer;
import net.spell_engine.entity.SpellProjectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Spell Engine's Forge renderer overrides the SRG method, not a method named render.
 * Explicit runtime selectors also avoid the annotation processor's generic-erasure L/; descriptor.
 */
@Mixin(value = SpellProjectileRenderer.class, remap = false)
public class ForgeSpellProjectileRendererClientMixin {
    @Inject(method = "m_7392_(Lnet/minecraft/world/entity/Entity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"), remap = false)
    private void smooth_classes$render(Entity entity, float yaw, float tickDelta, MatrixStack matrices,
            VertexConsumerProvider vertices, int light, CallbackInfo ci) {
        if (entity instanceof SpellProjectile projectile) {
            Spell.ProjectileModelComposite data = projectile.renderModels();
            if (data != null && data.models != null) {
                for (Spell.ProjectileModelComposite.Model model : data.models) {
                    if (model == null || model.fx == null || model.fx.model_id == null) continue;
                    String id = model.fx.model_id;
                    if (id.contains("swordfall") || id.contains("sword")) model.rotate_degrees_per_tick = 0;
                }
            }
        }
    }

    @ModifyArg(method = "renderComposite", at = @At(value = "INVOKE", remap = false,
            target = "Lnet/spell_engine/api/render/CustomModels;renderModel(Lnet/minecraft/client/renderer/RenderType;Lnet/spell_engine/mixin/client/render/ItemRendererAccessor;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/resources/model/BakedModel;)V"),
            index = 0, require = 0, remap = false)
    private static RenderLayer smooth_classes$heldItemCutout(RenderLayer original) {
        return RenderLayer.getEntityCutout(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE);
    }
}
