package com.github.touhoumaidaffection.bond.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MorningKissScheduleRulesTest {
    @Test
    void shouldParseClockRangeAndFormatForDisplay() {
        MorningKissScheduleRules.TimeRange range = MorningKissScheduleRules.TimeRange.parse("morning@06:00-08:00");
        assertNotNull(range);
        assertEquals(MorningKissScheduleRules.DialoguePool.MORNING, range.dialoguePool());
        assertEquals("06:00-08:00", range.toDisplayString());
        assertTrue(range.contains(0));
    }

    @Test
    void shouldSupportCrossMidnightRange() {
        MorningKissScheduleRules.TimeRange range = MorningKissScheduleRules.TimeRange.parse("evening@22000-2000");
        assertNotNull(range);
        assertTrue(range.crossesMidnight());
        assertTrue(range.contains(23000));
        assertTrue(range.contains(1000));
    }

    @Test
    void shouldFallbackToDefaultRangesWhenInputInvalid() {
        List<MorningKissScheduleRules.TimeRange> ranges = MorningKissScheduleRules.resolveAllowedTimeRanges(List.of("invalid", "25:61-26:00"));
        assertEquals(2, ranges.size());
        assertEquals("06:00-08:00", ranges.get(0).toDisplayString());
        assertEquals("18:00-20:00", ranges.get(1).toDisplayString());
    }

    @Test
    void shouldClampKissCountBounds() {
        assertEquals(1, MorningKissScheduleRules.safeMinKissCount(0, -1));
        assertEquals(3, MorningKissScheduleRules.safeMinKissCount(5, 3));
        assertEquals(5, MorningKissScheduleRules.safeMaxKissCount(5, 3));
        assertEquals(1, MorningKissScheduleRules.safeMaxKissCount(0, -3));
    }
}
