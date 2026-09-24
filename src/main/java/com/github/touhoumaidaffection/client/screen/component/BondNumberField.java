package com.github.touhoumaidaffection.client.screen.component;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

import java.util.OptionalInt;
import java.util.function.IntConsumer;

/**
 * Compact single-line integer field used by the settings panel for the numeric cache-policy keys.
 *
 * <p>It reuses the panel's existing field chrome ({@link BondGuiTokens#FIELD_BG} /
 * {@link BondGuiTokens#FIELD_BORDER}, the same colours the dropdown header draws) around a vanilla
 * {@link EditBox} whose own border is turned off, so a number field and a dropdown sit next to each
 * other without a visual mismatch. A slider would be unusable here: the accepted ranges span four
 * orders of magnitude ({@code 20..72000} ticks), which no pixel-resolution drag can cover.
 *
 * <p>Editing contract: only ASCII digits can be typed, the value is committed when the field loses
 * focus, and the committed value is clamped into {@code [min, max]} so the client can never send an
 * out-of-range number (the server still rejects one, see {@code TmaSettingsKeys#normalizeInt}).
 * Server pushes never overwrite an in-progress edit - the same rule as the prompt editor.
 */
public final class BondNumberField {
    /** Height of the field chrome; matches the panel's other compact field controls. */
    public static final int HEIGHT = 14;
    /** Text inset from the left border. */
    private static final int PADDING = 4;
    /** Gap between the editable text area and the right-aligned unit suffix. */
    private static final int UNIT_GAP = 2;
    /** Enough for the largest accepted value (72000). */
    private static final int MAX_DIGITS = 6;

    private final Font font;
    private final EditBox box;
    private final int min;
    private final int max;
    private final String unit;

    private int left;
    private int top;
    private int width;
    private int value;
    private IntConsumer responder;

    public BondNumberField(Font font, int min, int max, String unit, int initial) {
        this.font = font;
        this.min = Math.min(min, max);
        this.max = Math.max(min, max);
        this.unit = unit == null ? "" : unit;
        this.value = clamp(initial, this.min, this.max);
        this.box = new EditBox(font, 0, 0, 1, HEIGHT, Component.empty());
        this.box.setBordered(false);
        this.box.setMaxLength(MAX_DIGITS);
        this.box.setFilter(BondNumberField::isDigitsOnly);
        this.box.setTextColor(BondGuiTokens.COLOR_TEXT_BODY);
        this.box.setTextColorUneditable(BondGuiTokens.COLOR_TEXT_DISABLED);
        this.box.setValue(Integer.toString(this.value));
    }

    /** @return {@code true} when {@code raw} is empty or made of ASCII digits only. */
    public static boolean isDigitsOnly(String raw) {
        if (raw == null) {
            return false;
        }
        for (int index = 0; index < raw.length(); index++) {
            char character = raw.charAt(index);
            if (character < '0' || character > '9') {
                return false;
            }
        }
        return true;
    }

    /**
     * Parses a digit-only string and clamps it into {@code [min, max]}.
     *
     * @return the clamped value, or {@link OptionalInt#empty()} when the text is not a plain run of
     *         digits (blank, signed, decimal, exponential or otherwise malformed).
     */
    public static OptionalInt parseClamped(String raw, int min, int max) {
        if (raw == null) {
            return OptionalInt.empty();
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty() || !isDigitsOnly(trimmed)) {
            return OptionalInt.empty();
        }
        try {
            return OptionalInt.of(clamp((int) Math.min(Integer.MAX_VALUE, Long.parseLong(trimmed)), min, max));
        } catch (NumberFormatException overflow) {
            // A digit run that big can only clamp to the upper bound.
            return OptionalInt.of(Math.max(min, max));
        }
    }

    private static int clamp(int value, int min, int max) {
        int low = Math.min(min, max);
        int high = Math.max(min, max);
        return Math.max(low, Math.min(high, value));
    }

