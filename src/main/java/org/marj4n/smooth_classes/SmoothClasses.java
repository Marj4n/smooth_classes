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
        PuffishSkillFeedback.register();
        SmoothEffects.register();
        SmoothEntities.register();
        SmoothClassContent.register();
        AvengerServerRuntime.register();
        BasePathRuntime.register();
        ClassPassiveRuntime.register();
        SpellEvents.SPELL_CAST.register(args -> {
            if (args.caster() instanceof ServerPlayerEntity player) {
                CombatEventRuntime.onSpellCast(player, args.targets(), args.spell().value().school);
            }
        });
        SmoothClassesNetworking.registerServer();
        LOGGER.info("Smooth Classes initialized. Puffish Skills owns class trees and unlock state; {} class definitions registered.",
                org.marj4n.smooth_classes.registry.ClassRegistry.values().size());
    }
}
