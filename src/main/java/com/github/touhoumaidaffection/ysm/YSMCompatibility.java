package com.github.touhoumaidaffection.ysm;

import net.neoforged.fml.ModList;

public final class YSMCompatibility {
    private static final String YSM_MOD_ID = "yes_steve_model";
    private static final String LEGACY_YSM_MOD_ID = "yesstevemodel";

    private YSMCompatibility() {
    }

    public static boolean isYSMLoaded() {
        ModList modList = ModList.get();
        return modList.isLoaded(YSM_MOD_ID) || modList.isLoaded(LEGACY_YSM_MOD_ID);
    }

    public static String getFallbackHint() {
        return "YSM unavailable: fallback to particles + sound.";
    }
}
