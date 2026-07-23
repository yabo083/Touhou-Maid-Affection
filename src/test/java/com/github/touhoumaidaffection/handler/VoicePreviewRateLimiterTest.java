package com.github.touhoumaidaffection.handler;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VoicePreviewRateLimiterTest {
    @Test
    void permitsOnePreviewPerCooldownWindowForEachPlayer() {
        VoicePreviewRateLimiter limiter = new VoicePreviewRateLimiter(10);
        UUID player = UUID.randomUUID();

        assertTrue(limiter.tryAcquire(player, 100));
        assertFalse(limiter.tryAcquire(player, 109));
        assertTrue(limiter.tryAcquire(player, 110));
        assertTrue(limiter.tryAcquire(UUID.randomUUID(), 110));
    }
}
