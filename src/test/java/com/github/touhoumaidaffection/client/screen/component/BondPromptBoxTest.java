package com.github.touhoumaidaffection.client.screen.component;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Guards the line-grid arithmetic of the dialogue editor: the settings panel sizes the box as
 * {@code 9 * lines + 4} and expects the scroll offset to stay on the same grid, so a scrolled box
 * never shows a text line sliced in half.
 */
class BondPromptBoxTest {
    /** The height the settings panel uses for the dialogue editor. */
    private static final int BOX_HEIGHT = 49;

    @Test
    void editorHeightFitsWholeLines() {
        assertEquals(5, BondPromptBox.displayableLines(BOX_HEIGHT));
        assertEquals(4, BondPromptBox.displayableLines(40));
        assertEquals(1, BondPromptBox.displayableLines(13));
        assertEquals(1, BondPromptBox.displayableLines(1));
    }

    @Test
    void shortTextNeverScrolls() {
        for (int lines = 1; lines <= 5; lines++) {
            assertEquals(0, BondPromptBox.maxScrollAmount(lines, BOX_HEIGHT), "lines=" + lines);
        }
    }

    @Test
    void overflowScrollsByWholeLinesOnly() {
        assertEquals(9, BondPromptBox.maxScrollAmount(6, BOX_HEIGHT));
        assertEquals(18, BondPromptBox.maxScrollAmount(7, BOX_HEIGHT));
        assertEquals(45, BondPromptBox.maxScrollAmount(10, BOX_HEIGHT));
    }

    @Test
    void everyScrollOffsetIsSnappedToALine() {
        assertEquals(0.0, BondPromptBox.snapToLine(0.0));
        assertEquals(0.0, BondPromptBox.snapToLine(4.0));
        assertEquals(9.0, BondPromptBox.snapToLine(4.5));
        assertEquals(9.0, BondPromptBox.snapToLine(13.0));
        assertEquals(18.0, BondPromptBox.snapToLine(18.0));
        assertEquals(27.0, BondPromptBox.snapToLine(23.0));
    }
}