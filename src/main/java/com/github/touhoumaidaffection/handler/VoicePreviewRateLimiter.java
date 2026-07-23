package com.github.touhoumaidaffection.handler;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

final class VoicePreviewRateLimiter {
    private final long cooldownTicks;
    private final Map<UUID, Long> lastAcceptedTicks = new HashMap<>();

    VoicePreviewRateLimiter(long cooldownTicks) {
        this.cooldownTicks = Math.max(1L, cooldownTicks);
    }

    boolean tryAcquire(UUID playerUuid, long currentTick) {
        if (playerUuid == null) {
            return false;
        }
        Long previousTick = lastAcceptedTicks.get(playerUuid);
        if (previousTick != null && currentTick >= previousTick && currentTick - previousTick < cooldownTicks) {
            return false;
        }
        lastAcceptedTicks.put(playerUuid, currentTick);
        return true;
    }

    void remove(UUID playerUuid) {
        if (playerUuid != null) {
            lastAcceptedTicks.remove(playerUuid);
        }
    }
}
