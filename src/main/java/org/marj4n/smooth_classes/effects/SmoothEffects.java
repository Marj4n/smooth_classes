package org.marj4n.smooth_classes.effects;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.spell_power.api.SpellSchools;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import org.marj4n.smooth_classes.SmoothClasses;

/**
 * Full status-effect ID surface from Smooth Classes.
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
    public static final StatusEffect ARCANE_ATTUNEMENT = attunement("arcane_attunement", SpellSchools.ARCANE.ownedAttribute(), "3c19610f-6543-4f1a-a538-f6bcbb167120");
    public static final StatusEffect HOLY_ATTUNEMENT = attunement("holy_attunement", SpellSchools.HEALING.ownedAttribute(), "7b3dc959-518b-4ba2-b4c6-559802c864aa");
    public static final StatusEffect SOUL_ATTUNEMENT = attunement("soul_attunement", SpellSchools.SOUL.ownedAttribute(), "58a182e1-da16-4cdb-aa83-0e43ad769683");
    public static final StatusEffect FIRE_ATTUNEMENT = attunement("fire_attunement", SpellSchools.FIRE.ownedAttribute(), "1d60925b-1818-4f69-b73e-115695a5e49d");
    public static final StatusEffect FROST_ATTUNEMENT = attunement("frost_attunement", SpellSchools.FROST.ownedAttribute(), "ac14e6bb-ae54-4917-842a-f004041604bd");
    public static final StatusEffect LIGHTNING_ATTUNEMENT = attunement("lightning_attunement", SpellSchools.LIGHTNING.ownedAttribute(), "4d10144d-85ea-4e76-a7bb-9164c93b48bd");
    public static final StatusEffect PRECISION = register("precision", StatusEffectCategory.BENEFICIAL, 0x2FA5AF);
    public static final StatusEffect DEATH_MARK = register("death_mark", StatusEffectCategory.HARMFUL, 0x2FA5AF);
    public static final StatusEffect MARKSMAN = register("marksman", StatusEffectCategory.BENEFICIAL, 0x2FA5AF);
    public static final StatusEffect STEALTH = register("stealth", StatusEffectCategory.BENEFICIAL, 0x2FA5AF);
    public static final StatusEffect MIGHT = register("might", StatusEffectCategory.BENEFICIAL, 0x2FA5AF)
            .addAttributeModifier(EntityAttributes.GENERIC_ATTACK_DAMAGE,
                    "72143d4d-ca67-4167-90c7-1d5703ff9745", 0.10D,
                    EntityAttributeModifier.Operation.MULTIPLY_TOTAL);
    public static final StatusEffect EXHAUSTION = register("exhaustion", StatusEffectCategory.HARMFUL, 0x2FA5AF);
    public static final StatusEffect REVEALED = register("revealed", StatusEffectCategory.HARMFUL, 0x2FA5AF);
    public static final StatusEffect BARRIER = register("barrier", StatusEffectCategory.BENEFICIAL, 0x2FA5AF);
    public static final StatusEffect SOULSHOCK = register("soulshock", StatusEffectCategory.BENEFICIAL, 0x2FA5AF);
    public static final StatusEffect SPELLFORGED = register("spellforged", StatusEffectCategory.BENEFICIAL, 0x2FA5AF)
            .addAttributeModifier(SpellSchools.ARCANE.ownedAttribute(), "9ba51f60-f472-401e-bf64-f0d931dd6ea0", 0.25D, EntityAttributeModifier.Operation.ADDITION)
            .addAttributeModifier(SpellSchools.FIRE.ownedAttribute(), "3f55baef-eb34-4480-9bb6-8a229536e1cf", 0.25D, EntityAttributeModifier.Operation.ADDITION)
            .addAttributeModifier(SpellSchools.FROST.ownedAttribute(), "cce1f3dd-dd9d-465a-9dc4-d23c6ff8df81", 0.25D, EntityAttributeModifier.Operation.ADDITION)
            .addAttributeModifier(SpellSchools.HEALING.ownedAttribute(), "830b5e10-234c-4c72-a126-60763bc6d82f", 0.25D, EntityAttributeModifier.Operation.ADDITION)
            .addAttributeModifier(SpellSchools.LIGHTNING.ownedAttribute(), "66af1056-39fd-4c84-a8b8-6da0e53b46ad", 0.25D, EntityAttributeModifier.Operation.ADDITION)
            .addAttributeModifier(SpellSchools.SOUL.ownedAttribute(), "b28b2a02-dcbf-45c3-a242-a75fd895a2e3", 0.25D, EntityAttributeModifier.Operation.ADDITION);
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
    // Legacy transient ID retained for save compatibility; Raining Blood uses BloodRainRuntime.
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
    public static final StatusEffect CRIMSON_REVENANT_CHARGE = register("crimson_revenant_charge", StatusEffectCategory.BENEFICIAL, 0xA30000)
            .addAttributeModifier(EntityAttributes.GENERIC_MOVEMENT_SPEED,
                    "4c20a5c0-b508-4181-9ef2-4f2537db4ec4", -0.65D,
                    EntityAttributeModifier.Operation.MULTIPLY_TOTAL);
    public static final StatusEffect CRIMSON_REVENANT = register("crimson_revenant", StatusEffectCategory.BENEFICIAL, 0xC40000)
            .addAttributeModifier(EntityAttributes.GENERIC_MAX_HEALTH,
                    "9c7b47ca-4bea-46d1-b44f-23e3c58d08cb", 0.40D,
                    EntityAttributeModifier.Operation.MULTIPLY_BASE)
            .addAttributeModifier(EntityAttributes.GENERIC_ATTACK_DAMAGE,
                    "3db610ff-2da8-48dd-88e6-75cbf59dd3c4", 6.0D,
                    EntityAttributeModifier.Operation.ADDITION)
            .addAttributeModifier(EntityAttributes.GENERIC_MOVEMENT_SPEED,
                    "83d09c1f-4498-4e9c-b0c2-102558a78611", 0.28D,
                    EntityAttributeModifier.Operation.MULTIPLY_TOTAL)
            .addAttributeModifier(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE,
                    "19366da4-2425-43ae-b4b0-28c4f55fdb0f", 0.75D,
                    EntityAttributeModifier.Operation.ADDITION)
            .addAttributeModifier(EntityAttributes.GENERIC_ARMOR,
                    "0e6d25b3-526f-4610-bb96-df7e18d2f77e", 6.0D,
                    EntityAttributeModifier.Operation.ADDITION);


    private static StatusEffect attunement(String id, net.minecraft.entity.attribute.EntityAttribute attribute, String uuid) {
        return register(id, StatusEffectCategory.BENEFICIAL, 0x2FA5AF)
                .addAttributeModifier(attribute, uuid, 0.02D, EntityAttributeModifier.Operation.MULTIPLY_TOTAL);
    }

    private static StatusEffect register(String id, StatusEffectCategory category, int color) {
        return Registry.register(Registries.STATUS_EFFECT, SmoothClasses.id(id), new SmoothStatusEffect(id, category, color));
    }

    public static void register() {
        long count = Registries.STATUS_EFFECT.getIds().stream()
                .filter(id -> SmoothClasses.MOD_ID.equals(id.getNamespace()))
                .count();
        SmoothClasses.LOGGER.info("Registered {} Smooth Classes status effects.", count);
    }
}
