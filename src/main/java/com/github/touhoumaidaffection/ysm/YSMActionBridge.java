package com.github.touhoumaidaffection.ysm;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;

public final class YSMActionBridge {
    private YSMActionBridge() {
    }

    public static void playIfAvailable(EntityMaid maid, YSMMaidAnimation animation) {
        if (YSMCompatibility.isYSMLoaded()) {
            YSMAnimationHelper.triggerAnimation(maid, animation);
        }
    }
}
