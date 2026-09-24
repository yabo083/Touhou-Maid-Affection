package com.github.touhoumaidaffection.client.screen.component;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Geometry/hit-test contract of {@link BondDropdown}: the expanded list can be flipped above the
 * header, and hover/click hit testing must always follow the drawn position.
 */
class BondDropdownTest {
    private static final int LEFT = 10;
    private static final int TOP = 20;
    private static final int WIDTH = 78;
    private static final int HEADER = 20;
    private static final int ROW = 12;
    private static final int MAX_VISIBLE = 4;
    private static final int ITEMS = 7;

    private static BondDropdown<String> expanded() {
        BondDropdown<String> dropdown = new BondDropdown<>(LEFT, TOP, WIDTH, HEADER, ROW, MAX_VISIBLE);
        dropdown.mouseClicked(LEFT + 1, TOP + 1, ITEMS);
        assertTrue(dropdown.isExpanded());
        return dropdown;
    }

    @Test
    void listSitsBelowTheHeaderByDefault() {
        BondDropdown<String> dropdown = expanded();
        assertEquals(48, dropdown.overlayHeight(ITEMS));
        assertTrue(dropdown.contains(LEFT, TOP + HEADER, ITEMS));
        assertTrue(dropdown.contains(LEFT, TOP + HEADER + 47, ITEMS));
        assertFalse(dropdown.contains(LEFT, TOP + HEADER + 48, ITEMS));
        assertEquals(0, dropdown.getHoveredIndex(LEFT + 1, TOP + HEADER + 1, ITEMS));
        assertEquals(3, dropdown.getHoveredIndex(LEFT + 1, TOP + HEADER + 37, ITEMS));
        assertEquals(-1, dropdown.getHoveredIndex(LEFT + 1, TOP + HEADER + 48, ITEMS));
    }

    @Test
    void flippedListSitsAboveTheHeaderAndHitTestFollows() {
        BondDropdown<String> dropdown = expanded();
        dropdown.setOverlayAbove(true);
        int listTop = TOP - 48;
        assertTrue(dropdown.contains(LEFT, listTop, ITEMS));
        assertFalse(dropdown.contains(LEFT, listTop - 1, ITEMS));
        assertFalse(dropdown.contains(LEFT, TOP + HEADER, ITEMS), "flipped list must not extend below the header");
        assertTrue(dropdown.contains(LEFT, TOP + 1, ITEMS), "header stays clickable");
        assertEquals(0, dropdown.getHoveredIndex(LEFT + 1, listTop + 1, ITEMS));
        assertEquals(3, dropdown.getHoveredIndex(LEFT + 1, listTop + 37, ITEMS));
        assertEquals(-1, dropdown.getHoveredIndex(LEFT + 1, listTop - 1, ITEMS));
    }

    @Test
    void clickingAFlippedRowSelectsTheIndexDrawnAtThatPosition() {
        BondDropdown<String> dropdown = expanded();
        dropdown.setOverlayAbove(true);
        int listTop = TOP - 48;
        BondDropdown.ClickResult result = dropdown.mouseClicked(LEFT + 1, listTop + ROW + 1, ITEMS);
        assertTrue(result.handled());
        assertEquals(1, result.selectedIndex());
        assertFalse(dropdown.isExpanded());
    }

    @Test
    void scrollOffsetClampsToTheNumberOfItems() {
        BondDropdown<String> dropdown = expanded();
        for (int i = 0; i < 10; i++) {
            dropdown.mouseScrolled(LEFT + 1, TOP + HEADER + 1, -1.0D, ITEMS);
        }
        assertEquals(ITEMS - 1, dropdown.getHoveredIndex(LEFT + 1, TOP + HEADER + 37, ITEMS));
    }

    @Test
    void collapsedDropdownReportsNoExpandedHitArea() {
        BondDropdown<String> dropdown = new BondDropdown<>(LEFT, TOP, WIDTH, HEADER, ROW, MAX_VISIBLE);
        assertFalse(dropdown.contains(LEFT, TOP + HEADER + 1, ITEMS));
        assertEquals(-1, dropdown.getHoveredIndex(LEFT, TOP + HEADER + 1, ITEMS));
    }
}