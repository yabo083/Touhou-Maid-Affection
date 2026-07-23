package com.github.touhoumaidaffection.bond.lap;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LapPillowPoseSnapshotTest {
    @Test
    void replacesNonFiniteOffsetsWithNeutralValues() {
        LapPillowPoseSnapshot pose = new LapPillowPoseSnapshot(
                LapPillowMode.MAID_SIT_PLAYER_LIE,
                Double.NaN,
                Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY,
                Double.NaN,
                Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY,
                "",
                ""
        ).clamp();

        assertEquals(0.0D, pose.maidOffsetX());
        assertEquals(0.0D, pose.maidOffsetY());
        assertEquals(0.0D, pose.maidOffsetZ());
        assertEquals(0.0D, pose.playerOffsetX());
        assertEquals(0.0D, pose.playerOffsetY());
        assertEquals(0.0D, pose.playerOffsetZ());
    }

    @Test
    void boundsClientControlledActionIdsForNbtPersistence() {
        LapPillowPoseSnapshot pose = new LapPillowPoseSnapshot(
                LapPillowMode.MAID_SIT_PLAYER_LIE,
                0.0D,
                0.0D,
                0.0D,
                0.0D,
                0.0D,
                0.0D,
                "a".repeat(1_000),
                "b".repeat(1_000)
        ).clamp();

        assertEquals("", pose.maidActionId());
        assertEquals("", pose.playerActionId());
    }
}
