package org.marj4n.smooth_classes.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import org.marj4n.smooth_classes.origin.VampireBloodReserve;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class VampireBloodReserveMixin implements VampireBloodReserve.Holder {
    @Unique private static final TrackedData<Integer> smooth_classes$TRACKED_BLOOD =
            DataTracker.registerData(LivingEntity.class, TrackedDataHandlerRegistry.INTEGER);
    @Unique private long smooth_classes$bloodUpdatedAt;

    @Inject(method = "initDataTracker", at = @At("TAIL"))
    private void smooth_classes$initBloodData(CallbackInfo ci) {
        ((LivingEntity)(Object)this).getDataTracker().startTracking(smooth_classes$TRACKED_BLOOD,
                VampireBloodReserve.MAX_PIPS);
    }

    @Override public int smooth_classes$getBloodPips() {
        return ((LivingEntity)(Object)this).getDataTracker().get(smooth_classes$TRACKED_BLOOD);
    }
    @Override public long smooth_classes$getBloodUpdatedAt() { return smooth_classes$bloodUpdatedAt; }
    @Override public void smooth_classes$setBloodReserve(int pips, long timestamp) {
        ((LivingEntity)(Object)this).getDataTracker().set(smooth_classes$TRACKED_BLOOD,
                Math.max(0, Math.min(VampireBloodReserve.MAX_PIPS, pips)));
        smooth_classes$bloodUpdatedAt = timestamp;
    }

    // Refresh depleted reserves on the server even if nobody is feeding.
    // Once per second is enough for the slow 55s/75s regeneration and keeps
    // entity-tracked HUD portions up to date for nearby players.
    @Inject(method = "tick", at = @At("TAIL"))
    private void smooth_classes$recoverBlood(CallbackInfo ci) {
        LivingEntity self = (LivingEntity)(Object)this;
        if (!self.getWorld().isClient && self.age % 20 == 0
                && smooth_classes$getBloodPips() < VampireBloodReserve.MAX_PIPS
                && VampireBloodReserve.isFeedable(self)) {
            VampireBloodReserve.available(self);
        }
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    private void smooth_classes$saveBlood(NbtCompound nbt, CallbackInfo ci) {
        nbt.putInt("SmoothClassesBloodPips", smooth_classes$getBloodPips());
        nbt.putLong("SmoothClassesBloodUpdatedAt", smooth_classes$bloodUpdatedAt);
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void smooth_classes$loadBlood(NbtCompound nbt, CallbackInfo ci) {
        smooth_classes$setBloodReserve(nbt.contains("SmoothClassesBloodPips")
                        ? nbt.getInt("SmoothClassesBloodPips") : VampireBloodReserve.MAX_PIPS,
                nbt.getLong("SmoothClassesBloodUpdatedAt"));
    }
}
