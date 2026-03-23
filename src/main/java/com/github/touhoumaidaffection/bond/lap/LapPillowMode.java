package com.github.touhoumaidaffection.bond.lap;

import java.util.Locale;

public enum LapPillowMode {
    MAID_SIT_PLAYER_SIT(false, false),
    MAID_SIT_PLAYER_LIE(false, true),
    MAID_LIE_PLAYER_LIE(true, true),
    MAID_LIE_PLAYER_SIT(true, false);

    private final boolean maidLying;
    private final boolean playerLying;

    LapPillowMode(boolean maidLying, boolean playerLying) {
        this.maidLying = maidLying;
        this.playerLying = playerLying;
    }

    public boolean maidLying() {
        return maidLying;
    }

    public boolean playerLying() {
        return playerLying;
    }

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static LapPillowMode fromName(String raw) {
        if (raw == null || raw.isBlank()) {
            return MAID_SIT_PLAYER_LIE;
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        for (LapPillowMode mode : values()) {
            if (mode.name().equals(normalized) || mode.serializedName().equals(raw.trim().toLowerCase(Locale.ROOT))) {
                return mode;
            }
        }
        return MAID_SIT_PLAYER_LIE;
    }
}
