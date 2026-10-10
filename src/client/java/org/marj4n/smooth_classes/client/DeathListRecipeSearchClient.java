package org.marj4n.smooth_classes.client;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.marj4n.smooth_classes.SmoothClasses;
import org.marj4n.smooth_classes.content.avenger.runtime.AvengerSummonRecipes;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Client-only Soul Search chapter of the real Patchouli Death List.
 *
 * <p>Unlike R3, this NEVER adds widgets or overlays to the Soul Ledger's
 * normal recipe pages. Only smooth_classes:soul_search has interactive widgets,
 * and its right-hand Patchouli text page is intentionally empty.</p>
 */
public final class DeathListRecipeSearchClient {
    private static final String ENTRY_CLASS = "vazkii.patchouli.client.book.gui.GuiBookEntry";
    private static final String SEARCH_ENTRY = "smooth_classes:soul_search";
    private static final int RIGHT_PAGE_X = 141;
    private static final int PAGE_WIDTH = 116;
    private static final int PAGE_SIZE = 3;
    private static final Map<Screen, SearchState> STATES = new WeakHashMap<>();
    private static boolean registered;

    private DeathListRecipeSearchClient() {}

    public static void register() {
        if (registered) return;
        registered = true;
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (!isSearchChapter(screen)) return;
            int left = getBookCoordinate(screen, "bookLeft");
            int top = getBookCoordinate(screen, "bookTop");
            if (left == Integer.MIN_VALUE || top == Integer.MIN_VALUE) return;

            SearchState state = new SearchState(client, left, top);
            STATES.put(screen, state);
            // Add input/buttons to Patchouli's own screen; no secondary GUI screen.
            Screens.getButtons(screen).add(state.search);
            Screens.getButtons(screen).add(state.previous);
            Screens.getButtons(screen).add(state.next);
            Screens.getButtons(screen).add(state.back);

            ScreenEvents.afterRender(screen).register((currentScreen, context, mouseX, mouseY, delta) -> {
                if (STATES.get(currentScreen) == state) {
                    state.render(currentScreen, context, mouseX, mouseY, delta);
                }
            });
            ScreenMouseEvents.afterMouseClick(screen).register((currentScreen, mouseX, mouseY, button) -> {
                if (STATES.get(currentScreen) != state || button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return;
                float factor = (float) client.getWindow().getScaledWidth() / Math.max(1, currentScreen.width);
                int x = (int) (mouseX / factor);
                int y = (int) (mouseY / factor);
                boolean editing = state.contains(state.search, x, y);
                if (editing) {
                    currentScreen.setFocused(state.search);
                    state.showResults();
                }
                state.search.setFocused(editing);
                if (!editing) state.selectAt(x, y);
            });

            ScreenKeyboardEvents.allowKeyPress(screen).register((currentScreen, key, scanCode, modifiers) -> {
                if (STATES.get(currentScreen) != state || !state.search.isFocused()) return true;
                // Patchouli uses Backspace for navigation. Hand input to the field
                // before Patchouli can process it. Escape still closes the book.
                if (key == GLFW.GLFW_KEY_ESCAPE) return true;
                if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                    state.search.setFocused(false);
                    return false;
                }
                state.search.keyPressed(key, scanCode, modifiers);
                return false;
            });
        });
    }

    private static boolean isSearchChapter(Screen screen) {
        if (!ENTRY_CLASS.equals(screen.getClass().getName())) return false;
        try {
            Object entry = screen.getClass().getMethod("getEntry").invoke(screen);
            if (entry == null) return false;
            Object id = entry.getClass().getMethod("getId").invoke(entry);
            return SEARCH_ENTRY.equals(String.valueOf(id));
        } catch (ReflectiveOperationException | LinkageError ex) {
            SmoothClasses.LOGGER.debug("Soul Search could not identify its Patchouli entry", ex);
            return false;
        }
    }

    private static int getBookCoordinate(Screen screen, String name) {
        try {
            Field field = screen.getClass().getField(name);
            return field.getInt(screen);
        } catch (ReflectiveOperationException | LinkageError ex) {
            SmoothClasses.LOGGER.debug("Soul Search could not read Patchouli coordinate {}", name, ex);
            return Integer.MIN_VALUE;
        }
    }

    private static final class SearchState {
        private static final int PAPER = 0xFFF8F0DE;
        private static final int PAPER_ALT = 0xFFEDE0C7;
        private static final int BORDER = 0xFFAB947B;
        private static final int INK = 0xFF35253A;
        private static final int MUTED = 0xFF705865;
        private static final int ACCENT = 0xFF653158;
        private static final int INPUT_Y = 43;
        private static final int HEADER_Y = 19;
        private static final int ROW_Y = 76;
        private static final int ROW_HEIGHT = 25;
        private static final int NAV_Y = 158;

        final MinecraftClient client;
        final int left;
        final int top;
        final int pageX;
        final TextFieldWidget search;
        final ButtonWidget previous;
        final ButtonWidget next;
        final ButtonWidget back;
        List<AvengerSummonRecipes.Recipe> results = List.of();
        int resultPage;
        AvengerSummonRecipes.Recipe selected;

        SearchState(MinecraftClient client, int left, int top) {
            this.client = client;
            this.left = left;
            this.top = top;
            this.pageX = left + RIGHT_PAGE_X + 3;
            int x = pageX + 4;
            search = new TextFieldWidget(client.textRenderer, x, top + INPUT_Y, PAGE_WIDTH - 12, 15,
                    Text.literal("Search discovered souls"));
            search.setMaxLength(100);
            search.setPlaceholder(Text.literal("Mob, mod or ingredient..."));
            search.setDrawsBackground(false);
            search.setEditableColor(INK);
            search.setChangedListener(query -> {
                resultPage = 0;
                selected = null;
                results = query.isBlank() ? List.of() : DeathListBookClient.searchDiscoveredRecipes(query);
                updateNavigation();
            });

            previous = ButtonWidget.builder(Text.literal("<"), button -> {
                if (resultPage > 0) resultPage--;
                updateNavigation();
            }).dimensions(pageX + 16, top + NAV_Y, 18, 12).build();
            next = ButtonWidget.builder(Text.literal(">"), button -> {
                if ((resultPage + 1) * PAGE_SIZE < results.size()) resultPage++;
                updateNavigation();
            }).dimensions(pageX + 79, top + NAV_Y, 18, 12).build();
            back = ButtonWidget.builder(Text.literal("Back to results"), button -> showResults())
                    .dimensions(pageX + 8, top + NAV_Y, PAGE_WIDTH - 20, 12).build();
            updateNavigation();
        }

        boolean contains(TextFieldWidget widget, int x, int y) {
            return x >= widget.getX() && x < widget.getX() + widget.getWidth()
                    && y >= widget.getY() && y < widget.getY() + widget.getHeight();
        }

        void showResults() {
            selected = null;
            updateNavigation();
        }

        void selectAt(int x, int y) {
            if (selected != null || search.getText().isBlank() || results.isEmpty()) return;
            if (x < pageX + 2 || x > pageX + PAGE_WIDTH - 5) return;
            int row = (y - (top + ROW_Y)) / ROW_HEIGHT;
            if (y < top + ROW_Y || row < 0 || row >= PAGE_SIZE) return;
            int index = resultPage * PAGE_SIZE + row;
            if (index >= results.size()) return;
            selected = results.get(index);
            updateNavigation();
        }

        void updateNavigation() {
            boolean hasPages = selected == null && !search.getText().isBlank() && results.size() > PAGE_SIZE;
            previous.visible = hasPages;
            next.visible = hasPages;
            previous.active = resultPage > 0;
            next.active = (resultPage + 1) * PAGE_SIZE < results.size();
            back.visible = selected != null;
        }

        void render(Screen screen, DrawContext context, int mouseX, int mouseY, float delta) {
            float scale = (float) client.getWindow().getScaledWidth() / Math.max(1, screen.width);
            context.getMatrices().push();
            context.getMatrices().scale(scale, scale, 1F);
            int mx = (int) (mouseX / scale);
            int my = (int) (mouseY / scale);
            TextRenderer font = client.textRenderer;

            // This page has no Patchouli text to cover. Draw on the intentionally
            // empty right page, leaving stock and recipe spreads untouched.
            context.drawCenteredTextWithShadow(font, "SOUL SEARCH", pageX + PAGE_WIDTH / 2,
                    top + HEADER_Y, ACCENT);
            context.drawText(font, "Search discovered souls", pageX + 6, top + 31, MUTED, false);
            context.fill(pageX + 3, top + INPUT_Y - 2, pageX + PAGE_WIDTH - 3,
                    top + INPUT_Y + 17, BORDER);
            context.fill(pageX + 4, top + INPUT_Y - 1, pageX + PAGE_WIDTH - 4,
                    top + INPUT_Y + 16, PAPER);
            context.fill(pageX + 5, top + 65, pageX + PAGE_WIDTH - 5, top + 66, BORDER);

            if (selected != null) drawDetail(context, font);
            else drawResults(context, font, mx, my);

            // They are registered as real children for input; render after the
            // other page decorations to guarantee readable text and controls.
            search.render(context, mx, my, delta);
            if (previous.visible) previous.render(context, mx, my, delta);
            if (next.visible) next.render(context, mx, my, delta);
            if (back.visible) back.render(context, mx, my, delta);
            context.getMatrices().pop();
        }

        void drawResults(DrawContext context, TextRenderer font, int mx, int my) {
            int x = pageX + 6;
            if (search.getText().isBlank()) {
                drawTrimmed(context, font, "Type to find a soul", x, top + 81, INK, 102);
                drawTrimmed(context, font, "Mob name / mod ID / item", x, top + 98, MUTED, 102);
                drawTrimmed(context, font, "Only collected souls", x, top + 115, MUTED, 102);
                drawTrimmed(context, font, "have visible recipes.", x, top + 127, MUTED, 102);
                return;
            }
            drawTrimmed(context, font, "MATCHES " + results.size(), x, top + 68, ACCENT, 102);
            if (results.isEmpty()) {
                drawTrimmed(context, font, "No matching souls.", x, top + 87, INK, 102);
                drawTrimmed(context, font, "Try another keyword.", x, top + 101, MUTED, 102);
                return;
            }
            int start = resultPage * PAGE_SIZE;
            for (int i = 0; i < PAGE_SIZE && start + i < results.size(); i++) {
                AvengerSummonRecipes.Recipe recipe = results.get(start + i);
                int y = top + ROW_Y + i * ROW_HEIGHT;
                if ((i & 1) != 0 || (mx >= pageX + 2 && mx < pageX + PAGE_WIDTH - 5
                        && my >= y && my < y + ROW_HEIGHT)) {
                    context.fill(pageX + 2, y, pageX + PAGE_WIDTH - 5, y + ROW_HEIGHT - 1, PAPER_ALT);
                }
                drawTrimmed(context, font, AvengerSummonRecipes.friendlyEntityName(recipe.entityId()),
                        x, y + 2, INK, 98);
                int stock = DeathListBookClient.availableSouls(recipe.entityId());
                drawTrimmed(context, font, "Souls x" + stock + "  |  View >", x, y + 13, MUTED, 98);
            }
            int pages = (results.size() + PAGE_SIZE - 1) / PAGE_SIZE;
            context.drawCenteredTextWithShadow(font, (resultPage + 1) + "/" + pages,
                    pageX + PAGE_WIDTH / 2, top + NAV_Y + 2, ACCENT);
        }

        void drawDetail(DrawContext context, TextRenderer font) {
            String id = selected.entityId();
            int x = pageX + 6;
            drawTrimmed(context, font, AvengerSummonRecipes.friendlyEntityName(id),
                    x, top + 70, ACCENT, 101);
            drawTrimmed(context, font, "Soul stock: " + DeathListBookClient.availableSouls(id),
                    x, top + 85, INK, 101);
            drawTrimmed(context, font, "Defeated: " + DeathListBookClient.killCount(id),
                    x, top + 96, MUTED, 101);
            drawTrimmed(context, font, "MAINHAND", x, top + 111, ACCENT, 101);
            drawTrimmed(context, font, AvengerSummonRecipes.friendlyItemName(selected.mainItemId()),
                    x, top + 122, INK, 101);
            drawTrimmed(context, font, "OFFHAND", x, top + 135, ACCENT, 101);
            String off = selected.offhandItemId() == null ? "Not required"
                    : AvengerSummonRecipes.friendlyItemName(selected.offhandItemId());
            drawTrimmed(context, font, off, x, top + 146, INK, 101);
        }

        void drawTrimmed(DrawContext context, TextRenderer font, String label,
                         int x, int y, int color, int width) {
            String shown = font.trimToWidth(label, width);
            if (shown.length() != label.length()) {
                shown = font.trimToWidth(label, Math.max(1, width - font.getWidth("..."))) + "...";
            }
            context.drawText(font, shown, x, y, color, false);
        }
    }
}
