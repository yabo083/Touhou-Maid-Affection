package com.github.touhoumaidaffection.client;

public final class KissClientSettings {
    private static volatile boolean rightClickEnabled = true;

    private KissClientSettings() {
    }

    public static boolean isRightClickEnabled() {
        return rightClickEnabled;
    }

    public static void updateRightClickEnabled(boolean enabled) {
        rightClickEnabled = enabled;
    }
}
