package com.github.touhoumaidaffection.client.screen.component;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.Locale;

/**
 * Self-drawn compact slider used by the settings panel.
 *
 * <p>Geometry is fixed at construction; the caller owns positioning and scrolling. The current
 * value is drawn centered on the track in the highlight (gold) text color, formatted as
 * {@code 0.00×}. The control is allocation free while rendering apart from the value string.
 */
public final class BondSlider {
    private static final int BORDER = 1;
    private static final int KNOB_WIDTH = 3;
    private static final String VALUE_FORMAT = "%.2f×";

    private final double min;
    private final double max;
    private final double step;

    private int left;
    private int top;
    private int width;
    private int height;
    private double value;
    private boolean dragging;

    public BondSlider(int left, int top, int width, int height, double min, double max, double step, double value) {
        this.left = left;
        this.top = top;
        this.width = width;
        this.height = height;
        this.min = min;
        this.max = Math.max(min, max);
        this.step = step > 0.0D ? step : 0.0D;
        this.value = quantize(value);
    }

    public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY) {
        int right = right();
        int bottom = top + height;
        if (right <= left || bottom <= top) {
            return;
        }
        boolean hovered = contains(mouseX, mouseY);
        int border = dragging
                ? BondGuiTokens.TOGGLE_ON_BORDER
                : hovered ? BondGuiTokens.FIELD_BORDER_HOVER : BondGuiTokens.TOGGLE_TRACK_BORDER;
        graphics.fill(left, top, right, bottom, border);
        graphics.fill(left + BORDER, top + BORDER, right - BORDER, bottom - BORDER, BondGuiTokens.SLIDER_TRACK);
        graphics.fill(trackLeft(), top + BORDER, trackLeft() + filledWidth(), bottom - BORDER, BondGuiTokens.SLIDER_FILL);

        int knobLeft = Math.max(left, Math.min(right - KNOB_WIDTH, knobCenterX() - KNOB_WIDTH / 2));
        graphics.fill(knobLeft, top - 1, knobLeft + KNOB_WIDTH, bottom + 1, BondGuiTokens.SLIDER_KNOB);

        String text = String.format(Locale.ROOT, VALUE_FORMAT, value);
        graphics.drawString(
                font,
                text,
                left + (width - font.width(text)) / 2,
                top + Math.max(1, (height - font.lineHeight) / 2),
                BondGuiTokens.HIGHLIGHT_TEXT,
                true
        );
    }

    /** Starts dragging when the press lands inside the slider box. */
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
        return left + BORDER;
    }

    private int trackRight() {
        return Math.max(trackLeft() + 1, right() - BORDER);
    }

    private int right() {
        return left + width;
    }
}