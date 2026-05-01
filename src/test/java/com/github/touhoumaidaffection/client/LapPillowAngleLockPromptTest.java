package com.github.touhoumaidaffection.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LapPillowAngleLockPromptTest {
    @Test
    void suppressesUnavailablePromptWhenKissKeyContextCanHandleThePress() {
        assertFalse(LapPillowAngleLockPrompt.shouldShowUnavailable(false, true));
    }

    @Test
    void keepsUnavailablePromptForDedicatedAngleLockPressesOutsideLapPillow() {
        assertTrue(LapPillowAngleLockPrompt.shouldShowUnavailable(false, false));
    }

    @Test
    void neverShowsUnavailablePromptWhenAngleLockIsAvailable() {
        assertFalse(LapPillowAngleLockPrompt.shouldShowUnavailable(true, false));
    }
}
