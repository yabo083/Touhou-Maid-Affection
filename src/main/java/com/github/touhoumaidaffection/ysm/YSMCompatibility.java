package com.github.touhoumaidaffection.ysm;

import net.neoforged.fml.ModList;

public final class YSMCompatibility {
    private static final String YSM_MOD_ID = "yesstevemodel";

    private YSMCompatibility() {
    }

    public static boolean isYSMLoaded() {
        return ModList.get().isLoaded(YSM_MOD_ID);
    }

    public static String getFallbackHint() {
        return "YSM unavailable: fallback to particles + sound.";
    }
}
