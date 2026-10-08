package org.marj4n.smooth_classes.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.marj4n.smooth_classes.runtime.AscendancyRuntime;
import org.marj4n.smooth_classes.runtime.CombatEventRuntime;
import org.marj4n.smooth_classes.runtime.base.BasePathRuntime;
import org.marj4n.smooth_classes.runtime.classpass.ClassPassiveRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin implements org.marj4n.smooth_classes.origin.OriginDataHolder {
    @Unique
    private final org.marj4n.smooth_classes.origin.OriginState smooth_classes$originState = new org.marj4n.smooth_classes.origin.OriginState();

    @Override
    public org.marj4n.smooth_classes.origin.OriginState smooth_classes$getOriginState() {
        return smooth_classes$originState;
    }

    @Inject(method="writeCustomDataToNbt", at=@At("TAIL"))
    private void smooth_classes$writeOrigin(NbtCompound nbt, CallbackInfo ci) {
        NbtCompound origin = new NbtCompound();
        smooth_classes$originState.write(origin);
        nbt.put("SmoothClassesOrigin", origin);
    }

    @Inject(method="readCustomDataFromNbt", at=@At("TAIL"))
    private void smooth_classes$readOrigin(NbtCompound nbt, CallbackInfo ci) {
        if (nbt.contains("SmoothClassesOrigin")) {
            smooth_classes$originState.read(nbt.getCompound("SmoothClassesOrigin"));
        }
    }

    @Inject(method="onKilledOther", at=@At("HEAD"))
    private void smooth_classes$killedOther(ServerWorld world, LivingEntity other, CallbackInfoReturnable<Boolean> cir) {
        if ((Object)this instanceof ServerPlayerEntity player) {
            CombatEventRuntime.onKilledOther(player, world, other);
        }
    }

    @Inject(method="takeShieldHit", at=@At("HEAD"))
    private void smooth_classes$takeShieldHit(LivingEntity attacker, CallbackInfo ci) {
        if ((Object)this instanceof ServerPlayerEntity player) {
            BasePathRuntime.onShieldHit(player, attacker);
            ClassPassiveRuntime.onShieldHit(player, attacker);
            AscendancyRuntime.shieldHit(player);
        }
    }

    @Inject(method="getDimensions", at=@At("HEAD"), cancellable=true)
    private void smooth_classes$originDimensions(EntityPose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        PlayerEntity self = (PlayerEntity)(Object)this;
        var state = org.marj4n.smooth_classes.origin.OriginRuntime.state(self);
        var origin = state.origin();
        if (origin == org.marj4n.smooth_classes.origin.OriginType.VAMPIRE && state.hasFlag("vampire.form.man_bat")) {
            float height = pose == EntityPose.CROUCHING
                    ? org.marj4n.smooth_classes.origin.VampireManBatRuntime.CROUCH_HEIGHT
                    : org.marj4n.smooth_classes.origin.VampireManBatRuntime.HEIGHT;
            cir.setReturnValue(EntityDimensions.changing(
                    org.marj4n.smooth_classes.origin.VampireManBatRuntime.WIDTH, height));
            return;
        }
        if (origin == org.marj4n.smooth_classes.origin.OriginType.VAMPIRE && state.hasFlag("vampire.form.bat")) {
            // Keep the authoritative collision body centered on the same tiny volume as the
            // rendered vanilla bat. Use vanilla-bat-like proportions: narrow enough for a full
            // one-block opening, but tall enough that half-block slab gaps remain solid. fixed()
            // is intentional: flying/crouching must not silently restore a human-height pose.
            cir.setReturnValue(EntityDimensions.fixed(
                    org.marj4n.smooth_classes.origin.OriginBodyGeometry.BAT_WIDTH,
                    org.marj4n.smooth_classes.origin.OriginBodyGeometry.BAT_HEIGHT));
            return;
        }
        if (origin == org.marj4n.smooth_classes.origin.OriginType.SLIME && !state.hasFlag("slime.form.humanoid")) {
            if (state.hasFlag("slime.squeeze") || state.hasFlag("slime.size.small")) {
                cir.setReturnValue(EntityDimensions.changing(0.51F, 0.51F));
            } else if (state.hasFlag("slime.size.large")) {
                cir.setReturnValue(EntityDimensions.changing(2.04F, 2.04F));
            } else {
                cir.setReturnValue(EntityDimensions.changing(1.02F, 1.02F));
            }
        }
    }


    @Inject(method="attack", at=@At("TAIL"))
    private void smooth_classes$manBatBloodOnBasicAttack(Entity target, CallbackInfo ci) {
        if (!((Object)this instanceof ServerPlayerEntity player) || !(target instanceof LivingEntity living)) return;
        var state = org.marj4n.smooth_classes.origin.OriginRuntime.state(player);
        if (state.origin() != org.marj4n.smooth_classes.origin.OriginType.VAMPIRE
                || !state.hasFlag("vampire.form.man_bat")) return;

        // Only reward a real landed melee hit. hurtTime is set by successful LivingEntity.damage().
        if (living.timeUntilRegen <= 0) return;
        int cap = org.marj4n.smooth_classes.origin.OriginRuntime.vampireBloodCapacity(player);
        int before = state.blood();
        state.blood(Math.min(cap, before + 1));
        if (state.blood() != before) {
            org.marj4n.smooth_classes.network.SmoothClassesNetworking.sendOriginState(player);
        }
    }

    @Inject(method = "getBlockBreakingSpeed", at = @At("RETURN"), cancellable = true)
    private void smooth_classes$manBatBeastMining(BlockState block, CallbackInfoReturnable<Float> cir) {
        PlayerEntity self = (PlayerEntity)(Object)this;
        var state = org.marj4n.smooth_classes.origin.OriginRuntime.state(self);
        if (state.origin() == org.marj4n.smooth_classes.origin.OriginType.VAMPIRE
                && state.hasFlag("vampire.form.man_bat")) {
            // Nycto Dark Form breaks beast-mineable terrain with supernatural hands.
            // 1.20.1 has no matching Nycto tag, so preserve vanilla harvest checks and
            // apply the reference 4x hand/body mining multiplier to the final speed.
            cir.setReturnValue(cir.getReturnValueF() * 4.0F);
        }
    }

}
