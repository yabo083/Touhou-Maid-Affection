package com.github.touhoumaidaffection.bond;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VoicePoolSelectionTest {
    @Test
    void replaceModeLimitsAvailablePoolToDataPackVoicesWhenPresent() {
        assertFalse(VoicePoolSelection.shouldIncludeBasePool("replace", List.of("pack.ogg")));
        assertTrue(VoicePoolSelection.shouldIncludeBasePool("replace", List.of()));
        assertTrue(VoicePoolSelection.shouldIncludeBasePool("append", List.of("pack.ogg")));
    }

    @Test
    void savedSelectionIsFilteredAgainstCurrentAvailablePoolAndFallsBackWhenEmpty() {
        List<String> available = List.of(VoicePoolIds.dataPack("pack.ogg"));
        List<String> defaults = List.of(VoicePoolIds.dataPack("pack.ogg"));
        List<String> saved = List.of(VoicePoolIds.tlm("old"), VoicePoolIds.BUILTIN_MORNING_KISS);

        assertEquals(defaults, VoicePoolSelection.initialSelection(saved, defaults, available));
    }

    @Test
    void savedSelectionKeepsStillAvailableItemsInOriginalOrder() {
        List<String> available = List.of(VoicePoolIds.dataPack("pack.ogg"), VoicePoolIds.tlm("a"));
        List<String> defaults = List.of(VoicePoolIds.dataPack("pack.ogg"), VoicePoolIds.tlm("a"));
        List<String> saved = List.of(VoicePoolIds.tlm("a"), VoicePoolIds.tlm("missing"), VoicePoolIds.dataPack("pack.ogg"));

        assertEquals(List.of(VoicePoolIds.tlm("a"), VoicePoolIds.dataPack("pack.ogg")),
                VoicePoolSelection.initialSelection(saved, defaults, available));
    }
}
