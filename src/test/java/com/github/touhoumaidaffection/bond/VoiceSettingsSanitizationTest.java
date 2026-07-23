package com.github.touhoumaidaffection.bond;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VoiceSettingsSanitizationTest {
    @Test
    void boundsClientControlledVoiceSettingsForNbtPersistence() {
        String oversized = "声".repeat(1_000);
        List<String> manyIds = IntStream.range(0, 100)
                .mapToObj(index -> "datapack:" + index + oversized)
                .toList();

        MorningKissVoiceSettings settings = MorningKissVoiceSettings.of(
                "random_all", oversized, oversized, oversized, manyIds
        );

        assertEquals("", settings.selectedGroup());
        assertEquals("", settings.selectedClip());
        assertEquals("", settings.soundPackId());
        assertEquals(100, settings.selectedVoiceIds().size());
        assertTrue(VoicePoolIds.encode(settings.selectedVoiceIds()).getBytes(StandardCharsets.UTF_8).length > 65_535);
        assertFalse(VoicePoolIds.isPersistableSelection(settings.selectedVoiceIds()));
    }
}
