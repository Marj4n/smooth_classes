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
 * Search UI embedded in the right-hand recipe page of the real Patchouli Death List.
 *
 * The feature is intentionally implemented through Fabric's screen events, not a
 * hard Patchouli class dependency or global Patchouli mixin. Other Patchouli books
 * are untouched and the ledger/recipe authority remains server-side.
 *
 * The vanilla Patchouli pages remain visible until the user enters a query.
 * During search the matching recipes are drawn over the right-hand recipe page,
 * avoiding Patchouli's cached text-formatting callback results.
 */
public final class DeathListRecipeSearchClient {
    private static final String ENTRY_CLASS = "vazkii.patchouli.client.book.gui.GuiBookEntry";
    private static final String LEDGER_ENTRY = "smooth_classes:ledger";
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
            if (!isOurLedgerEntry(screen)) return;
            int left = getBookCoordinate(screen, "bookLeft");
            int top = getBookCoordinate(screen, "bookTop");
            if (left == Integer.MIN_VALUE || top == Integer.MIN_VALUE) return;

            SearchState state = new SearchState(client, left, top);
            STATES.put(screen, state);
            // Patchouli's GuiBook dispatches mouse input to its children, so the
            // controls belong to the real book instead of opening another screen.
            Screens.getButtons(screen).add(state.search);
            Screens.getButtons(screen).add(state.previous);
            Screens.getButtons(screen).add(state.next);

            ScreenEvents.afterRender(screen).register((currentScreen, context, mouseX, mouseY, delta) -> {
                if (STATES.get(currentScreen) == state) state.render(currentScreen, context, mouseX, mouseY, delta);
            });

