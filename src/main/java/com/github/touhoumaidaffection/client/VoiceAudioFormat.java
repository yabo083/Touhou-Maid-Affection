package com.github.touhoumaidaffection.client;

enum VoiceAudioFormat {
    MP3,
    OPUS,
    VORBIS,
    UNSUPPORTED;

    static VoiceAudioFormat resolve(boolean mp3, String oggType) {
        if (mp3) {
            return MP3;
        }
        if ("OPUS".equals(oggType)) {
            return OPUS;
        }
        if ("VORBIS".equals(oggType)) {
            return VORBIS;
        }
        return UNSUPPORTED;
    }
}
