package org.marj4n.smooth_classes.client.origin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

/** Custom Vampire HUD artwork. Other Origins retain their existing item renderer. */
public final class OriginSkillIconRenderer {
    private static final Identifier BAT = icon("bat_form");
    private static final Identifier MAN_BAT = icon("man_bat");
    private static final Identifier BLOOD_SENSE = icon("blood_sense");

    private OriginSkillIconRenderer() {}

    private static Identifier icon(String name) {
        return Identifier.of("smooth_classes", "textures/icons/origin_vampire/" + name + ".png");
    }

    public static void draw(DrawContext context, ItemStack item, int x, int y) {
        Identifier texture = null;
        if ("vampire".equals(OriginClientState.originId)) {
            if (item.getItem() == OriginAbilityHud.slot(0).icon()) texture = BAT;
            else if (item.getItem() == OriginAbilityHud.slot(1).icon()) texture = MAN_BAT;
            else if (item.getItem() == OriginAbilityHud.slot(2).icon()) texture = BLOOD_SENSE;
        }
        if (texture == null) context.drawItem(item, x, y);
        else context.drawTexture(texture, x, y, 16, 16, 0F, 0F, 32, 32, 32, 32);
    }
}
