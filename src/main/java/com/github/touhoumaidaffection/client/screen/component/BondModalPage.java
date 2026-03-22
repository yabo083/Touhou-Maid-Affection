package com.github.touhoumaidaffection.client.screen.component;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class BondModalPage {
    private final int pageLeft;
    private final int pageTop;
    private final int pageRight;
    private final int pageBottom;
    private final int left;
    private final int top;
    private final int width;
    private final int height;
    private final Component title;

    public BondModalPage(int pageLeft, int pageTop, int pageWidth, int pageHeight, int width, int height, Component title) {
        this.pageLeft = pageLeft;
        this.pageTop = pageTop;
        this.pageRight = pageLeft + pageWidth;
        this.pageBottom = pageTop + pageHeight;
        this.width = width;
        this.height = height;
        this.left = pageLeft + (pageWidth - width) / 2;
        this.top = pageTop + (pageHeight - height) / 2;
        this.title = title;
    }

    public void renderChrome(GuiGraphics graphics, Font font) {
        graphics.fill(pageLeft, pageTop, pageRight, pageBottom, 0xA0000000);
        graphics.fill(left - 2, top - 2, right() + 2, bottom() + 2, 0xCC000000);
        graphics.fill(left, top, right(), bottom(), 0xFF3A3A3A);
        graphics.fill(left + 1, top + 1, right() - 1, bottom() - 1, 0xFF1F1F1F);
        graphics.drawCenteredString(font, title, left + width / 2, top + 8, 0xFFFFFFFF);
    }

    public boolean contains(double mouseX, double mouseY) {
        return mouseX >= left && mouseX < right() && mouseY >= top && mouseY < bottom();
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

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }
}
