package com.github.touhoumaidaffection.bond.lap;

public record LapPillowPoseSnapshot(
        LapPillowMode mode,
        double maidOffsetX,
        double maidOffsetY,
        double maidOffsetZ,
        double playerOffsetX,
        double playerOffsetY,
        double playerOffsetZ,
        String maidActionId,
        String playerActionId
) {
    public static final double MIN_OFFSET = -2.5D;
    public static final double MAX_OFFSET = 2.5D;

    public static LapPillowPoseSnapshot maidSitPlayerLieDefault() {
        return new LapPillowPoseSnapshot(
                LapPillowMode.MAID_SIT_PLAYER_LIE,
                0.0D,
                0.0D,
                0.0D,
                0.85D,
                0.35D,
                0.0D,
                "",
                ""
        );
    }

    public boolean playerLying() {
        return mode().playerLying();
    }

    public boolean maidLying() {
        return mode().maidLying();
    }

    public LapPillowPoseSnapshot clamp() {
        LapPillowMode safeMode = mode == null ? LapPillowMode.MAID_SIT_PLAYER_LIE : mode;
        return new LapPillowPoseSnapshot(
                safeMode,
                clampOffset(maidOffsetX),
                clampOffset(maidOffsetY),
                clampOffset(maidOffsetZ),
                clampOffset(playerOffsetX),
                clampOffset(playerOffsetY),
                clampOffset(playerOffsetZ),
                normalizeAction(maidActionId),
                normalizeAction(playerActionId)
        );
    }

    public LapPillowPoseSnapshot withMode(LapPillowMode nextMode) {
        return new LapPillowPoseSnapshot(
                nextMode,
                maidOffsetX,
                maidOffsetY,
                maidOffsetZ,
                playerOffsetX,
                playerOffsetY,
                playerOffsetZ,
                maidActionId,
                playerActionId
        ).clamp();
    }

    public LapPillowPoseSnapshot withMaidOffset(double x, double y, double z) {
        return new LapPillowPoseSnapshot(
                mode,
                x,
                y,
                z,
                playerOffsetX,
                playerOffsetY,
                playerOffsetZ,
                maidActionId,
                playerActionId
        ).clamp();
    }

    public LapPillowPoseSnapshot withPlayerOffset(double x, double y, double z) {
        return new LapPillowPoseSnapshot(
                mode,
                maidOffsetX,
                maidOffsetY,
                maidOffsetZ,
                x,
                y,
                z,
                maidActionId,
                playerActionId
        ).clamp();
    }

    private static double clampOffset(double value) {
        return Math.max(MIN_OFFSET, Math.min(MAX_OFFSET, value));
    }

    private static String normalizeAction(String actionId) {
        return actionId == null ? "" : actionId.trim();
    }
}