            ScreenMouseEvents.afterMouseClick(screen).register((currentScreen, mouseX, mouseY, button) -> {
                if (STATES.get(currentScreen) != state || button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return;
                float factor = (float) client.getWindow().getScaledWidth() / Math.max(1, currentScreen.width);
                double scaledX = mouseX / factor;
                double scaledY = mouseY / factor;
                boolean editing = scaledX >= state.search.getX()
                        && scaledX < state.search.getX() + state.search.getWidth()
                        && scaledY >= state.search.getY()
                        && scaledY < state.search.getY() + state.search.getHeight();
                if (editing) currentScreen.setFocused(state.search);
                state.search.setFocused(editing);
            });

            ScreenKeyboardEvents.allowKeyPress(screen).register((currentScreen, key, scanCode, modifiers) -> {
                if (STATES.get(currentScreen) != state || !state.search.isFocused()) return true;
                // Patchouli intercepts Backspace (go back) and the inventory key
                // (close book) before normal child widgets see them. While editing,
                // consume ALL non-Escape keystrokes at the text field first.
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

    private static boolean isOurLedgerEntry(Screen screen) {
        if (!ENTRY_CLASS.equals(screen.getClass().getName())) return false;
        try {
            Object entry = screen.getClass().getMethod("getEntry").invoke(screen);
            if (entry == null) return false;
            Object id = entry.getClass().getMethod("getId").invoke(entry);
            return LEDGER_ENTRY.equals(String.valueOf(id));
        } catch (ReflectiveOperationException | LinkageError ex) {
            SmoothClasses.LOGGER.debug("Death List recipe search could not identify Patchouli ledger", ex);
            return false;
        }
    }

    private static int getBookCoordinate(Screen screen, String name) {
        try {
            Field field = screen.getClass().getField(name);
            return field.getInt(screen);
        } catch (ReflectiveOperationException | LinkageError ex) {
            SmoothClasses.LOGGER.debug("Death List recipe search could not read Patchouli book coordinate: {}", name, ex);
            return Integer.MIN_VALUE;
        }
    }

    private static final class SearchState {
        private static final int PARCHMENT = 0xFFF1E8D5;
        private static final int PARCHMENT_ALT = 0xFFE8DCC4;
        private static final int BORDER = 0xFF8A6B5A;
        private static final int INK = 0xFF38273A;
        private static final int MUTED = 0xFF755E66;
        private static final int ACCENT = 0xFF683B60;

        final MinecraftClient client;
        final int left;
        final int top;
        final TextFieldWidget search;
        final ButtonWidget previous;
        final ButtonWidget next;
        List<AvengerSummonRecipes.Recipe> results = List.of();
        int resultPage = 0;

        SearchState(MinecraftClient client, int left, int top) {
            this.client = client;
            this.left = left;
            this.top = top;
            int x = left + RIGHT_PAGE_X + 5;
            search = new TextFieldWidget(client.textRenderer, x, top + 20,
                    PAGE_WIDTH - 12, 15, Text.literal("Search Death List recipes"));
            search.setMaxLength(100);
            search.setPlaceholder(Text.literal("Search recipes..."));
            search.setChangedListener(query -> {
                resultPage = 0;
                results = DeathListBookClient.searchDiscoveredRecipes(query);
                updateNavigation();
            });
            previous = ButtonWidget.builder(Text.literal("<"), button -> {
                if (resultPage > 0) resultPage--;
                updateNavigation();
            }).dimensions(x + 14, top + 154, 18, 13).build();
            next = ButtonWidget.builder(Text.literal(">"), button -> {
                if ((resultPage + 1) * PAGE_SIZE < results.size()) resultPage++;
                updateNavigation();
            }).dimensions(x + 75, top + 154, 18, 13).build();
            updateNavigation();
        }

        void updateNavigation() {
            boolean hasQuery = !search.getText().isBlank();
            previous.visible = hasQuery && results.size() > PAGE_SIZE;
            next.visible = previous.visible;
            previous.active = resultPage > 0;
            next.active = (resultPage + 1) * PAGE_SIZE < results.size();
        }

        void render(Screen screen, DrawContext context, int mouseX, int mouseY, float delta) {
            // GuiBook uses its own potentially adjusted GUI scale. Match that scale
            // when drawing and when forwarding mouse coordinates to search widgets.
            float scale = (float) client.getWindow().getScaledWidth() / Math.max(1, screen.width);
            context.getMatrices().push();
            context.getMatrices().scale(scale, scale, 1.0F);
            int mx = (int) (mouseX / scale);
            int my = (int) (mouseY / scale);
            int x = left + RIGHT_PAGE_X + 2;

            // Reserve a small band for the search box over Patchouli's RECIPES title.
            context.fill(x, top + 17, x + PAGE_WIDTH - 2, top + 38, PARCHMENT);
            context.fill(x + 1, top + 18, x + PAGE_WIDTH - 3, top + 19, BORDER);

            if (!search.getText().isBlank()) {
                drawMatchingRecipes(context, x, client.textRenderer);
            }

            // The field and navigation are children of the Patchouli screen for input,
            // but explicitly rendered here so they appear above the recipe overlay.
            search.render(context, mx, my, delta);
            if (previous.visible) previous.render(context, mx, my, delta);
            if (next.visible) next.render(context, mx, my, delta);
            context.getMatrices().pop();
        }

        void drawMatchingRecipes(DrawContext context, int x, TextRenderer font) {
            int rx = x + 4;
            int right = x + PAGE_WIDTH - 4;
            context.fill(x, top + 39, x + PAGE_WIDTH - 2, top + 169, PARCHMENT);
            context.drawText(font, "MATCHES " + results.size(), rx, top + 40, ACCENT, false);
            context.fill(rx, top + 51, right, top + 52, BORDER);

            if (results.isEmpty()) {
                context.drawText(font, "No recipes found", rx, top + 65, INK, false);
                context.drawText(font, "Try another name or", rx, top + 80, MUTED, false);
                context.drawText(font, "summon ingredient.", rx, top + 91, MUTED, false);
                return;
            }

            int start = resultPage * PAGE_SIZE;
            for (int i = 0; i < PAGE_SIZE && start + i < results.size(); i++) {
                AvengerSummonRecipes.Recipe recipe = results.get(start + i);
                int y = top + 54 + i * 32;
                if ((i & 1) != 0) context.fill(x + 2, y - 1, x + PAGE_WIDTH - 4, y + 30, PARCHMENT_ALT);
                drawTrimmed(context, font, AvengerSummonRecipes.friendlyEntityName(recipe.entityId()), rx, y, INK, 104);
                drawTrimmed(context, font, "Main: " + AvengerSummonRecipes.friendlyItemName(recipe.mainItemId()), rx, y + 9, MUTED, 104);
                String off = recipe.offhandItemId() == null ? "Not required"
                        : AvengerSummonRecipes.friendlyItemName(recipe.offhandItemId());
                drawTrimmed(context, font, "Off: " + off, rx, y + 18, MUTED, 104);
            }
            int totalPages = (results.size() + PAGE_SIZE - 1) / PAGE_SIZE;
            context.fill(rx, top + 151, right, top + 152, BORDER);
            context.drawCenteredTextWithShadow(font, (resultPage + 1) + "/" + totalPages,
                    x + PAGE_WIDTH / 2, top + 156, ACCENT);
        }

        void drawTrimmed(DrawContext context, TextRenderer font, String text, int x, int y, int color, int maxWidth) {
            String clipped = font.trimToWidth(text, maxWidth);
            if (clipped.length() != text.length()) {
                clipped = font.trimToWidth(text, Math.max(1, maxWidth - font.getWidth("..."))) + "...";
            }
            context.drawText(font, clipped, x, y, color, false);
        }
    }
}
