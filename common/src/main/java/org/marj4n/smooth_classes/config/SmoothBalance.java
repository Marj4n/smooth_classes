package org.marj4n.smooth_classes.config;

/**
 * Chapter 9 balance surface.
 *
 * Defaults intentionally mirror Smooth Classes where Smooth Classes
 * currently implements the corresponding mechanic. Values are kept in one
 * dependency-free class so later JSON/TOML config serialization can be added
 * without scattering gameplay constants again.
 */
public final class SmoothBalance {
    private SmoothBalance(){}

    public static final class General {
        public static boolean enableAscendancy=true;
        public static float minimumTimeBetweenAbilityUse=0.5F;
        public static float spellHasteCooldownReductionModifier=1.6F;
        public static int minimumAchievableCooldown=5;
    }
    public static final class Magic {
        public static int frailArmorThreshold=35, frailAttackThreshold=5;
        public static int nullificationFrequency=80, nullificationRadius=12;
        public static float slowFallDistance=3F;
        public static int empowerChance=15, empowerDuration=300, empowerMaxStacks=15;
        public static int lightningRodDuration=600, lightningRodMaxStacks=5, lightningRodFrequency=500;
    }
    public static final class Agility {
        public static int slenderArmorThreshold=35, sneakSpeedAmplifier=2;
        public static int reflexiveDuration=100, reflexiveChance=75;
        public static int guardingFrequency=800, guardingDuration=3400, guardingMaxStacks=3;
    }
    public static final class Strength {
        public static int armorThreshold=15, armorMasteryChance=10;
        public static int rebukeChance=25, spellbreakerChance=25, spellbreakerRadius=4;
        public static float goliathFallDistance=3F, heavyWeightDamagePerTick=0.3F;
        public static int twinstrikeChance=15, swordfallChance=8;
    }
    public static final class Berserker {
        public static double ignorePainHealthThreshold=.4, recklessnessHealthThreshold=.7;
        public static int challengeRadius=2, challengeMaxAmplifier=5;
        public static int rampageDuration=300, rampageCooldown=30;
        public static int bloodthirstyDuration=400, bloodthirstyCooldown=25;
        public static float bloodthirstyHealPercent=.25F;
        public static int bloodthirstyTirelessChance=85, bloodthirstyTremorChance=15;
        public static int berserkingCooldown=25;
    }
    public static final class Assassin {
        public static int smokeBombRadius=6, smokeBombChance=10;
        public static int evasionChance=15;
        public static int preparationDuration=80, preparationCooldown=30;
        public static double siphoningLeech=.15;
        public static int siphoningDuration=600, siphoningStacks=10, siphoningCooldown=25;
        public static int evasionDuration=160, evasionCooldown=25;
    }
    public static final class Archer {
        public static int elementalRenewalChance=35, elementalRenewalDuration=600, elementalRenewalMaxStacks=20;
        public static int portalOfSovereigntyCooldown=30;
        public static int elementalArrowsDuration=600, elementalArrowsCooldown=40;
        public static int arrowRainDuration=600, arrowRainCooldown=14, arrowRainRange=64;
    }
    public static final class Foreigner {
        public static int spellweavingChance=15, weaponExpertSpellforgedChance=25;
        public static int elementalSurgeDuration=300, elementalSurgeCooldown=30;
        public static int elementalSurgeRenewalChance=15, elementalSurgeRenewalTicks=60;
        public static int elementalImpactCooldown=25;
        public static int spellweaverChance=30, spellweaverDuration=600, spellweaverCooldown=40;
    }
    public static final class Saber {
        public static int retributionChance=15, exhaustiveRecoveryChance=15;
        public static int divineAdjudicationCooldown=40, sacredOnslaughtCooldown=15, consecrationCooldown=30;
        public static int consecrationDuration=250, consecrationRadius=3;
        public static double consecrationDamageMultiplier=1.9;
    }
    public static final class Ruler {
        public static int divineInterventionCooldown=45, sacredOrbCooldown=120, anointWeaponCooldown=35;
    }
    public static final class Caster {
        public static int spellEchoChance=12;
        public static int meteorCooldown=36, iceCometCooldown=28, staticDischargeCooldown=18, arcaneBoltCooldown=24;
        public static int meteorRange=120, iceCometRange=120, staticDischargeRange=120, arcaneBoltRange=120;
    }
    public static final class Avenger {
        public static int summoningRitualCooldown=120; // legacy
        public static int curtainCallCooldown=2;
        public static int endlessDevourCooldown=12;
    }
    public static final class Lancer {
        public static int impalingVolleyCooldown=28, dragonThrustCooldown=14, spearstormCooldown=20;
        public static int baseMomentumPercent=40, maxMomentumPercent=80, hitMomentumGain=2, killMomentumGain=10;
    }
    public static final class Rider {
        public static int summonCooldown=6;
        public static int chargeCooldown=12, auraCooldown=24, blazingHoovesCooldown=20;
        public static int chargeTicks=12, chargeLongTicks=20, maxPhaseBlocks=3;
        public static double chargeSpeed=1.15D, chargeImpactSpeed=1.35D;
        public static float chargeDamageMultiplier=1.15F, chargeImpactDamageMultiplier=1.75F;
        public static int auraDuration=12*20, auraRadius=12;
        public static int fireDuration=8*20, fireLongDuration=12*20;
    }
}
