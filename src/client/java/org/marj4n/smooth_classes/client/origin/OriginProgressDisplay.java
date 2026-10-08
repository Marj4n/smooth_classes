package org.marj4n.smooth_classes.client.origin;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.marj4n.smooth_classes.origin.OriginType;
import org.marj4n.smooth_classes.origin.VampireProgressionRequirements;

import java.util.ArrayList;
import java.util.List;

/** Client-only presentation helpers for live Origin milestone counters. */
public final class OriginProgressDisplay {
    private OriginProgressDisplay() {}

    public static List<Text> compactPanel(OriginType origin) {
        List<Text> lines = new ArrayList<>();
        lines.add(Text.literal(origin.displayName() + " Progress").formatted(Formatting.GOLD));
        switch (origin) {
            case HUMAN -> {
                lines.add(count("Harsh environments", OriginClientState.countFlags("human.environment_mastered."), 3));
                lines.add(count("Major enemies", OriginClientState.progress("human.major_kills"), 3));
            }
            case VAMPIRE -> vampireCompact(lines);
            case WEREWOLF -> {
                lines.add(count("Meat species eaten", OriginClientState.countFlags("werewolf.meat_species."), 8));
                lines.add(count("Hunted meat eaten", OriginClientState.progress("werewolf.meat_eaten"), 32));
                lines.add(count("Night prey", OriginClientState.progress("werewolf.night_prey"), 20));
            }
            case MERMAID -> {
                lines.add(check("Nautilus Shell found", OriginClientState.hasFlag("mermaid.has_nautilus")));
                lines.add(count("Drowned defeated", OriginClientState.progress("mermaid.drowned_kills"), 10));
                lines.add(time("Time submerged", OriginClientState.progress("mermaid.submerged_ticks"), 20 * 60 * 20));
            }
            case DEMON -> {
                lines.add(time("Time in lava", OriginClientState.progress("demon.lava_ticks"), 60 * 20));
                lines.add(count("Magma Cubes", OriginClientState.progress("demon.magma_kills"), 10));
                lines.add(count("Blazes", OriginClientState.progress("demon.blaze_kills"), 10));
            }
            case ANGEL -> {
                lines.add(count("Hostile species", OriginClientState.countFlags("angel.hostile_species."), 6));
                lines.add(time("Time airborne", OriginClientState.progress("angel.airborne_ticks"), 5 * 60 * 20));
            }
            case SLIME -> {
                lines.add(count("Humanoid residue", OriginClientState.progress("slime.humanoid_residue"), 20));
                lines.add(count("Humanoid species", OriginClientState.countFlags("slime.humanoid_species."), 4));
                lines.add(time("Observe humanoids", OriginClientState.progress("slime.observe_ticks"), 10 * 60 * 20));
            }
            case HOMUNCULUS -> {
                lines.add(count("Weapon archetypes", OriginClientState.countFlags("homunculus.weapon_archetype."), 3));
                lines.add(count("Manifested kills", OriginClientState.progress("homunculus.manifested_kills"), 30));
                lines.add(check("Enchanted weapon absorbed", OriginClientState.hasFlag("homunculus.absorbed_enchanted")));
            }
            case VOID -> {
                lines.add(count("Chorus teleports", OriginClientState.progress("void.chorus_uses"), 32));
                lines.add(time("Time in the End", OriginClientState.progress("void.end_ticks"), 10 * 60 * 20));
                lines.add(resource("Instability", OriginClientState.instability, 100));
            }
            case UNDEAD -> {
                lines.add(count("Soul harvested", OriginClientState.progress("undead.soul_harvested"), 50));
                lines.add(count("Skeletal species", OriginClientState.countFlags("undead.skeletal_species."), 3));
                lines.add(resource("Soul Hunger", OriginClientState.soul, 100));
            }
            case SPRIGGAN -> {
                lines.add(time("Time in nature", OriginClientState.progress("spriggan.nature_ticks"), 20 * 60 * 20));
                lines.add(resource("Living Mass", OriginClientState.livingMass, 10));
                lines.add(Text.literal("○ Tree-growth trials: upcoming").formatted(Formatting.GRAY));
            }
            case DOPPELGANGER -> {
                lines.add(count("Forms studied", OriginClientState.countFlags("doppelganger.form_studied."), 8));
                lines.add(Text.literal("Stay within 8 blocks for 10s").formatted(Formatting.GRAY));
            }
        }
        return lines;
    }


