package com.github.touhoumaidaffection.util;

public final class SoundVolumeSettings {
    public static final double DEFAULT_VOLUME = 1.0D;
    public static final double MIN_VOLUME = 0.0D;
    public static final double MAX_VOLUME = 4.0D;

    private SoundVolumeSettings() {
    }

    public static float resolveVolume(double configuredVolume) {
        return (float) Math.clamp(configuredVolume, MIN_VOLUME, MAX_VOLUME);
    }
}
