package org.marj4n.smooth_classes.mixin;

import net.puffish.skillsmod.api.config.ConfigContext;
import net.puffish.skillsmod.api.json.JsonObject;
import net.puffish.skillsmod.api.util.Problem;
import net.puffish.skillsmod.api.util.Result;
import net.puffish.skillsmod.config.experience.ExperienceConfig;
import org.marj4n.smooth_classes.platform.PlatformLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/** Keep legacy Smooth Progression's explicit XP-disable overrides compatible
 * with our schema-3 trees. Preserve disabled XP, rather than enabling a second
 * leveling system or modifying another mod's resources. */
@Mixin(value = ExperienceConfig.class, remap = false)
public abstract class SmoothProgressionExperienceCompatMixin {
    @Inject(method = "parse(Lnet/puffish/skillsmod/api/json/JsonObject;Lnet/puffish/skillsmod/api/config/ConfigContext;)Lnet/puffish/skillsmod/api/util/Result;",
            at = @At("HEAD"), cancellable = true, remap = false)
    private static void smoothClasses$legacyDisabledExperience(JsonObject json, ConfigContext context,
            CallbackInfoReturnable<Result<Optional<ExperienceConfig>, Problem>> cir) {
        if (!PlatformLoader.getInstance().isModLoaded("smooth_progression")) return;
        if (!json.getPath().toString().contains("smooth_classes:puffish_skills/categories/")) return;
        var enabled = json.getBoolean("enabled").getSuccess();
        if (enabled.isEmpty() || enabled.get()) return;
        // Consume only the known legacy schema fields. Unknown fields still go
        // through Puffish's normal unused-field/error validation.
        for (String key : new String[]{"level_limit", "experience_per_level", "sources", "reset_on_death"}) json.get(key);
        cir.setReturnValue(Result.success(Optional.empty()));
    }
}