    /** Shows only the next unfinished Vampire milestone instead of pinning the first quest forever. */
    private static void vampireCompact(List<Text> lines) {
        int bats = OriginClientState.progress("vampire.bats_killed");
        boolean filled = OriginClientState.hasFlag("vampire.filled_blood_once");
        boolean night = OriginClientState.hasFlag("vampire.full_night_survived");
        if (bats < VampireProgressionRequirements.BAT_KILLS || !filled || !night) {
            lines.add(count("Bats killed", bats, VampireProgressionRequirements.BAT_KILLS));
            lines.add(check("Blood reservoir filled", filled));
            lines.add(check("Full night survived", night));
            return;
        }

        int humanoid = OriginClientState.progress("vampire.humanoid_blood");
        int nightKills = OriginClientState.progress("vampire.night_kills");
        if (humanoid < VampireProgressionRequirements.MAN_BAT_HUMANOID_BLOOD || nightKills < VampireProgressionRequirements.MAN_BAT_NIGHT_KILLS) {
            lines.add(Text.literal("Next: Man-Bat").formatted(Formatting.LIGHT_PURPLE));
            lines.add(count("Humanoid Blood", humanoid, VampireProgressionRequirements.MAN_BAT_HUMANOID_BLOOD));
            lines.add(count("Night kills", nightKills, VampireProgressionRequirements.MAN_BAT_NIGHT_KILLS));
            return;
        }

        int lifetime = OriginClientState.progress("vampire.lifetime_blood");
        int categories = OriginClientState.countFlags("vampire.blood_type.");
        if (lifetime < VampireProgressionRequirements.BLOOD_FLASK_LIFETIME || categories < 4) {
            lines.add(Text.literal("Next: Blood Arts").formatted(Formatting.LIGHT_PURPLE));
            lines.add(count("Lifetime Blood", lifetime, VampireProgressionRequirements.BLOOD_FLASK_LIFETIME));
            lines.add(count("Blood Types Tasted", categories, 4));
            return;
        }

        if (lifetime < VampireProgressionRequirements.NOBILITY_LIFETIME || categories < 4) {
            lines.add(Text.literal("Next: Nobility").formatted(Formatting.LIGHT_PURPLE));
            lines.add(count("Lifetime Blood", lifetime, VampireProgressionRequirements.NOBILITY_LIFETIME));
            lines.add(count("Blood Types Tasted", categories, 4));
            return;
        }

        if (nightKills < VampireProgressionRequirements.SUN_TOLERANCE_NIGHT_KILLS || !night) {
            lines.add(Text.literal("Next: Sun Tolerance").formatted(Formatting.LIGHT_PURPLE));
            lines.add(count("Night kills", nightKills, VampireProgressionRequirements.SUN_TOLERANCE_NIGHT_KILLS));
            lines.add(check("Full night survived", night));
            return;
        }

        if (lifetime < VampireProgressionRequirements.LORD_LIFETIME || !OriginClientState.hasFlag("vampire.found_forlorn_hollows")
                || OriginClientState.progress("vampire.forlorn_vesper_kills") < 5
                || OriginClientState.progress("vampire.forlorn_forsaken_kills") < 2) {
            lines.add(Text.literal("Next: Vampire Lord").formatted(Formatting.LIGHT_PURPLE));
            lines.add(count("Lifetime Blood", lifetime, VampireProgressionRequirements.LORD_LIFETIME));
            lines.add(check("Find Forlorn Hollows", OriginClientState.hasFlag("vampire.found_forlorn_hollows")));
            lines.add(count("Vespers defeated in cave", OriginClientState.progress("vampire.forlorn_vesper_kills"), 5));
            lines.add(count("Forsaken defeated in cave", OriginClientState.progress("vampire.forlorn_forsaken_kills"), 2));
            return;
        }

        lines.add(Text.literal("✓ Vampire Lord awakened").formatted(Formatting.GREEN));
        lines.add(Text.literal("Bloodline progression complete").formatted(Formatting.DARK_GRAY));
    }

