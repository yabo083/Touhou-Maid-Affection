package com.github.touhoumaidaffection.client.screen.component;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;

public final class BondDropdown<T> {
    private final int left;
    private final int top;
    private final int width;
    private final int headerHeight;
    private final int rowHeight;
    private final int maxVisibleRows;
    private boolean expanded;
    private int scrollOffset;

    public BondDropdown(int left, int top, int width, int headerHeight, int rowHeight, int maxVisibleRows) {
        this.left = left;
        this.top = top;
        this.width = width;
        this.headerHeight = headerHeight;
        this.rowHeight = rowHeight;
        this.maxVisibleRows = Math.max(1, maxVisibleRows);
    }

    public void renderBase(GuiGraphics graphics, Font font, List<T> items, int selectedIndex, int mouseX, int mouseY, Renderer<T> renderer) {
        clamp(items);
        int headerBottom = top + headerHeight;
        boolean headerHovered = containsHeader(mouseX, mouseY);

        graphics.fill(left, top, right(), headerBottom, 0xFF3B3B3B);
        graphics.fill(left + 1, top + 1, right() - 1, headerBottom - 1, 0xFF1C1C1C);
        if (headerHovered) {
            graphics.fill(left, top, right(), headerBottom, 0x22FFFFFF);
        }

        if (selectedIndex >= 0 && selectedIndex < items.size()) {
            renderer.render(graphics, font, items.get(selectedIndex), selectedIndex, left + 4, top + 3, right() - 14, headerHeight - 6, headerHovered, true);
        }
        graphics.drawString(font, expanded ? "▲" : "▼", right() - 10, top + 4, 0xFFE0E0E0, false);
    }

    public void renderOverlay(GuiGraphics graphics, Font font, List<T> items, int selectedIndex, int mouseX, int mouseY, Renderer<T> renderer) {
        clamp(items);
        if (!expanded || items.isEmpty()) {
            return;
        }

        int headerBottom = top + headerHeight;
        int visibleRows = Math.min(maxVisibleRows, items.size());
        int listTop = headerBottom + 1;
        int listBottom = listTop + visibleRows * rowHeight;
        graphics.fill(left, listTop, right(), listBottom, 0xFF2A2A2A);
        graphics.enableScissor(left, listTop, right(), listBottom);
        try {
            int max = Math.min(items.size(), scrollOffset + visibleRows);
            for (int index = scrollOffset; index < max; index++) {
                int rowTop = listTop + (index - scrollOffset) * rowHeight;
                boolean hovered = containsExpanded(mouseX, mouseY, items.size())
                        && mouseX >= left && mouseX < right()
                        && mouseY >= rowTop && mouseY < rowTop + rowHeight;
                renderer.render(graphics, font, items.get(index), index, left + 4, rowTop + 2, right() - 4, rowHeight - 4, hovered, false);
            }
        } finally {
            graphics.disableScissor();
        }
    }

    public void render(GuiGraphics graphics, Font font, List<T> items, int selectedIndex, int mouseX, int mouseY, Renderer<T> renderer) {
        renderBase(graphics, font, items, selectedIndex, mouseX, mouseY, renderer);
        renderOverlay(graphics, font, items, selectedIndex, mouseX, mouseY, renderer);
    }

    public ClickResult mouseClicked(double mouseX, double mouseY, int itemCount) {
        if (containsHeader(mouseX, mouseY)) {
            expanded = !expanded;
            return new ClickResult(true, -1);
        }
        if (!expanded) {
            return ClickResult.notHandled();
        }
        int hoveredIndex = getHoveredIndex(mouseX, mouseY, itemCount);
        if (hoveredIndex >= 0) {
            expanded = false;
            return new ClickResult(true, hoveredIndex);
        }
        if (containsExpanded(mouseX, mouseY, itemCount)) {
            return new ClickResult(true, -1);
        }
        expanded = false;
        return new ClickResult(true, -1);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double delta, int itemCount) {
        if (!expanded || !containsExpanded(mouseX, mouseY, itemCount)) {
            return false;
        }
        int maxOffset = Math.max(0, itemCount - Math.min(maxVisibleRows, itemCount));
        if (delta > 0) {
            scrollOffset = Math.max(0, scrollOffset - 1);
        } else if (delta < 0) {
            scrollOffset = Math.min(maxOffset, scrollOffset + 1);
        }
        return true;
    }

    public int getHoveredIndex(double mouseX, double mouseY, int itemCount) {
        if (!expanded || !containsExpanded(mouseX, mouseY, itemCount)) {
            return -1;
        }
        int local = (int) ((mouseY - (top + headerHeight + 1)) / rowHeight);
        int index = scrollOffset + local;
        return index >= 0 && index < itemCount ? index : -1;
    }

    public boolean contains(double mouseX, double mouseY, int itemCount) {
        return containsHeader(mouseX, mouseY) || (expanded && containsExpanded(mouseX, mouseY, itemCount));
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void collapse() {
        expanded = false;
    }

    private void clamp(List<T> items) {
        int maxOffset = Math.max(0, items.size() - Math.min(maxVisibleRows, items.size()));
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxOffset));
    }

    private boolean containsHeader(double mouseX, double mouseY) {
        return mouseX >= left && mouseX < right() && mouseY >= top && mouseY < top + headerHeight;
    }

    private boolean containsExpanded(double mouseX, double mouseY, int itemCount) {
        int visibleRows = Math.min(maxVisibleRows, Math.max(0, itemCount));
        return mouseX >= left && mouseX < right()
                && mouseY >= top + headerHeight + 1
                && mouseY < top + headerHeight + 1 + visibleRows * rowHeight;
    }

    private int right() {
        return left + width;
    }

    @FunctionalInterface
    public interface Renderer<T> {
        void render(GuiGraphics graphics, Font font, T item, int index, int left, int top, int right, int height, boolean hovered, boolean selectedHeader);
    }

    public record ClickResult(boolean handled, int selectedIndex) {
        public static ClickResult notHandled() {
            return new ClickResult(false, -1);
        }
    }
}
