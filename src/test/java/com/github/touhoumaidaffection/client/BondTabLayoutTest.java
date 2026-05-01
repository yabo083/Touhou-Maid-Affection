package com.github.touhoumaidaffection.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BondTabLayoutTest {
    @Test
    void placesBondTabAfterRightmostExistingTopTab() {
        assertEquals(219, BondTabLayout.nextTopTabX(0, new int[]{194}));
    }

    @Test
    void fillsEarlierSlotWhenTlmRemovesATab() {
        assertEquals(169, BondTabLayout.nextTopTabX(0, new int[]{144}));
    }

    @Test
    void fallsBackToFirstExternalTabSlotWhenNoTopTabsAreObserved() {
        assertEquals(194, BondTabLayout.nextTopTabX(0, new int[]{}));
    }
}
