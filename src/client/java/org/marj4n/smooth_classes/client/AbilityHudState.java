package org.marj4n.smooth_classes.client;

import net.minecraft.util.Identifier;
import org.marj4n.smooth_classes.SmoothClasses;

/** Client mirror of ability HUD state. Server remains authoritative. */
public final class AbilityHudState {
    private AbilityHudState() {}

    public static boolean bannerActive;
    public static boolean bloodRainActive;
    public static boolean whenOnHighActive;
    public static String signatureAbility = "";
    public static String ascendancyAbility = "";
    public static int signatureCooldownMs = 500;
    public static int ascendancyCooldownMs = 500;
    public static long signatureReadyAtMs = 0L;
    public static long ascendancyReadyAtMs = 0L;

    public static boolean riderMountVisible;
    public static boolean riderMountActive;
    public static int riderMountCooldownMs = 6000;
    public static long riderMountReadyAtMs = 0L;

    public static boolean avengerSummonVisible;
    public static int avengerSummonCharges;
    public static int avengerSummonMaxCharges = 3;
    public static int avengerSummonRechargeMs = 20000;
    public static long avengerSummonReadyAtMs = 0L;

    public static void sync(String signature, int signatureTotalTicks, long signatureRemainingTicks,
                            String ascendancy, int ascendancyTotalTicks, long ascendancyRemainingTicks) {
        signatureAbility = signature == null ? "" : signature;
        ascendancyAbility = ascendancy == null ? "" : ascendancy;
        signatureCooldownMs = Math.max(1, signatureTotalTicks * 50);
        ascendancyCooldownMs = Math.max(1, ascendancyTotalTicks * 50);
        long now = System.currentTimeMillis();
        signatureReadyAtMs = now + Math.max(0L, signatureRemainingTicks) * 50L;
        ascendancyReadyAtMs = now + Math.max(0L, ascendancyRemainingTicks) * 50L;
    }

    public static void syncRiderMount(boolean visible, boolean active, int totalTicks, long remainingTicks) {
        riderMountVisible = visible;
        riderMountActive = active;
        riderMountCooldownMs = Math.max(1, totalTicks * 50);
        riderMountReadyAtMs = System.currentTimeMillis() + Math.max(0L, remainingTicks) * 50L;
    }

    public static long signatureRemainingMs() { return Math.max(0L, signatureReadyAtMs - System.currentTimeMillis()); }
    public static long ascendancyRemainingMs() { return Math.max(0L, ascendancyReadyAtMs - System.currentTimeMillis()); }
    public static long riderMountRemainingMs() { return Math.max(0L, riderMountReadyAtMs - System.currentTimeMillis()); }

    public static void syncAvengerSummon(boolean visible, int charges, int maxCharges, int totalTicks, long remainingTicks) {
        avengerSummonVisible = visible;
        avengerSummonCharges = Math.max(0, charges);
        avengerSummonMaxCharges = Math.max(1, maxCharges);
        avengerSummonRechargeMs = Math.max(1, totalTicks * 50);
        avengerSummonReadyAtMs = System.currentTimeMillis() + Math.max(0L, remainingTicks) * 50L;
    }

    public static long avengerSummonRemainingMs() { return Math.max(0L, avengerSummonReadyAtMs - System.currentTimeMillis()); }

    public static Identifier signatureIcon() { return icon(signatureAbility, false); }
    public static Identifier ascendancyIcon() { return icon(ascendancyAbility, true); }
    public static Identifier riderMountIcon() { return SmoothClasses.id("textures/icons/alternate_reduced/rider_path.png"); }
    public static Identifier avengerSummonIcon() { return SmoothClasses.id("textures/icons/alternate_reduced/necromancer_signature_summoning_ritual.png"); }

    private static Identifier icon(String ability, boolean ascendancy) {
        if (ability == null || ability.isBlank()) return SmoothClasses.id("textures/gui/cooldown_overlay.png");
        String path;
        if (ascendancy) {
            path = "ascendancy_" + ability;
        } else {
            path = switch (ability) {
                case "meteor_shower" -> "wizard_signature_meteor_shower";
                case "ice_comet" -> "wizard_signature_ice_comet";
                case "static_discharge" -> "wizard_signature_lightning_beam";
                case "arcane_bolt" -> "wizard_signature_arcane_bolt";
                case "rampage" -> "berserker_signature_rampage";
                case "bloodthirsty" -> "berserker_signature_bloodthirsty";
                case "berserking" -> "berserker_signature_berserking";
                case "evasion" -> "rogue_signature_evasion";
                case "preparation" -> "rogue_signature_preparation";
                case "siphoning_strikes" -> "rogue_signature_siphoning_strikes";
                case "disengage" -> "ranger_signature_disengage";
                case "arrow_rain" -> "ranger_signature_arrow_rain";
                case "elemental_arrows" -> "ranger_signature_elemental_arrows";
                case "elemental_surge" -> "spellblade_signature_elemental_surge";
                case "elemental_impact" -> "spellblade_signature_elemental_impact";
                case "spellweaver" -> "spellblade_signature_spellweaver";
                case "consecration" -> "crusader_signature_consecration";
                case "sacred_onslaught" -> "crusader_signature_sacred_onslaught";
                case "heavensmiths_call" -> "crusader_signature_heavensmiths_call";
                case "sacred_orb" -> "cleric_signature_sacred_orb";
                case "divine_intervention" -> "cleric_signature_divine_intervention";
                case "anoint_weapon" -> "cleric_signature_anoint_weapon";
                case "curtain_call" -> "necromancer_shadow_combust";
                case "endless_devour" -> "necromancer_blood_harvest";
                case "rider_charge" -> "berserker_signature_rampage";
                case "rider_war_aura" -> "cleric_signature_anoint_weapon";
                case "rider_blazing_hooves" -> "spellblade_signature_elemental_surge";
                case "impaling_volley" -> "lancer_signature_impaling_volley";
                case "dragon_thrust" -> "lancer_signature_dragon_thrust";
                case "spearstorm" -> "lancer_signature_spearstorm";
                default -> "";
            };
        }
        if (path.isBlank()) return SmoothClasses.id("textures/gui/cooldown_overlay.png");
        return SmoothClasses.id("textures/icons/alternate_reduced/" + path + ".png");
    }
}
