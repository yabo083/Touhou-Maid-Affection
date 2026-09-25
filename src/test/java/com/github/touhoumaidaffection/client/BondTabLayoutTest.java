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

    @Test
    void firstFreeSlotSkipsAConcurrentAddonTabSharingTheSlot() {
        // Built-in tabs 94/119/144 plus another mod's tab at 169 (the slot the bond tab also picks
        // when it runs before that mod). Re-seating must land on the first genuinely free slot.
        assertEquals(194, BondTabLayout.firstFreeTopTabX(94, new int[]{94, 119, 144, 169}));
    }

    @Test
    void firstFreeSlotFillsTheEarliestGap() {
        assertEquals(119, BondTabLayout.firstFreeTopTabX(94, new int[]{94, 144, 169}));
    }

    @Test
    void firstFreeSlotReturnsBaseWhenRowIsEmpty() {
        assertEquals(94, BondTabLayout.firstFreeTopTabX(94, new int[]{}));
    }
}
