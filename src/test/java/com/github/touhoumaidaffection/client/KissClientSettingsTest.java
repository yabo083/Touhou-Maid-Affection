package com.github.touhoumaidaffection.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KissClientSettingsTest {
    @Test
    void followsTheServerSynchronizedRightClickSetting() {
        KissClientSettings.updateRightClickEnabled(false);
        assertFalse(KissClientSettings.isRightClickEnabled());

        KissClientSettings.updateRightClickEnabled(true);
        assertTrue(KissClientSettings.isRightClickEnabled());
    }
}
