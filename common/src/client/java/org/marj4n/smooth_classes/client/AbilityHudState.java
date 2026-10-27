package org.marj4n.smooth_classes.client;

import net.minecraft.util.Identifier;
import net.minecraft.item.ItemStack;
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

    public static boolean avengerSummonVisible;
    public static int avengerSummonCharges;
    public static int avengerSummonMaxCharges = 3;
    public static int avengerSummonRechargeMs = 20000;
    public static long avengerSummonReadyAtMs = 0L;

    public static boolean shadowActive;
    public static long shadowExpiresAtMs = 0L;
    public static int shadowRange = 10;

    public static boolean classSpecialVisible;
    public static String classSpecialId = "";
    public static int classSpecialCooldownMs = 500;
    public static long classSpecialReadyAtMs = 0L;
    public static boolean classSpecialActive;
    public static int classSpecialVariant = -1;
    public static int classSpecialModeTotalMs = 1;
    public static long classSpecialModeReadyAtMs = 0L;
    public static int classSpecialSecondaryTotalMs = 1;
    public static long classSpecialSecondaryReadyAtMs = 0L;
    public static int treasuryCapacity = 4;
    public static ItemStack[] treasurySlots = new ItemStack[]{
            ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
            ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY
    };

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

    public static long signatureRemainingMs() { return Math.max(0L, signatureReadyAtMs - System.currentTimeMillis()); }
    public static long ascendancyRemainingMs() { return Math.max(0L, ascendancyReadyAtMs - System.currentTimeMillis()); }
    public static void syncAvengerSummon(boolean visible, int charges, int maxCharges, int totalTicks, long remainingTicks) {
        avengerSummonVisible = visible;
        avengerSummonCharges = Math.max(0, charges);
        avengerSummonMaxCharges = Math.max(1, maxCharges);
        avengerSummonRechargeMs = Math.max(1, totalTicks * 50);
        avengerSummonReadyAtMs = System.currentTimeMillis() + Math.max(0L, remainingTicks) * 50L;
    }

    public static long avengerSummonRemainingMs() { return Math.max(0L, avengerSummonReadyAtMs - System.currentTimeMillis()); }

    public static void syncShadow(boolean active, long remainingTicks, int range) {
        shadowActive = active;
        shadowExpiresAtMs = System.currentTimeMillis() + Math.max(0L, remainingTicks) * 50L;
        shadowRange = Math.max(1, range);
    }

    public static long shadowRemainingMs() { return shadowActive ? Math.max(0L, shadowExpiresAtMs - System.currentTimeMillis()) : 0L; }

    public static void syncClassSpecial(String id, int totalTicks, long remainingTicks, boolean active, int variant,
                                        int modeTotalTicks, long modeRemainingTicks,
                                        int secondaryTotalTicks, long secondaryRemainingTicks) {
        classSpecialId = id == null ? "" : id;
        classSpecialVisible = !classSpecialId.isBlank();
        classSpecialCooldownMs = Math.max(1, totalTicks * 50);
        long now = System.currentTimeMillis();
        classSpecialReadyAtMs = now + Math.max(0L, remainingTicks) * 50L;
        classSpecialActive = active;
        classSpecialVariant = variant;
        classSpecialModeTotalMs = Math.max(1, modeTotalTicks * 50);
        classSpecialModeReadyAtMs = now + Math.max(0L, modeRemainingTicks) * 50L;
        classSpecialSecondaryTotalMs = Math.max(1, secondaryTotalTicks * 50);
        classSpecialSecondaryReadyAtMs = now + Math.max(0L, secondaryRemainingTicks) * 50L;
    }

    public static long classSpecialRemainingMs() {
        return Math.max(0L, classSpecialReadyAtMs - System.currentTimeMillis());
    }
    public static long classSpecialModeRemainingMs() {
        return Math.max(0L, classSpecialModeReadyAtMs - System.currentTimeMillis());
    }
    public static long classSpecialSecondaryRemainingMs() {
        return Math.max(0L, classSpecialSecondaryReadyAtMs - System.currentTimeMillis());
    }

    public static void syncTreasury(int capacity, ItemStack[] slots) {
        treasuryCapacity = Math.max(4, Math.min(8, capacity));
        ItemStack[] copy = new ItemStack[]{
                ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
                ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY
        };
        if (slots != null) {
            for (int i = 0; i < Math.min(copy.length, slots.length); i++) {
                ItemStack stack = slots[i];
                copy[i] = stack == null || stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
            }
        }
        treasurySlots = copy;
    }

    public static boolean treasuryHasAny() {
        for (int i = 0; i < treasuryCapacity; i++) { ItemStack stack = treasurySlots[i]; if (stack != null && !stack.isEmpty()) return true; }
        return false;
    }

    public static Identifier classSpecialIcon() {
        return switch (classSpecialId) {
            case "summoning_ritual" -> avengerSummonIcon();
            case "conjure_mount" -> SmoothClasses.id("textures/icons/classes/rider.png");
            case "crimson_revenant" -> berserkerSpecialIcon();
            case "treasury_key" -> SmoothClasses.id("textures/icons/classes/archer.png");
            case "radiant_burst" -> SmoothClasses.id("textures/icons/classes/saber.png");
            case "high_jump" -> SmoothClasses.id("textures/icons/classes/lancer.png");
            case "vanish" -> SmoothClasses.id("textures/icons/classes/assassin.png");
            case "arcane_attunement" -> schoolIcon(classSpecialVariant);
            case "spell_imprint" -> schoolIcon(classSpecialVariant);
            case "divine_edict" -> SmoothClasses.id("textures/icons/classes/ruler.png");
            default -> SmoothClasses.id("textures/gui/cooldown_overlay.png");
        };
    }

    public static String classSpecialBadge() {
        return switch (classSpecialId) {
            // Radial selectors already show the selected icon/name; repeating D/A/E/etc.
            // on the HUD only adds visual noise.
            case "treasury_key", "arcane_attunement", "spell_imprint" -> "";
            case "high_jump" -> classSpecialVariant == 1 ? "^" : classSpecialVariant == 2 ? "v" : "";
            case "divine_edict" -> classSpecialVariant == 1 ? "P" : classSpecialVariant == 2 ? "C" : "";
            default -> "";
        };
    }

    /** Drop every client-side timer when leaving a server/world so stale HUD state never flashes on join. */
    public static void reset() {
        bannerActive = false;
        bloodRainActive = false;
        whenOnHighActive = false;
        signatureAbility = "";
        ascendancyAbility = "";
        signatureCooldownMs = 500;
        ascendancyCooldownMs = 500;
        signatureReadyAtMs = 0L;
        ascendancyReadyAtMs = 0L;
        avengerSummonVisible = false;
        avengerSummonCharges = 0;
        avengerSummonMaxCharges = 3;
        avengerSummonRechargeMs = 20000;
        avengerSummonReadyAtMs = 0L;
        shadowActive = false;
        shadowExpiresAtMs = 0L;
        shadowRange = 10;
        classSpecialVisible = false;
        classSpecialId = "";
        classSpecialCooldownMs = 500;
        classSpecialReadyAtMs = 0L;
        classSpecialActive = false;
        classSpecialVariant = -1;
        classSpecialModeTotalMs = 1;
        classSpecialModeReadyAtMs = 0L;
        classSpecialSecondaryTotalMs = 1;
        classSpecialSecondaryReadyAtMs = 0L;
        treasuryCapacity = 4;
        treasurySlots = new ItemStack[]{ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY};
    }

    public static Identifier classSpecialFallbackIcon() {
        return switch (classSpecialId) {
            case "arcane_attunement" -> SmoothClasses.id("textures/icons/classes/caster.png");
            case "spell_imprint" -> SmoothClasses.id("textures/icons/classes/foreigner.png");
            default -> classSpecialIcon();
        };
    }

    private static Identifier schoolIcon(int variant) {
        return switch (variant) {
            case 0 -> new Identifier("wizards", "textures/item/spell_book/arcane.png");
            case 1 -> new Identifier("wizards", "textures/item/spell_book/fire.png");
            case 2 -> new Identifier("wizards", "textures/item/spell_book/frost.png");
            case 3 -> new Identifier("elemental_wizards_rpg", "textures/item/spellbooks/wind_spell_book.png");
            case 4 -> new Identifier("elemental_wizards_rpg", "textures/item/spellbooks/aqua_spell_book.png");
            case 5 -> new Identifier("elemental_wizards_rpg", "textures/item/spellbooks/terra_spell_book.png");
            default -> SmoothClasses.id("textures/icons/classes/caster.png");
        };
    }

    public static Identifier signatureIcon() { return icon(signatureAbility, false); }
    public static Identifier ascendancyIcon() { return icon(ascendancyAbility, true); }
    public static Identifier avengerSummonIcon() { return SmoothClasses.id("textures/icons/alternate_reduced/necromancer_signature_summoning_ritual.png"); }
    public static Identifier berserkerSpecialIcon() { return SmoothClasses.id("textures/gui/berserker_class_special.png"); }

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
                case "unlimited_blade_works" -> "unlimited_blade_works";
                case "arrow_rain" -> "ranger_signature_arrow_rain";
                case "elemental_arrows" -> "ranger_signature_elemental_arrows";
                case "elemental_surge" -> "spellblade_signature_elemental_surge";
                case "elemental_impact" -> "spellblade_signature_elemental_impact";
                case "spellweaver" -> "spellblade_signature_spellweaver";
                case "consecration" -> "crusader_signature_consecration";
                case "sacred_onslaught" -> "crusader_signature_sacred_onslaught";
                case "heavensmiths_call" -> "crusader_signature_divine_adjudication";
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
