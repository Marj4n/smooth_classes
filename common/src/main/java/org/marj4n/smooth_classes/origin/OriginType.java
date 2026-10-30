package org.marj4n.smooth_classes.origin;

import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import org.marj4n.smooth_classes.SmoothClasses;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/**
 * The twelve base Origins used by Smooth Classes.
 *
 * Base Origins currently start in the Overworld, but Vampire wakes inside the
 * Forgotten Graveyard crypt. Spawn implementation lives outside this enum,
 * avoiding player save data changes as more origin birthplaces are introduced.
 */
public enum OriginType {
    HUMAN("Human", "Ordinary Human", Items.COMPASS, 0,
            "A completely ordinary human. Play vanilla Minecraft with no Origin powers, passives, weaknesses, evolutions or quests. Only your chosen Class grants special abilities.",
            new String[]{}, new String[]{},
            "Vanilla", "None"),

    VAMPIRE("Vampire", "Blood of the Night", Items.FERMENTED_SPIDER_EYE, 3,
            "Once human, you were buried among the forgotten dead of an old church. The shattered Holy Grail stirred your heart, but its long sleep has left you with no memory of your past. Blood now sustains you, while daylight threatens your second life.",
            new String[]{"Night Vision", "Faster and stronger at night", "Blood feeding restores the body"},
            new String[]{"Only 15 health", "Normal food is rejected", "Direct sunlight is dangerous"},
            "Blood", "Vampire Lord"),

    WEREWOLF("Werewolf", "Child of the Moon", Items.RABBIT_FOOT, 2,
            "The Grail awakened a beast that follows the lunar cycle. By day it sleeps beneath the skin; by night it hunts.",
            new String[]{"Night transformation", "Moon-powered body", "Excellent hunter and meat eater"},
            new String[]{"Almost ordinary during the day", "Peak power depends on Full Moon", "Plant food is inefficient"},
            "Meat", "Alpha Werewolf"),

    MERMAID("Mermaid", "Daughter of the Tides", Items.NAUTILUS_SHELL, 3,
            "The sea claimed its own after the war. Mermaids rule the water, but an unevolved Mermaid stranded on land will eventually suffocate.",
            new String[]{"Water Breathing", "Fast underwater movement", "Aquatic armor and recovery"},
            new String[]{"Only 16 health", "Must stay wet", "Base form eventually dies on land"},
            "Aquatic food", "Mermaid Queen"),

    DEMON("Demon", "Appleborn", Items.BLAZE_POWDER, 3,
            "Born from a mysterious Nether apple, your infernal soul must conquer its own evolutionary path. The Nether is your homeland; prolonged exile causes Netherbound Decay.",
            new String[]{"Cinder Reflex: fire protection with recovery", "Safe Nether rest", "World-Ender evolution through objectives"},
            new String[]{"Netherbound Decay outside the Nether", "World-Ender requires Infernal Diet", "Evolution must be conquered"},
            "Normal; Infernal Diet after World-Ender", "Demon Lord"),

    ANGEL("Angel", "Winged by Ruin and Grace", Items.FEATHER, 3,
            "Celestial grace survived the broken Grail, but imperfectly. Angels begin fragile, carrying wings that must be strengthened through trial.",
            new String[]{"Limited starting flight", "Excellent vertical mobility", "Strong fall protection"},
            new String[]{"Only 16 health", "Light body is easier to knock back", "Nether suppresses celestial recovery"},
            "Normal / Aether", "Seraph"),

    SLIME("Slime", "The Shapeless Survivor", Items.SLIME_BALL, 2,
            "Some survivors abandoned a fixed body completely. Slimes bend, bounce and eventually learn to imitate humanoid shape.",
            new String[]{"No fall damage", "Elastic body and bounce", "Later gains size and shape control"},
            new String[]{"Only 16 health", "Fire is especially dangerous", "Equipment is awkward before Humanoid Form"},
            "Organic matter", "Slime Sovereign"),

