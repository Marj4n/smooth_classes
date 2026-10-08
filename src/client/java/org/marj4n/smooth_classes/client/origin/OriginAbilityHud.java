package org.marj4n.smooth_classes.client.origin;

import net.minecraft.item.Item;
import net.minecraft.item.Items;
import org.marj4n.smooth_classes.origin.OriginType;

/** Three shared-key slots displayed while the HUD is on the Origin page. */
public final class OriginAbilityHud {
    public record Slot(String name, Item icon) {}

    private OriginAbilityHud() {}

    public static Slot slot(int index) {
        OriginType origin = OriginType.byId(OriginClientState.originId).orElse(null);
        if (origin == null) return new Slot("No Origin", Items.BARRIER);
        Slot[] slots = switch (origin) {
            case HUMAN -> new Slot[]{s("Adaptation", Items.COMPASS), s("Second Wind", Items.RABBIT_FOOT), s("Awaken", Items.NETHER_STAR)};
            case VAMPIRE -> new Slot[]{s("Bat Form", Items.PHANTOM_MEMBRANE), s("Man-Bat", Items.FERMENTED_SPIDER_EYE), s("Blood Sense", Items.SPIDER_EYE)};
            case WEREWOLF -> new Slot[]{s("Wolf / Lycan", Items.RABBIT_FOOT), s("Blood Scent", Items.SPIDER_EYE), s("Howl", Items.GOAT_HORN)};
            case MERMAID -> new Slot[]{s("Siren Song", Items.NAUTILUS_SHELL), s("Tidal Rush", Items.HEART_OF_THE_SEA), s("Royal Tide", Items.TRIDENT)};
            case DEMON -> new Slot[]{s("Emberblood", Items.BLAZE_POWDER), s("Infernal Anchor", Items.CRYING_OBSIDIAN), s("Infernal Recall", Items.ENDER_PEARL)};
            case ANGEL -> new Slot[]{s("Wings", Items.FEATHER), s("Consecration", Items.GLOWSTONE_DUST), s("Grace", Items.TOTEM_OF_UNDYING)};
            case SLIME -> new Slot[]{s("Humanoid Form", Items.SLIME_BALL), s("Shape", Items.SLIME_BLOCK), s("Fragment", Items.MAGMA_CREAM)};
            case HOMUNCULUS -> new Slot[]{s("Weapon Arm", Items.IRON_SWORD), s("Body Config", Items.IRON_CHESTPLATE), s("Alchemical Core", Items.BREWING_STAND)};
            case VOID -> new Slot[]{s("Riftstep", Items.CHORUS_FRUIT), s("Hollow", Items.ENDER_PEARL), s("Null Rejection", Items.END_CRYSTAL)};
            case UNDEAD -> new Slot[]{s("Consume Soul", Items.SOUL_LANTERN), s("Bone Body", Items.BONE), s("Carbon Layer", Items.COAL)};
            case SPRIGGAN -> new Slot[]{s("Green Thumb", Items.OAK_SAPLING), s("Living Mass", Items.OAK_LOG), s("Root Recall", Items.OAK_LEAVES)};
            case DOPPELGANGER -> new Slot[]{s("Imitation", Items.NAME_TAG), s("Social Mask", Items.PLAYER_HEAD), s("Quick Shift", Items.SPYGLASS)};
        };
        return slots[Math.max(0, Math.min(2, index))];
    }

    private static Slot s(String name, Item item) { return new Slot(name, item); }
}
