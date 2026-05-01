package com.github.touhoumaidaffection.client.screen.component;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Set;

public final class BondVoicePoolList {
    private static final int CHECK_SIZE = 8;

    private final BondScrollableList<Entry> list;

    public BondVoicePoolList(int left, int top, int width, int height, int rowHeight) {
        this.list = new BondScrollableList<>(left, top, width, height, rowHeight);
    }

    public void render(GuiGraphics graphics, Font font, List<Entry> entries, Set<String> selectedIds, int mouseX, int mouseY) {
        list.clamp(entries);
        list.render(graphics, font, entries, mouseX, mouseY,
                (g, f, entry, index, left, top, right, height, hovered) -> renderEntry(g, f, entry, selectedIds.contains(entry.id()), left, top, right, height, hovered));
    }

    public int getHoveredIndex(double mouseX, double mouseY, int itemCount) {
        return list.getHoveredIndex(mouseX, mouseY, itemCount);
    }

    public boolean scroll(double mouseX, double mouseY, double delta, int itemCount) {
        return list.scroll(mouseX, mouseY, delta, itemCount);
    }

    private static void renderEntry(GuiGraphics graphics, Font font, Entry entry, boolean selected,
                                    int left, int top, int right, int height, boolean hovered) {
        BondGuiTokens.drawSelectableRow(graphics, left, top, right, top + height, selected, hovered);
        int checkLeft = left + 2;
        int checkTop = top + Math.max(0, (height - CHECK_SIZE) / 2);
        graphics.fill(checkLeft, checkTop, checkLeft + CHECK_SIZE, checkTop + CHECK_SIZE, BondGuiTokens.BORDER_INNER);
        graphics.fill(checkLeft + 1, checkTop + 1, checkLeft + CHECK_SIZE - 1, checkTop + CHECK_SIZE - 1,
                selected ? BondGuiTokens.COLOR_TEXT_SELECTED : BondGuiTokens.COLOR_BG_ELEMENT);
        if (selected) {
            graphics.drawString(font, "x", checkLeft + 2, checkTop - 2, BondGuiTokens.COLOR_BG_PANEL, false);
        }
        int textLeft = checkLeft + CHECK_SIZE + 4;
        int sourceWidth = font.width(entry.sourceLabel());
        int sourceLeft = Math.max(textLeft + 16, right - sourceWidth - 2);
        int labelMaxWidth = Math.max(0, sourceLeft - textLeft - 4);
        String labelText = clip(font, entry.label(), labelMaxWidth);
        String sourceText = clip(font, entry.sourceLabel(), Math.max(0, right - sourceLeft - 2));
        graphics.drawString(font, labelText, textLeft, top + 1,
                selected ? BondGuiTokens.COLOR_TEXT_SELECTED : BondGuiTokens.COLOR_TEXT_BODY, false);
        graphics.drawString(font, sourceText, sourceLeft, top + 1, BondGuiTokens.COLOR_TEXT_HINT, false);
    }

    private static String clip(Font font, Component text, int maxWidth) {
        if (maxWidth <= 0) {
            return "";
        }
        String raw = text.getString();
        return font.plainSubstrByWidth(raw, maxWidth);
    }

    public record Entry(String id, Component label, Component sourceLabel, Component detail) {
    }
}