    /** Full recap shown when hovering the root Origin node. */
    public static List<Text> overviewLines(OriginType origin) {
        List<Text> lines = new ArrayList<>();
        lines.add(Text.literal(origin.subtitle()).formatted(Formatting.GOLD));
        lines.add(Text.literal(origin.lore()).formatted(Formatting.GRAY));
        lines.add(Text.empty());

        lines.add(Text.literal("Buffs").formatted(Formatting.GREEN));
        for (String strength : origin.strengths()) {
            lines.add(Text.literal("+ " + strength).formatted(Formatting.GREEN));
        }

        lines.add(Text.empty());
        lines.add(Text.literal("Nerfs").formatted(Formatting.RED));
        for (String weakness : origin.weaknesses()) {
            lines.add(Text.literal("- " + weakness).formatted(Formatting.RED));
        }

        lines.add(Text.empty());
        lines.add(Text.literal("Diet: ").formatted(Formatting.AQUA)
                .append(Text.literal(origin.diet()).formatted(Formatting.WHITE)));
        if (origin == OriginType.VAMPIRE) {
            lines.add(Text.empty());
            lines.add(Text.literal("Blood Diet — Quality Tiers").formatted(Formatting.GOLD));
            lines.add(Text.literal("Tier IV  Humanoid: +16 Blood / portion (villagers, traders)").formatted(Formatting.LIGHT_PURPLE));
            lines.add(Text.literal("Tier III Livestock: +10 Blood / portion (farm animals)").formatted(Formatting.YELLOW));
            lines.add(Text.literal("Tier II  Wild: +6 Blood / portion (wildlife, modded fauna)").formatted(Formatting.GREEN));
            lines.add(Text.literal("Tier I   Aquatic: +4 Blood / portion (fish, squid, axolotl)").formatted(Formatting.AQUA));
            lines.add(Text.literal("Sleeping villagers grant +4 bonus Blood.").formatted(Formatting.GRAY));
            lines.add(Text.literal("Each creature holds 5 portions. Survivors slowly regenerate;").formatted(Formatting.GRAY));
            lines.add(Text.literal("draining the last portion kills the creature.").formatted(Formatting.GRAY));
            lines.add(Text.literal("Taste all four types to unlock Blood Sense.").formatted(Formatting.LIGHT_PURPLE));
        }
        lines.add(Text.literal("Final Evolution: ").formatted(Formatting.LIGHT_PURPLE)
                .append(Text.literal(origin.evolution()).formatted(Formatting.WHITE)));
        return lines;
    }

    public static List<Text> nodeLines(OriginType origin, String skillId) {
        List<Text> lines = new ArrayList<>();
        switch (origin) {
            case HUMAN -> human(skillId, lines);
            case VAMPIRE -> vampire(skillId, lines);
            case WEREWOLF -> werewolf(skillId, lines);
            case MERMAID -> mermaid(skillId, lines);
            case DEMON -> demon(skillId, lines);
            case ANGEL -> angel(skillId, lines);
            case SLIME -> slime(skillId, lines);
            case HOMUNCULUS -> homunculus(skillId, lines);
            case VOID -> voidOrigin(skillId, lines);
            case UNDEAD -> undead(skillId, lines);
            case SPRIGGAN -> spriggan(skillId, lines);
            case DOPPELGANGER -> doppelganger(skillId, lines);
        }
        return lines;
    }

    private static void human(String id, List<Text> out) {
        switch (id) {
            case "survivor" -> {
                out.add(count("Harsh environments mastered", OriginClientState.countFlags("human.environment_mastered."), 3));
                out.add(Text.literal("10 minutes in each environment").formatted(Formatting.DARK_GRAY));
                out.add(Text.literal("Reward: first Adaptation").formatted(Formatting.AQUA));
            }
            case "wayfarer" -> {
                out.add(count("Dimensions visited", OriginClientState.countFlags("human.dimension_visited."), 3));
                out.add(Text.literal("Reward: second concurrent Adaptation").formatted(Formatting.AQUA));
            }
            case "indomitable" -> {
                out.add(count("Major enemies defeated", OriginClientState.progress("human.major_kills"), 3));
                out.add(count("Adaptations learned", OriginClientState.countFlags("human.environment_mastered."), 3));
            }
            case "endurance", "memory" -> out.add(count("Adaptations learned", OriginClientState.countFlags("human.environment_mastered."), 4));
            case "second_wind" -> {
                out.add(count("Major enemies defeated", OriginClientState.progress("human.major_kills"), 5));
                out.add(Text.literal("Reward: automatic + manual Second Wind").formatted(Formatting.AQUA));
            }
            case "final" -> {
                out.add(count("Adaptations learned", OriginClientState.countFlags("human.environment_mastered."), 5));
                out.add(Text.literal("Reward: three learned Adaptations at once").formatted(Formatting.LIGHT_PURPLE));
            }
        }
    }

