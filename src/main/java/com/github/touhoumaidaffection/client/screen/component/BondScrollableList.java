package com.github.touhoumaidaffection.client.screen.component;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;

public class BondScrollableList<T> {
    private final int left;
    private final int top;
    private final int width;
    private final int height;
    private final int rowHeight;
    private int scrollOffset;

    public BondScrollableList(int left, int top, int width, int height, int rowHeight) {
        this.left = left;
        this.top = top;
        this.width = width;
        this.height = height;
        this.rowHeight = rowHeight;
    }

    public void clamp(List<T> items) {
        scrollOffset = Math.max(0, Math.min(scrollOffset, Math.max(0, items.size() - visibleRows())));
    }

    public void render(GuiGraphics graphics, Font font, List<T> items, int mouseX, int mouseY, Renderer<T> renderer) {
        graphics.fill(left, top, right(), bottom(), 0xFF111111);
        graphics.enableScissor(left, top, right(), bottom());
        try {
            int max = Math.min(items.size(), scrollOffset + visibleRows());
            for (int index = scrollOffset; index < max; index++) {
                int rowTop = top + (index - scrollOffset) * rowHeight;
                renderer.render(graphics, font, items.get(index), index, left + 2, rowTop + 2, right() - 2, rowHeight - 2, contains(mouseX, mouseY) && mouseY >= rowTop && mouseY < rowTop + rowHeight);
            }
        } finally {
            graphics.disableScissor();
        }
    }

    public int getHoveredIndex(double mouseX, double mouseY, int itemCount) {
        if (!contains(mouseX, mouseY)) {
            return -1;
        }
        int local = (int) ((mouseY - top) / rowHeight);
        int index = scrollOffset + local;
        return index >= 0 && index < itemCount ? index : -1;
    }

    public boolean scroll(double mouseX, double mouseY, double delta, int itemCount) {
        if (!contains(mouseX, mouseY)) {
            return false;
        }
        int maxOffset = Math.max(0, itemCount - visibleRows());
        if (delta > 0) {
            scrollOffset = Math.max(0, scrollOffset - 1);
        } else if (delta < 0) {
            scrollOffset = Math.min(maxOffset, scrollOffset + 1);
        }
        return true;
    }

    public boolean contains(double mouseX, double mouseY) {
        return mouseX >= left && mouseX < right() && mouseY >= top && mouseY < bottom();
    }

    public int visibleRows() {
        return Math.max(1, height / rowHeight);
    }

    public int left() {
        return left;
    }

    public int top() {
        return top;
    }

    public int right() {
        return left + width;
    }

    public int bottom() {
        return top + height;
    }

    @FunctionalInterface
    public interface Renderer<T> {
        void render(GuiGraphics graphics, Font font, T entry, int index, int left, int top, int right, int height, boolean hovered);
    }
}
