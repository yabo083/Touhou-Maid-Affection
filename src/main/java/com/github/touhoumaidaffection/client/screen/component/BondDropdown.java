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
        int background = headerHovered ? BondGuiTokens.STATE_HOVER_BG : BondGuiTokens.STATE_DEFAULT_BG;
        int innerBorder = headerHovered ? BondGuiTokens.STATE_HOVER_BORDER : BondGuiTokens.STATE_DEFAULT_BORDER;
        BondGuiTokens.drawFramedPanelWithInnerBorder(graphics, left, top, right(), headerBottom, background, innerBorder);
        if (expanded) {
            graphics.hLine(left + 2, right() - 3, headerBottom - 1, background);
        }

        if (selectedIndex >= 0 && selectedIndex < items.size()) {
            renderer.render(
                    graphics,
                    font,
                    items.get(selectedIndex),
                    selectedIndex,
                    left + BondGuiTokens.SPACING_SM,
                    top + BondGuiTokens.SPACING_XS,
                    right() - BondGuiTokens.SPACING_XL,
                    headerHeight - BondGuiTokens.SPACING_SM,
                    headerHovered,
                    true
            );
        }
        if (headerHovered) {
            graphics.fill(left + 2, top + 2, right() - 2, headerBottom - 2, BondGuiTokens.HOVER_OVERLAY);
        }
        graphics.drawString(font, expanded ? "▲" : "▼", right() - 10, top + Math.max(1, (headerHeight - font.lineHeight) / 2), BondGuiTokens.COLOR_TEXT_BODY, false);
    }

    public void renderOverlay(GuiGraphics graphics, Font font, List<T> items, int selectedIndex, int mouseX, int mouseY, Renderer<T> renderer) {
        clamp(items);
        if (!expanded || items.isEmpty()) {
            return;
        }

        int headerBottom = top + headerHeight;
        int visibleRows = Math.min(maxVisibleRows, items.size());
        int listTop = headerBottom;
        int listBottom = listTop + visibleRows * rowHeight;
        BondGuiTokens.drawFramedPanel(graphics, left, listTop, right(), listBottom, BondGuiTokens.COLOR_BG_PANEL);
        graphics.enableScissor(left + 2, listTop + 2, right() - 2, listBottom - 2);
        try {
            int max = Math.min(items.size(), scrollOffset + visibleRows);
            for (int index = scrollOffset; index < max; index++) {
                int rowTop = listTop + (index - scrollOffset) * rowHeight;
                boolean hovered = containsExpanded(mouseX, mouseY, items.size())
                        && mouseX >= left && mouseX < right()
                        && mouseY >= rowTop && mouseY < rowTop + rowHeight;
                renderer.render(
                        graphics,
                        font,
                        items.get(index),
                        index,
                        left + BondGuiTokens.SPACING_SM,
                        rowTop + BondGuiTokens.SPACING_XS,
                        right() - BondGuiTokens.SPACING_SM,
                        rowHeight - BondGuiTokens.SPACING_SM,
                        hovered,
                        false
                );
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
        int local = (int) ((mouseY - (top + headerHeight)) / rowHeight);
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
                && mouseY >= top + headerHeight
                && mouseY < top + headerHeight + visibleRows * rowHeight;
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
