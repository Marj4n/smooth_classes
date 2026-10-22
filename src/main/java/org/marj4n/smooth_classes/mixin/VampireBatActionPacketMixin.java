package org.marj4n.smooth_classes.mixin;

import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.marj4n.smooth_classes.origin.VampireBatAbilityLock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Server boundary: deny vanilla interaction packets even from modified clients. */
@Mixin(ServerPlayNetworkHandler.class)
public abstract class VampireBatActionPacketMixin {
    @Shadow public ServerPlayerEntity player;

    @Inject(method = "onPlayerInteractBlock", at = @At("HEAD"), cancellable = true)
    private void smooth_classes$noBatBlockUse(PlayerInteractBlockC2SPacket packet, CallbackInfo ci) {
        // Vanilla packet handlers dispatch once off-thread and once on the server thread.
        // Never read mutable Origin flags from Netty's networking thread.
        if (player.getServer() != null && player.getServer().isOnThread()
                && VampireBatAbilityLock.isLocked(player)) ci.cancel();
    }

    @Inject(method = "onPlayerInteractItem", at = @At("HEAD"), cancellable = true)
    private void smooth_classes$noBatItemUse(PlayerInteractItemC2SPacket packet, CallbackInfo ci) {
        // Vanilla packet handlers dispatch once off-thread and once on the server thread.
        // Never read mutable Origin flags from Netty's networking thread.
        if (player.getServer() != null && player.getServer().isOnThread()
                && VampireBatAbilityLock.isLocked(player)) ci.cancel();
    }

    @Inject(method = "onPlayerInteractEntity", at = @At("HEAD"), cancellable = true)
    private void smooth_classes$noBatEntityUse(PlayerInteractEntityC2SPacket packet, CallbackInfo ci) {
        // Vanilla packet handlers dispatch once off-thread and once on the server thread.
        // Never read mutable Origin flags from Netty's networking thread.
        if (player.getServer() != null && player.getServer().isOnThread()
                && VampireBatAbilityLock.isLocked(player)) ci.cancel();
    }

    @Inject(method = "onPlayerAction", at = @At("HEAD"), cancellable = true)
    private void smooth_classes$noBatDigDropSwap(PlayerActionC2SPacket packet, CallbackInfo ci) {
        // Vanilla packet handlers dispatch once off-thread and once on the server thread.
        // Never read mutable Origin flags from Netty's networking thread.
        if (player.getServer() != null && player.getServer().isOnThread()
                && VampireBatAbilityLock.isLocked(player)) ci.cancel();
    }

    @Inject(method = "onClickSlot", at = @At("HEAD"), cancellable = true)
    private void smooth_classes$noBatInventoryClicks(ClickSlotC2SPacket packet, CallbackInfo ci) {
        if (player.getServer() != null && player.getServer().isOnThread()
                && VampireBatAbilityLock.isLocked(player)) ci.cancel();
    }

    @Inject(method = "onCreativeInventoryAction", at = @At("HEAD"), cancellable = true)
    private void smooth_classes$noBatCreativeSlot(CreativeInventoryActionC2SPacket packet, CallbackInfo ci) {
        if (player.getServer() != null && player.getServer().isOnThread()
                && VampireBatAbilityLock.isLocked(player)) ci.cancel();
    }

    @Inject(method = "onHandSwing", at = @At("HEAD"), cancellable = true)
    private void smooth_classes$noBatSwing(HandSwingC2SPacket packet, CallbackInfo ci) {
        // Vanilla packet handlers dispatch once off-thread and once on the server thread.
        // Never read mutable Origin flags from Netty's networking thread.
        if (player.getServer() != null && player.getServer().isOnThread()
                && VampireBatAbilityLock.isLocked(player)) ci.cancel();
    }
}
