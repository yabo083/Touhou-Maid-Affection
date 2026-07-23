package com.github.touhoumaidaffection.bond.rescue;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EmergencyRescueThresholdTest {
    @Test
    void percentageModeMatchesLegacyThresholdAtVanillaHealth() {
        assertEquals(4.0F, EmergencyRescueThreshold.resolve(20.0F, 4.0F, true, 0.2D));
    }

    @Test
    void percentageModeScalesWithModdedMaximumHealth() {
        assertEquals(20.0F, EmergencyRescueThreshold.resolve(100.0F, 4.0F, true, 0.2D));
        assertEquals(4.0F, EmergencyRescueThreshold.resolve(100.0F, 4.0F, false, 0.2D));
    }
}
