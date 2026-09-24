package com.github.touhoumaidaffection.bond;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BondRetentionTest {

    private static final long NOW = 1_700_000_000_000L;
    private static final long DAY = BondRetention.MILLIS_PER_DAY;

    @Test
    void entryOlderThanRetentionWindowIsStale() {
        assertTrue(BondRetention.isStale(NOW - 91 * DAY, NOW, 90));
    }

    @Test
    void entryInsideRetentionWindowIsKept() {
        assertFalse(BondRetention.isStale(NOW - 89 * DAY, NOW, 90));
    }

    @Test
    void exactlyAtThresholdIsKept() {
        assertFalse(BondRetention.isStale(NOW - 90 * DAY, NOW, 90));
        assertTrue(BondRetention.isStale(NOW - 90 * DAY - 1, NOW, 90));
    }

    @Test
    void missingLastSeenIsStaleWhenRetentionEnabled() {
        assertTrue(BondRetention.isStale(0L, NOW, 90));
        assertTrue(BondRetention.isStale(-1L, NOW, 90));
        assertTrue(BondRetention.isStale(0L, NOW, 1));
    }

    @Test
    void disabledRetentionNeverDeletes() {
        assertFalse(BondRetention.isStale(0L, NOW, 0));
        assertFalse(BondRetention.isStale(0L, NOW, -5));
        assertFalse(BondRetention.isStale(NOW - 10_000 * DAY, NOW, 0));
    }

    @Test
    void oneDayRetentionKeepsRecentEntries() {
        assertFalse(BondRetention.isStale(NOW - DAY + 1, NOW, 1));
        assertTrue(BondRetention.isStale(NOW - DAY - 1, NOW, 1));
    }
}