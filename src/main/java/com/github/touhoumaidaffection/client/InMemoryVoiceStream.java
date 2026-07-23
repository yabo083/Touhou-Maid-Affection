package com.github.touhoumaidaffection.client;

import com.github.tartaricacid.touhoulittlemaid.client.sound.OggReader;
import com.github.tartaricacid.touhoulittlemaid.client.sound.data.Mp3AudioStream;
import com.github.tartaricacid.touhoulittlemaid.client.sound.data.OpusAudioStream;
import com.github.touhoumaidaffection.TouhouMaidAffection;
import net.minecraft.Util;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.JOrbisAudioStream;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.util.concurrent.CompletableFuture;

final class InMemoryVoiceStream {
    private InMemoryVoiceStream() {
    }

    static CompletableFuture<AudioStream> openOgg(byte[] data, String label) {
        return open(data, null, false, label);
    }

    static CompletableFuture<AudioStream> open(
            byte[] data,
            @Nullable OggReader.Type knownOggType,
            boolean mp3,
            String label
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                OggReader.Type oggType = mp3 || knownOggType != null
                        ? knownOggType
                        : OggReader.getOggType(data);
                return switch (VoiceAudioFormat.resolve(mp3, oggType == null ? "" : oggType.name())) {
                    case MP3 -> new Mp3AudioStream(data);
                    case OPUS -> new OpusAudioStream(data);
                    case VORBIS -> new JOrbisAudioStream(new ByteArrayInputStream(data));
                    case UNSUPPORTED -> {
                        TouhouMaidAffection.LOGGER.warn("Voice audio '{}' has an unsupported format.", label);
                        yield null;
                    }
                };
            } catch (Exception ex) {
                TouhouMaidAffection.LOGGER.warn("Failed to decode voice audio '{}'.", label, ex);
                return null;
            }
        }, Util.backgroundExecutor());
    }

}
