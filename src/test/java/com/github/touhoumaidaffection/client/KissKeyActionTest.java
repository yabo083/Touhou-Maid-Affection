package com.github.touhoumaidaffection.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KissKeyActionTest {
    @Test
    void carriedMaidTakesPriorityOverTargetedMaidWhenBothKeysShareDefaultBinding() {
        assertEquals(KissKeyAction.CARRIED_MAID, KissKeyAction.choose(true, true));
    }

    @Test
    void targetedMaidIsUsedWhenPlayerIsNotCarryingAMaid() {
        assertEquals(KissKeyAction.TARGETED_MAID, KissKeyAction.choose(false, true));
    }

    @Test
    void noRequestIsSentWhenNoMaidCanBeKissedByKey() {
        assertEquals(KissKeyAction.NONE, KissKeyAction.choose(false, false));
    }
}
