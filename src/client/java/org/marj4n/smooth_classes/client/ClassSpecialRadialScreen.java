package org.marj4n.smooth_classes.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.marj4n.smooth_classes.SmoothClasses;

/**
 * Hold-H radial selector inspired by the interaction style of tool-belt wheels.
 * It is implemented independently: hold H, point at an option, release H to confirm.
 */
public final class ClassSpecialRadialScreen extends Screen {
    private static final int RADIUS = 68;
    private static final int DEAD_ZONE = 24;
    private static final int SLOT = 28;

    private static final String[] SCHOOL_NAMES = {"Arcane", "Fire", "Frost", "Wind", "Water", "Earth"};
    private static final Identifier[] SCHOOL_ICONS = {
            new Identifier("wizards", "textures/item/spell_book/arcane.png"),
            new Identifier("wizards", "textures/item/spell_book/fire.png"),
            new Identifier("wizards", "textures/item/spell_book/frost.png"),
            new Identifier("elemental_wizards_rpg", "textures/item/spellbooks/wind_spell_book.png"),
            new Identifier("elemental_wizards_rpg", "textures/item/spellbooks/aqua_spell_book.png"),
            new Identifier("elemental_wizards_rpg", "textures/item/spellbooks/terra_spell_book.png")
    };

    private final String specialId;
    private final int initialSelection;
    private int hovered = -1;
    private boolean finished;

    public ClassSpecialRadialScreen(String specialId, int initialSelection) {
        super(Text.literal(titleFor(specialId)));
        this.specialId = specialId == null ? "" : specialId;
        this.initialSelection = Math.max(0, initialSelection);
    }

    public static boolean supports(String id) {
        return "treasury_key".equals(id) || "arcane_attunement".equals(id) || "spell_imprint".equals(id);
    }

    private static String titleFor(String id) {
        return switch (id == null ? "" : id) {
            case "treasury_key" -> "Treasury";
            case "spell_imprint" -> "Spell Imprint";
            default -> "Elemental Attunement";
        };
    }

    private boolean schoolMode() {
        return "arcane_attunement".equals(specialId) || "spell_imprint".equals(specialId);
    }

    private int optionCount() { return schoolMode() ? SCHOOL_NAMES.length : AbilityHudState.treasuryCapacity; }

    @Override
    protected void init() {
        super.init();
        SmoothClassesClient.sendClassSpecialHold(true);
        if (client != null) {
            // Open centered like a proper hold-to-select wheel instead of inheriting
            // whatever hidden cursor position Minecraft had before opening the screen.
            GLFW.glfwSetCursorPos(client.getWindow().getHandle(),
                    client.getWindow().getWidth() / 2.0D,
                    client.getWindow().getHeight() / 2.0D);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!specialKeyHeld()) finishSelection();
    }

