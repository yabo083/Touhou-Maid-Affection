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
        int right = right();
        int bottom = bottom();
        graphics.fill(pageLeft, pageTop, pageRight, pageBottom, BondGuiTokens.COLOR_BG_OVERLAY);
        BondGuiTokens.drawFramedPanel(graphics, left, top, right, bottom, BondGuiTokens.COLOR_BG_PANEL);
        int titleBottom = Math.min(bottom - 1, top + BondGuiTokens.MODAL_TITLE_HEIGHT);
        graphics.fill(left + 2, top + 2, right - 2, titleBottom, BondGuiTokens.TITLE_PANEL_BG);
        graphics.hLine(left + 2, right - 3, titleBottom, BondGuiTokens.DIVIDER_COLOR);

        int titleY = top + Math.max(2, (BondGuiTokens.MODAL_TITLE_HEIGHT - font.lineHeight) / 2);
        int titleX = left + (width - font.width(title)) / 2;
        graphics.drawString(font, title, titleX, titleY, BondGuiTokens.COLOR_TEXT_TITLE, true);
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

    public int contentLeft() {
        return left + BondGuiTokens.CONTENT_SIDE_PADDING;
    }

    public int contentRight() {
        return right() - BondGuiTokens.CONTENT_SIDE_PADDING;
    }

    public int contentTop() {
        return top + BondGuiTokens.MODAL_TITLE_HEIGHT + BondGuiTokens.SPACING_MD;
    }

    public int contentBottom() {
        return bottom() - BondGuiTokens.MODAL_FOOTER_HEIGHT;
    }

    public int footerTop() {
        return bottom() - BondGuiTokens.MODAL_FOOTER_HEIGHT;
    }

    public int footerButtonY(int buttonHeight) {
        return bottom() - BondGuiTokens.SPACING_SM - buttonHeight;
    }
}