    HOMUNCULUS("Homunculus", "The Artificial Inheritor", Items.BREWING_STAND, 3,
            "A Homunculus is assembled rather than born. Its unfinished body is dangerously fragile, but every part can eventually be rebuilt.",
            new String[]{"Dual armor body design", "Weapon assimilation", "Exceptional alchemy growth"},
            new String[]{"Only 10 health", "Poor natural recovery", "Depends heavily on equipment and reconstruction"},
            "Reduced normal food / alchemy", "Perfect Homunculus"),

    VOID("Void", "Fractured by the End", Items.CHORUS_FRUIT, 3,
            "The Void-touched were partially erased by the shattered Grail. Their bodies exist imperfectly between one position and the next.",
            new String[]{"Fast unstable body", "End and Chorus affinity", "Later gains Riftstep and Hollow survival"},
            new String[]{"Only 14 health", "Water causes Instability", "Raw durability stays low"},
            "Chorus / End-biased", "Null"),

    UNDEAD("Undead", "The Hunger Beyond Death", Items.BONE, 3,
            "Death removed the flesh but not the will. Undead feed on souls and physically rebuild their skeleton with bone and carbon.",
            new String[]{"Poison immunity", "Does not drown", "Bone Mass can become an enormous tank body"},
            new String[]{"Only 12 starting health", "Normal food is useless", "Soul Hunger must be maintained"},
            "Souls", "Bone Colossus"),

    SPRIGGAN("Spriggan", "Born of Root and Bark", Items.OAK_SAPLING, 3,
            "When the land was wounded, part of it stood up. Spriggans grow their own bodies, homes and forests from living wood.",
            new String[]{"Natural bark armor", "Living Mass can create huge health", "Grows Living Shelter and vegetation"},
            new String[]{"Slow body", "Fire can burn away maximum health", "Meat is poorly tolerated"},
            "Plants", "Worldroot"),

    DOPPELGANGER("Doppelganger", "The Many-Faced", Items.NAME_TAG, 3,
            "Identity became unstable after the Broken Holy Grail War. Doppelgangers survive by learning the shape and social presence of other creatures.",
            new String[]{"Learns creature forms", "Can infiltrate factions", "Forms grant curated biological traits"},
            new String[]{"Mediocre natural body", "Cannot copy full combat kits", "Power requires building a Form Archive"},
            "Adaptive", "Many-Faced One");

    private final String displayName;
    private final String subtitle;
    private final Item icon;
    private final int impact;
    private final String lore;
    private final String[] strengths;
    private final String[] weaknesses;
    private final String diet;
    private final String evolution;

    OriginType(String displayName, String subtitle, Item icon, int impact, String lore,
               String[] strengths, String[] weaknesses, String diet, String evolution) {
        this.displayName = displayName;
        this.subtitle = subtitle;
        this.icon = icon;
        this.impact = impact;
        this.lore = lore;
        this.strengths = strengths;
        this.weaknesses = weaknesses;
        this.diet = diet;
        this.evolution = evolution;
    }

    public String id() { return name().toLowerCase(Locale.ROOT); }
    public String displayName() { return displayName; }
    public String subtitle() { return subtitle; }
    public Item icon() { return icon; }
    public int impact() { return impact; }
    public String lore() { return lore; }
    public String[] strengths() { return strengths.clone(); }
    public String[] weaknesses() { return weaknesses.clone(); }
    public String diet() { return diet; }
    public String evolution() { return evolution; }
    public String v1Spawn() { return this == VAMPIRE ? "Forgotten Graveyard (Overworld)" : this == DEMON ? "Safe Nether birthplace" : "Overworld (V1)"; }
    public Identifier categoryId() { return SmoothClasses.id("origin_" + id()); }


    private static final OriginType[] V1_PLAYABLE = {HUMAN, VAMPIRE, DEMON};

    public boolean isV1Playable() {
        return this == HUMAN || this == VAMPIRE || this == DEMON;
    }

    public static OriginType[] v1PlayableValues() {
        return V1_PLAYABLE.clone();
    }

    public static Optional<OriginType> byId(String id) {
        if (id == null || id.isBlank()) return Optional.empty();
        return Arrays.stream(values()).filter(type -> type.id().equalsIgnoreCase(id)).findFirst();
    }
}
