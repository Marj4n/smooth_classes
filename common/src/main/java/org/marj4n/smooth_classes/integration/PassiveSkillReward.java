package org.marj4n.smooth_classes.integration;

import net.minecraft.util.Identifier;
import net.puffish.skillsmod.api.SkillsAPI;
import net.puffish.skillsmod.api.json.JsonElement;
import net.puffish.skillsmod.api.reward.Reward;
import net.puffish.skillsmod.api.reward.RewardConfigContext;
import net.puffish.skillsmod.api.reward.RewardDisposeContext;
import net.puffish.skillsmod.api.reward.RewardUpdateContext;
import net.puffish.skillsmod.api.util.Problem;
import net.puffish.skillsmod.api.util.Result;

import java.util.List;
import org.marj4n.smooth_classes.platform.PlatformLoader;

/** Marker reward: unlock state stays in Puffish and is read by the gameplay runtimes. */
public final class PassiveSkillReward implements Reward {
    public static final Identifier ID = Identifier.of("smooth_classes", "passive_skill");
    private static final Identifier LEGACY_ID = Identifier.of("puffish_skills", "passive_skill");

    private final String passiveSkillId;

    private PassiveSkillReward(String passiveSkillId) {
        this.passiveSkillId = passiveSkillId;
    }

    public static void register() {
        for (var id : registrationIds(PlatformLoader.getInstance().isModLoaded("simplyskills"))) {
            SkillsAPI.registerReward(id, PassiveSkillReward::create);
        }
    }

    static List<Identifier> registrationIds(boolean simplySkillsPresent) {
        // Simply Skills owns the old shared ID when present. Both legacy rewards are markers.
        return simplySkillsPresent ? List.of(ID) : List.of(ID, LEGACY_ID);
    }

    public String passiveSkillId() {
        return passiveSkillId;
    }

    private static Result<PassiveSkillReward, Problem> create(RewardConfigContext context) {
        return context.getData()
                .andThen(JsonElement::getAsObject)
                .andThen(data -> data.get("passive_skill"))
                .andThen(JsonElement::getAsString)
                .andThen(value -> Result.success(new PassiveSkillReward(value)));
    }

    @Override
    public void update(RewardUpdateContext context) {
        // Unlock state is consumed by Smooth Classes runtime; no direct side effect here.
    }

    @Override
    public void dispose(RewardDisposeContext context) {
        // Nothing persistent is attached by this compatibility reward.
    }
}
