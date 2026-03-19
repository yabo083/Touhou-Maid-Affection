package com.github.touhoumaidaffection.ysm;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;

public final class YSMAnimationHelper {
    private YSMAnimationHelper() {
    }

    public static void triggerAnimation(EntityMaid maid, YSMMaidAnimation animation) {
        if (!YSMCompatibility.isYSMLoaded()) {
            return;
        }
        // Placeholder: resolve concrete YSM API in Phase 6.
    }
}