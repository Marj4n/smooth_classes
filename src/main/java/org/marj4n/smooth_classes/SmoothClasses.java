package org.marj4n.smooth_classes;

import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import net.minecraft.server.network.ServerPlayerEntity;
import net.spell_engine.api.spell.event.SpellEvents;
import org.marj4n.smooth_classes.registry.SmoothClassContent;
import org.marj4n.smooth_classes.content.avenger.runtime.AvengerServerRuntime;
import org.marj4n.smooth_classes.registry.SmoothEntities;
import org.marj4n.smooth_classes.registry.SmoothSounds;
import org.marj4n.smooth_classes.effects.SmoothEffects;
import org.marj4n.smooth_classes.network.SmoothClassesNetworking;
import org.marj4n.smooth_classes.integration.PassiveSkillReward;
import org.marj4n.smooth_classes.integration.PuffishSkillFeedback;
import org.marj4n.smooth_classes.runtime.base.BasePathRuntime;
import org.marj4n.smooth_classes.runtime.classpass.ClassPassiveRuntime;
import org.marj4n.smooth_classes.runtime.CombatEventRuntime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SmoothClasses implements ModInitializer {
    public static final String MOD_ID = "smooth_classes";
    public static final Logger LOGGER = LoggerFactory.getLogger("Smooth Classes");

    public SmoothClasses() {}

    public static Identifier id(String path) {
        return new Identifier(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        PassiveSkillReward.register();
        SmoothSounds.register();
        org.marj4n.smooth_classes.registry.SmoothItems.register();
        org.marj4n.smooth_classes.registry.SmoothParticles.register();
        PuffishSkillFeedback.register();
        SmoothEffects.register();
        org.marj4n.smooth_classes.runtime.DivineRayLightRuntime.register();
        org.marj4n.smooth_classes.registry.SmoothBlocks.register();
        SmoothEntities.register();
        org.marj4n.smooth_classes.content.archer.runtime.PortalOfSovereigntyRuntime.register();
        org.marj4n.smooth_classes.content.ruler.runtime.SacredBannerRuntime.register();
        org.marj4n.smooth_classes.runtime.BloodRainRuntime.register();
        org.marj4n.smooth_classes.content.assassin.runtime.ShadowTechniqueRuntime.register();
        org.marj4n.smooth_classes.runtime.WhenOnHighRuntime.register();
        SmoothClassContent.register();
        AvengerServerRuntime.register();
        org.marj4n.smooth_classes.content.rider.runtime.RiderRuntime.register();
        org.marj4n.smooth_classes.content.lancer.runtime.LancerRuntime.register();
        org.marj4n.smooth_classes.gameplay.ClassSpecialDispatcher.register();
        BasePathRuntime.register();
        ClassPassiveRuntime.register();
        // Spell Engine's official PRE-cast event runs client-side and server-side.
        // Returning a non-success attempt blocks spells even if a mod bypasses keybinds.
        SpellEvents.CASTING_ATTEMPT.PRE.register(args ->
                org.marj4n.smooth_classes.origin.VampireBatAbilityLock.isLocked(args.caster())
                        ? net.spell_engine.internals.casting.SpellCast.Attempt.none() : null);
        SpellEvents.SPELL_CAST.register(args -> {
            if (args.caster() instanceof ServerPlayerEntity player) {
                CombatEventRuntime.onSpellCast(player, args.targets(), args.spell().value().school);
                // DIRECT delivery has applied its impact to the selected targets here.
                // Projectile spells consume a hammer only at their entity collision hook.
                var delivery=args.spell().value().deliver;
                if (delivery!=null && delivery.type!=null
                        && "DIRECT".equals(String.valueOf(delivery.type))) {
                    for (var entity:args.targets()) {
                        if (entity instanceof net.minecraft.entity.LivingEntity target)
                            org.marj4n.smooth_classes.runtime.RighteousHammerChargeRuntime.onTriggeredHit(player,target);
                    }
                }
            }
        });
        SmoothClassesNetworking.registerServer();
        org.marj4n.smooth_classes.origin.OriginRuntime.register();
        org.marj4n.smooth_classes.origin.OriginCommands.register();
        LOGGER.info("Smooth Classes initialized. Puffish Skills owns class trees and unlock state; {} class definitions registered.",
                org.marj4n.smooth_classes.registry.ClassRegistry.values().size());
    }
}
