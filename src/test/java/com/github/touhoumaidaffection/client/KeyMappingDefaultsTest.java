package com.github.touhoumaidaffection.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class KeyMappingDefaultsTest {
    @Test
    void targetedKissKeepsRequestedDefaultKey() {
        assertEquals(86, TmaKeyDefaults.KISS_TARGETED_MAID);
    }

    @Test
    void lapPillowAngleLockStartsUnboundSoItDoesNotConsumeTheKissKey() {
        assertEquals(-1, TmaKeyDefaults.LAP_PILLOW_ANGLE_LOCK);
        assertNotEquals(TmaKeyDefaults.KISS_TARGETED_MAID, TmaKeyDefaults.LAP_PILLOW_ANGLE_LOCK);
    }
}
