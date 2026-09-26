package org.marj4n.smooth_classes.content.avenger;

import org.marj4n.smooth_classes.api.ability.Ability;
import org.marj4n.smooth_classes.api.talent.Talent;
import org.marj4n.smooth_classes.content.avenger.ability.SummoningRitualAbility;
import org.marj4n.smooth_classes.content.avenger.talent.AvengerTalent;
import org.marj4n.smooth_classes.registry.AbilityRegistry;
import org.marj4n.smooth_classes.registry.TalentRegistry;

import java.util.List;

/**
 * All Avenger-owned content in one registration point.
 * Names and gameplay roles are migrated from the previously edited Necromancer tree,
 * while the implementation follows Smooth Classes' modular architecture.
 */
public final class AvengerContent {
    private AvengerContent() {}

    public static final Ability SUMMONING_RITUAL = new SummoningRitualAbility();

    public static final Talent NECROTIC_FORTIFICATION = talent("necrotic_fortification", "Necrotic Fortification",
            "Summoned minions inherit part of the Avenger's armor and armor toughness.");
    public static final Talent UNDEAD_LEGION = talent("undead_legion", "Undead Legion",
            "Increases the number of minions created by Summoning Ritual.", 4);
    public static final Talent SUMMON_WRAITH = talent("summon_wraith", "Summon Wraith",
            "Allows Summoning Ritual to summon Wraiths alongside the default minion.");
    public static final Talent WITHER_WRAITHS = talent("wither_wraiths", "Wither Wraiths",
            "Wraith attacks specialize in withering their victims.");
    public static final Talent FROST_WRAITHS = talent("frost_wraiths", "Frost Wraiths",
            "Wraith attacks specialize in frost effects.");
    public static final Talent BLOOD_HARVEST = talent("blood_harvest", "Blood Harvest",
            "Empowers the Dreadglare branch with its blood-harvest behavior.");
    public static final Talent GREATER_DREADGLARE = talent("greater_dreadglare", "Greater Dreadglare",
            "Upgrades Summoning Ritual to summon the much stronger Greater Dreadglare.");
    public static final Talent DEATH_ESSENCE = talent("death_essence", "Death Essence",
            "Grants Bone Armor through the Avenger's death-oriented branch.");
    public static final Talent DEATH_WARDEN = talent("death_warden", "Death Warden",
            "Sacrifices nearby owned minion health to restore the Avenger when the effect triggers.");
    public static final Talent ENRAGE = talent("enrage", "Enrage",
            "Empowers nearby friendly summons with Strength and Resistance.");
    public static final Talent WINTERBORN = talent("winterborn", "Winterborn",
            "Converts frost spell power into the Avenger's Soulshock-oriented effect.");
    public static final Talent PLAGUE = talent("plague", "Plague",
            "Allows an owned summon to take harmful effects away from the Avenger.");
    public static final Talent PESTILENCE = talent("pestilence", "Pestilence",
            "Allows minions to steal beneficial effects from their target.");
    public static final Talent DELIGHTFUL_SUFFERING = talent("delightful_suffering", "Delightful Suffering",
            "Periodically embraces a random harmful effect as part of the suffering branch.");
    public static final Talent SHADOW_AURA = talent("shadow_aura", "Shadow Aura",
            "Newly summoned minions gain the Shadow Aura effect.");
    public static final Talent SHADOW_COMBUST = talent("shadow_combust", "Shadow Combust",
            "Detonates a minion in a soul-powered area attack; stronger summons create a larger blast.");
    public static final Talent ENDLESS_SERVITUDE = talent("endless_servitude", "Endless Servitude",
            "Gives fallen minions a chance to be summoned again, scaling with harmful effects on them.");
    public static final Talent WRAITH_LEGION = talent("wraith_legion", "Wraith Legion",
            "Pushes the Wraith branch toward consistently producing Wraith summons.");

    private static final List<Talent> TALENTS = List.of(
            NECROTIC_FORTIFICATION, UNDEAD_LEGION, SUMMON_WRAITH, WITHER_WRAITHS,
            FROST_WRAITHS, BLOOD_HARVEST, GREATER_DREADGLARE, DEATH_ESSENCE,
            DEATH_WARDEN, ENRAGE, WINTERBORN, PLAGUE, PESTILENCE,
            DELIGHTFUL_SUFFERING, SHADOW_AURA, SHADOW_COMBUST,
            ENDLESS_SERVITUDE, WRAITH_LEGION
    );

    public static void register() {
        AbilityRegistry.register(SUMMONING_RITUAL);
        TALENTS.forEach(TalentRegistry::register);
    }

    public static List<Talent> talents() { return TALENTS; }

    private static Talent talent(String path, String name, String description) {
        return new AvengerTalent(path, name, description);
    }

    private static Talent talent(String path, String name, String description, int maxRank) {
        return new AvengerTalent(path, name, description, maxRank);
    }
}
