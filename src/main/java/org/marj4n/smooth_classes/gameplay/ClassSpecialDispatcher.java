package org.marj4n.smooth_classes.gameplay;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.marj4n.smooth_classes.content.archer.ArcherClass;
import org.marj4n.smooth_classes.content.archer.runtime.ArcherSpecialRuntime;
import org.marj4n.smooth_classes.content.assassin.AssassinClass;
import org.marj4n.smooth_classes.content.assassin.runtime.AssassinSpecialRuntime;
import org.marj4n.smooth_classes.content.avenger.AvengerClass;
import org.marj4n.smooth_classes.content.avenger.runtime.AvengerReworkRuntime;
import org.marj4n.smooth_classes.content.berserker.BerserkerClass;
import org.marj4n.smooth_classes.content.berserker.runtime.BerserkerSpecialRuntime;
import org.marj4n.smooth_classes.content.caster.CasterClass;
import org.marj4n.smooth_classes.content.caster.runtime.CasterSpecialRuntime;
import org.marj4n.smooth_classes.content.foreigner.ForeignerClass;
import org.marj4n.smooth_classes.content.foreigner.runtime.ForeignerSpecialRuntime;
import org.marj4n.smooth_classes.content.lancer.LancerClass;
import org.marj4n.smooth_classes.content.lancer.runtime.LancerSpecialRuntime;
import org.marj4n.smooth_classes.content.rider.RiderClass;
import org.marj4n.smooth_classes.content.rider.runtime.RiderRuntime;
import org.marj4n.smooth_classes.content.ruler.RulerClass;
import org.marj4n.smooth_classes.content.ruler.runtime.RulerSpecialRuntime;
import org.marj4n.smooth_classes.content.saber.SaberClass;
import org.marj4n.smooth_classes.content.saber.runtime.SaberSpecialRuntime;
import org.marj4n.smooth_classes.runtime.AbilityRuntime;
import org.marj4n.smooth_classes.runtime.ExecutionResult;

/**
 * One H-channel for every class. H is the class identity button; signature and
 * Ascendancy remain independent channels.
 */
public final class ClassSpecialDispatcher {
    public record HudState(String id, int totalTicks, long remainingTicks, boolean blockedActive, int variant,
                           int modeTotalTicks, long modeRemainingTicks,
                           int secondaryTotalTicks, long secondaryRemainingTicks) {
        private static final HudState EMPTY = new HudState("", 1, 0L, false, -1, 0, 0L, 0, 0L);
    }

