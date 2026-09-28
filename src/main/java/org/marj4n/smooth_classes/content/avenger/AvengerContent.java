package org.marj4n.smooth_classes.content.avenger;

import org.marj4n.smooth_classes.api.ability.Ability;
import org.marj4n.smooth_classes.api.talent.Talent;
import org.marj4n.smooth_classes.content.avenger.ability.AvengerAbility;
import org.marj4n.smooth_classes.content.avenger.ability.SummoningRitualAbility;
import org.marj4n.smooth_classes.content.avenger.talent.AvengerTalent;
import org.marj4n.smooth_classes.registry.AbilityRegistry;
import org.marj4n.smooth_classes.registry.TalentRegistry;

import java.util.List;

/** All Avenger-owned content. The old custom-minion talents are retained only for save compatibility. */
public final class AvengerContent {
    private AvengerContent() {}

    // Rework abilities. H/From Corpses We Arise is a class-special cast and is not a V signature.
    public static final Ability CURTAIN_CALL = new AvengerAbility("curtain_call", "Curtain Call",
            "Detonate a targeted Death List summon with Soul-powered force.", 40);
    public static final Ability ENDLESS_DEVOUR = new AvengerAbility("endless_devour", "Endless Devour",
            "Channel on a weakened mob and consume it directly into the Death List.", 240);

    // Kept registered so old worlds referencing the id do not break, but no reworked tree node can cast it.
    public static final Ability SUMMONING_RITUAL = new SummoningRitualAbility();

    public static final Talent CURTAIN_CALL_I = talent("curtain_call_i", "Grand Finale I",
            "Curtain Call gains a larger TNT blast and stronger Soul detonation.");
    public static final Talent CURTAIN_CALL_II = talent("curtain_call_ii", "Grand Finale II",
            "Curtain Call reaches its catastrophic final blast tier.");
    public static final Talent DEVOUR_150 = talent("devour_150", "Expanded Appetite",
            "Endless Devour accepts targets up to 150% of your current health.");
    public static final Talent DEVOUR_200 = talent("devour_200", "Bottomless Hunger",
            "Endless Devour accepts targets up to 200% of your current health.");
    public static final Talent DEVOUR_FAST = talent("devour_fast", "Voracious",
            "Greatly shortens the Endless Devour channel.");
    public static final Talent SUMMON_DAMAGE_I = talent("summon_damage_i", "Soulbound Claws I",
            "Death List summons gain +30% attack damage.");
    public static final Talent SUMMON_DAMAGE_II = talent("summon_damage_ii", "Soulbound Claws II",
            "Death List summons gain another +30% attack damage.");
    public static final Talent SUMMON_HEALTH_I = talent("summon_health_i", "Grave Fortification I",
            "Death List summons gain +30% max health.");
    public static final Talent SUMMON_HEALTH_II = talent("summon_health_ii", "Grave Fortification II",
            "Death List summons gain another +30% max health.");
    public static final Talent SUMMON_ARMOR_I = talent("summon_armor_i", "Corpseplate I",
            "Death List summons gain +4 armor.");
    public static final Talent SUMMON_ARMOR_II = talent("summon_armor_ii", "Corpseplate II",
            "Death List summons gain another +4 armor.");
    public static final Talent DEATH_MARCH = talent("death_march", "Death March",
            "Death List summons gain +15% movement speed.");
    public static final Talent SOUL_PACT = talent("soul_pact", "Soul Pact",
            "Summons gain +25% max health and inherit 35% of your armor.");

    // Legacy constants used by dormant compatibility code.
    public static final Talent NECROTIC_FORTIFICATION = talent("necrotic_fortification", "Necrotic Fortification", "Legacy Avenger talent.");
    public static final Talent UNDEAD_LEGION = talent("undead_legion", "Undead Legion", "Legacy Avenger talent.", 4);
    public static final Talent SUMMON_WRAITH = talent("summon_wraith", "Summon Wraith", "Legacy Avenger talent.");
    public static final Talent WITHER_WRAITHS = talent("wither_wraiths", "Wither Wraiths", "Legacy Avenger talent.");
    public static final Talent FROST_WRAITHS = talent("frost_wraiths", "Frost Wraiths", "Legacy Avenger talent.");
    public static final Talent BLOOD_HARVEST = talent("blood_harvest", "Blood Harvest", "Legacy Avenger talent.");
    public static final Talent GREATER_DREADGLARE = talent("greater_dreadglare", "Greater Dreadglare", "Legacy Avenger talent.");
    public static final Talent DEATH_ESSENCE = talent("death_essence", "Death Essence", "Legacy Avenger talent.");
    public static final Talent DEATH_WARDEN = talent("death_warden", "Death Warden", "Legacy Avenger talent.");
    public static final Talent ENRAGE = talent("enrage", "Enrage", "Legacy Avenger talent.");
    public static final Talent WINTERBORN = talent("winterborn", "Winterborn", "Legacy Avenger talent.");
    public static final Talent PLAGUE = talent("plague", "Plague", "Legacy Avenger talent.");
    public static final Talent PESTILENCE = talent("pestilence", "Pestilence", "Legacy Avenger talent.");
    public static final Talent DELIGHTFUL_SUFFERING = talent("delightful_suffering", "Delightful Suffering", "Legacy Avenger talent.");
    public static final Talent SHADOW_AURA = talent("shadow_aura", "Shadow Aura", "Legacy Avenger talent.");
    public static final Talent SHADOW_COMBUST = talent("shadow_combust", "Shadow Combust", "Legacy Avenger talent.");
    public static final Talent ENDLESS_SERVITUDE = talent("endless_servitude", "Endless Servitude", "Legacy Avenger talent.");
    public static final Talent WRAITH_LEGION = talent("wraith_legion", "Wraith Legion", "Legacy Avenger talent.");

    private static final List<Talent> TALENTS = List.of(
            CURTAIN_CALL_I, CURTAIN_CALL_II, DEVOUR_150, DEVOUR_200, DEVOUR_FAST,
            SUMMON_DAMAGE_I, SUMMON_DAMAGE_II, SUMMON_HEALTH_I, SUMMON_HEALTH_II,
            SUMMON_ARMOR_I, SUMMON_ARMOR_II, DEATH_MARCH, SOUL_PACT,
            NECROTIC_FORTIFICATION, UNDEAD_LEGION, SUMMON_WRAITH, WITHER_WRAITHS,
            FROST_WRAITHS, BLOOD_HARVEST, GREATER_DREADGLARE, DEATH_ESSENCE,
            DEATH_WARDEN, ENRAGE, WINTERBORN, PLAGUE, PESTILENCE,
            DELIGHTFUL_SUFFERING, SHADOW_AURA, SHADOW_COMBUST, ENDLESS_SERVITUDE, WRAITH_LEGION
    );

    public static void register() {
        AbilityRegistry.register(CURTAIN_CALL);
        AbilityRegistry.register(ENDLESS_DEVOUR);
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
