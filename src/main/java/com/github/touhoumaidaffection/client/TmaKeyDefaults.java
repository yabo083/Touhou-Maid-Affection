package com.github.touhoumaidaffection.client;

public final class TmaKeyDefaults {
    public static final int KISS_MAID = 75; // GLFW_KEY_K: press once to kiss once, holding does not repeat
    public static final int LAP_PILLOW = 66;
    public static final int LAP_PILLOW_ANGLE_LOCK = -1;
    public static final int VOICE_PREVIEW = 1;

    private TmaKeyDefaults() {
        throw new IllegalStateException("Utility class");
    }
}
