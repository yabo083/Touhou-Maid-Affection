package com.github.touhoumaidaffection.client;

public final class LapPillowAngleLockPrompt {
    private LapPillowAngleLockPrompt() {
        throw new IllegalStateException("Utility class");
    }

    public static boolean shouldShowUnavailable(boolean angleLockAvailable, boolean kissContextCanHandlePress) {
        return !angleLockAvailable && !kissContextCanHandlePress;
    }
}