    private static void vampire(String id, List<Text> out) {
        switch (id) {
            case "bat_form" -> {
                out.add(count("Bats killed", OriginClientState.progress("vampire.bats_killed"), VampireProgressionRequirements.BAT_KILLS));
                out.add(check("Fill Blood Reservoir once", OriginClientState.hasFlag("vampire.filled_blood_once")));
                out.add(check("Survive a full night", OriginClientState.hasFlag("vampire.full_night_survived")));
                out.add(Text.literal("Reward: true Bat travel form + flight").formatted(Formatting.AQUA));
            }
            case "man_bat" -> {
                out.add(count("Humanoid Blood", OriginClientState.progress("vampire.humanoid_blood"), VampireProgressionRequirements.MAN_BAT_HUMANOID_BLOOD));
                out.add(count("Night kills", OriginClientState.progress("vampire.night_kills"), VampireProgressionRequirements.MAN_BAT_NIGHT_KILLS));
                out.add(Text.literal("Reward: combat Man-Bat transformation").formatted(Formatting.AQUA));
            }
            case "blood_sense" -> {
                out.add(count("Lifetime Blood", OriginClientState.progress("vampire.lifetime_blood"), VampireProgressionRequirements.BLOOD_SENSE_LIFETIME));
                out.add(count("Blood Types Tasted", OriginClientState.countFlags("vampire.blood_type."), 4));
                out.add(Text.literal("Reward: reveal nearby mobs for 10s (20-block radius)").formatted(Formatting.AQUA));
            }
            case "blood_flask" -> out.add(count("Lifetime Blood", OriginClientState.progress("vampire.lifetime_blood"), VampireProgressionRequirements.BLOOD_FLASK_LIFETIME));
            case "nobility" -> {
                out.add(count("Lifetime Blood", OriginClientState.progress("vampire.lifetime_blood"), VampireProgressionRequirements.NOBILITY_LIFETIME));
                out.add(count("Blood Types Tasted", OriginClientState.countFlags("vampire.blood_type."), 4));
            }
            case "sun_tolerance" -> {
                out.add(count("Night kills", OriginClientState.progress("vampire.night_kills"), VampireProgressionRequirements.SUN_TOLERANCE_NIGHT_KILLS));
                out.add(check("Full night survived", OriginClientState.hasFlag("vampire.full_night_survived")));
            }
            case "final" -> {
                out.add(count("Lifetime Blood", OriginClientState.progress("vampire.lifetime_blood"), VampireProgressionRequirements.LORD_LIFETIME));
                out.add(check("Discover Alex's Caves: Forlorn Hollows",
                        OriginClientState.hasFlag("vampire.found_forlorn_hollows")));
                out.add(count("Vespers slain inside Forlorn Hollows",
                        OriginClientState.progress("vampire.forlorn_vesper_kills"), 5));
                out.add(count("Forsaken slain inside Forlorn Hollows",
                        OriginClientState.progress("vampire.forlorn_forsaken_kills"), 2));
                out.add(Text.literal("Reward: 2x Man-Bat damage/movement/flight, normal attack speed, 15s cooldown").formatted(Formatting.LIGHT_PURPLE));
                out.add(Text.literal("Blood Sense: 40 blocks, 20s reveal, 10s cooldown").formatted(Formatting.AQUA));
                out.add(Text.literal("Royal form colors; sunlight Slowness + Weakness, no burning").formatted(Formatting.GRAY));
            }
        }
    }

