package org.marj4n.smooth_classes.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.World;
import net.spell_engine.entity.SpellProjectile;
import org.marj4n.smooth_classes.runtime.CombatEventRuntime;
import org.marj4n.smooth_classes.runtime.ProjectileEntityRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SpellProjectile.class)
public abstract class SpellProjectileMixin extends ProjectileEntity {
    protected SpellProjectileMixin(EntityType<? extends ProjectileEntity> type, World world) { super(type, world); }

    private SpellProjectile smooth$self(){ return (SpellProjectile)(Object)this; }
    private Identifier smooth$spellId(){
        SpellProjectile p=smooth$self();
        if(p.getSpellEntry()==null)return null;
        return p.getSpellEntry().getKey().map(k->k.getValue()).orElse(null);
    }

    @Inject(method="tick",at=@At("HEAD"))
    private void smooth$tick(CallbackInfo ci){
        if(getWorld().isClient || !(getOwner() instanceof ServerPlayerEntity owner))return;
        CombatEventRuntime.onSpellProjectileTick(owner,this);
        ProjectileEntityRuntime.tickSpellProjectile(owner,smooth$self(),smooth$spellId());
    }

    @Inject(method="onEntityHit",at=@At("HEAD"))
    private void smooth$entityHit(EntityHitResult hit,CallbackInfo ci){
        if(getWorld().isClient || !(getOwner() instanceof ServerPlayerEntity owner)
                || !(hit.getEntity() instanceof LivingEntity target))return;
        if (smooth$spellId()==null || !smooth$spellId().getPath().equals("righteous_hammer_projectile"))
            CombatEventRuntime.onSpellProjectileHit(owner,this,target);
        ProjectileEntityRuntime.onSpellProjectileHit(owner,smooth$self(),smooth$spellId(),target);
    }

    @Inject(method="onBlockHit",at=@At("HEAD"),cancellable=true)
    private void smooth$blockHit(BlockHitResult hit,CallbackInfo ci){
        Identifier id=smooth$spellId();
        if(!getWorld().isClient && ProjectileEntityRuntime.ignoreBlockHit(id))ci.cancel();
    }
}
