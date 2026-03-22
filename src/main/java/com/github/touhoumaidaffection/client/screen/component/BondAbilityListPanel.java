package com.github.touhoumaidaffection.client.screen.component;

import net.minecraft.client.gui.GuiGraphics;

public final class BondAbilityListPanel {
    private final int left;
    private final int top;
    private final int width;
    private final int height;
    private final int rowStartY;
    private final int rowHeight;
    private final int rowSpacing;

    public BondAbilityListPanel(int left, int top, int width, int height, int rowStartY, int rowHeight, int rowSpacing) {
        this.left = left;
        this.top = top;
        this.width = width;
        this.height = height;
        this.rowStartY = rowStartY;
        this.rowHeight = rowHeight;
        this.rowSpacing = rowSpacing;
    }

    public void renderViewport(GuiGraphics graphics, Runnable renderer) {
        graphics.enableScissor(left + 1, top + 1, right() - 1, bottom() - 1);
        try {
            renderer.run();
        } finally {
            graphics.disableScissor();
        }
    }

    public int getRowTop(int index) {
        return top + rowStartY + index * rowSpacing;
    }

    public int getVisibleRowCount() {
        return Math.max(0, (height - rowStartY) / rowSpacing);
    }

    public int getRowIndexAt(double mouseX, double mouseY, int itemCount) {
        if (!contains(mouseX, mouseY)) {
            return -1;
        }
        for (int i = 0; i < Math.min(itemCount, getVisibleRowCount()); i++) {
            int rowTop = getRowTop(i);
            int rowBottom = rowTop + rowHeight - 1;
            if (mouseY >= rowTop && mouseY <= rowBottom) {
                return i;
            }
        }
        return -1;
    }

    public boolean contains(double mouseX, double mouseY) {
        return mouseX >= left && mouseX < right() && mouseY >= top && mouseY < bottom();
    }

    public int left() {
        return left;
    }

    public int right() {
        return left + width;
    }

    public int top() {
        return top;
    }

    public int bottom() {
        return top + height;
    }
}
