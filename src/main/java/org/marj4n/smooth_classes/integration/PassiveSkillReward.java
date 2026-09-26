package org.marj4n.smooth_classes.integration;

import net.minecraft.util.Identifier;
import net.puffish.skillsmod.api.SkillsAPI;
import net.puffish.skillsmod.api.json.JsonElement;
import net.puffish.skillsmod.api.json.JsonObject;
import net.puffish.skillsmod.api.reward.Reward;
import net.puffish.skillsmod.api.reward.RewardConfigContext;
import net.puffish.skillsmod.api.reward.RewardDisposeContext;
import net.puffish.skillsmod.api.reward.RewardUpdateContext;
import net.puffish.skillsmod.api.util.Problem;
import net.puffish.skillsmod.api.util.Result;

import java.util.ArrayList;

/**
 * Compatibility reward used by the SimplySkills-derived Puffish trees.
 *
 * The reward intentionally stores only the passive skill id. Gameplay is owned by
 * Smooth Classes runtimes, which query Puffish unlock state. Registering this
 * reward makes Puffish able to deserialize the original tree definitions without
 * coupling the datapack to the old SimplySkills Java code.
 */
public final class PassiveSkillReward implements Reward {
    public static final Identifier ID = new Identifier("puffish_skills", "passive_skill");

    private final String passiveSkillId;

    private PassiveSkillReward(String passiveSkillId) {
        this.passiveSkillId = passiveSkillId;
    }

    public static void register() {
        SkillsAPI.registerReward(ID, PassiveSkillReward::create);
    }

    public String passiveSkillId() {
        return passiveSkillId;
    }

    private static Result<PassiveSkillReward, Problem> create(RewardConfigContext context) {
        return context.getData()
                .andThen(JsonElement::getAsObject)
                .andThen(PassiveSkillReward::create);
    }

    private static Result<PassiveSkillReward, Problem> create(JsonObject rootObject) {
        var problems = new ArrayList<Problem>();

        var passiveSkillId = rootObject.get("passive_skill")
                .andThen(JsonElement::getAsString)
                .ifFailure(problems::add)
                .getSuccess();

        if (problems.isEmpty()) {
            return Result.success(new PassiveSkillReward(passiveSkillId.orElseThrow()));
        }
        return Result.failure(Problem.combine(problems));
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
