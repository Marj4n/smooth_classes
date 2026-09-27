package org.marj4n.smooth_classes.gameplay;

import org.marj4n.smooth_classes.config.SmoothBalance;

/** Central signature cooldown table. Values are seconds in SmoothBalance, returned as ticks here. */
public final class SignatureCooldowns {
    private SignatureCooldowns() {}
    public static int ticks(String ability) {
        int seconds=switch(ability){
            case "meteor_shower" -> SmoothBalance.Caster.meteorCooldown;
            case "ice_comet" -> SmoothBalance.Caster.iceCometCooldown;
            case "static_discharge" -> SmoothBalance.Caster.staticDischargeCooldown;
            case "arcane_bolt" -> SmoothBalance.Caster.arcaneBoltCooldown;
            case "rampage" -> SmoothBalance.Berserker.rampageCooldown;
            case "bloodthirsty" -> SmoothBalance.Berserker.bloodthirstyCooldown;
            case "berserking" -> SmoothBalance.Berserker.berserkingCooldown;
            case "evasion" -> SmoothBalance.Assassin.evasionCooldown;
            case "preparation" -> SmoothBalance.Assassin.preparationCooldown;
            case "siphoning_strikes" -> SmoothBalance.Assassin.siphoningCooldown;
            case "disengage" -> SmoothBalance.Archer.disengageCooldown;
            case "arrow_rain" -> SmoothBalance.Archer.arrowRainCooldown;
            case "elemental_arrows" -> SmoothBalance.Archer.elementalArrowsCooldown;
            case "elemental_surge" -> SmoothBalance.Foreigner.elementalSurgeCooldown;
            case "elemental_impact" -> SmoothBalance.Foreigner.elementalImpactCooldown;
            case "spellweaver" -> SmoothBalance.Foreigner.spellweaverCooldown;
            case "consecration" -> SmoothBalance.Saber.consecrationCooldown;
            case "sacred_onslaught" -> SmoothBalance.Saber.sacredOnslaughtCooldown;
            case "heavensmiths_call" -> SmoothBalance.Saber.heavensmithCooldown;
            case "sacred_orb" -> 120*20;
            case "divine_intervention" -> SmoothBalance.Ruler.divineInterventionCooldown;
            case "anoint_weapon" -> SmoothBalance.Ruler.anointWeaponCooldown;
            case "summoning_ritual" -> SmoothBalance.Avenger.summoningRitualCooldown;
            case "rider_charge" -> SmoothBalance.Rider.chargeCooldown;
            case "rider_war_aura" -> SmoothBalance.Rider.auraCooldown;
            case "rider_blazing_hooves" -> SmoothBalance.Rider.blazingHoovesCooldown;
            default -> 20;
        };
        return seconds*20;
    }
}
