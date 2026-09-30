package com.github.touhoumaidaffection.client.screen.component;

import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BondButtonRowTest {
    private static List<BondButtonRow.ButtonSpec> footer(int preferredWidth) {
        return BondButtonRow.createCentered(168, 119, preferredWidth, 17, 8,
                new BondButtonRow.ButtonSpec(0, 0, 42, 17, Component.literal("Save"), "save", true, true),
                new BondButtonRow.ButtonSpec(0, 0, 42, 17, Component.literal("Sequential"), "mode", true),
                new BondButtonRow.ButtonSpec(0, 0, 42, 17, Component.literal("Cancel"), "cancel", false));
    }

    @Test
    void translatedFootersStayPaddedCentredAndNonOverlapping() {
        for (int preferredWidth : new int[]{40, 72, 1000}) {
            List<BondButtonRow.ButtonSpec> buttons = footer(preferredWidth);
            int previousRight = buttons.get(0).x() - 8;
            for (BondButtonRow.ButtonSpec button : buttons) {
                assertTrue(button.x() >= BondGuiTokens.CONTENT_SIDE_PADDING);
                assertTrue(button.x() + button.width() <= 168 - BondGuiTokens.CONTENT_SIDE_PADDING);
                assertTrue(button.width() > 0);
                assertEquals(8, button.x() - previousRight);
                assertEquals(buttons.get(0).width(), button.width());
                previousRight = button.x() + button.width();
            }
            assertTrue(Math.abs(buttons.get(0).x() - (168 - previousRight)) <= 1);
        }
        assertEquals(40, footer(40).get(0).width(), "fitting labels must not expand to fill the row");
    }

    @Test
    void constrainedButtonsUseTheirNewHitboxesAndKeepDisabledButtonsInert() {
        List<BondButtonRow.ButtonSpec> buttons = footer(1000);
        int baseLeft = 53;
        BondButtonRow.ButtonSpec first = buttons.get(0);
        BondButtonRow.ButtonSpec middle = buttons.get(1);
        BondButtonRow.ButtonSpec last = buttons.get(2);
        assertEquals("save", BondButtonRow.click(buttons, baseLeft, baseLeft + first.x(), first.y()));
        assertEquals("", BondButtonRow.click(buttons, baseLeft, baseLeft + first.x() - 1, first.y()));
        assertEquals("", BondButtonRow.click(buttons, baseLeft, baseLeft + first.x() + first.width(), first.y()));
        assertEquals("mode", BondButtonRow.click(buttons, baseLeft, baseLeft + middle.x() + middle.width() - 1, middle.y() + middle.height() - 1));
        assertEquals("", BondButtonRow.click(buttons, baseLeft, baseLeft + middle.x(), middle.y() + middle.height()));
        assertEquals("", BondButtonRow.click(buttons, baseLeft, baseLeft + last.x() + last.width() / 2, last.y()));
    }
}
