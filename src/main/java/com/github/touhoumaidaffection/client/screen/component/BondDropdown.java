package com.github.touhoumaidaffection.client.screen.component;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;

public final class BondDropdown<T> {
    /** Opaque variant of {@link BondGuiTokens#COLOR_BG_PANEL} used by the expanded list. */
    private static final int OPAQUE_PANEL = 0xFF000000 | (BondGuiTokens.COLOR_BG_PANEL & 0xFFFFFF);
    private int left;
    private int top;
    private final int width;
    private final int headerHeight;
    private final int rowHeight;
    private final int maxVisibleRows;
    private boolean expanded;
    private boolean overlayAbove;
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

        int visibleRows = Math.min(maxVisibleRows, items.size());
        int listTop = overlayTop(items.size());
        int listBottom = listTop + visibleRows * rowHeight;
        // An expanded list must be opaque: the panel token is translucent for windows, but a dropdown
        // that lets the row underneath bleed through reads as a layering bug.
        BondGuiTokens.drawFramedPanel(graphics, left, listTop, right(), listBottom, OPAQUE_PANEL);
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
        int local = (int) ((mouseY - overlayTop(itemCount)) / rowHeight);
        int index = scrollOffset + local;
        return index >= 0 && index < itemCount ? index : -1;
    }

    public boolean contains(double mouseX, double mouseY, int itemCount) {
        return containsHeader(mouseX, mouseY) || (expanded && containsExpanded(mouseX, mouseY, itemCount));
    }

    /** Repositions the control; used by pages that scroll their content. */
    public void setPosition(int left, int top) {
        this.left = left;
        this.top = top;
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
        int listTop = overlayTop(itemCount);
        int visibleRows = Math.min(maxVisibleRows, Math.max(0, itemCount));
        return mouseX >= left && mouseX < right()
                && mouseY >= listTop
                && mouseY < listTop + visibleRows * rowHeight;
    }

    /**
     * Places the expanded list above the header instead of below it. The caller decides this from the
     * available screen space; hit testing, hover highlighting and scrolling all follow the same
     * placement, so the click target always matches the drawn rows.
     */
    public void setOverlayAbove(boolean above) {
        this.overlayAbove = above;
    }

    /** @return the height of the expanded list for {@code itemCount} items. */
    /**
     * True when the expanded list covers the given screen rectangle (used by callers to skip drawing
     * controls the open list would hide anyway).
     */
    public boolean overlayCovers(int rectLeft, int rectTop, int rectRight, int rectBottom, int itemCount) {
        if (!expanded || itemCount <= 0) {
            return false;
        }
        int listTop = overlayTop(itemCount);
        int listBottom = listTop + overlayHeight(itemCount);
        return rectLeft < right() && rectRight > left && rectTop < listBottom && rectBottom > listTop;
    }

    public int overlayHeight(int itemCount) {
        return Math.max(0, Math.min(maxVisibleRows, itemCount)) * rowHeight;
    }

    private int overlayTop(int itemCount) {
        return overlayAbove ? top - overlayHeight(itemCount) : top + headerHeight;
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
