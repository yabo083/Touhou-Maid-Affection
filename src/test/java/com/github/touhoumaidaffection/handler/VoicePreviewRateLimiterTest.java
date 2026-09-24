package com.github.touhoumaidaffection.handler;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VoicePreviewRateLimiterTest {
    @Test
    void usesHalfSecondCooldownForDataPackPreviews() {
        assertEquals(10L, VoicePreviewRequestHandler.COOLDOWN_TICKS);
    }

    @Test
    void permitsOnePreviewPerCooldownWindowForEachPlayer() {
        VoicePreviewRateLimiter limiter = new VoicePreviewRateLimiter(10);
        UUID player = UUID.randomUUID();

        assertTrue(limiter.tryAcquire(player, 100));
        assertFalse(limiter.tryAcquire(player, 109));
        assertTrue(limiter.tryAcquire(player, 110));
        assertTrue(limiter.tryAcquire(UUID.randomUUID(), 110));
    }

    @Test
    void treatsCooldownBoundaryAsElapsed() {
        VoicePreviewRateLimiter limiter = new VoicePreviewRateLimiter(10);
        UUID player = UUID.randomUUID();

        assertTrue(limiter.tryAcquire(player, 0));
        assertFalse(limiter.tryAcquire(player, 9));
        assertTrue(limiter.tryAcquire(player, 10));
        assertFalse(limiter.tryAcquire(player, 19));
        assertTrue(limiter.tryAcquire(player, 20));
    }

    @Test
    void forgettingAPlayerClearsItsCooldown() {
        VoicePreviewRateLimiter limiter = new VoicePreviewRateLimiter(10);
        UUID player = UUID.randomUUID();

        assertTrue(limiter.tryAcquire(player, 50));
        assertFalse(limiter.tryAcquire(player, 55));
        limiter.remove(player);
        assertTrue(limiter.tryAcquire(player, 55));
    }
}