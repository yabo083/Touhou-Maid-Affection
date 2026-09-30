package com.github.touhoumaidaffection.client.screen.component;

public final class BondTextFit {
    private BondTextFit() {
    }

    /** Shrink uniformly to both bounds; short labels keep their natural size. */
    public static float scale(int textWidth, int textHeight, int availableWidth, int availableHeight) {
        if (textWidth <= 0 || textHeight <= 0 || availableWidth <= 0 || availableHeight <= 0) {
            return 0.0F;
        }
        return Math.min(1.0F, Math.min((float) availableWidth / textWidth, (float) availableHeight / textHeight));
    }
}
