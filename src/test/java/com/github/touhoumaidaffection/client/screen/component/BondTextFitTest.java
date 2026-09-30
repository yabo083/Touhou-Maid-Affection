package com.github.touhoumaidaffection.client.screen.component;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BondTextFitTest {
    @Test
    void longLabelsFitBothDimensionsWithoutEnlargingShortLabels() {
        for (int textWidth : new int[]{1, 30, 40, 150, 600}) {
            for (int textHeight : new int[]{9, 24}) {
                float scale = BondTextFit.scale(textWidth, textHeight, 40, 13);
                assertTrue(scale > 0 && scale <= 1);
                assertTrue(textWidth * scale <= 40.0001F);
                assertTrue(textHeight * scale <= 13.0001F);
                if (scale < 1) {
                    assertTrue(Math.abs(textWidth * scale - 40) < 0.0001F
                            || Math.abs(textHeight * scale - 13) < 0.0001F,
                            "use the largest readable scale that fits");
                }
            }
        }
        assertEquals(1.0F, BondTextFit.scale(30, 9, 40, 13));
    }

    @Test
    void noDrawableAreaProducesNoText() {
        assertEquals(0.0F, BondTextFit.scale(20, 9, 0, 13));
        assertEquals(0.0F, BondTextFit.scale(20, 9, 40, -1));
        assertEquals(0.0F, BondTextFit.scale(0, 9, 40, 13));
        assertEquals(0.0F, BondTextFit.scale(20, 0, 40, 13));
    }
}