    private static void werewolf(String id, List<Text> out) {
        if ("lycan".equals(id) || "night_wolf".equals(id)) {
            out.add(count("Meat species eaten", OriginClientState.countFlags("werewolf.meat_species."), 8));
            out.add(count("Hunted meat eaten", OriginClientState.progress("werewolf.meat_eaten"), 32));
            out.add(count("Night prey", OriginClientState.progress("werewolf.night_prey"), 20));
        } else if ("predator".equals(id) || "blood_scent".equals(id)) {
            out.add(count("Prey defeated", OriginClientState.progress("werewolf.prey_kills"), 50));
            out.add(count("Prey species", OriginClientState.countFlags("werewolf.prey_species."), 8));
        } else if ("moonbound".equals(id) || "pack_howl".equals(id) || "final".equals(id)) {
            out.add(Text.literal("○ Full-Moon trial tracking: upcoming").formatted(Formatting.GRAY));
            out.add(count("Night prey", OriginClientState.progress("werewolf.night_prey"), 50));
        }
    }

    private static void mermaid(String id, List<Text> out) {
        switch (id) {
            case "siren_voice" -> {
                out.add(check("Obtain a Nautilus Shell", OriginClientState.hasFlag("mermaid.has_nautilus")));
                out.add(count("Drowned defeated", OriginClientState.progress("mermaid.drowned_kills"), 10));
                out.add(time("Time submerged", OriginClientState.progress("mermaid.submerged_ticks"), 10 * 60 * 20));
                out.add(Text.literal("Reward: Siren Voice charm").formatted(Formatting.AQUA));
            }
            case "drowned_song" -> {
                out.add(count("Drowned defeated", OriginClientState.progress("mermaid.drowned_kills"), 25));
                out.add(count("Trident Drowned", OriginClientState.progress("mermaid.trident_drowned_kills"), 3));
            }
            case "sea_kinship" -> {
                out.add(time("Time submerged", OriginClientState.progress("mermaid.submerged_ticks"), 20 * 60 * 20));
                out.add(Text.literal("Reward: faster swim + aquatic regeneration").formatted(Formatting.AQUA));
            }
            case "oceans_favor" -> {
                out.add(count("Elder Guardians", OriginClientState.progress("mermaid.elder_guardian_kills"), 3));
                out.add(Text.literal("Reward: Guardians and Drowned stop auto-targeting you").formatted(Formatting.AQUA));
            }
            case "royal_court" -> {
                out.add(count("Guardians defeated", OriginClientState.progress("mermaid.guardian_kills"), 20));
                out.add(time("Time submerged", OriginClientState.progress("mermaid.submerged_ticks"), 40 * 60 * 20));
            }
            case "landwalker" -> {
                out.add(time("Time survived dry", OriginClientState.progress("mermaid.dry_ticks"), 30 * 60 * 20));
                out.add(Text.literal("Reward: Dryness can no longer kill you").formatted(Formatting.AQUA));
            }
            case "final" -> {
                out.add(count("Elder Guardians", OriginClientState.progress("mermaid.elder_guardian_kills"), 5));
                out.add(Text.literal("Reward: Mermaid Queen / Royal Tide").formatted(Formatting.LIGHT_PURPLE));
            }
        }
    }

    private static void demon(String id, List<Text> out) {
        if ("emberblood".equals(id)) {
            out.add(time("Time in lava", OriginClientState.progress("demon.lava_ticks"), 60 * 20));
            out.add(count("Magma Cubes defeated", OriginClientState.progress("demon.magma_kills"), 10));
            out.add(count("Blazes defeated", OriginClientState.progress("demon.blaze_kills"), 10));
            out.add(Text.literal("○ Sleep in the Nether: upcoming").formatted(Formatting.GRAY));
        } else if ("conqueror".equals(id) || "hellwalker".equals(id) || "lava_heart".equals(id)) {
            out.add(count("Nether hostiles", OriginClientState.progress("demon.nether_hostile_kills"), 30));
            out.add(Text.literal("○ Fortress + Bastion conquest: upcoming").formatted(Formatting.GRAY));
        } else if ("dominion".equals(id) || "authority".equals(id) || "final".equals(id)) {
            out.add(Text.literal("○ Infernal Anchor / boss trial: upcoming").formatted(Formatting.GRAY));
        }
    }

