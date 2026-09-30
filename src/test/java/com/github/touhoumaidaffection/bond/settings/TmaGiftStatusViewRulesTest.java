package com.github.touhoumaidaffection.bond.settings;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TmaGiftStatusViewRulesTest {
    @Test
    void countdownUsesServerEpochAndMonotonicElapsedNotClientWallClock() {
        long elapsed = TmaGiftStatusViewRules.elapsedMillis(4_000_000_000L, 5_500_000_000L);
        assertEquals(1500L, elapsed);
        long now = TmaGiftStatusViewRules.serverNow(1_800_000_000_000L, elapsed);
        assertEquals(1_800_000_001_500L, now);
        assertEquals(2L, TmaGiftStatusViewRules.remainingSeconds(now + 1001L, now));
        assertEquals(1L, TmaGiftStatusViewRules.remainingSeconds(now + 1L, now));
        assertEquals(0L, TmaGiftStatusViewRules.remainingSeconds(now, now));
        assertEquals(0L, TmaGiftStatusViewRules.remainingSeconds(now - 1000L, now));
    }

    @Test
    void scrollAndHoverStopAtTheSameViewportEdges() {
        assertEquals(0, TmaGiftStatusViewRules.clampScroll(200, 100, 172));
        assertEquals(328, TmaGiftStatusViewRules.clampScroll(400, 500, 172));
        assertEquals(0, TmaGiftStatusViewRules.clampScroll(-14, 500, 172));
        assertTrue(TmaGiftStatusViewRules.inViewport(10, 20, 10, 20, 110, 192));
        assertTrue(TmaGiftStatusViewRules.inViewport(109.9, 191.9, 10, 20, 110, 192));
        assertFalse(TmaGiftStatusViewRules.inViewport(110, 20, 10, 20, 110, 192));
        assertFalse(TmaGiftStatusViewRules.inViewport(10, 192, 10, 20, 110, 192));
        assertFalse(TmaGiftStatusViewRules.inViewport(10, 19.9, 10, 20, 110, 192));
    }
}
