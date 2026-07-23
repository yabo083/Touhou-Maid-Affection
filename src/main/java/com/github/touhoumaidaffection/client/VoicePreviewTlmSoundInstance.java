package com.github.touhoumaidaffection.client;

import com.github.touhoumaidaffection.TouhouMaidAffection;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

import java.util.concurrent.CompletableFuture;

public final class VoicePreviewTlmSoundInstance extends AbstractSoundInstance {
    private static final SoundEvent STREAM_ANCHOR_SOUND_EVENT =
            SoundEvent.createVariableRangeEvent(ResourceLocation.parse(VoicePreviewSoundRouting.ANCHOR_SOUND_EVENT_ID));

    private final byte[] data;
    private final String fileName;

    public VoicePreviewTlmSoundInstance(byte[] data, String fileName, float volume, float pitch) {
        super(STREAM_ANCHOR_SOUND_EVENT, SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
        this.data = data;
        this.fileName = fileName;
        this.volume = volume;
        this.pitch = pitch;
        this.relative = VoicePreviewSoundRouting.PLAYER_CENTRIC;
        this.attenuation = Attenuation.NONE;
        this.looping = false;
        this.delay = 0;
    }

    @Override
    public CompletableFuture<AudioStream> getStream(SoundBufferLibrary library, Sound sound, boolean looping) {
        TouhouMaidAffection.LOGGER.info(
                "TLM voice preview getStream invoked: file='{}', bytes={}, looping={}",
                fileName,
                data == null ? -1 : data.length,
                looping
        );
        return InMemoryVoiceStream.openOgg(data, "TLM voice preview " + fileName);
    }
}