    private boolean specialKeyHeld() {
        if (SmoothClassesClient.classSpecialKey().isPressed()) return true;
        return client != null && GLFW.glfwGetKey(client.getWindow().getHandle(), GLFW.GLFW_KEY_H) == GLFW.GLFW_PRESS;
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (SmoothClassesClient.classSpecialKey().matchesKey(keyCode, scanCode)) {
            finishSelection();
            return true;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    private void finishSelection() {
        if (finished) return;
        finished = true;
        int selected = hovered >= 0 ? hovered : Math.min(initialSelection, optionCount() - 1);
        if (!schoolMode()) {
            boolean explicitlyHovered = hovered >= 0;
            if (selected < 0 || selected >= AbilityHudState.treasurySlots.length
                    || AbilityHudState.treasurySlots[selected] == null
                    || AbilityHudState.treasurySlots[selected].isEmpty()) {
                if (explicitlyHovered) selected = -1;
                else selected = firstStoredTreasurySlot();
            }
            if (selected < 0) {
                SmoothClassesClient.sendClassSpecialHold(false);
                if (client != null) client.setScreen(null);
                return;
            }
        }
        SmoothClassesClient.sendClassSpecialSelection(selected);
        SmoothClassesClient.sendClassSpecialHold(false);
        if (client != null) client.setScreen(null);
    }

    private int firstStoredTreasurySlot() {
        for (int i = 0; i < AbilityHudState.treasurySlots.length; i++) {
            ItemStack stack = AbilityHudState.treasurySlots[i];
            if (stack != null && !stack.isEmpty()) return i;
        }
        return -1;
    }

    @Override
    public void close() {
        if (!finished) SmoothClassesClient.sendClassSpecialHold(false);
        finished = true; // ESC cancels instead of accidentally selecting.
        super.close();
    }

    @Override
    public boolean shouldPause() { return false; }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int cx = width / 2;
        int cy = height / 2;
        hovered = hoveredIndex(mouseX, mouseY, cx, cy, optionCount());

        context.fill(0, 0, width, height, 0x66000000);
        context.fill(cx - 39, cy - 15, cx + 39, cy + 15, 0xB0101018);
        context.drawCenteredTextWithShadow(textRenderer, title, cx, cy - 5, 0xFFE8C76A);

        int count = optionCount();
        int radius = count > 6 ? 78 : RADIUS;
        for (int i = 0; i < count; i++) {
            double angle = -Math.PI / 2.0D + (Math.PI * 2.0D * i / count);
            int x = cx + (int) Math.round(Math.cos(angle) * radius);
            int y = cy + (int) Math.round(Math.sin(angle) * radius);
            boolean treasuryEmpty = !schoolMode() && (AbilityHudState.treasurySlots[i] == null || AbilityHudState.treasurySlots[i].isEmpty());
            boolean selected = !treasuryEmpty && (hovered == i || (hovered < 0 && i == Math.min(initialSelection, count - 1)));

            int bg = treasuryEmpty ? 0xB0101014 : selected ? 0xE0B58A2A : 0xC0181820;
            int border = treasuryEmpty ? 0xFF34343A : selected ? 0xFFFFD86A : 0xFF666670;
            context.fill(x - SLOT / 2, y - SLOT / 2, x + SLOT / 2, y + SLOT / 2, border);
            context.fill(x - SLOT / 2 + 1, y - SLOT / 2 + 1, x + SLOT / 2 - 1, y + SLOT / 2 - 1, bg);

            if (schoolMode()) renderSchoolIcon(context, i, x - 8, y - 8);
            else renderTreasuryIcon(context, i, x - 8, y - 8);
        }

        int selected = hovered >= 0 ? hovered : Math.min(initialSelection, count - 1);
        String name;
        if (schoolMode()) {
            name = SCHOOL_NAMES[selected];
        } else {
            ItemStack stack = AbilityHudState.treasurySlots[selected];
            name = stack == null || stack.isEmpty() ? "Empty" : stack.getName().getString();
        }
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(name), cx, cy + 19,
                "Empty".equals(name) ? 0x888888 : 0xFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal("Hold H • Move cursor • Release H to select"), cx, cy + (count > 6 ? 78 : RADIUS) + 30, 0xB8B8C8);

        super.render(context, mouseX, mouseY, delta);
    }

    private void renderTreasuryIcon(DrawContext context, int index, int x, int y) {
        ItemStack stack = index >= 0 && index < AbilityHudState.treasurySlots.length
                ? AbilityHudState.treasurySlots[index] : ItemStack.EMPTY;
        if (stack == null || stack.isEmpty()) return;
        context.drawItem(stack, x, y);
        if (stack.getCount() > 1) context.drawItemInSlot(textRenderer, stack, x, y);
    }

    private void renderSchoolIcon(DrawContext context, int index, int x, int y) {
        Identifier icon = SCHOOL_ICONS[Math.max(0, Math.min(index, SCHOOL_ICONS.length - 1))];
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getResourceManager().getResource(icon).isEmpty()) {
            icon = SmoothClasses.id("textures/icons/classes/caster.png");
        }
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        context.drawTexture(icon, x, y, 0, 0, 16, 16, 16, 16);
        RenderSystem.disableBlend();
    }

    private static int hoveredIndex(int mouseX, int mouseY, int cx, int cy, int count) {
        double dx = mouseX - cx;
        double dy = mouseY - cy;
        if (dx * dx + dy * dy < DEAD_ZONE * DEAD_ZONE) return -1;
        double angle = Math.atan2(dy, dx) + Math.PI / 2.0D;
        if (angle < 0) angle += Math.PI * 2.0D;
        double sector = Math.PI * 2.0D / count;
        return Math.floorMod((int) Math.floor((angle + sector / 2.0D) / sector), count);
    }
}
