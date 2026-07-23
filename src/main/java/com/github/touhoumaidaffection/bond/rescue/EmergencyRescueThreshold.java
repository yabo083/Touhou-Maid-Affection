package com.github.touhoumaidaffection.bond.rescue;

final class EmergencyRescueThreshold {
    private EmergencyRescueThreshold() {
    }

    static float resolve(float maxHealth, float absoluteThreshold, boolean usePercentage, double percentage) {
        float safeAbsolute = Float.isFinite(absoluteThreshold) ? Math.max(0.0F, absoluteThreshold) : 0.0F;
        if (!usePercentage || !Float.isFinite(maxHealth) || maxHealth <= 0.0F || !Double.isFinite(percentage)) {
            return safeAbsolute;
        }
        double safePercentage = Math.max(0.0D, Math.min(1.0D, percentage));
        return (float) (maxHealth * safePercentage);
    }
}
