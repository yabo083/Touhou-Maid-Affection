package com.github.touhoumaidaffection.client.screen.component;

import net.minecraft.client.gui.GuiGraphics;

public final class BondGuiTokens {
    private BondGuiTokens() {
    }

    public static final int SPACING_XS = 2;
    public static final int SPACING_SM = 4;
    public static final int SPACING_MD = 8;
    public static final int SPACING_LG = 12;
    public static final int SPACING_XL = 16;

    public static final int MODAL_TITLE_HEIGHT = 24;
    public static final int MODAL_FOOTER_HEIGHT = 30;
    public static final int CONTENT_SIDE_PADDING = SPACING_MD;

    public static final int CONTROL_HEIGHT = 20;
    public static final int DROPDOWN_ROW_HEIGHT = 16;
    public static final int LIST_ROW_HEIGHT = 24;

    public static final int BUTTON_MIN_WIDTH = 40;
    public static final int BUTTON_HORIZONTAL_PADDING = 8;
    public static final int SECONDARY_MODAL_WIDTH = 172;
    public static final int SECONDARY_MODAL_HEIGHT = 150;

    public static final int COLOR_BG_OVERLAY = 0xAA0F0A0D;
    public static final int COLOR_BG_PANEL = 0xF5241C21;
    public static final int COLOR_BG_ELEMENT = 0x99120C10;
    public static final int COLOR_BG_ELEMENT_HOVER = 0xCC2B2127;

    public static final int COLOR_TEXT_TITLE = 0xFFF5E6E8;
    public static final int COLOR_TEXT_BODY = 0xFFD2C4C8;
    public static final int COLOR_TEXT_HINT = 0xFFB29EA5;
    public static final int COLOR_TEXT_SELECTED = 0xFFE8C87E;
    public static final int COLOR_TEXT_DISABLED = 0xFF5A4C52;

    public static final int COLOR_SUCCESS = 0xFF7BC96F;
    public static final int COLOR_WARNING = 0xFFE8C87E;
    public static final int COLOR_ERROR = 0xFFFF5555;
    public static final int COLOR_ACCENT = 0xFF9E435D;

    public static final int BORDER_OUTER = 0xFF110A0D;
    public static final int BORDER_INNER = 0xFF45353D;
    public static final int BORDER_INNER_HOVER = 0xFF5E4A54;
    public static final int BORDER_INNER_DISABLED = 0xFF2F252A;

    public static final int STATE_DEFAULT_BG = 0xAA241C21;
    public static final int STATE_DEFAULT_BORDER = BORDER_INNER;
    public static final int STATE_HOVER_BG = 0xAA241C21;
    public static final int STATE_HOVER_BORDER = BORDER_INNER_HOVER;
    public static final int STATE_PRESSED_BG = 0xCC1D151A;
    public static final int STATE_PRESSED_BORDER = BORDER_INNER_HOVER;
    public static final int STATE_SELECTED_BG = 0xAA452331;
    public static final int STATE_SELECTED_BORDER = 0xFF9E435D;
    public static final int STATE_DISABLED_BG = 0x882A2427;
    public static final int STATE_DISABLED_BORDER = BORDER_INNER_DISABLED;

    public static final int PRIMARY_BUTTON_BG = 0xDD9E435D;
    public static final int PRIMARY_BUTTON_HOVER_BG = 0xDDB3536E;

    public static final int DIVIDER_COLOR = 0x4445353D;
    public static final int HOVER_OVERLAY = 0x33FFFFFF;
    public static final int SELECTED_ROW_GLOW = 0xAA452331;
    public static final int SELECTED_ROW_STRIPE = 0xFFB86A84;
    public static final int TITLE_PANEL_BG = 0x9932272E;

    public static void drawFramedPanel(GuiGraphics graphics, int left, int top, int right, int bottom, int fillColor) {
        if (right <= left || bottom <= top) {
            return;
        }
        graphics.fill(left, top, right, bottom, BORDER_OUTER);
        if (right - left > 2 && bottom - top > 2) {
            graphics.fill(left + 1, top + 1, right - 1, bottom - 1, BORDER_INNER);
        }
        if (right - left > 4 && bottom - top > 4) {
            graphics.fill(left + 2, top + 2, right - 2, bottom - 2, fillColor);
        }
    }

    public static void drawFramedPanelWithInnerBorder(GuiGraphics graphics, int left, int top, int right, int bottom, int fillColor, int innerBorderColor) {
        if (right <= left || bottom <= top) {
            return;
        }
        graphics.fill(left, top, right, bottom, BORDER_OUTER);
        if (right - left > 2 && bottom - top > 2) {
            graphics.fill(left + 1, top + 1, right - 1, bottom - 1, innerBorderColor);
        }
        if (right - left > 4 && bottom - top > 4) {
            graphics.fill(left + 2, top + 2, right - 2, bottom - 2, fillColor);
        }
    }

    public static void drawSelectableRow(GuiGraphics graphics, int left, int top, int right, int bottom, boolean selected, boolean hovered) {
        graphics.fill(left, top, right, bottom, STATE_DEFAULT_BG);
        if (selected) {
            graphics.fill(left, top, right, bottom, SELECTED_ROW_GLOW);
            int stripeRight = Math.min(right, left + 2);
            graphics.fill(left, top, stripeRight, bottom, SELECTED_ROW_STRIPE);
        }
        if (hovered) {
            graphics.fill(left, top, right, bottom, HOVER_OVERLAY);
        }
    }
}
