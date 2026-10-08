package org.marj4n.smooth_classes.client.origin;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import org.marj4n.smooth_classes.network.SmoothClassesNetworking;
import org.marj4n.smooth_classes.origin.OriginType;

import java.util.ArrayList;
import java.util.List;

/** Origins-inspired first-join selector, recreated with vanilla GUI primitives. */
public final class OriginSelectionScreen extends Screen {
    private static final int PANEL_W = 176;
    private static final int PANEL_H = 182;
    private static final int CONTENT_PAD = 8;
    private int index;
    private int scroll;
    private int maxScroll;
    private ButtonWidget previous;
    private ButtonWidget next;
    private ButtonWidget select;

    public OriginSelectionScreen() {
        super(Text.literal("Choose your Origin"));
    }

    @Override
    protected void init() {
        int left = (width - PANEL_W) / 2;
        int top = (height - PANEL_H) / 2;

        previous = addDrawableChild(ButtonWidget.builder(Text.literal("<"), button -> move(-1))
                .dimensions(left - 28, top + PANEL_H / 2 - 10, 20, 20).build());
        next = addDrawableChild(ButtonWidget.builder(Text.literal(">"), button -> move(1))
                .dimensions(left + PANEL_W + 8, top + PANEL_H / 2 - 10, 20, 20).build());
        select = addDrawableChild(ButtonWidget.builder(Text.literal("Select"), button -> choose())
                .dimensions(left + 48, top + PANEL_H + 8, 80, 20).build());
    }

    private void move(int delta) {
        OriginType[] values = OriginType.v1PlayableValues();
        index = Math.floorMod(index + delta, values.length);
        scroll = 0;
    }

    private void choose() {
        OriginType origin = OriginType.v1PlayableValues()[index];
        if (client == null || client.getNetworkHandler() == null) return;
        client.setScreen(new ConfirmOriginScreen(this, origin));
    }

    void confirm(OriginType origin) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(origin.id());
        ClientPlayNetworking.send(SmoothClassesNetworking.SELECT_ORIGIN, buf);
        if (client != null) client.setScreen(null);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        int left = (width - PANEL_W) / 2;
        int top = (height - PANEL_H) / 2;

        // Origins-like compact grey frame and black scroll area.
        context.fill(left - 4, top - 4, left + PANEL_W + 4, top + PANEL_H + 4, 0xFF242424);
        context.fill(left - 2, top - 2, left + PANEL_W + 2, top + PANEL_H + 2, 0xFFD5D5D5);
        context.fill(left, top, left + PANEL_W, top + PANEL_H, 0xFF111111);
        context.drawBorder(left, top, PANEL_W, PANEL_H, 0xFF5B5B5B);

        OriginType origin = OriginType.v1PlayableValues()[index];
        drawHeader(context, origin, left, top);
        drawScrollableBody(context, origin, left, top);

