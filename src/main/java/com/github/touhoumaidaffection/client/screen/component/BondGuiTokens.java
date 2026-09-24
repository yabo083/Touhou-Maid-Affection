package com.github.touhoumaidaffection.client.screen.component;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Shared visual tokens for every bond GUI surface.
 *
 * <p>Palette: warm wood surfaces with a rose interaction accent and gold reserved for highlighted
 * text (values, current selection, scope tags). Only the values live here; call sites must always
 * reference these constants instead of hard-coding an ARGB literal so a palette change stays a
 * single-file edit.
 */
public final class BondGuiTokens {
    private BondGuiTokens() {
    }

    public static final int SPACING_XS = 2;
    public static final int SPACING_SM = 4;
    public static final int SPACING_MD = 8;
    public static final int SPACING_LG = 12;
    public static final int SPACING_XL = 16;

    /**
     * Height of a modal's title bar. The settings panel's background artwork paints its header
     * separator at exactly this fraction of the panel height (20 / 230), so the two must stay in
     * sync; the other modals simply inherit the slightly shorter title bar.
     */
    public static final int MODAL_TITLE_HEIGHT = 21;
    public static final int MODAL_FOOTER_HEIGHT = 30;
    public static final int CONTENT_SIDE_PADDING = SPACING_MD;

    public static final int CONTROL_HEIGHT = 20;
    public static final int DROPDOWN_ROW_HEIGHT = 16;
    public static final int LIST_ROW_HEIGHT = 24;

    public static final int BUTTON_MIN_WIDTH = 40;
    public static final int BUTTON_HORIZONTAL_PADDING = 8;
    public static final int SECONDARY_MODAL_WIDTH = 172;
    public static final int SECONDARY_MODAL_HEIGHT = 150;
    /** Wider modal used by the settings panel so it can host the 50px navigation rail and the content columns. */
    public static final int SETTINGS_MODAL_WIDTH = 340;
    /** Taller modal used by the settings panel: the rail must fit four tabs plus the footer. */
    public static final int SETTINGS_MODAL_HEIGHT = 230;

    public static final int COLOR_BG_OVERLAY = 0xAA120D09;
    public static final int COLOR_BG_PANEL = 0xF52E241C;
    public static final int COLOR_BG_ELEMENT = 0x993A2E22;
    public static final int COLOR_BG_ELEMENT_HOVER = 0xCC463829;

    public static final int COLOR_TEXT_TITLE = 0xFFF2E9DC;
    public static final int COLOR_TEXT_BODY = 0xFFD6CAB8;
    public static final int COLOR_TEXT_HINT = 0xFFA8917A;
    public static final int COLOR_TEXT_SELECTED = 0xFFE8C87E;
    public static final int COLOR_TEXT_DISABLED = 0xFF6B5A47;

    public static final int COLOR_SUCCESS = 0xFF7BC96F;
    public static final int COLOR_WARNING = 0xFFE8C87E;
    public static final int COLOR_ERROR = 0xFFFF5555;
    public static final int COLOR_ACCENT = 0xFFC05E77;

    public static final int BORDER_OUTER = 0xFF120D09;
    public static final int BORDER_INNER = 0xFF4A3B2C;
    public static final int BORDER_INNER_HOVER = 0xFF6B563F;
    public static final int BORDER_INNER_DISABLED = 0xFF33291F;

    public static final int STATE_DEFAULT_BG = 0xAA3A2E22;
    public static final int STATE_DEFAULT_BORDER = BORDER_INNER;
    public static final int STATE_HOVER_BG = COLOR_BG_ELEMENT_HOVER;
    public static final int STATE_HOVER_BORDER = BORDER_INNER_HOVER;
    public static final int STATE_PRESSED_BG = 0xCC2A211A;
    public static final int STATE_PRESSED_BORDER = BORDER_INNER_HOVER;
    public static final int STATE_SELECTED_BG = 0x3DC05E77;
    public static final int STATE_SELECTED_BORDER = 0xFFC05E77;
    public static final int STATE_DISABLED_BG = 0xFF2C231A;
    public static final int STATE_DISABLED_BORDER = 0xFF443528;

    public static final int PRIMARY_BUTTON_BG = 0xEBC05E77;
    public static final int PRIMARY_BUTTON_HOVER_BG = 0xEBD4748D;

    public static final int DIVIDER_COLOR = 0x1AFFFFFF;
    public static final int HOVER_OVERLAY = 0x33FFFFFF;
    public static final int SELECTED_ROW_GLOW = 0x3DC05E77;
    public static final int SELECTED_ROW_STRIPE = 0xFFC05E77;
    public static final int TITLE_PANEL_BG = 0x00000000;

    // ---- Semantic control colors (settings panel and later pages) ----
    public static final int TOGGLE_TRACK = 0xFF3A2E22;
    public static final int TOGGLE_TRACK_BORDER = 0xFF6B563F;
    public static final int TOGGLE_KNOB = 0xFFA8917A;
    public static final int TOGGLE_ON_TRACK = 0xFFC05E77;
    public static final int TOGGLE_ON_BORDER = 0xFFDE8AA2;
    public static final int TOGGLE_ON_KNOB = 0xFFFFF4F7;
    public static final int TOGGLE_DISABLED_BG = 0xFF2C231A;
    public static final int TOGGLE_DISABLED_BORDER = 0xFF443528;
    public static final int TOGGLE_DISABLED_KNOB = 0xFF6B563F;

    public static final int FIELD_BG = 0xFF3A2E22;
    public static final int FIELD_HOVER = 0xFF463829;
    public static final int FIELD_BORDER = 0xFF6B563F;
    public static final int FIELD_BORDER_HOVER = 0xFF8A7050;

    public static final int SLIDER_TRACK = 0xFF3A2E22;
    public static final int SLIDER_FILL = 0xFFC05E77;
    public static final int SLIDER_KNOB = 0xFFFFF4F7;

    public static final int HIGHLIGHT_TEXT = 0xFFE8C87E;
    public static final int TAG_SERVER = 0xFFE8C87E;
    public static final int TAG_LOCAL = 0xFF9ECFA4;
    public static final int NAV_SELECTED_TEXT = 0xFFF0B8C6;
    public static final int NAV_SELECTED_BG = 0x3DC05E77;

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