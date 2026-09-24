package com.github.touhoumaidaffection.client.screen.component;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.network.chat.Component;

/**
 * The settings panel's dialogue editor: a vanilla {@link MultiLineEditBox} whose scrolling stays on
 * the text-line grid.
 *
 * <p>The vanilla widget scrolls by {@code 9 / 2} pixels per wheel notch and derives its maximum
 * scroll offset from the raw pixel height, so a scrolled box shows lines sliced in half at the top
 * and bottom borders. This subclass snaps every scroll offset to a whole number of text lines
 * ({@link #LINE_HEIGHT}) and reports a maximum that is itself a multiple of that height, which keeps
 * the editor showing whole lines only - the next line starts on the scissor border instead of being
 * cut. The editor is created with a height of {@code 9 * lines + 4} (see
 * {@code TmaSettingsScreen#PROMPT_BOX_HEIGHT}) so the first, unscrolled frame is aligned too.
 *
 * <p>Cursor visibility is handled by the vanilla widget: {@code MultilineTextField} notifies its
 * cursor listener on every move and the widget scrolls to the caret line. Because every scroll
 * offset passes through {@link #setScrollAmount(double)}, that automatic scroll lands on the line
 * grid as well.
 */
public final class BondPromptBox extends MultiLineEditBox {
    /** Height of one text line; the grid every scroll offset is snapped to. */
    private static final int LINE_HEIGHT = 9;
    /** Padding the vanilla widget keeps above the first text line. */
    private static final int TOP_PADDING = 4;

    public BondPromptBox(Font font, int x, int y, int width, int height,
                         Component placeholder, Component message) {
        super(font, x, y, width, height, placeholder, message);
    }

    /** @return the largest scroll offset (a multiple of {@link #LINE_HEIGHT}) that is still useful. */
    public static int maxScrollAmount(int lineCount, int height) {
        return LINE_HEIGHT * Math.max(0, lineCount - displayableLines(height));
    }

    /** @return how many whole lines fit into an editor of {@code height} pixels. */
    public static int displayableLines(int height) {
        // The widget paints its first line 4px below the top border, so the usable strip is
        // height - 4; the editor height is chosen as 9 * lines + 4 so this is exact.
        return Math.max(1, (height - TOP_PADDING) / LINE_HEIGHT);
    }

    /** @return {@code amount} snapped to the nearest whole line. */
    public static double snapToLine(double amount) {
        return Math.round(amount / (double) LINE_HEIGHT) * (double) LINE_HEIGHT;
    }

    @Override
    protected double scrollRate() {
        // One wheel notch scrolls exactly one line, so the grid is never left half way.
        return LINE_HEIGHT;
    }

    @Override
    protected int getMaxScrollAmount() {
        return maxScrollAmount(getInnerHeight() / LINE_HEIGHT, getHeight());
    }

    @Override
    protected void setScrollAmount(double scrollAmount) {
        super.setScrollAmount(snapToLine(scrollAmount));
    }

    @Override
    protected boolean scrollbarVisible() {
        return getMaxScrollAmount() > 0;
    }
}