    private static void angel(String id, List<Text> out) {
        if ("true_wings".equals(id) || "skyborne".equals(id)) {
            out.add(count("Hostile species defeated", OriginClientState.countFlags("angel.hostile_species."), 6));
            out.add(time("Time airborne", OriginClientState.progress("angel.airborne_ticks"), 5 * 60 * 20));
            out.add(Text.literal("○ 60-block wing fall trial: upcoming").formatted(Formatting.GRAY));
        } else if ("consecrated".equals(id)) {
            out.add(count("Hostiles defeated", OriginClientState.progress("angel.hostile_kills"), 25));
        } else if ("guardian_halo".equals(id) || "grace".equals(id) || "final".equals(id)) {
            out.add(Text.literal("○ Celestial boss / protection trial: upcoming").formatted(Formatting.GRAY));
        }
    }

    private static void slime(String id, List<Text> out) {
        switch (id) {
            case "humanoid" -> {
                out.add(count("Humanoid residue", OriginClientState.progress("slime.humanoid_residue"), 20));
                out.add(count("Humanoid species", OriginClientState.countFlags("slime.humanoid_species."), 4));
                out.add(time("Observe humanoids", OriginClientState.progress("slime.observe_ticks"), 10 * 60 * 20));
                out.add(Text.literal("Reward: toggle player-like Humanoid Form").formatted(Formatting.AQUA));
            }
            case "elastic_core" -> {
                out.add(count("Slimes defeated", OriginClientState.progress("slime.slimes_defeated"), 20));
                out.add(count("Large Slimes defeated", OriginClientState.progress("slime.large_slimes_defeated"), 3));
                out.add(time("Airborne / bouncing", OriginClientState.progress("slime.airborne_ticks"), 5 * 60 * 20));
            }
            case "squeeze" -> {
                out.add(time("Time compressed", OriginClientState.progress("slime.squeeze_ticks"), 2 * 60 * 20));
                out.add(Text.literal("Reward: half-block squeeze body").formatted(Formatting.AQUA));
            }
            case "fragment" -> {
                out.add(count("Large Slimes defeated", OriginClientState.progress("slime.large_slimes_defeated"), 8));
                out.add(Text.literal("Reward: critical-health fragmentation survival").formatted(Formatting.AQUA));
            }
            case "perfect_gel" -> out.add(time("Airborne / bouncing", OriginClientState.progress("slime.airborne_ticks"), 10 * 60 * 20));
            case "size_control" -> {
                out.add(count("Slimes defeated", OriginClientState.progress("slime.slimes_defeated"), 50));
                out.add(Text.literal("Reward: Small / Normal / Large forms").formatted(Formatting.AQUA));
            }
            case "final" -> {
                out.add(count("Large Slimes defeated", OriginClientState.progress("slime.large_slimes_defeated"), 15));
                out.add(Text.literal("Reward: Slime Sovereign body").formatted(Formatting.LIGHT_PURPLE));
            }
        }
    }

    private static void homunculus(String id, List<Text> out) {
        if ("weapon_assimilation".equals(id)) {
            out.add(count("Weapon archetypes absorbed", OriginClientState.countFlags("homunculus.weapon_archetype."), 3));
            out.add(count("Manifested weapon kills", OriginClientState.progress("homunculus.manifested_kills"), 30));
            out.add(check("Absorb an enchanted weapon", OriginClientState.hasFlag("homunculus.absorbed_enchanted")));
        } else {
            out.add(Text.literal("○ Body reconstruction tracking: upcoming").formatted(Formatting.GRAY));
        }
    }

    private static void voidOrigin(String id, List<Text> out) {
        if ("riftborn".equals(id)) {
            out.add(count("Chorus Fruit used", OriginClientState.progress("void.chorus_uses"), 32));
            out.add(time("Time in the End", OriginClientState.progress("void.end_ticks"), 10 * 60 * 20));
            out.add(Text.literal("○ Teleport-distance trial: upcoming").formatted(Formatting.GRAY));
        } else if ("void_shell".equals(id) || "phase".equals(id) || "absence".equals(id)) {
            out.add(Text.literal("○ End City / Shulker trial: upcoming").formatted(Formatting.GRAY));
        } else if ("hollow".equals(id) || "rejection".equals(id) || "final".equals(id)) {
            out.add(Text.literal("○ End boss / Null ritual: upcoming").formatted(Formatting.GRAY));
            out.add(resource("Instability", OriginClientState.instability, 100));
        }
    }

