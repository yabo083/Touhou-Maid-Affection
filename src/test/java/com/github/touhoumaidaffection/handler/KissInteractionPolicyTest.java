package com.github.touhoumaidaffection.handler;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KissInteractionPolicyTest {
    @Test
    void rightClickKissingCanBeDisabledToPreserveMaidInteractions() {
        assertFalse(KissInteractionPolicy.shouldHandle(false, true, true, false, false));
    }

    @Test
    void preservesExistingSneakEmptyHandAndCarryOnRulesWhenEnabled() {
        assertTrue(KissInteractionPolicy.shouldHandle(true, true, true, false, false));
        assertFalse(KissInteractionPolicy.shouldHandle(true, false, true, false, false));
        assertFalse(KissInteractionPolicy.shouldHandle(true, true, false, false, false));
        assertFalse(KissInteractionPolicy.shouldHandle(true, true, true, true, true));
        assertTrue(KissInteractionPolicy.shouldHandle(true, true, true, true, false));
    }
}
