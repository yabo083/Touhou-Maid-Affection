package com.github.touhoumaidaffection.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VoicePreviewTlmSoundInstanceTest {
    @Test
    void tlmPreviewUsesStablePlayerCentricSoundRouting() {
        assertEquals("minecraft:music.menu", VoicePreviewSoundRouting.ANCHOR_SOUND_EVENT_ID);
        assertEquals("players", VoicePreviewSoundRouting.SOUND_SOURCE);
        assertTrue(VoicePreviewSoundRouting.PLAYER_CENTRIC);
    }
}
