package com.github.touhoumaidaffection.client.screen.component;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BondButtonRowLayoutTest {
    @Test
    void longTranslatedLabelsFitTheModalAndRetainSeparateClickTargets() {
        Font font = new Font(id -> null, false) {
            @Override
            public int width(FormattedText text) {
                return text.getString().length() * 6;
            }
        };
        int width = BondGuiTokens.SECONDARY_MODAL_WIDTH;
        int padding = BondGuiTokens.CONTENT_SIDE_PADDING;
        var buttons = BondButtonRow.createCenteredUniform(font, width, 110, 17, 4, 8,
                new BondButtonRow.ButtonSpec(0, 0, 0, 0, Component.literal("Sequential"), "mode", true),
                new BondButtonRow.ButtonSpec(0, 0, 0, 0, Component.literal("Select none"), "select", true),
                new BondButtonRow.ButtonSpec(0, 0, 0, 0, Component.literal("Save settings"), "save", false));

        int previousRight = padding - 4;
        for (var button : buttons) {
            assertTrue(button.x() >= previousRight + 4, "button boxes must remain separate");
            assertTrue(button.x() + button.width() <= width - padding, "button must fit inside modal");
            assertTrue(button.width() > 0);
            assertEquals(button.enabled() ? button.id() : "",
                    BondButtonRow.click(buttons, 30, 30 + button.x() + button.width() / 2.0, 118));
            previousRight = button.x() + button.width();
        }
        assertEquals("", BondButtonRow.click(buttons, 30, 29, 118));
        assertEquals("", BondButtonRow.click(buttons, 30, 30 + width, 118));
    }
}
