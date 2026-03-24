package com.github.touhoumaidaffection.client;

import com.github.tartaricacid.touhoulittlemaid.client.sound.OggReader;
import com.github.tartaricacid.touhoulittlemaid.client.sound.data.OpusAudioStream;
import com.github.touhoumaidaffection.TouhouMaidAffection;
import net.minecraft.Util;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.JOrbisAudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

import java.io.ByteArrayInputStream;
import java.util.concurrent.CompletableFuture;

public final class EmergencyRescueStreamSoundInstance extends AbstractSoundInstance {
    private final byte[] data;
    private final OggReader.Type oggType;

    public EmergencyRescueStreamSoundInstance(SoundEvent soundEvent, byte[] data, OggReader.Type oggType,
                                              float volume, float pitch) {
        super(soundEvent, SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
        this.data = data;
        this.oggType = oggType;
        this.volume = volume;
        this.pitch = pitch;
        // Keep rescue audio fully player-centric.
        this.relative = true;
        this.attenuation = Attenuation.NONE;
        this.looping = false;
        this.delay = 0;
    }

    @Override
    public boolean canPlaySound() {
        return true;
    }

    @Override
    public CompletableFuture<AudioStream> getStream(SoundBufferLibrary library, Sound sound, boolean looping) {
        TouhouMaidAffection.LOGGER.info(
                "Emergency rescue custom sound getStream invoked: oggType={}, bytes={}, looping={}",
                oggType,
                data == null ? -1 : data.length,
                looping
        );
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (oggType == OggReader.Type.OPUS) {
                    TouhouMaidAffection.LOGGER.info("Emergency rescue custom sound stream decoder selected: OPUS");
                    return new OpusAudioStream(data);
                }
                if (oggType == OggReader.Type.VORBIS) {
                    TouhouMaidAffection.LOGGER.info("Emergency rescue custom sound stream decoder selected: VORBIS");
                    return new JOrbisAudioStream(new ByteArrayInputStream(data));
                }
                TouhouMaidAffection.LOGGER.warn("Emergency rescue custom sound stream decoder skipped: unsupported type {}", oggType);
            } catch (Exception ex) {
                TouhouMaidAffection.LOGGER.warn("Failed to decode emergency rescue stream sound.", ex);
            }
            return null;
        }, Util.backgroundExecutor());
    }
}
