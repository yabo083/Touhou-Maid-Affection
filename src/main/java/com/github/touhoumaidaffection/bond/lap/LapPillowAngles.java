package com.github.touhoumaidaffection.bond.lap;

final class LapPillowAngles {
    private LapPillowAngles() {
    }

    static float sanitizeYaw(float yaw) {
        return Float.isFinite(yaw) ? yaw : 0.0F;
    }
}
