package org.marj4n.smooth_classes.client;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
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
 * Interactive Soul Search inside Patchouli's dedicated Death List chapter.
 *
 * Only the intentionally blank right page receives custom drawing. All other
 * Patchouli entries, including Soul Ledger's original recipes, are untouched.
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
            // Keep a real TextFieldWidget as a child so typing, selecting, IME,
            // clipboard, and keyboard focus use Minecraft's native behavior.
            // Its ordinary screen render pass is suppressed: only render the
            // smaller, book-sized text in the overlay pass below.
            Screens.getButtons(screen).add(state.search);

            ScreenEvents.afterRender(screen).register((current, context, mouseX, mouseY, delta) -> {
                if (STATES.get(current) == state) state.render(current, context, mouseX, mouseY, delta);
            });
            ScreenMouseEvents.afterMouseClick(screen).register((current, mouseX, mouseY, button) -> {
                if (STATES.get(current) != state || button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return;
                float scale = (float) client.getWindow().getScaledWidth() / Math.max(1, current.width);
                int x = (int) (mouseX / scale);
                int y = (int) (mouseY / scale);
                boolean insideInput = state.insideSearch(x, y);
                current.setFocused(insideInput ? state.search : null);
                state.search.setFocused(insideInput);
                if (insideInput) {
                    // Native click coordinates use the unscaled widget width,
                    // so align the edit cursor with the end of the visible text.
                    state.search.setCursorToEnd();
                    state.showResults();
                } else if (!state.handleNavigationClick(x, y)) {
                    state.selectAt(x, y);
                }
            });
            ScreenKeyboardEvents.allowKeyPress(screen).register((current, key, scanCode, modifiers) -> {
                if (STATES.get(current) != state || !state.search.isFocused()) return true;
                if (key == GLFW.GLFW_KEY_ESCAPE) return true;
                if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                    state.search.setFocused(false);
                    current.setFocused(null);
                    return false;
                }
                // Patchouli binds Backspace to page navigation; consume editing
                // keys ourselves while the search box has focus.
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

    private static int getBookCoordinate(Screen screen, String fieldName) {
        try {
            Field field = screen.getClass().getField(fieldName);
            return field.getInt(screen);
        } catch (ReflectiveOperationException | LinkageError ex) {
            SmoothClasses.LOGGER.debug("Soul Search could not read Patchouli coordinate {}", fieldName, ex);
            return Integer.MIN_VALUE;
        }
    }

    /**
     * Retain native text editing and focus, but never paint TextFieldWidget.
     * Soul Search renders text with the same unshadowed Patchouli typography
     * as the recipe results. This prevents duplicate/shadowed glyphs.
     */
    private static final class BookSearchField extends TextFieldWidget {
        BookSearchField(TextRenderer font, int x, int y, int width, int height) {
            super(font, x, y, width, height, Text.literal("Search souls"));
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            // No native screen pass. SearchState.drawInputText owns the visuals.
        }

        @Override
        public void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
            // No native shadowed text in any render path.
        }
    }

    private static final class SearchState {
        // Match the parchment text palette used in Soul Ledger's Patchouli JSON.
        private static final int INK = 0xFF241C24;
        private static final int MUTED = 0xFF5A4A50;
        private static final int ACCENT = 0xFF5C244E;
        private static final int RULE = 0xFFAA987C;
        private static final float BOOK_TEXT_SCALE = 0.76F;
        private static final int INPUT_Y = 43;
        private static final int INPUT_H = 15;
        private static final int INPUT_VISUAL_W = PAGE_WIDTH - 12;
        private static final int ROW_Y = 77;
        private static final int ROW_HEIGHT = 24;
        private static final int NAV_Y = 160;

        final MinecraftClient client;
        final int top;
        final int pageX;
        final BookSearchField search;
        List<AvengerSummonRecipes.Recipe> results = List.of();
        int resultPage;
        AvengerSummonRecipes.Recipe selected;

        SearchState(MinecraftClient client, int left, int top) {
            this.client = client;
            this.top = top;
            this.pageX = left + RIGHT_PAGE_X + 3;
            int x = pageX + 6;
            // The field's internal text renderer needs extra logical width,
            // since we scale its *visuals* to the book's smaller body font.
            search = new BookSearchField(client.textRenderer, x, top + INPUT_Y,
                    Math.round(INPUT_VISUAL_W / BOOK_TEXT_SCALE),
                    Math.round(INPUT_H / BOOK_TEXT_SCALE));
            search.setMaxLength(100);
            search.setPlaceholder(Text.literal("Mob, mod or item..."));
            search.setDrawsBackground(false);
            search.setEditableColor(INK);
            search.setChangedListener(query -> {
                resultPage = 0;
                selected = null;
                results = query.isBlank() ? List.of() : DeathListBookClient.searchDiscoveredRecipes(query);
            });
        }

        boolean insideSearch(int x, int y) {
            return x >= pageX + 6 && x < pageX + 6 + INPUT_VISUAL_W
                    && y >= top + INPUT_Y && y < top + INPUT_Y + INPUT_H;
        }

        void showResults() { selected = null; }

        boolean handleNavigationClick(int x, int y) {
            if (y < top + NAV_Y - 2 || y > top + NAV_Y + 11
                    || x < pageX || x > pageX + PAGE_WIDTH) return false;
            if (selected != null) {
                showResults();
                return true;
            }
            if (search.getText().isBlank() || results.size() <= PAGE_SIZE) return false;
            if (x < pageX + 43) {
                if (resultPage > 0) resultPage--;
            } else if (x > pageX + PAGE_WIDTH - 43) {
                if ((resultPage + 1) * PAGE_SIZE < results.size()) resultPage++;
            }
            return true;
        }

        void selectAt(int x, int y) {
            if (selected != null || search.getText().isBlank() || results.isEmpty()) return;
            if (x < pageX + 3 || x >= pageX + PAGE_WIDTH - 3) return;
            int relativeY = y - (top + ROW_Y);
            if (relativeY < 0 || relativeY >= PAGE_SIZE * ROW_HEIGHT) return;
            int index = resultPage * PAGE_SIZE + relativeY / ROW_HEIGHT;
            if (index >= results.size()) return;
            selected = results.get(index);
        }

        void render(Screen screen, DrawContext context, int mouseX, int mouseY, float delta) {
            float screenScale = (float) client.getWindow().getScaledWidth() / Math.max(1, screen.width);
            context.getMatrices().push();
            context.getMatrices().scale(screenScale, screenScale, 1F);
            int mx = (int) (mouseX / screenScale);
            int my = (int) (mouseY / screenScale);
            TextRenderer font = client.textRenderer;

            // The stock Patchouli page title is already rendered by the book.
            // Do not draw a second, oversized SOUL SEARCH title over it.
            bookText(context, font, "Search discovered souls", pageX + 6, top + 31, MUTED, 103);
            context.fill(pageX + 5, top + INPUT_Y + INPUT_H, pageX + PAGE_WIDTH - 4,
                    top + INPUT_Y + INPUT_H + 1, RULE);
            context.fill(pageX + 5, top + 65, pageX + PAGE_WIDTH - 4, top + 66, RULE);

            if (selected != null) drawDetail(context, font);
            else drawResults(context, font, mx, my);

            // Draw typed text, placeholder, and caret with one unshadowed pass.
            drawInputText(context, font);
            context.getMatrices().pop();
        }

        /** The only code path that draws search text on the book page. */
        void drawInputText(DrawContext context, TextRenderer font) {
            int x = pageX + 6;
            int y = top + INPUT_Y + 2;
            String value = search.getText();
            int maxLogicalWidth = Math.max(1,
                    (int) ((INPUT_VISUAL_W - 7) / BOOK_TEXT_SCALE));

            if (value.isEmpty()) {
                bookText(context, font, "Mob, mod or item...", x, y, MUTED,
                        INPUT_VISUAL_W - 7);
                if (search.isFocused() && (System.currentTimeMillis() / 500L) % 2L == 0L) {
                    context.fill(x, y, x + 1, y + 8, INK);
                }
                return;
            }

            // Horizontal viewport tracks the native cursor without allowing
            // a long search query to draw outside the book's parchment.
            int cursor = Math.max(0, Math.min(value.length(), search.getCursor()));
            int start = 0;
            while (start < cursor
                    && font.getWidth(value.substring(start, cursor)) > maxLogicalWidth - 3) {
                start++;
            }
            String visible = font.trimToWidth(value.substring(start), maxLogicalWidth);
            bookText(context, font, visible, x, y, INK, INPUT_VISUAL_W - 7);

            if (search.isFocused() && (System.currentTimeMillis() / 500L) % 2L == 0L) {
                int localCursor = Math.max(0, Math.min(visible.length(), cursor - start));
                int caretX = x + Math.round(
                        font.getWidth(visible.substring(0, localCursor)) * BOOK_TEXT_SCALE);
                int rightEdge = x + INPUT_VISUAL_W - 4;
                caretX = Math.min(caretX, rightEdge);
                context.fill(caretX, y, caretX + 1, y + 8, INK);
            }
        }

        void drawResults(DrawContext context, TextRenderer font, int mx, int my) {
            int x = pageX + 6;
            if (search.getText().isBlank()) {
                bookText(context, font, "Type to find a soul", x, top + 81, INK, 103);
                bookText(context, font, "Mob name / mod ID / item", x, top + 96, MUTED, 103);
                bookText(context, font, "Only collected souls", x, top + 115, MUTED, 103);
                bookText(context, font, "have visible recipes.", x, top + 126, MUTED, 103);
                return;
            }
            bookText(context, font, "Matches: " + results.size(), x, top + 68, ACCENT, 103);
            if (results.isEmpty()) {
                bookText(context, font, "No matching souls.", x, top + 87, INK, 103);
                bookText(context, font, "Try another keyword.", x, top + 99, MUTED, 103);
                return;
            }
            int start = resultPage * PAGE_SIZE;
            for (int i = 0; i < PAGE_SIZE && start + i < results.size(); i++) {
                AvengerSummonRecipes.Recipe recipe = results.get(start + i);
                int y = top + ROW_Y + i * ROW_HEIGHT;
                boolean hovered = mx >= pageX + 3 && mx < pageX + PAGE_WIDTH - 3
                        && my >= y && my < y + ROW_HEIGHT;
                bookText(context, font, AvengerSummonRecipes.friendlyEntityName(recipe.entityId()),
                        x, y + 1, hovered ? ACCENT : INK, 103);
                int stock = DeathListBookClient.availableSouls(recipe.entityId());
                bookText(context, font, "Souls x" + stock + "  |  View >", x, y + 12, MUTED, 103);
            }
            if (results.size() > PAGE_SIZE) {
                int pages = (results.size() + PAGE_SIZE - 1) / PAGE_SIZE;
                bookText(context, font, "< Prev", pageX + 6, top + NAV_Y, ACCENT, 37);
                bookCentered(context, font, (resultPage + 1) + "/" + pages,
                        pageX + PAGE_WIDTH / 2, top + NAV_Y, MUTED);
                bookText(context, font, "Next >", pageX + PAGE_WIDTH - 39,
                        top + NAV_Y, ACCENT, 35);
            }
        }

        void drawDetail(DrawContext context, TextRenderer font) {
            String id = selected.entityId();
            int x = pageX + 6;
            bookText(context, font, AvengerSummonRecipes.friendlyEntityName(id),
                    x, top + 70, ACCENT, 103);
            bookText(context, font, "Souls: " + DeathListBookClient.availableSouls(id),
                    x, top + 83, INK, 103);
            bookText(context, font, "Defeated: " + DeathListBookClient.killCount(id),
                    x, top + 94, MUTED, 103);
            bookText(context, font, "Mainhand:", x, top + 108, ACCENT, 103);
            bookText(context, font, AvengerSummonRecipes.friendlyItemName(selected.mainItemId()),
                    x, top + 120, INK, 103);
            bookText(context, font, "Offhand:", x, top + 133, ACCENT, 103);
            String off = selected.offhandItemId() == null ? "Not required"
                    : AvengerSummonRecipes.friendlyItemName(selected.offhandItemId());
            bookText(context, font, off, x, top + 145, INK, 103);
            bookText(context, font, "< Back to results", x, top + NAV_Y, ACCENT, 103);
        }

        private void bookCentered(DrawContext context, TextRenderer font,
                                  String content, int centerX, int y, int color) {
            int width = Math.round(font.getWidth(content) * BOOK_TEXT_SCALE);
            bookText(context, font, content, centerX - width / 2, y, color, PAGE_WIDTH);
        }

        private void bookText(DrawContext context, TextRenderer font, String label,
                              int x, int y, int color, int maxVisibleWidth) {
            int logicalWidth = Math.max(1, (int) (maxVisibleWidth / BOOK_TEXT_SCALE));
            String visible = font.trimToWidth(label, logicalWidth);
            if (visible.length() < label.length()) {
                visible = font.trimToWidth(label,
                        Math.max(1, logicalWidth - font.getWidth("..."))) + "...";
            }
            context.getMatrices().push();
            context.getMatrices().translate(x, y, 0);
            context.getMatrices().scale(BOOK_TEXT_SCALE, BOOK_TEXT_SCALE, 1F);
            context.drawText(font, visible, 0, 0, color, false);
            context.getMatrices().pop();
        }
    }
}
