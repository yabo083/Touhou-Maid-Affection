package com.github.touhoumaidaffection.util;

/**
 * Clamps the configured sound volume into the range supported by the mod.
 *
 * <p>Volume is attenuation only: {@link #MIN_VOLUME} mutes, {@link #DEFAULT_VOLUME} keeps the
 * original loudness, and {@link #MAX_VOLUME} is the loudest the mod will play. Anything louder
 * has to come from Minecraft or the system volume.
 */
public final class SoundVolumeSettings {
    public static final double DEFAULT_VOLUME = 1.0D;
    public static final double MIN_VOLUME = 0.0D;
    public static final double MAX_VOLUME = 1.0D;

    private SoundVolumeSettings() {
    }

    public static float resolveVolume(double configuredVolume) {
        return (float) Math.max(MIN_VOLUME, Math.min(MAX_VOLUME, configuredVolume));
    }
}
