package com.github.touhoumaidaffection.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VoiceAudioFormatTest {
    @Test
    void resolvesKnownVoiceFormats() {
        assertEquals(VoiceAudioFormat.MP3, VoiceAudioFormat.resolve(true, ""));
        assertEquals(VoiceAudioFormat.OPUS, VoiceAudioFormat.resolve(false, "OPUS"));
        assertEquals(VoiceAudioFormat.VORBIS, VoiceAudioFormat.resolve(false, "VORBIS"));
        assertEquals(VoiceAudioFormat.UNSUPPORTED, VoiceAudioFormat.resolve(false, ""));
    }
}