    public void setBounds(int left, int top, int width) {
        this.left = left;
        this.top = top;
        this.width = Math.max(PADDING + UNIT_GAP + 8, width);
        this.box.setX(this.left + PADDING);
        this.box.setY(this.top + (HEIGHT - 8) / 2);
        this.box.setWidth(Math.max(8, this.width - PADDING - unitWidth() - UNIT_GAP - PADDING));
    }

    public int left() {
        return left;
    }

    public int top() {
        return top;
    }

    public int width() {
        return width;
    }

    public int value() {
        return value;
    }

    public void setResponder(IntConsumer responder) {
        this.responder = responder;
    }

    public void setEditable(boolean editable) {
        this.box.setEditable(editable);
    }

    public boolean isFocused() {
        return this.box.isFocused();
    }

    public boolean contains(double mouseX, double mouseY) {
        return mouseX >= left && mouseX < left + width && mouseY >= top && mouseY < top + HEIGHT;
    }

    /** Focuses the field and selects the whole value so typing replaces it. */
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!contains(mouseX, mouseY)) {
            return false;
        }
        this.box.setFocused(true);
        this.box.setCursorPosition(0);
        this.box.setHighlightPos(this.box.getValue().length());
        return true;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return this.box.isFocused() && this.box.mouseReleased(mouseX, mouseY, button);
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return this.box.isFocused() && this.box.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return this.box.keyPressed(keyCode, scanCode, modifiers);
    }

    public boolean charTyped(char codePoint, int modifiers) {
        return this.box.charTyped(codePoint, modifiers);
    }

    /**
     * Commits the edited text: clamps it into range, rewrites the editor with the canonical value
     * and notifies the responder when the committed value actually changed.
     *
     * @return {@code true} when a new value was committed.
     */
    public boolean commit() {
        OptionalInt parsed = parseClamped(this.box.getValue(), this.min, this.max);
        if (parsed.isEmpty()) {
            this.box.setValue(Integer.toString(this.value));
            return false;
        }
        int next = parsed.getAsInt();
        this.box.setValue(Integer.toString(next));
        if (next == this.value) {
            return false;
        }
        this.value = next;
        if (this.responder != null) {
            this.responder.accept(next);
        }
        return true;
    }

    /** Unfocuses the field, committing whatever was typed (the panel's "commit on blur" rule). */
    public boolean blur() {
        if (!this.box.isFocused()) {
            return false;
        }
        this.box.setFocused(false);
        return commit();
    }

    /** Mirrors an authoritative value; an in-progress edit is never clobbered. */
    public void setValue(int next) {
        this.value = clamp(next, this.min, this.max);
        if (!this.box.isFocused() && !Integer.toString(this.value).equals(this.box.getValue())) {
            this.box.setValue(Integer.toString(this.value));
        }
    }

    /** @return the last authoritative value, for the pending marker comparison. */
    public String wireValue() {
        return Integer.toString(this.value);
    }

    private int unitWidth() {
        return this.unit.isEmpty() ? 0 : this.font.width(this.unit);
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY, boolean enabled) {
        boolean hovered = contains(mouseX, mouseY);
        int background = !enabled
                ? BondGuiTokens.TOGGLE_DISABLED_BG
                : hovered ? BondGuiTokens.FIELD_HOVER : BondGuiTokens.FIELD_BG;
        int border = !enabled
                ? BondGuiTokens.TOGGLE_DISABLED_BORDER
                : hovered ? BondGuiTokens.FIELD_BORDER_HOVER : BondGuiTokens.FIELD_BORDER;
        graphics.fill(left, top, left + width, top + HEIGHT, border);
        graphics.fill(left + 1, top + 1, left + width - 1, top + HEIGHT - 1, background);
        this.box.render(graphics, mouseX, mouseY, 0.0F);
        if (!this.unit.isEmpty()) {
            int unitLeft = left + width - PADDING - unitWidth();
            graphics.drawString(font, this.unit, unitLeft, top + (HEIGHT - font.lineHeight) / 2,
                    BondGuiTokens.COLOR_TEXT_HINT, false);
        }
    }
}