        context.drawCenteredTextWithShadow(textRenderer, Text.literal("Choose your Origin"), width / 2, top - 18, 0xFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal((index + 1) + " / " + OriginType.v1PlayableValues().length), width / 2, top + PANEL_H + 32, 0xA0A0A0);
        super.render(context, mouseX, mouseY, delta);
    }

    private void drawHeader(DrawContext context, OriginType origin, int left, int top) {
        int headerY = top + 6;
        context.drawItem(new ItemStack(origin.icon()), left + 8, headerY + 1);
        context.drawTextWithShadow(textRenderer, Text.literal(origin.displayName()), left + 30, headerY + 1, 0xFFFFFF);

        // Keep the Origins-style Impact indicator entirely on the title row.
        Text impactText = Text.literal("Impact");
        int pipStartX = left + PANEL_W - 25;
        int impactX = pipStartX - textRenderer.getWidth(impactText) - 5;
        context.drawText(textRenderer, impactText, impactX, headerY + 2, 0x808080, false);
        for (int i = 0; i < 3; i++) {
            int color = i < origin.impact() ? 0xFF76D66A : 0xFF454545;
            int px = pipStartX + i * 6;
            context.fill(px, headerY + 3, px + 4, headerY + 7, color);
        }

        // Subtitle owns its own row and gets a little extra breathing room.
        context.drawText(textRenderer, Text.literal(origin.subtitle()), left + 30, headerY + 13, 0xB0B0B0, false);
        context.fill(left + 6, top + 31, left + PANEL_W - 6, top + 32, 0xFF3A3A3A);
    }

    private void drawScrollableBody(DrawContext context, OriginType origin, int left, int top) {
        int x = left + CONTENT_PAD;
        int y = top + 37 - scroll;
        int contentWidth = PANEL_W - CONTENT_PAD * 2 - 5;
        int clipTop = top + 35;
        int clipBottom = top + PANEL_H - 6;

        context.enableScissor(left + 4, clipTop, left + PANEL_W - 4, clipBottom);
        List<Line> lines = buildLines(origin, contentWidth);
        int lineHeight = 10;
        for (Line line : lines) {
            if (y >= clipTop - lineHeight && y <= clipBottom) {
                context.drawText(textRenderer, line.text(), x, y, line.color(), false);
            }
            y += lineHeight;
        }
        context.disableScissor();

        int totalHeight = lines.size() * lineHeight;
        int visible = clipBottom - clipTop;
        maxScroll = Math.max(0, totalHeight - visible + 6);
        scroll = Math.max(0, Math.min(scroll, maxScroll));

        if (maxScroll > 0) {
            int barX = left + PANEL_W - 4;
            int trackTop = clipTop + 1;
            int trackHeight = visible - 2;
            int thumb = Math.max(14, trackHeight * visible / Math.max(visible, totalHeight));
            int thumbY = trackTop + (trackHeight - thumb) * scroll / maxScroll;
            context.fill(barX, trackTop, barX + 2, trackTop + trackHeight, 0xFF313131);
            context.fill(barX, thumbY, barX + 2, thumbY + thumb, 0xFFC8C8C8);
        }
    }

    private List<Line> buildLines(OriginType origin, int width) {
        List<Line> result = new ArrayList<>();
        wrap(result, origin.lore(), width, 0xC7C7C7);
        result.add(new Line(Text.literal("").asOrderedText(), 0xFFFFFF));
        result.add(new Line(Text.literal("Strengths").asOrderedText(), 0x66E36F));
        for (String value : origin.strengths()) wrap(result, "+ " + value, width, 0x93E89A);
        result.add(new Line(Text.literal("").asOrderedText(), 0xFFFFFF));
        result.add(new Line(Text.literal("Weaknesses").asOrderedText(), 0xE05D5D));
        for (String value : origin.weaknesses()) wrap(result, "- " + value, width, 0xE58A8A);
        result.add(new Line(Text.literal("").asOrderedText(), 0xFFFFFF));
        wrap(result, "Spawn: " + origin.v1Spawn(), width, 0xE5C76B);
        wrap(result, "Diet: " + origin.diet(), width, 0xD0D0D0);
        wrap(result, "Evolution: " + origin.evolution(), width, 0xBCA7FF);
        return result;
    }

    private void wrap(List<Line> out, String value, int width, int color) {
        for (OrderedText text : textRenderer.wrapLines(Text.literal(value), width)) {
            out.add(new Line(text, color));
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (maxScroll > 0) {
            scroll = Math.max(0, Math.min(maxScroll, scroll - (int) Math.signum(amount) * 18));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    private record Line(OrderedText text, int color) { }

    private static final class ConfirmOriginScreen extends Screen {
        private final OriginSelectionScreen parent;
        private final OriginType origin;

        private ConfirmOriginScreen(OriginSelectionScreen parent, OriginType origin) {
            super(Text.literal("Confirm Origin"));
            this.parent = parent;
            this.origin = origin;
        }

        @Override
        protected void init() {
            int y = height / 2 + 35;
            addDrawableChild(ButtonWidget.builder(Text.literal("Go Back"), b -> client.setScreen(parent))
                    .dimensions(width / 2 - 104, y, 100, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("Choose"), b -> parent.confirm(origin))
                    .dimensions(width / 2 + 4, y, 100, 20).build());
        }

        @Override
        public boolean shouldCloseOnEsc() { return false; }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            renderBackground(context);
            int boxW = 260;
            int boxH = 110;
            int x = (width - boxW) / 2;
            int y = (height - boxH) / 2;
            context.fill(x, y, x + boxW, y + boxH, 0xE5151515);
            context.drawBorder(x, y, boxW, boxH, 0xFFB8B8B8);
            context.drawCenteredTextWithShadow(textRenderer, Text.literal("Choose " + origin.displayName() + "?"), width / 2, y + 12, 0xFFFFFF);
            context.drawCenteredTextWithShadow(textRenderer, Text.literal(origin.subtitle()), width / 2, y + 27, 0xC9B7FF);
            context.drawCenteredTextWithShadow(textRenderer, Text.literal("This permanently sets your body and Origin tree."), width / 2, y + 47, 0xBDBDBD);
            context.drawCenteredTextWithShadow(textRenderer, Text.literal("V1 birthplace: Overworld"), width / 2, y + 61, 0xE5C76B);
            super.render(context, mouseX, mouseY, delta);
        }
    }
}
