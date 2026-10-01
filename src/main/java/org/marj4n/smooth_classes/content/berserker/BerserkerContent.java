package org.marj4n.smooth_classes.content.berserker;

import java.util.List;
import org.marj4n.smooth_classes.api.ability.Ability;
import org.marj4n.smooth_classes.api.talent.Talent;
import org.marj4n.smooth_classes.content.berserker.ability.BerserkerAbility;
import org.marj4n.smooth_classes.content.berserker.talent.BerserkerTalent;
import org.marj4n.smooth_classes.registry.AbilityRegistry;
import org.marj4n.smooth_classes.registry.TalentRegistry;

/** Class abilities and talents; runtime behavior is implemented separately. */
public final class BerserkerContent {
    private BerserkerContent() {}

    public static final Ability RAMPAGE = ability("rampage", "Rampage", 30 * 20);
    public static final Ability BLOODTHIRSTY = ability("bloodthirsty", "Bloodthirsty", 25 * 20);
    public static final Ability BERSERKING = ability("berserking", "Berserking", 25 * 20);

    public static final Talent SWORD_MASTERY = talent("sword_mastery", "Sword Mastery");
    public static final Talent SWORD_MASTERY_PROFICIENT = talent("sword_mastery_proficient", "Sword Mastery Proficient");
    public static final Talent SWORD_MASTERY_SKILLED = talent("sword_mastery_skilled", "Sword Mastery Skilled");
    public static final Talent AXE_MASTERY = talent("axe_mastery", "Axe Mastery");
    public static final Talent AXE_MASTERY_PROFICIENT = talent("axe_mastery_proficient", "Axe Mastery Proficient");
    public static final Talent AXE_MASTERY_SKILLED = talent("axe_mastery_skilled", "Axe Mastery Skilled");
    public static final Talent IGNORE_PAIN = talent("ignore_pain", "Ignore Pain");
    public static final Talent IGNORE_PAIN_PROFICIENT = talent("ignore_pain_proficient", "Ignore Pain Proficient");
    public static final Talent IGNORE_PAIN_SKILLED = talent("ignore_pain_skilled", "Ignore Pain Skilled");
    public static final Talent RECKLESSNESS = talent("recklessness", "Recklessness");
    public static final Talent CHALLENGE = talent("challenge", "Challenge");
    public static final Talent EXPLOIT = talent("exploit", "Exploit");
    public static final Talent RAMPAGE_CHARGE = talent("rampage_charge", "Rampage Charge");
    public static final Talent RAMPAGE_CHARGE_RELENTLESS = talent("rampage_charge_relentless", "Rampage Charge Relentless");
    public static final Talent RAMPAGE_CHARGE_IMMOBILIZE = talent("rampage_charge_immobilize", "Rampage Charge Immobilize");
    public static final Talent BLOODTHIRSTY_MIGHTY = talent("bloodthirsty_mighty", "Bloodthirsty Mighty");
    public static final Talent BLOODTHIRSTY_TIRELESS = talent("bloodthirsty_tireless", "Bloodthirsty Tireless");
    public static final Talent BLOODTHIRSTY_TREMOR = talent("bloodthirsty_tremor", "Bloodthirsty Tremor");
    public static final Talent BERSERKING_LEAP = talent("berserking_leap", "Berserking Leap");
    public static final Talent BERSERKING_LEAP_IMMOBILIZE = talent("berserking_leap_immobilize", "Berserking Leap Immobilize");
    public static final Talent BERSERKING_LEAP_PULL = talent("berserking_leap_pull", "Berserking Leap Pull");
    public static final Talent CRIMSON_REVENANT_ENDURING = talent("crimson_revenant_enduring", "Crimson Revenant Enduring");
    public static final Talent CRIMSON_REVENANT_FORTRESS = talent("crimson_revenant_fortress", "Crimson Revenant Fortress");
    public static final Talent CRIMSON_REVENANT_MASSACRE = talent("crimson_revenant_massacre", "Crimson Revenant Massacre");

    private static final List<Ability> ABILITIES = List.of(RAMPAGE, BLOODTHIRSTY, BERSERKING);
    private static final List<Talent> TALENTS = List.of(
            SWORD_MASTERY, SWORD_MASTERY_PROFICIENT, SWORD_MASTERY_SKILLED,
            AXE_MASTERY, AXE_MASTERY_PROFICIENT, AXE_MASTERY_SKILLED,
            IGNORE_PAIN, IGNORE_PAIN_PROFICIENT, IGNORE_PAIN_SKILLED,
            RECKLESSNESS, CHALLENGE, EXPLOIT,
            RAMPAGE_CHARGE, RAMPAGE_CHARGE_RELENTLESS, RAMPAGE_CHARGE_IMMOBILIZE,
            BLOODTHIRSTY_MIGHTY, BLOODTHIRSTY_TIRELESS, BLOODTHIRSTY_TREMOR,
            BERSERKING_LEAP, BERSERKING_LEAP_IMMOBILIZE, BERSERKING_LEAP_PULL,
            CRIMSON_REVENANT_ENDURING, CRIMSON_REVENANT_FORTRESS, CRIMSON_REVENANT_MASSACRE);

    public static void register() {
        ABILITIES.forEach(AbilityRegistry::register);
        TALENTS.forEach(TalentRegistry::register);
    }
    public static List<Ability> abilities() { return ABILITIES; }
    public static List<Talent> talents() { return TALENTS; }
    private static Ability ability(String path, String name, int cooldownTicks) { return new BerserkerAbility(path, name, "Berserker signature ability.", cooldownTicks); }
    private static Talent talent(String path, String name) { return new BerserkerTalent(path, name, "Berserker talent node."); }
}
