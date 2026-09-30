package com.github.touhoumaidaffection.client.screen.component;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BondHeaderLayoutTest {
    @Test
    void titleAndSettingsShareTheHeaderWithoutTouchingTabsOrAbilityRows() {
        for (int origin : new int[]{-37, 0, 113}) {
            int pageX = origin + 80;
            int pageY = origin + 28;
            var header = BondHeaderLayout.fromPage(pageX, pageY, 176);
            var title = header.title();
            var gear = header.settings();
            assertTrue(title.x() >= pageX + 3);
            assertTrue(title.x() + title.width() < gear.x());
            assertTrue(gear.x() + gear.width() <= pageX + 176 - 3);
            assertTrue(gear.y() >= pageY + 3, "settings must not escape upward into TLM tabs");
            assertTrue(gear.y() + gear.height() <= pageY + 22, "settings must remain above the ability viewport");
            assertTrue(title.y() >= pageY + 3);
            assertTrue(title.y() + title.height() <= pageY + 22);
            assertEquals(title.y() + title.height() / 2.0, gear.y() + gear.height() / 2.0);
            assertFalse(gear.contains(gear.x() + gear.width() / 2.0, pageY - 1));
        }
    }

    @Test
    void pointerTargetsExactlyTheDrawnSettingsRectangle() {
        var gear = BondHeaderLayout.fromPage(80, 28, 176).settings();
        assertTrue(gear.contains(gear.x(), gear.y()));
        assertTrue(gear.contains(gear.x() + gear.width() - 0.01, gear.y() + gear.height() - 0.01));
        assertFalse(gear.contains(gear.x() - 0.01, gear.y()));
        assertFalse(gear.contains(gear.x(), gear.y() - 0.01));
        assertFalse(gear.contains(gear.x() + gear.width(), gear.y()));
        assertFalse(gear.contains(gear.x(), gear.y() + gear.height()));
    }
}
