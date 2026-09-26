package org.marj4n.smooth_classes.mixin.client;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.FlyingItemEntity;
import net.minecraft.util.Identifier;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.client.render.SpellProjectileRenderer;
import net.spell_engine.entity.SpellProjectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SpellProjectileRenderer.class)
public class SpellProjectileRendererClientMixin<T extends Entity & FlyingItemEntity> extends EntityRenderer<T> {
    protected SpellProjectileRendererClientMixin(EntityRendererFactory.Context ctx){ super(ctx); }

    @Inject(at=@At("HEAD"), method="render")
    private void smooth_classes$render(T entity,float yaw,float tickDelta,MatrixStack matrices,
            VertexConsumerProvider vertices,int light,CallbackInfo ci){
        if(entity instanceof SpellProjectile projectile){
            Spell.ProjectileModelComposite data=projectile.renderModels();
            if(data!=null && data.models!=null){
                for(Spell.ProjectileModelComposite.Model model:data.models){
                    if(model==null || model.fx==null || model.fx.model_id==null) continue;
                    String id=model.fx.model_id;
                    if(id.contains("swordfall") || id.contains("sword")) model.rotate_degrees_per_tick=0;
                }
            }
        }
    }

    @ModifyArg(method="renderComposite",at=@At(value="INVOKE",
            target="Lnet/spell_engine/api/render/CustomModels;renderModel(Lnet/minecraft/client/render/RenderLayer;Lnet/spell_engine/mixin/client/render/ItemRendererAccessor;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/render/model/BakedModel;)V"),
            index=0,require=0)
    private static RenderLayer smooth_classes$heldItemCutout(RenderLayer original){
        return RenderLayer.getEntityCutout(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE);
    }

    @Override public Identifier getTexture(T entity){ return SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE; }
}
