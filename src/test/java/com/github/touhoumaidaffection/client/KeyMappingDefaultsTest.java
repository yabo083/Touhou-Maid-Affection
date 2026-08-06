package com.github.touhoumaidaffection.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class KeyMappingDefaultsTest {
    @Test
    void kissDefaultsToKKey() {
        assertEquals(75, TmaKeyDefaults.KISS_MAID);
    }

    @Test
    void lapPillowAngleLockStartsUnboundSoItDoesNotConsumeTheKissKey() {
        assertEquals(-1, TmaKeyDefaults.LAP_PILLOW_ANGLE_LOCK);
        assertNotEquals(TmaKeyDefaults.KISS_MAID, TmaKeyDefaults.LAP_PILLOW_ANGLE_LOCK);
    }

    @Test
    void voicePreviewDefaultsToRightMouseButton() {
        assertEquals(1, TmaKeyDefaults.VOICE_PREVIEW);
    }
}
