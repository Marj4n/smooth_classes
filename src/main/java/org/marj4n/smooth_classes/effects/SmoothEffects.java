package org.marj4n.smooth_classes.effects;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import org.marj4n.smooth_classes.SmoothClasses;

/**
 * Full status-effect ID surface from SimplySkills Continued.
 *
 * IDs are owned by Smooth Classes. Behavioral logic lives in class/ascendancy
 * runtimes instead of one giant legacy effect registry, keeping this registry
 * data-oriented and safe for future development.
 */
public final class SmoothEffects {
    private SmoothEffects() {}

    public static final StatusEffect BERSERKING = register("berserking", StatusEffectCategory.BENEFICIAL, 0x2FA5AF);
    public static final StatusEffect BLOODTHIRSTY = register("bloodthirsty", StatusEffectCategory.BENEFICIAL, 0x2FA5AF);
    public static final StatusEffect RAMPAGE = register("rampage", StatusEffectCategory.BENEFICIAL, 0x2FA5AF);
    public static final StatusEffect EVASION = register("evasion", StatusEffectCategory.BENEFICIAL, 0x2FA5AF);
    public static final StatusEffect SIPHONING_STRIKES = register("siphoning_strikes", StatusEffectCategory.BENEFICIAL, 0x2FA5AF);
    public static final StatusEffect ELEMENTAL_ARROWS = register("elemental_arrows", StatusEffectCategory.BENEFICIAL, 0x2FA5AF);
    public static final StatusEffect ARROW_RAIN = register("arrow_rain", StatusEffectCategory.BENEFICIAL, 0x2FA5AF);
    public static final StatusEffect FROST_VOLLEY = register("frost_volley", StatusEffectCategory.BENEFICIAL, 0x2FA5AF);
    public static final StatusEffect ARCANE_VOLLEY = register("arcane_volley", StatusEffectCategory.BENEFICIAL, 0x2FA5AF);
    public static final StatusEffect METEORIC_WRATH = register("meteoric_wrath", StatusEffectCategory.BENEFICIAL, 0x2FA5AF);
    public static final StatusEffect STATIC_CHARGE = register("static_charge", StatusEffectCategory.BENEFICIAL, 0x2FA5AF);
    public static final StatusEffect ELEMENTAL_SURGE = register("elemental_surge", StatusEffectCategory.BENEFICIAL, 0x2FA5AF);
    public static final StatusEffect ELEMENTAL_IMPACT = register("elemental_impact", StatusEffectCategory.BENEFICIAL, 0x2FA5AF);
    public static final StatusEffect SPELLWEAVER = register("spellweaver", StatusEffectCategory.BENEFICIAL, 0x2FA5AF);
    public static final StatusEffect FANOFBLADES = register("fanofblades", StatusEffectCategory.BENEFICIAL, 0x2FA5AF);
    public static final StatusEffect DISENCHANTMENT = register("disenchantment", StatusEffectCategory.HARMFUL, 0x2FA5AF);
    public static final StatusEffect BULLRUSH = register("bullrush", StatusEffectCategory.BENEFICIAL, 0x2FA5AF);
    public static final StatusEffect IMMOBILIZE = register("immobilize", StatusEffectCategory.HARMFUL, 0x2FA5AF);
    public static final StatusEffect LEAPSLAM = register("leapslam", StatusEffectCategory.BENEFICIAL, 0x2FA5AF);
    public static final StatusEffect IMMOBILIZING_AURA = register("immobilizing_aura", StatusEffectCategory.HARMFUL, 0x2FA5AF);
    public static final StatusEffect SPELLBREAKING = register("spellbreaking", StatusEffectCategory.BENEFICIAL, 0x2FA5AF);
    public static final StatusEffect EARTHSHAKER = register("earthshaker", StatusEffectCategory.BENEFICIAL, 0x2FA5AF);
    public static final StatusEffect ARCANE_ATTUNEMENT = register("arcane_attunement", StatusEffectCategory.BENEFICIAL, 0x2FA5AF);
    public static final StatusEffect HOLY_ATTUNEMENT = register("holy_attunement", StatusEffectCategory.BENEFICIAL, 0x2FA5AF);
    public static final StatusEffect SOUL_ATTUNEMENT = register("soul_attunement", StatusEffectCategory.BENEFICIAL, 0x2FA5AF);
    public static final StatusEffect FIRE_ATTUNEMENT = register("fire_attunement", StatusEffectCategory.BENEFICIAL, 0x2FA5AF);
    public static final StatusEffect FROST_ATTUNEMENT = register("frost_attunement", StatusEffectCategory.BENEFICIAL, 0x2FA5AF);
    public static final StatusEffect LIGHTNING_ATTUNEMENT = register("lightning_attunement", StatusEffectCategory.BENEFICIAL, 0x2FA5AF);
    public static final StatusEffect PRECISION = register("precision", StatusEffectCategory.BENEFICIAL, 0x2FA5AF);
    public static final StatusEffect DEATH_MARK = register("death_mark", StatusEffectCategory.HARMFUL, 0x2FA5AF);
    public static final StatusEffect MARKSMAN = register("marksman", StatusEffectCategory.BENEFICIAL, 0x2FA5AF);
    public static final StatusEffect STEALTH = register("stealth", StatusEffectCategory.BENEFICIAL, 0x2FA5AF);
    public static final StatusEffect MIGHT = register("might", StatusEffectCategory.BENEFICIAL, 0x2FA5AF);
    public static final StatusEffect EXHAUSTION = register("exhaustion", StatusEffectCategory.HARMFUL, 0x2FA5AF);
    public static final StatusEffect REVEALED = register("revealed", StatusEffectCategory.HARMFUL, 0x2FA5AF);
    public static final StatusEffect BARRIER = register("barrier", StatusEffectCategory.BENEFICIAL, 0x2FA5AF);
    public static final StatusEffect SOULSHOCK = register("soulshock", StatusEffectCategory.BENEFICIAL, 0x2FA5AF);
    public static final StatusEffect SPELLFORGED = register("spellforged", StatusEffectCategory.BENEFICIAL, 0x2FA5AF);
    public static final StatusEffect DIVINE_ADJUDICATION = register("divine_adjudication", StatusEffectCategory.BENEFICIAL, 0x2FA5AF);
    public static final StatusEffect TAUNTED = register("taunted", StatusEffectCategory.HARMFUL, 0x2FA5AF);
    public static final StatusEffect UNDYING = register("undying", StatusEffectCategory.BENEFICIAL, 0x2FA5AF);
    public static final StatusEffect DIVINE_RAY = register("divine_ray", StatusEffectCategory.BENEFICIAL, 0xFFF2C6);
    public static final StatusEffect RAGE = register("rage", StatusEffectCategory.BENEFICIAL, 0x2FA5AF);
    public static final StatusEffect OVERLOAD = register("overload", StatusEffectCategory.BENEFICIAL, 0x2FA5AF);
    public static final StatusEffect BLADESTORM = register("bladestorm", StatusEffectCategory.BENEFICIAL, 0x2FA5AF);
    public static final StatusEffect VITALITY_BOND = register("vitality_bond", StatusEffectCategory.BENEFICIAL, 0x2FA5AF);
    public static final StatusEffect ANOINTED = register("anointed", StatusEffectCategory.BENEFICIAL, 0x2FA5AF);
    public static final StatusEffect MARKSMANSHIP = register("marksmanship", StatusEffectCategory.BENEFICIAL, 0x2FA5AF);
    public static final StatusEffect AGILE = register("agile", StatusEffectCategory.BENEFICIAL, 0x2FA5AF);
    public static final StatusEffect RIGHTEOUS_HAMMERS = register("righteous_hammers", StatusEffectCategory.BENEFICIAL, 0x2FA5AF);
    public static final StatusEffect BONE_ARMOR = register("bone_armor", StatusEffectCategory.BENEFICIAL, 0x2FA5AF);
    public static final StatusEffect CYCLONIC_CLEAVE = register("cyclonic_cleave", StatusEffectCategory.BENEFICIAL, 0x2FA5AF);
    public static final StatusEffect MAGIC_CIRCLE = register("magic_circle", StatusEffectCategory.BENEFICIAL, 0x2FA5AF);
    public static final StatusEffect ARCANE_SLASH = register("arcane_slash", StatusEffectCategory.BENEFICIAL, 0x2FA5AF)
            .addAttributeModifier(net.minecraft.entity.attribute.EntityAttributes.GENERIC_MOVEMENT_SPEED,
                    "e12e9fa1-a053-4e6a-9ead-186993319ec4", -0.7D,
                    net.minecraft.entity.attribute.EntityAttributeModifier.Operation.MULTIPLY_TOTAL);
    public static final StatusEffect AGONY = register("agony", StatusEffectCategory.HARMFUL, 0x2FA5AF);
    public static final StatusEffect TORMENT = register("torment", StatusEffectCategory.HARMFUL, 0x2FA5AF);
    public static final StatusEffect RAPIDFIRE = register("rapidfire", StatusEffectCategory.BENEFICIAL, 0x2FA5AF);
    public static final StatusEffect CATACLYSM = register("cataclysm", StatusEffectCategory.BENEFICIAL, 0x2FA5AF);
    public static final StatusEffect GHOSTWALK = register("ghostwalk", StatusEffectCategory.BENEFICIAL, 0x2FA5AF);
    public static final StatusEffect SKYWARD_SUNDER = register("skyward_sunder", StatusEffectCategory.BENEFICIAL, 0x2FA5AF);
    public static final StatusEffect RIGHTEOUS_SHIELD = register("righteous_shield", StatusEffectCategory.BENEFICIAL, 0x2FA5AF);
    public static final StatusEffect GOLDEN_AEGIS = register("golden_aegis", StatusEffectCategory.BENEFICIAL, 0x2FA5AF);
    public static final StatusEffect SHADOW_AURA = register("shadow_aura", StatusEffectCategory.BENEFICIAL, 0x2FA5AF);
    public static final StatusEffect FOCUS = register("focus", StatusEffectCategory.BENEFICIAL, 0x2FA5AF);
    public static final StatusEffect TITANS_GRIP = register("titans_grip", StatusEffectCategory.BENEFICIAL, 0x2FA5AF);
    public static final StatusEffect MELODY_OF_WAR = register("melody_of_war", StatusEffectCategory.BENEFICIAL, 0x2FA5AF);
    public static final StatusEffect MELODY_OF_SWIFTNESS = register("melody_of_swiftness", StatusEffectCategory.BENEFICIAL, 0x2FA5AF);
    public static final StatusEffect MELODY_OF_PROTECTION = register("melody_of_protection", StatusEffectCategory.BENEFICIAL, 0x2FA5AF);
    public static final StatusEffect MELODY_OF_SAFETY = register("melody_of_safety", StatusEffectCategory.BENEFICIAL, 0x2FA5AF);
    public static final StatusEffect MELODY_OF_CONCENTRATION = register("melody_of_concentration", StatusEffectCategory.BENEFICIAL, 0x2FA5AF);
    public static final StatusEffect MELODY_OF_BLOODLUST = register("melody_of_bloodlust", StatusEffectCategory.BENEFICIAL, 0x2FA5AF);
    public static final StatusEffect RAGING_JAVELIN = register("raging_javelin", StatusEffectCategory.BENEFICIAL, 0x2FA5AF);
    public static final StatusEffect CONSECRATION = register("consecration", StatusEffectCategory.BENEFICIAL, 0x2FA5AF);
    public static final StatusEffect SACRED_ONSLAUGHT = register("sacred_onslaught", StatusEffectCategory.BENEFICIAL, 0x2FA5AF);

    private static StatusEffect register(String id, StatusEffectCategory category, int color) {
        return Registry.register(Registries.STATUS_EFFECT, SmoothClasses.id(id), new SmoothStatusEffect(id, category, color));
    }

    public static void register() {
        SmoothClasses.LOGGER.info("Registered {} Smooth Classes status effects (SimplySkills parity surface).", 73);
    }
}
