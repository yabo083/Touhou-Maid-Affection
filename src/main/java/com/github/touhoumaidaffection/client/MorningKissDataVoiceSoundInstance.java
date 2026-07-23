package com.github.touhoumaidaffection.client;

import com.github.tartaricacid.touhoulittlemaid.client.sound.OggReader;
import com.github.touhoumaidaffection.TouhouMaidAffection;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;

import java.util.concurrent.CompletableFuture;

public final class MorningKissDataVoiceSoundInstance extends TrackedEntityVoiceSoundInstance {
    private final byte[] data;
    private final OggReader.Type oggType;
    private final boolean mp3;
    private final String fileName;

    public MorningKissDataVoiceSoundInstance(SoundEvent soundEvent, byte[] data, OggReader.Type oggType,
                                             boolean mp3, String fileName, Entity trackedEntity,
                                             double x, double y, double z,
                                             float volume, float pitch) {
        super(soundEvent, SoundSource.PLAYERS, trackedEntity, x, y, z, volume, pitch);
        this.data = data;
        this.oggType = oggType;
        this.mp3 = mp3;
        this.fileName = fileName;
    }

    @Override
    public CompletableFuture<AudioStream> getStream(SoundBufferLibrary library, Sound sound, boolean looping) {
        TouhouMaidAffection.LOGGER.info(
                "Morning kiss data-pack voice getStream invoked: file='{}', oggType={}, bytes={}, looping={}",
                fileName,
                mp3 ? "MP3" : oggType,
                data == null ? -1 : data.length,
                looping
        );
        return InMemoryVoiceStream.open(data, oggType, mp3, "morning kiss data-pack voice " + fileName);
    }
}
