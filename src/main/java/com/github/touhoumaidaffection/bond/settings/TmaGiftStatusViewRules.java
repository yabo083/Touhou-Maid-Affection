package com.github.touhoumaidaffection.bond.settings;

/** Time and viewport math shared by the gift screen and headless checks. */
public final class TmaGiftStatusViewRules {
    private TmaGiftStatusViewRules() { }

    public static long elapsedMillis(long snapshotNanos, long nowNanos) {
        return Math.max(0L, (nowNanos - snapshotNanos) / 1_000_000L);
    }

    public static long serverNow(long serverTimeMs, long elapsedMillis) {
        return serverTimeMs + Math.max(0L, elapsedMillis);
    }

    public static long remainingSeconds(long deadlineMs, long serverNowMs) {
        if (deadlineMs <= serverNowMs) return 0L;
        long remaining = deadlineMs - serverNowMs;
        return remaining / 1000L + (remaining % 1000L == 0L ? 0L : 1L);
    }

    public static int clampScroll(int offset, int contentHeight, int viewportHeight) {
        return Math.max(0, Math.min(offset, Math.max(0, contentHeight - viewportHeight)));
    }

    public static boolean inViewport(double x, double y, int left, int top, int right, int bottom) {
        return x >= left && x < right && y >= top && y < bottom;
    }
}
