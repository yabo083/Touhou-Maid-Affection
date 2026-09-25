package com.github.touhoumaidaffection.client;

final class BondTabLayout {
    static final int TOP_TAB_Y_OFFSET = 5;

    private static final int FIRST_TOP_TAB_X_OFFSET = 94;
    private static final int FIRST_EXTERNAL_TAB_X_OFFSET = 194;
    private static final int TOP_TAB_SPACING = 25;

    private BondTabLayout() {
    }

    static int nextTopTabX(int leftPos, int[] existingTopTabXs) {
        if (existingTopTabXs.length == 0) {
            return leftPos + FIRST_EXTERNAL_TAB_X_OFFSET;
        }

        int nextX = leftPos + FIRST_TOP_TAB_X_OFFSET;
        for (int x : existingTopTabXs) {
            if (x >= leftPos + FIRST_TOP_TAB_X_OFFSET) {
                nextX = Math.max(nextX, x + TOP_TAB_SPACING);
            }
        }
        return nextX == leftPos + FIRST_TOP_TAB_X_OFFSET ? leftPos + FIRST_EXTERNAL_TAB_X_OFFSET : nextX;
    }

    /**
     * Returns the first top-tab slot on the 25px grid starting at {@code baseX} that no other tab occupies.
     * Used after full screen init to move the bond tab off a slot another mod's tab claimed concurrently
     * (addon tab listeners run in a non-deterministic order, so two addons can pick the same slot).
     */
    static int firstFreeTopTabX(int baseX, int[] occupiedXs) {
        java.util.Set<Integer> occupied = new java.util.HashSet<>();
        for (int x : occupiedXs) {
            occupied.add(x);
        }
        int x = baseX;
        while (occupied.contains(x)) {
            x += TOP_TAB_SPACING;
        }
        return x;
    }
}
