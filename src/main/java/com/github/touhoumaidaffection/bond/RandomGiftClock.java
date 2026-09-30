package com.github.touhoumaidaffection.bond;

/** Pure wall-clock accumulation shared by mutation and read-only status projection. */
public final class RandomGiftClock {
    private RandomGiftClock() { }

    public record State(int queued, long lastWallClockMs, long nextReadyAtMs) { }

    public static State reconcile(int queued, long lastMs, int previousMinutes,
                                  int intervalMinutes, int maxQueued, long nowMs) {
        queued = Math.max(0, Math.min(RandomGiftQueue.MAX_QUEUED, queued));
        int cap = Math.max(1, Math.min(RandomGiftQueue.MAX_QUEUED, maxQueued));
        long intervalMs = Math.max(1, intervalMinutes) * 60_000L;
        if (lastMs <= 0 || (previousMinutes > 0 && previousMinutes != intervalMinutes)) {
            lastMs = nowMs;
        }
        // Lowering the earning cap never deletes already earned or prepared gifts.
        if (queued >= cap) return new State(queued, nowMs, 0L);
        long elapsed = Math.max(0L, nowMs - lastMs);
        long earned = elapsed / intervalMs;
        if (earned >= cap - queued) return new State(cap, nowMs, 0L);
        queued += (int) earned;
        lastMs += earned * intervalMs;
        return new State(queued, lastMs, lastMs + intervalMs);
    }
}
