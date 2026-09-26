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
        graphics.fill(pageLeft, pageTop, pageRight, pageBottom, BondGuiTokens.COLOR_BG_OVERLAY);
        // Frame, panel body and the header separator come from one replaceable texture drawn as a
        // nine-patch, so every modal size reuses the same artwork and no frame fill is drawn here.
        BondGuiArt.drawModalChrome(graphics, left, top, width, height);

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
