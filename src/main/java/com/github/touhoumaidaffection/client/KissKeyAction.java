package com.github.touhoumaidaffection.client;

public enum KissKeyAction {
    NONE,
    CARRIED_MAID,
    TARGETED_MAID;

    public static KissKeyAction choose(boolean carryingMaid, boolean targetingMaid) {
        if (carryingMaid) {
            return CARRIED_MAID;
        }
        return targetingMaid ? TARGETED_MAID : NONE;
    }
}
