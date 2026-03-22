package com.github.touhoumaidaffection.ysm;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;

public final class YSMAnimationHelper {
    private YSMAnimationHelper() {
    }

    public static void triggerAnimation(EntityMaid maid, YSMMaidAnimation animation) {
        if (animation == null) {
            return;
        }
        triggerAnimation(maid, animation.animationName());
    }

    public static void triggerAnimation(EntityMaid maid, String animationId) {
        if (!YSMCompatibility.isYSMLoaded()) {
            return;
        }
        if (maid == null || animationId == null || animationId.isBlank()) {
            return;
        }
        try {
            maid.playRouletteAnim(animationId.trim());
        } catch (Exception ignored) {
            // Best-effort bridge. If a model does not support this roulette animation,
            // the caller should gracefully fall back to non-YSM visuals.
        }
    }
}
