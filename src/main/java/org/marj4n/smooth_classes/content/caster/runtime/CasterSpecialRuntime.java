package org.marj4n.smooth_classes.content.caster.runtime;

import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.spell_power.api.SpellSchool;
import org.marj4n.smooth_classes.content.caster.CasterClass;
import org.marj4n.smooth_classes.runtime.AbilityRuntime;
import org.marj4n.smooth_classes.runtime.ExecutionResult;
import org.marj4n.smooth_classes.runtime.SpellSchoolSelection;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Caster H special: hold H, choose a school, release H to attune. */
public final class CasterSpecialRuntime {
    public static final int HUD_COOLDOWN_TICKS = 1;
    private static final double UNIVERSAL_BONUS = 0.08D;
    private static final double SELECTED_EXTRA_BONUS = 0.07D;
    private static final UUID UNIVERSAL_MODIFIER_ID = UUID.fromString("cd71d826-2a2b-41bb-806f-45584d32363d");
    private static final UUID SELECTED_MODIFIER_ID = UUID.fromString("b17bb1b6-7e1f-45e3-9e5f-08d1b2c80511");
    private static final UUID ORB_RESONANCE_MODIFIER_ID = UUID.fromString("7dc29d85-8dd2-4bc7-826c-e8d255ae06ef");
    private static final Map<UUID, Integer> SELECTED = new HashMap<>();

    private CasterSpecialRuntime() {}

    public static ExecutionResult activate(ServerPlayerEntity player, boolean ignored) {
        if (!AbilityRuntime.isClass(player, CasterClass.ID))
            return ExecutionResult.failure("Only Caster can use Elemental Attunement.");
        return ExecutionResult.failure("Hold H, choose a spell school, then release H.");
    }

    public static ExecutionResult select(ServerPlayerEntity player, int index) {
        if (!AbilityRuntime.isClass(player, CasterClass.ID))
            return ExecutionResult.failure("Only Caster can use Elemental Attunement.");
        if (index < 0 || index >= SpellSchoolSelection.SIZE)
            return ExecutionResult.failure("No spell school selected.");

        SpellSchool school = SpellSchoolSelection.school(index);
        if (school == null)
            return ExecutionResult.failure(SpellSchoolSelection.name(index) + " magic is not registered in this modpack.");

        SELECTED.put(player.getUuid(), index);
        apply(player, index);
        attunementFx(player, index);
        player.sendMessage(Text.literal("Attunement: " + SpellSchoolSelection.name(index))
                .formatted(SpellSchoolSelection.color(index)), true);
        return ExecutionResult.success(1, SpellSchoolSelection.name(index) + " Attunement: +15% selected school, +8% other schools.");
    }

    public static void tick(ServerPlayerEntity player) {
        Integer selected = SELECTED.get(player.getUuid());
        if (!AbilityRuntime.isClass(player, CasterClass.ID)) {
            if (selected != null) {
                clearModifiers(player);
                SELECTED.remove(player.getUuid());
            }
            return;
        }
        // Modifiers are applied on selection and remain on the player until the
        // class changes. Avoid removing/re-adding six attributes every server tick.
    }

    public static int variant(ServerPlayerEntity player) { return SELECTED.getOrDefault(player.getUuid(), -1); }
    public static SpellSchool selectedSchool(ServerPlayerEntity player) {
        Integer index = SELECTED.get(player.getUuid());
        return index == null ? null : SpellSchoolSelection.school(index);
    }
    public static boolean selected(ServerPlayerEntity player) { return SELECTED.containsKey(player.getUuid()); }

    /** Lightning Orb upgrade: each nearby owned orb reinforces whichever school H currently attunes. */
    public static void applyOrbResonance(ServerPlayerEntity player, int stacks) {
        clearOrbResonance(player);
        if (stacks <= 0) return;
        SpellSchool school = selectedSchool(player);
        if (school == null) return;
        EntityAttribute attribute = attributeOf(school);
        if (attribute == null) return;
        EntityAttributeInstance instance = player.getAttributeInstance(attribute);
        if (instance == null) return;
        double bonus = Math.min(5, stacks) * 0.03D;
        instance.addTemporaryModifier(new EntityAttributeModifier(
                ORB_RESONANCE_MODIFIER_ID, "Smooth Classes Caster Orb Resonance", bonus,
                EntityAttributeModifier.Operation.MULTIPLY_TOTAL));
    }

    private static void clearOrbResonance(ServerPlayerEntity player) {
        for (int i = 0; i < SpellSchoolSelection.SIZE; i++) {
            SpellSchool school = SpellSchoolSelection.school(i);
            if (school == null) continue;
            EntityAttribute attribute = attributeOf(school);
            if (attribute == null) continue;
            EntityAttributeInstance instance = player.getAttributeInstance(attribute);
            if (instance != null) instance.removeModifier(ORB_RESONANCE_MODIFIER_ID);
        }
    }

    public static void cleanup(ServerPlayerEntity player) { SELECTED.remove(player.getUuid()); clearModifiers(player); }
    public static void clear() { SELECTED.clear(); }

    private static void apply(ServerPlayerEntity player, int selected) {
        clearModifiers(player);
        for (int i = 0; i < SpellSchoolSelection.SIZE; i++) {
            SpellSchool school = SpellSchoolSelection.school(i);
            if (school == null) continue;
            EntityAttribute attribute = attributeOf(school);
            if (attribute == null) continue;
            EntityAttributeInstance instance = player.getAttributeInstance(attribute);
            if (instance == null) continue;
            instance.addTemporaryModifier(new EntityAttributeModifier(
                    UNIVERSAL_MODIFIER_ID, "Smooth Classes Caster Universal Mastery", UNIVERSAL_BONUS,
                    EntityAttributeModifier.Operation.MULTIPLY_TOTAL));
            if (i == selected) {
                instance.addTemporaryModifier(new EntityAttributeModifier(
                        SELECTED_MODIFIER_ID, "Smooth Classes Caster Attunement", SELECTED_EXTRA_BONUS,
                        EntityAttributeModifier.Operation.MULTIPLY_TOTAL));
            }
        }
    }

    private static void clearModifiers(ServerPlayerEntity player) {
        for (int i = 0; i < SpellSchoolSelection.SIZE; i++) {
            SpellSchool school = SpellSchoolSelection.school(i);
            if (school == null) continue;
            EntityAttribute attribute = attributeOf(school);
            if (attribute == null) continue;
            EntityAttributeInstance instance = player.getAttributeInstance(attribute);
            if (instance != null) {
                instance.removeModifier(UNIVERSAL_MODIFIER_ID);
                instance.removeModifier(SELECTED_MODIFIER_ID);
                instance.removeModifier(ORB_RESONANCE_MODIFIER_ID);
            }
        }
    }

    private static EntityAttribute attributeOf(SpellSchool school) {
        EntityAttribute owned = school.ownedAttribute();
        if (owned != null) return owned;
        var entry = school.getAttributeEntry();
        return entry == null ? null : entry.value();
    }

    private static void attunementFx(ServerPlayerEntity player, int selected) {
        player.getServerWorld().spawnParticles(SpellSchoolSelection.particle(selected),
                player.getX(), player.getBodyY(0.55D), player.getZ(), 28, 0.7D, 0.9D, 0.7D, 0.08D);
        player.getServerWorld().playSound(null, player.getBlockPos(), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME,
                SoundCategory.PLAYERS, 0.65F, 0.9F + selected * 0.07F);
    }
}
