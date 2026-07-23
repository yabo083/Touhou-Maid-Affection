package com.github.touhoumaidaffection.bond.lap;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LapPillowAnglesTest {
    @Test
    void replacesNonFiniteAngleLockYawWithNeutralRotation() {
        assertEquals(0.0F, LapPillowAngles.sanitizeYaw(Float.NaN));
        assertEquals(0.0F, LapPillowAngles.sanitizeYaw(Float.POSITIVE_INFINITY));
        assertEquals(0.0F, LapPillowAngles.sanitizeYaw(Float.NEGATIVE_INFINITY));
        assertEquals(45.0F, LapPillowAngles.sanitizeYaw(45.0F));
    }
}
