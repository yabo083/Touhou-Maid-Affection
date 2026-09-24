package com.github.touhoumaidaffection.client.screen.component;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Self-drawn slider matching the rest of the bond GUI components.
 *
 * <p>Geometry is fixed at construction; the caller owns positioning and scrolling. The control is
 * intentionally small and allocation free while rendering so it can be reused by other pages.
 */
public final class BondSlider {
    private static final int TRACK_THICKNESS = 4;
    private static final int KNOB_HALF_WIDTH = 2;
    private static final int KNOB_HALF_HEIGHT = 4;
    private static final String VALUE_FORMAT = "%.2f";

    private final int height;
    private final int labelWidth;
    private final int valueWidth;
    private final double min;
    private final double max;
    private final double step;

    private int left;
    private int top;
    private int width;
    private double value;
    private boolean dragging;

    public BondSlider(int left, int top, int width, int height, int labelWidth, int valueWidth,
                      double min, double max, double step, double value) {
        this.left = left;
        this.top = top;
        this.width = width;
        this.height = height;
        this.labelWidth = labelWidth;
        this.valueWidth = valueWidth;
        this.min = min;
        this.max = Math.max(min, max);
        this.step = step > 0.0D ? step : 0.0D;
        this.value = quantize(value);
    }

    public void render(GuiGraphics graphics, Font font, Component label, int mouseX, int mouseY) {
        int textY = top + Math.max(0, (height - font.lineHeight) / 2);
        graphics.drawString(font, label, left, textY, BondGuiTokens.COLOR_TEXT_BODY, false);

        int trackLeft = trackLeft();
        int trackRight = trackRight();
        int centerY = top + height / 2;
        int trackTop = centerY - TRACK_THICKNESS / 2;
        int trackBottom = trackTop + TRACK_THICKNESS;

        graphics.fill(trackLeft, trackTop, trackRight, trackBottom, BondGuiTokens.STATE_DEFAULT_BG);
        graphics.fill(trackLeft, trackTop, trackLeft + filledWidth(), trackBottom, BondGuiTokens.COLOR_ACCENT);
        graphics.fill(trackLeft, trackTop, trackRight, trackTop + 1, BondGuiTokens.BORDER_INNER);

        boolean hovered = contains(mouseX, mouseY);
        int knobColor = dragging || hovered ? BondGuiTokens.COLOR_TEXT_TITLE : BondGuiTokens.COLOR_TEXT_SELECTED;
        int knobX = knobCenterX();
        graphics.fill(knobX - KNOB_HALF_WIDTH, centerY - KNOB_HALF_HEIGHT, knobX + KNOB_HALF_WIDTH, centerY + KNOB_HALF_HEIGHT, knobColor);

        String formatted = String.format(java.util.Locale.ROOT, VALUE_FORMAT, value);
        graphics.drawString(font, formatted, right() - font.width(formatted), textY, BondGuiTokens.COLOR_TEXT_HINT, false);
    }

    /** Starts dragging when the press lands inside the slider row. */
    public boolean mousePressed(double mouseX, double mouseY) {
        if (!contains(mouseX, mouseY)) {
            return false;
        }
        dragging = true;
        updateFromMouse(mouseX);
        return true;
    }

    public boolean mouseDragged(double mouseX, double mouseY) {
        if (!dragging) {
            return false;
        }
        updateFromMouse(mouseX);
        return true;
    }

    public boolean mouseReleased() {
        if (!dragging) {
            return false;
        }
        dragging = false;
        return true;
    }

    public boolean contains(double mouseX, double mouseY) {
        return mouseX >= left && mouseX < right() && mouseY >= top && mouseY < top + height;
    }

    /** Repositions the control; used by pages that scroll their content. */
    public void setPosition(int left, int top) {
        this.left = left;
        this.top = top;
    }

    public void setSize(int width) {
        this.width = Math.max(0, width);
    }

    public boolean isDragging() {
        return dragging;
    }

    public double value() {
        return value;
    }

    public void setValue(double newValue) {
        value = quantize(newValue);
    }

    private void updateFromMouse(double mouseX) {
        int trackLeft = trackLeft();
        int trackWidth = Math.max(1, trackRight() - trackLeft);
        double ratio = (mouseX - trackLeft) / trackWidth;
        value = quantize(min + Math.max(0.0D, Math.min(1.0D, ratio)) * (max - min));
    }

    private double quantize(double raw) {
        double clamped = Math.max(min, Math.min(max, raw));
        if (step <= 0.0D) {
            return clamped;
        }
        double snapped = min + Math.round((clamped - min) / step) * step;
        return Math.max(min, Math.min(max, Math.round(snapped / step) * step));
    }

    private int filledWidth() {
        int trackWidth = Math.max(1, trackRight() - trackLeft());
        double ratio = max > min ? (value - min) / (max - min) : 0.0D;
        return (int) Math.round(trackWidth * Math.max(0.0D, Math.min(1.0D, ratio)));
    }

    private int knobCenterX() {
        return trackLeft() + filledWidth();
    }

    private int trackLeft() {
        return left + labelWidth;
    }

    private int trackRight() {
        return Math.max(trackLeft() + 1, right() - valueWidth);
    }

    private int right() {
        return left + width;
    }
}