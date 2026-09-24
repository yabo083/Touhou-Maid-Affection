package com.github.touhoumaidaffection.bond.settings;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TmaMaidLabelsTest {
    @Test
    void uniqueNamesAreReturnedUnchanged() {
        List<String> labels = TmaMaidLabels.displayLabels(List.of(
                maid("a1b2c3d4-0000-0000-0000-000000000001", "芙兰朵露"),
                maid("a1b2c3d4-0000-0000-0000-000000000002", "十六夜咲夜")
        ));

        assertEquals(List.of("芙兰朵露", "十六夜咲夜"), labels);
    }

    @Test
    void duplicateNamesAllGetTheirShortUuidSuffix() {
        List<String> labels = TmaMaidLabels.displayLabels(List.of(
                maid("a1b2c3d4-0000-0000-0000-000000000001", "精灵酒狐"),
                maid("9f8e7d6c-0000-0000-0000-000000000002", "精灵酒狐")
        ));

        assertEquals(List.of("精灵酒狐 #a1b2", "精灵酒狐 #9f8e"), labels);
    }

    @Test
    void onlyTheDuplicatedNameGetsASuffix() {
        List<String> labels = TmaMaidLabels.displayLabels(List.of(
                maid("a1b2c3d4-0000-0000-0000-000000000001", "精灵酒狐"),
                maid("a1b2c3d4-0000-0000-0000-000000000002", "芙兰朵露"),
                maid("9f8e7d6c-0000-0000-0000-000000000003", "精灵酒狐")
        ));

        assertEquals(List.of("精灵酒狐 #a1b2", "芙兰朵露", "精灵酒狐 #9f8e"), labels);
    }

    @Test
    void blankNamesFallBackToUnknownWording() {
        List<String> labels = TmaMaidLabels.displayLabels(List.of(
                maid("a1b2c3d4-0000-0000-0000-000000000001", ""),
                maid("9f8e7d6c-0000-0000-0000-000000000002", "   ")
        ));

        assertEquals(List.of("未知女仆 #a1b2", "未知女仆 #9f8e"), labels);
        assertEquals("未知女仆", TmaMaidLabels.UNKNOWN_NAME);
    }

    @Test
    void namesAreComparedAfterTrimmingButRenderedVerbatim() {
        List<String> labels = TmaMaidLabels.displayLabels(List.of(
                maid("a1b2c3d4-0000-0000-0000-000000000001", "芙兰"),
                maid("9f8e7d6c-0000-0000-0000-000000000002", "芙兰 ")
        ));

        assertEquals(List.of("芙兰 #a1b2", "芙兰  #9f8e"), labels);
    }

    @Test
    void missingInputsAreTolerated() {
        assertTrue(TmaMaidLabels.displayLabels(null).isEmpty());
        assertTrue(TmaMaidLabels.displayLabels(List.of()).isEmpty());

        List<String> labels = TmaMaidLabels.displayLabels(Arrays.asList(
                maid("a1b2c3d4-0000-0000-0000-000000000001", "芙兰朵露"),
                null
        ));
        assertEquals(List.of("芙兰朵露", "未知女仆"), labels);
    }

    private static TmaAiStatusWire.MaidStatus maid(String uuid, String name) {
        return new TmaAiStatusWire.MaidStatus(uuid, name, 0, 0, List.of());
    }
}