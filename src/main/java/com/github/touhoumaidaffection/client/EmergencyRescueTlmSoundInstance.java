package com.github.touhoumaidaffection.client;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;

public final class EmergencyRescueTlmSoundInstance extends AbstractTickableSoundInstance {
    private final byte[] data;
    private final String fileName;

    public EmergencyRescueTlmSoundInstance(
            SoundEvent soundEvent,
            byte[] data,
            String fileName,
            double x,
            double y,
            double z,
            float volume,
            float pitch
    ) {
        super(soundEvent, SoundSource.NEUTRAL, SoundInstance.createUnseededRandom());
        this.data = data;
        this.fileName = fileName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.volume = volume;
        this.pitch = pitch;
    }

    @Override
    public boolean canPlaySound() {
        return true;
    }

    @Override
    public void tick() {
        // Static sound at trigger position.
    }

    @Nullable
    @Override
    public CompletableFuture<AudioStream> getStream(SoundBufferLibrary library, Sound sound, boolean looping) {
        return InMemoryVoiceStream.openOgg(data, "emergency rescue TLM voice " + fileName);
    }
}
