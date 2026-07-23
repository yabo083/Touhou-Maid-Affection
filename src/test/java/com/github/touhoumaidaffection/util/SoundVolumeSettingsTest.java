package com.github.touhoumaidaffection.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SoundVolumeSettingsTest {
    @Test
    void keepsConfiguredVolumeInsideAllowedRange() {
        assertEquals(0.35F, SoundVolumeSettings.resolveVolume(0.35), 0.0001F);
    }

    @Test
    void clampsMutedAndBoostedVolumesToSafeRange() {
        assertEquals(0.0F, SoundVolumeSettings.resolveVolume(-0.5), 0.0001F);
        assertEquals(4.0F, SoundVolumeSettings.resolveVolume(8.0), 0.0001F);
    }
}