    private static void undead(String id, List<Text> out) {
        if ("soul_eater".equals(id)) {
            out.add(count("Soul harvested", OriginClientState.progress("undead.soul_harvested"), 50));
            out.add(resource("Soul Hunger", OriginClientState.soul, 100));
        } else if ("bone_growth".equals(id) || "ribcage".equals(id)) {
            out.add(resource("Bone Mass", OriginClientState.boneMass, 150));
            out.add(count("Skeletal species", OriginClientState.countFlags("undead.skeletal_species."), 3));
        } else if ("carbon".equals(id)) {
            out.add(resource("Carbon Layer", OriginClientState.carbonLayer, 100));
        } else if ("gravebound".equals(id) || "soul_storage".equals(id) || "final".equals(id)) {
            out.add(resource("Bone Mass", OriginClientState.boneMass, 150));
            out.add(resource("Carbon Layer", OriginClientState.carbonLayer, 100));
            out.add(Text.literal("○ Grave / undead boss trial: upcoming").formatted(Formatting.GRAY));
        }
    }

    private static void spriggan(String id, List<Text> out) {
        if ("green_thumb".equals(id)) {
            out.add(time("Time rooted in nature", OriginClientState.progress("spriggan.nature_ticks"), 20 * 60 * 20));
            out.add(Text.literal("○ Grow 8 tree + 6 plant families: upcoming").formatted(Formatting.GRAY));
        } else if ("living_mass".equals(id) || "heartwood".equals(id)) {
            out.add(resource("Living Mass", OriginClientState.livingMass, 10));
        } else if ("shelter".equals(id) || "communion".equals(id) || "bark".equals(id) || "final".equals(id)) {
            out.add(resource("Living Mass", OriginClientState.livingMass, 10));
            out.add(Text.literal("○ Living Home / ecosystem trial: upcoming").formatted(Formatting.GRAY));
        }
    }

    private static void doppelganger(String id, List<Text> out) {
        if ("imitation".equals(id)) {
            out.add(count("Forms studied", OriginClientState.countFlags("doppelganger.form_studied."), 8));
            out.add(Text.literal("Observe each creature within 8 blocks for 10s").formatted(Formatting.DARK_GRAY));
        } else if ("social_mask".equals(id) || "biology".equals(id)) {
            out.add(count("Forms studied", OriginClientState.countFlags("doppelganger.form_studied."), 16));
            out.add(Text.literal("○ Faction infiltration trials: upcoming").formatted(Formatting.GRAY));
        } else if ("perfect_copy".equals(id) || "favorites".equals(id) || "quick_shift".equals(id) || "final".equals(id)) {
            out.add(count("Forms studied", OriginClientState.countFlags("doppelganger.form_studied."), 24));
            out.add(Text.literal("○ Identity Trial: upcoming").formatted(Formatting.GRAY));
        }
    }

    private static MutableText count(String label, int value, int target) {
        int shown = Math.min(value, target);
        boolean done = value >= target;
        return Text.literal((done ? "✓ " : "• ") + label + "  " + shown + "/" + target)
                .formatted(done ? Formatting.GREEN : Formatting.WHITE);
    }

    private static MutableText time(String label, int ticks, int targetTicks) {
        int current = Math.min(ticks, targetTicks) / 20;
        int target = targetTicks / 20;
        boolean done = ticks >= targetTicks;
        return Text.literal((done ? "✓ " : "• ") + label + "  " + formatSeconds(current) + "/" + formatSeconds(target))
                .formatted(done ? Formatting.GREEN : Formatting.WHITE);
    }

    private static MutableText resource(String label, int value, int max) {
        return Text.literal("• " + label + "  " + value + "/" + max).formatted(Formatting.AQUA);
    }

    private static MutableText check(String label, boolean done) {
        return Text.literal((done ? "✓ " : "□ ") + label).formatted(done ? Formatting.GREEN : Formatting.GRAY);
    }

    private static String formatSeconds(int seconds) {
        if (seconds < 60) return seconds + "s";
        return (seconds / 60) + "m" + (seconds % 60 == 0 ? "" : " " + (seconds % 60) + "s");
    }
}