    private ClassSpecialDispatcher() {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) tick(player);
        });
    }

    public static ExecutionResult activate(ServerPlayerEntity player, boolean alternate) {
        if (org.marj4n.smooth_classes.origin.VampireBatAbilityLock.isLocked(player)) return ExecutionResult.failure("Return from Bat Form to use class abilities.");
        if (AbilityRuntime.isClass(player, ArcherClass.ID)) return ArcherSpecialRuntime.activate(player, alternate);
        if (AbilityRuntime.isClass(player, AssassinClass.ID)) return AssassinSpecialRuntime.activate(player);
        if (AbilityRuntime.isClass(player, AvengerClass.ID)) return AvengerReworkRuntime.summonFromHands(player);
        if (AbilityRuntime.isClass(player, BerserkerClass.ID)) return BerserkerSpecialRuntime.activate(player);
        if (AbilityRuntime.isClass(player, CasterClass.ID)) return CasterSpecialRuntime.activate(player, alternate);
        if (AbilityRuntime.isClass(player, ForeignerClass.ID)) return ForeignerSpecialRuntime.activate(player);
        if (AbilityRuntime.isClass(player, LancerClass.ID)) return LancerSpecialRuntime.activate(player);
        if (AbilityRuntime.isClass(player, RiderClass.ID)) return RiderRuntime.summonMount(player);
        if (AbilityRuntime.isClass(player, RulerClass.ID)) return RulerSpecialRuntime.activate(player);
        if (AbilityRuntime.isClass(player, SaberClass.ID)) return SaberSpecialRuntime.activate(player);
        return ExecutionResult.failure("No class special is available for the current class.");
    }


    public static ExecutionResult select(ServerPlayerEntity player, int selection) {
        if (org.marj4n.smooth_classes.origin.VampireBatAbilityLock.isLocked(player)) return ExecutionResult.failure("Return from Bat Form to use class abilities.");
        if (AbilityRuntime.isClass(player, ArcherClass.ID)) return ArcherSpecialRuntime.select(player, selection);
        if (AbilityRuntime.isClass(player, CasterClass.ID)) return CasterSpecialRuntime.select(player, selection);
        if (AbilityRuntime.isClass(player, ForeignerClass.ID)) return ForeignerSpecialRuntime.select(player, selection);
        return ExecutionResult.failure("This class special does not use the H selector.");
    }

    public static void hold(ServerPlayerEntity player, boolean held) {
        // Always accept release packets so a channel started before transforming can end cleanly.
        if (held && org.marj4n.smooth_classes.origin.VampireBatAbilityLock.isLocked(player)) return;
        if (AbilityRuntime.isClass(player, ArcherClass.ID)) ArcherSpecialRuntime.hold(player, held);
        if (AbilityRuntime.isClass(player, BerserkerClass.ID)) BerserkerSpecialRuntime.hold(player, held);
    }


    public static void afterMeleeHit(ServerPlayerEntity player, LivingEntity target) {
        SaberSpecialRuntime.onMeleeHit(player, target);
        ForeignerSpecialRuntime.onMeleeHit(player, target);
        RulerSpecialRuntime.onMeleeHit(player, target);
    }

    private static void tick(ServerPlayerEntity player) {
        ArcherSpecialRuntime.tick(player);
        SaberSpecialRuntime.tick(player);
        LancerSpecialRuntime.tick(player);
        AssassinSpecialRuntime.tick(player);
        CasterSpecialRuntime.tick(player);
        ForeignerSpecialRuntime.tick(player);
        RulerSpecialRuntime.tick(player);
    }

    public static HudState hudState(ServerPlayerEntity player) {
        if (AbilityRuntime.isClass(player, ArcherClass.ID))
            return new HudState("treasury_key", 1, 0L,
                    ArcherSpecialRuntime.drawing(player), -1,
                    0, 0L, 0, 0L);
        if (AbilityRuntime.isClass(player, AssassinClass.ID))
            return new HudState("vanish", AssassinSpecialRuntime.cooldownTicks(), AssassinSpecialRuntime.remainingTicks(player), AssassinSpecialRuntime.active(player), -1, 0, 0L, 0, 0L);
        if (AbilityRuntime.isClass(player, AvengerClass.ID))
            return new HudState("summoning_ritual", AvengerReworkRuntime.summonRechargeTotalTicks(), AvengerReworkRuntime.summonRechargeRemainingTicks(player), false, AvengerReworkRuntime.summonCharges(player), 0, 0L, 0, 0L);
        if (AbilityRuntime.isClass(player, BerserkerClass.ID))
            return new HudState("crimson_revenant", BerserkerSpecialRuntime.hudCooldownTotalTicks(player), BerserkerSpecialRuntime.cooldownRemainingTicks(player), BerserkerSpecialRuntime.isCharging(player) || BerserkerSpecialRuntime.isActive(player), BerserkerSpecialRuntime.isCharging(player) ? 1 : BerserkerSpecialRuntime.isActive(player) ? 2 : 0, 0, 0L, 0, 0L);
        if (AbilityRuntime.isClass(player, CasterClass.ID))
            return new HudState("arcane_attunement", CasterSpecialRuntime.HUD_COOLDOWN_TICKS, 0L, false, CasterSpecialRuntime.variant(player), 0, 0L, 0, 0L);
        if (AbilityRuntime.isClass(player, ForeignerClass.ID))
            return new HudState("spell_imprint", ForeignerSpecialRuntime.cooldownTicks(), ForeignerSpecialRuntime.remainingTicks(player), ForeignerSpecialRuntime.active(player), ForeignerSpecialRuntime.variant(player), ForeignerSpecialRuntime.ACTIVE_TICKS, ForeignerSpecialRuntime.activeRemainingTicks(player), 0, 0L);
        if (AbilityRuntime.isClass(player, LancerClass.ID))
            return new HudState("high_jump", LancerSpecialRuntime.cooldownTicks(), LancerSpecialRuntime.active(player) ? 0L : LancerSpecialRuntime.remainingTicks(player), false, LancerSpecialRuntime.variant(player), 0, 0L, 0, 0L);
        if (AbilityRuntime.isClass(player, RiderClass.ID))
            return new HudState("conjure_mount", RiderRuntime.summonCooldownTicks(player), RiderRuntime.summonRemainingTicks(player), RiderRuntime.hasActiveMount(player), 0, 0, 0L, 0, 0L);
        if (AbilityRuntime.isClass(player, RulerClass.ID))
            return new HudState("divine_edict", RulerSpecialRuntime.cooldownTicks(), RulerSpecialRuntime.remainingTicks(player), false, RulerSpecialRuntime.variant(player), 0, 0L, 0, 0L);
        if (AbilityRuntime.isClass(player, SaberClass.ID))
            return new HudState("radiant_burst", SaberSpecialRuntime.cooldownTicks(), SaberSpecialRuntime.remainingTicks(player), SaberSpecialRuntime.active(player), -1, 0, 0L, 0, 0L);
        return HudState.EMPTY;
    }

    public static void cleanup(ServerPlayerEntity player) {
        BerserkerSpecialRuntime.disconnect(player);
        ArcherSpecialRuntime.cleanup(player);
        SaberSpecialRuntime.cleanup(player);
        LancerSpecialRuntime.cleanup(player);
        AssassinSpecialRuntime.cleanup(player);
        CasterSpecialRuntime.cleanup(player);
        ForeignerSpecialRuntime.cleanup(player);
        RulerSpecialRuntime.cleanup(player);
    }

    public static void clear() {
        BerserkerSpecialRuntime.clear();
        ArcherSpecialRuntime.clear();
        SaberSpecialRuntime.clear();
        LancerSpecialRuntime.clear();
        AssassinSpecialRuntime.clear();
        CasterSpecialRuntime.clear();
        ForeignerSpecialRuntime.clear();
        RulerSpecialRuntime.clear();
    }
}
